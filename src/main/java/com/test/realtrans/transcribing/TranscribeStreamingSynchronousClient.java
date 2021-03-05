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

import com.test.realtrans.utils.AWSUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
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

    public TranscribeStreamingSynchronousClient(TranscribeStreamingAsyncClient asyncClient,AWSUtils awsUtils) {
        this.asyncClient = asyncClient;
        this.awsUtils = awsUtils;
    }

    public String transcribeFile(File audioFile) {
        try {
            int sampleRate = (int) AudioSystem.getAudioInputStream(audioFile).getFormat().getSampleRate();
            StartStreamTranscriptionRequest request = StartStreamTranscriptionRequest.builder()
                    .languageCode(LanguageCode.EN_US.toString())
                    .mediaEncoding(MediaEncoding.PCM)
                    .enableChannelIdentification(true)
                    .numberOfChannels(2)
                    .mediaSampleRateHertz(sampleRate)
                    .build();
            AudioStreamPublisher audioStream = new AudioStreamPublisher(new FileInputStream(audioFile));
            StartStreamTranscriptionResponseHandler responseHandler = getResponseHandler();
            System.out.println("launching request");
            CompletableFuture<Void> resultFuture = asyncClient.startStreamTranscription(request, audioStream, responseHandler);
            System.out.println("waiting for response, this will take some time depending on the length of the audio file");
            resultFuture.get(MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS); //block until done
        } catch (IOException e) {
            System.out.println("Error reading audio file (" + audioFile.getName() + ") : " + e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            System.out.println("Error streaming audio to AWS Transcribe service: " + e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.out.println("Stream thread interupted: " + e);
            throw new RuntimeException(e);
        } catch (UnsupportedAudioFileException e) {
            System.out.println("File type not recognized: " + audioFile.getName() + ", error: " + e);
        } catch (TimeoutException e) {
            System.out.println("Stream not closed within timeout window of " + MAX_TIMEOUT_MS + " ms");
            throw new RuntimeException(e);
        }
        return suggestions;
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
