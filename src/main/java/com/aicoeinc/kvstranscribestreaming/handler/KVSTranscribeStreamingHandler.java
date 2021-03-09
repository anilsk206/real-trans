package com.aicoeinc.kvstranscribestreaming.handler;

import com.aicoeinc.kvstranscribestreaming.publisher.TranscriptionPublisher;
import com.aicoeinc.kvstranscribestreaming.publisher.TranscriptionPublisherImpl;
import com.aicoeinc.kvstranscribestreaming.streaming.KVSTransactionIdTagProcessor;
import com.aicoeinc.kvstranscribestreaming.transcribe.KVSByteToAudioEventSubscription;
import com.aicoeinc.kvstranscribestreaming.transcribe.StreamTranscriptionBehaviorImpl;
import com.aicoeinc.kvstranscribestreaming.transcribe.TranscribeStreamingRetryClient;
import com.aicoeinc.kvstranscribestreaming.utils.KVSUtils;
import com.aicoeinc.kvstranscribestreaming.utils.MetricsUtil;
import com.aicoeinc.streamingeventmodel.StreamingStatus;
import com.aicoeinc.streamingeventmodel.StreamingStatusStartedDetail;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.kinesisvideo.parser.ebml.InputStreamParserByteSource;
import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.LanguageCode;
import software.amazon.awssdk.services.transcribestreaming.model.MediaEncoding;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Demonstrate Amazon VoiceConnectors's real-time transcription feature using
 * AWS Kinesis Video Streams and AWS Transcribe. The data flow is :
 *
 * Amazon CloudWatch Events => Amazon SQS => AWS Lambda => AWS Transcribe => AWS
 * DynamoDB & S3
 */

@NoArgsConstructor
public class KVSTranscribeStreamingHandler {

    private static final int CHUNK_SIZE_IN_KB = 4;
    private static final Regions REGION = Regions.fromName(System.getenv("AWS_REGION"));
    private static final String TRANSCRIBE_ENDPOINT = "https://transcribestreaming." + REGION.getName()
            + ".amazonaws.com";
    private static final String RECORDINGS_BUCKET_NAME = System.getenv("RECORDINGS_BUCKET_NAME");
    private static final String RECORDINGS_KEY_PREFIX = "voiceConnectorToKVS_";
    private static final boolean CONSOLE_LOG_TRANSCRIPT_FLAG = true;
    private static final boolean RECORDINGS_PUBLIC_READ_ACL = false;

    // Lambda maximum timeout is 900 seconds(15 minutes). 5-second buffer is for handler's post actions(stream close, audio upload, etc)
    private static final int LAMBDA_RECORDING_TIMEOUT_IN_SECOND = 895;

    private static final Logger logger = LoggerFactory.getLogger(KVSTranscribeStreamingHandler.class);
    public static final MetricsUtil metricsUtil = new MetricsUtil(AmazonCloudWatchClientBuilder.defaultClient());
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private final Boolean shouldWriteAudioToFile = Boolean.TRUE;

//    private static final DynamoDB dynamoDB = new DynamoDB(
//            AmazonDynamoDBClientBuilder.standard().withRegion(REGION.getName()).build());

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public String handleRequest(String eventBody) {
        try {

            Map<String, Object> eventBodyMap = objectMapper.readValue(eventBody, Map.class);
            Map<String, String> eventDetail = (Map) eventBodyMap.get("detail");

            String streamingStatus = eventDetail.get("streamingStatus");
            String transactionId = eventDetail.get("transactionId");
            logger.info("Received STARTED event");

            if (StreamingStatus.STARTED.name().equals(streamingStatus)) {
                final StreamingStatusStartedDetail streamingStatusStartedDetail = objectMapper.convertValue(eventDetail,
                        StreamingStatusStartedDetail.class);

                logger.info("[{}] Streaming status {} , EventDetail: {}", transactionId, streamingStatus, streamingStatusStartedDetail);
                startKVSToTranscribeStreaming(streamingStatusStartedDetail);
            }

            logger.info("Finished processing request");
        } catch (Exception e) {
            logger.error("KVS to Transcribe Streaming failed with: ", e);
            return "{ \"result\": \"Failed\" }";
        }
        return "{ \"result\": \"Success\" }";
    }

    /**
     * Starts streaming between KVS and Transcribe The transcript segments are
     * continuously saved to the Dynamo DB table At end of the streaming session,
     * the raw audio is saved as an s3 object
     *
     * @param detail
     * @throws Exception
     */
    private void startKVSToTranscribeStreaming(StreamingStatusStartedDetail detail) throws Exception {

        final String transactionId = detail.getTransactionId();
        final String callId = detail.getCallId();
        final String streamArn = detail.getStreamArn();
        final String startFragmentNumber = detail.getStartFragmentNumber();
        final String startTime = detail.getStartTime();

        InputStream kvsInputStream = KVSUtils.getInputStreamFromKVS(streamArn, REGION, startFragmentNumber,
                getAWSCredentials());
        StreamingMkvReader streamingMkvReader = StreamingMkvReader
                .createDefault(new InputStreamParserByteSource(kvsInputStream));

        KVSTransactionIdTagProcessor tagProcessor = new KVSTransactionIdTagProcessor(transactionId);
        FragmentMetadataVisitor fragmentVisitor = FragmentMetadataVisitor.create(Optional.of(tagProcessor));

            try (TranscribeStreamingRetryClient client = new TranscribeStreamingRetryClient(getTranscribeCredentials(),
                    TRANSCRIBE_ENDPOINT, REGION, metricsUtil)) {

                logger.info("Calling Transcribe service..");

                List<TranscriptionPublisher> publishers = Arrays.asList(new TranscriptionPublisherImpl());

                CompletableFuture<Void> result = client.startStreamTranscription(
                        // since we're definitely working with telephony audio, we know that's 8 kHz
                        getRequest(8000),
                        new KVSAudioStreamPublisher(streamingMkvReader, transactionId, tagProcessor, fragmentVisitor),
                        new StreamTranscriptionBehaviorImpl(publishers));

                result.get();
            } catch (Exception e) {
                logger.error("Error during streaming: ", e);
                throw e;

            } finally {
                kvsInputStream.close();
            }
    }

    /**
     * Closes the FileOutputStream and uploads the Raw audio file to S3
     *
     * @param kvsInputStream
     * @param fileOutputStream
     * @param saveAudioFilePath
     * @param transactionId
     * @throws IOException
     */
    /*
    private void closeFileAndUploadRawAudio(InputStream kvsInputStream, FileOutputStream fileOutputStream,
            Path saveAudioFilePath, String transactionId, String startTime) throws IOException {

        kvsInputStream.close();
        fileOutputStream.close();

        // Upload the Raw Audio file to S3
        if (new File(saveAudioFilePath.toString()).length() > 0) {
            AudioUtils.uploadRawAudio(REGION, RECORDINGS_BUCKET_NAME, RECORDINGS_KEY_PREFIX,
                    saveAudioFilePath.toString(), transactionId, startTime, RECORDINGS_PUBLIC_READ_ACL, getAWSCredentials());
        } else {
            logger.info("Skipping upload to S3. Audio file has 0 bytes: " + saveAudioFilePath);
        }
    }
    */


    /**
     * @return AWS credentials to be used to connect to s3 (for fetching and
     *         uploading audio) and KVS
     */
    private static AWSCredentialsProvider getAWSCredentials() {
        return DefaultAWSCredentialsProviderChain.getInstance();
    }

    /**
     * @return AWS credentials to be used to connect to Transcribe service. This
     *         example uses the default credentials provider, which looks for
     *         environment variables (AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY)
     *         or a credentials file on the system running this program.
     */
    private static AwsCredentialsProvider getTranscribeCredentials() {
        return DefaultCredentialsProvider.create();
    }

    /**
     * Build StartStreamTranscriptionRequestObject containing required parameters to
     * open a streaming transcription request, such as audio sample rate and
     * language spoken in audio
     *
     * @param mediaSampleRateHertz sample rate of the audio to be streamed to the
     *                             service in Hertz
     * @return StartStreamTranscriptionRequest to be used to open a stream to
     *         transcription service
     */
    private static StartStreamTranscriptionRequest getRequest(Integer mediaSampleRateHertz) {
        return StartStreamTranscriptionRequest.builder().languageCode(LanguageCode.EN_US.toString())
                .mediaEncoding(MediaEncoding.PCM).mediaSampleRateHertz(mediaSampleRateHertz).build();
    }

    /**
     * KVSAudioStreamPublisher implements audio stream publisher. It emits audio
     * events from a KVS stream asynchronously in a separate thread
     */
    private static class KVSAudioStreamPublisher implements Publisher<AudioStream> {
        private final StreamingMkvReader streamingMkvReader;
        private String callId;
        private KVSTransactionIdTagProcessor tagProcessor;
        private FragmentMetadataVisitor fragmentVisitor;

        private KVSAudioStreamPublisher(StreamingMkvReader streamingMkvReader, String callId,
                                        KVSTransactionIdTagProcessor tagProcessor,
                                        FragmentMetadataVisitor fragmentVisitor) {
            this.streamingMkvReader = streamingMkvReader;
            this.callId = callId;
            this.tagProcessor = tagProcessor;
            this.fragmentVisitor = fragmentVisitor;
        }

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {
            s.onSubscribe(new KVSByteToAudioEventSubscription(s, streamingMkvReader, callId, tagProcessor,
                    fragmentVisitor));
        }
    }

}
