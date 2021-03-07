package com.test.realtrans.transcribing;

import com.test.realtrans.utils.AWSUtils;
import com.test.realtrans.utils.KinesisUtils;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.MkvElementVisitException;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.regions.Regions;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RealTimeTranscriber {

    private TranscribeStreamingClientWrapper client;
    private TranscribeStreamingSynchronousClient synchronousClient;
    private CompletableFuture<Void> inProgressStreamingRequest;
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    final String streamName = "rtttest-connect-durga-contact-411a8369-693e-4802-8c90-173811c2b039";
    private static final int CHUNK_SIZE_IN_KB = 4;
    private AWSUtils awsUtils;

    public RealTimeTranscriber(AWSUtils awsUtils) throws Exception {
        this.awsUtils = awsUtils;
        client = new TranscribeStreamingClientWrapper();
        synchronousClient = new TranscribeStreamingSynchronousClient(TranscribeStreamingClientWrapper.getClient(),awsUtils);
    }


    public String initialize() throws Exception {
        return synchronousClient.transcribeFile();
        //String suggestions = "";

/*
        try{
            while(true)
            {
                ByteBuffer outputBuffer = KinesisUtils.getByteBufferFromStream(streamingMkvReader, fragmentVisitor,
                        CHUNK_SIZE_IN_KB);

                if (outputBuffer.remaining() > 0) {
                    //Write audioBytes to a temporary file as they are received from the stream
                    byte[] audioBytes = new byte[outputBuffer.remaining()];
                    outputBuffer.get(audioBytes);
                    fileOutputStream.write(audioBytes);
                } else {
                    break;
                }
            }
        }finally {
            kvsInputStream.close();
            fileOutputStream.close();
        }
        *//*ClassLoader classLoader = getClass().getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream(MKV_FILE_PATH);*//*
        File inputFile = new File("src/main/resources/audio.raw");*/

    }

    private void stopTranscription() {
        if (inProgressStreamingRequest != null) {
            try {
                client.stopTranscription();
                inProgressStreamingRequest.get();
            } catch (ExecutionException | InterruptedException e) {
                System.out.println("error closing stream");
            } finally {
                inProgressStreamingRequest = null;
            }

        }
    }


}
