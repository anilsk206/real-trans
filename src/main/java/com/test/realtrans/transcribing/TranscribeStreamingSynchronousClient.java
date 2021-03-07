/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.test.realtrans.transcribing;

import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.regions.Regions;
import com.test.realtrans.streaming.KVSTransactionIdTagProcessor;
import com.test.realtrans.utils.AWSUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.realtrans.utils.KinesisUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An example implementation of a simple synchronous wrapper around the async client
 */
public class TranscribeStreamingSynchronousClient {

    public static final int MAX_TIMEOUT_MS = 15 * 60 * 1000; //15 minutes

    private TranscribeStreamingAsyncClient asyncClient;
    private String finalTranscript = "";
    private String suggestions = "";
    private AWSUtils awsUtils;
    private String analysisUrl = "";
    final String streamName = "rtttest-connect-durga-contact-411a8369-693e-4802-8c90-173811c2b039";

    public TranscribeStreamingSynchronousClient(TranscribeStreamingAsyncClient asyncClient,AWSUtils awsUtils) {
        this.asyncClient = asyncClient;
        this.awsUtils = awsUtils;
    }

    public String transcribeFile() {
        try {
            Path saveAudioFilePath = Paths.get("src/main/resources/",
                    "audio"+ ".raw");
            FileOutputStream fileOutputStream = new FileOutputStream(saveAudioFilePath.toString());
            InputStream kvsInputStream = KinesisUtils.getInputStreamFromKVS(streamName, Regions.US_EAST_1, null);
            StreamingMkvReader streamingMkvReader = StreamingMkvReader
                    .createDefault(new InputStreamParserByteSource(kvsInputStream));
            FragmentMetadataVisitor fragmentVisitor = FragmentMetadataVisitor.create();
            //int sampleRate = (int) AudioSystem.getAudioInputStream(audioFile).getFormat().getSampleRate();
            StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                    .languageCode(LanguageCode.EN_US.toString())
                    .mediaEncoding(MediaEncoding.PCM)
                    .enableChannelIdentification(true)
                    .numberOfChannels(2)
                    .mediaSampleRateHertz(8000)
                    .build();
            //AudioStreamPublisher audioStream = new AudioStreamPublisher(new FileInputStream(audioFile));
            StartStreamTranscriptionResponseHandler responseHandler = getResponseHandler();
            System.out.println("launching request");
            CompletableFuture<Void> resultFuture = asyncClient.startStreamTranscription(request, new KVSAudioStreamPublisher(streamingMkvReader, null, fileOutputStream,null,
                    fragmentVisitor, true), responseHandler);
            System.out.println("waiting for response, this will take some time depending on the length of the audio file");
            resultFuture.get(MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS); //block until done
        } catch (IOException e) {
            System.out.println("Error reading audio file" + e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            System.out.println("Error streaming audio to AWS Transcribe service: " + e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.out.println("Stream thread interupted: " + e);
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            System.out.println("Stream not closed within timeout window of " + MAX_TIMEOUT_MS + " ms");
            throw new RuntimeException(e);
        }
        return finalTranscript;
    }

    /**
     * Get a response handler that aggregates the transcripts as they arrive
     * @return Response handler used to handle events from AWS Transcribe service.
     */
    private StartStreamTranscriptionResponseHandler getResponseHandler(){
        return StartStreamTranscriptionResponseHandler.builder()
                .subscriber(event -> {
                    List<Result> results = ((TranscriptEvent) event).transcript().results();
                    System.out.println("transcribe results size ........"+results.size());
                    if(results.size()>0) {
                        Result firstResult = results.get(0);
                        if (firstResult.alternatives().size() > 0 &&
                                !firstResult.alternatives().get(0).transcript().isEmpty()) {
                            String transcript = firstResult.alternatives().get(0).transcript();
                            if(!transcript.isEmpty() && !firstResult.isPartial()) {
                                System.out.println(transcript);
                                finalTranscript += transcript;
//                                sendTranscribeData();
                            }
                        }

                    }
                }).build();
    }
    private static class KVSAudioStreamPublisher implements Publisher<AudioStream> {
        private final StreamingMkvReader streamingMkvReader;
        private String callId;
        private OutputStream outputStream;
        private KVSTransactionIdTagProcessor tagProcessor;
        private FragmentMetadataVisitor fragmentVisitor;
        private boolean shouldWriteToOutputStream;

        private KVSAudioStreamPublisher(StreamingMkvReader streamingMkvReader, String callId, OutputStream outputStream,
                                        KVSTransactionIdTagProcessor tagProcessor, FragmentMetadataVisitor fragmentVisitor,
                                        boolean shouldWriteToOutputStream) {
            this.streamingMkvReader = streamingMkvReader;
            this.callId = callId;
            this.outputStream = outputStream;
            this.tagProcessor = tagProcessor;
            this.fragmentVisitor = fragmentVisitor;
            this.shouldWriteToOutputStream = shouldWriteToOutputStream;
        }

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new KVSByteToAudioEventSubscription(s, streamingMkvReader, callId, outputStream,
                    fragmentVisitor, shouldWriteToOutputStream));
        }
    }

    private void sendTranscribeData() {
        final ObjectMapper mapper = new ObjectMapper();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JsonFactory factory = new JsonFactory();
        StringWriter jsonObjectWriter = new StringWriter();
        JsonGenerator generator = null;
        try {
            generator = factory.createGenerator(jsonObjectWriter);
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            generator.writeFieldName("agent");
            generator.writeString(finalTranscript);
            generator.writeEndObject();
            String resultJson = awsUtils.post(jsonObjectWriter.toString(),
                    headers, analysisUrl).getBody();
            JsonNode tokenNode = mapper.readTree(resultJson);
            suggestions = tokenNode.get("suggestions").asText();
            //generator.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }


}
