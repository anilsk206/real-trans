package com.test.realtrans.utils;


import com.test.realtrans.auth.AuthHelper;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.services.kinesisvideo.*;
import com.amazonaws.services.kinesisvideo.model.AckEvent;
import com.amazonaws.services.kinesisvideo.model.FragmentTimecodeType;
import com.amazonaws.services.kinesisvideo.model.GetDataEndpointRequest;
import com.amazonaws.services.kinesisvideo.model.PutMediaRequest;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

@Component
@Getter
@Setter
public class AWSUtils {


    private static final String DEFAULT_REGION = "us-east-1";
    private static final String PUT_MEDIA_API = "/putMedia";

    /* the name of the stream */
    private static final String STREAM_NAME = "ess-aicoeinc-video-service";

    /* sample MKV file */
//    @Value("${MKV_FILE_PATH}")
    private String MKV_FILE_PATH;

    //private static final String FRAME_DIR = "src/main/resources/data/audio-video-frames";

    /* max upload bandwidth */
    private static final long MAX_BANDWIDTH_KBPS = 15 * 1024L;

    /* response read timeout */
    private static final int READ_TIMEOUT_IN_MILLIS = 1_000_000;

    /* connect timeout */
    private static final int CONNECTION_TIMEOUT_IN_MILLIS = 10_000;

//    @Autowired
    private RestTemplate restTemplate = new RestTemplate();

    public ResponseEntity<String> get(HttpHeaders headers, String url) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    public ResponseEntity<String> post(String body, HttpHeaders headers, String url) {
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, entity, String.class);
    }

    private static ClientConfiguration config = null;

    private static ClientConfiguration createClientProxy() {
        if (config != null)
            return config;
        config = new ClientConfiguration();
        config.setProxyHost("myproxy.com");
        config.setProxyPort(Integer.parseInt("8080"));
        return config;
    }


    public  void sendStremsToKinesis() throws Exception{

        AmazonKinesisVideoAsyncClientBuilder amazonKinesisVideoAsyncClientBuilder = AmazonKinesisVideoAsyncClientBuilder.standard()
                .withRegion(DEFAULT_REGION)
                .withCredentials(new SystemPropertiesCredentialsProvider());

        //amazonKinesisVideoAsyncClientBuilder.withClientConfiguration(createClientProxy());
        final AmazonKinesisVideo frontendClient = amazonKinesisVideoAsyncClientBuilder.build();
        /*final AmazonKinesisVideo frontendClient = AmazonKinesisVideoAsyncClient.builder()
                .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider())
                .withRegion(DEFAULT_REGION)
                .withClientConfiguration(PredefinedClientConfigurations.defaultConfig()
                        .withProxyHost(proxyHost)
                        .withProxyPort(proxyPort)
                        .withNonProxyHosts("no proxy hosts"))
                .build();*/
        //String path = env.getProperty("mkv_file_path");
        System.out.println("mkv path......................:  "+MKV_FILE_PATH);
        System.out.println("testing aws access");
        Thread.sleep(10 * 1000L);
        System.out.println("testing aws access");

        //System.out.println("testing aws access ........................"+frontendClient.listStreams(new ListStreamsRequest().withMaxResults(2)));
        final String dataEndpoint = frontendClient.getDataEndpoint(
                new GetDataEndpointRequest()
                        .withStreamName(STREAM_NAME)
                        .withAPIName("PUT_MEDIA")).getDataEndpoint();
        System.out.println("dataEndpoint............."+dataEndpoint);
        while (true) {
            /* actually URI to send PutMedia request */
            final URI uri = URI.create(dataEndpoint + PUT_MEDIA_API);

            /* input stream for sample MKV file */
            ClassLoader classLoader = getClass().getClassLoader();
            final InputStream inputStream = classLoader.getResourceAsStream(MKV_FILE_PATH);

            // the stream holding the file content
            if (inputStream == null) {
                throw new IllegalArgumentException("file not found! " + MKV_FILE_PATH);
            } else {
                System.out.println("Successfully got the input stream from " + MKV_FILE_PATH);
            }


            //PushbackInputStream pushbackInputStr = new PushbackInputStream(inputStream);

            /* use a latch for main thread to wait for response to complete */
            final CountDownLatch latch = new CountDownLatch(1);

            /* PutMedia client */
            final AmazonKinesisVideoPutMedia dataClient = AmazonKinesisVideoPutMediaClient.builder()
                    .withRegion(DEFAULT_REGION)
                    .withEndpoint(URI.create(dataEndpoint))
                    .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider())
                    .withConnectionTimeoutInMillis(CONNECTION_TIMEOUT_IN_MILLIS)
                    .build();

           /* AmazonKinesisVideoMediaClientBuilder amazonKinesisVideoMediaClientBuilder = AmazonKinesisVideoMediaClientBuilder.standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(dataEndpoint, DEFAULT_REGION))
                    .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider());
            AmazonKinesisVideoMedia amazonKinesisVideoMedia = amazonKinesisVideoMediaClientBuilder.build();*/

            /*GetMediaResult getMediaResult = amazonKinesisVideoMedia.getMedia(new GetMediaRequest()
            .withStreamARN(streamArn)
            .withStartSelector(startSelector));*/
            final PutMediaAckResponseHandler responseHandler = new PutMediaAckResponseHandler() {
                @Override
                public void onAckEvent(AckEvent event) {
                    System.out.println("onAckEvent " + event);
                }

                @Override
                public void onFailure(Throwable t) {
                    latch.countDown();
                    System.out.println("onFailure: " + t.getMessage());
                    // TODO: Add your failure handling logic here
                }

                @Override
                public void onComplete() {
                    System.out.println("onComplete");
                    latch.countDown();
                }
            };
            /* start streaming video in a background thread */
            dataClient.putMedia(new PutMediaRequest()
                            .withStreamName(STREAM_NAME)
                            .withFragmentTimecodeType(FragmentTimecodeType.RELATIVE)
                            .withPayload(inputStream)
                            .withProducerStartTimestamp(Date.from(Instant.now())),
                    responseHandler);

            /* wait for request/response to complete */
            latch.await();

            /* close the client */
            dataClient.close();
        }

    }

    /*public static void sendMediaStremsToKinesis() throws Exception{
        try {
            System.out.println("started client creation streaming ......");
            // create Kinesis Video high level client
            final KinesisVideoClient kinesisVideoClient = KinesisVideoJavaClientFactory
                    .createKinesisVideoClient(
                            Regions.US_EAST_1,
                            AuthHelper.getSystemPropertiesCredentialsProvider());

            // create a media source. this class produces the data and pushes it into
            // Kinesis Video Producer lower level components
            //final MediaSource mediaSource = createImageFileMediaSource();

            // Audio/Video sample is available for playback on HLS (Http Live Streaming)
            final MediaSource mediaSource = createFileMediaSource();

            // register media source with Kinesis Video Client
            kinesisVideoClient.registerMediaSource(mediaSource);

            // start streaming
            mediaSource.start();
        } catch (final KinesisVideoException e) {
            throw new RuntimeException(e);
        }
    }

    private static MediaSource createFileMediaSource() {
        final AudioVideoFileMediaSourceConfiguration configuration =
                new AudioVideoFileMediaSourceConfiguration.AudioVideoBuilder()
                        .withDir(FRAME_DIR)
                        .withRetentionPeriodInHours(RETENTION_ONE_HOUR)
                        .withAbsoluteTimecode(ABSOLUTE_TIMECODES)
                        .withTrackInfoList(DemoTrackInfos.createTrackInfoList())
                        .build();
        final AudioVideoFileMediaSource mediaSource = new AudioVideoFileMediaSource(STREAM_NAME);
        mediaSource.configure(configuration);

        return mediaSource;
    }*/
}
