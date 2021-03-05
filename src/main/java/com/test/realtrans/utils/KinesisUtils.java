package com.test.realtrans.utils;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.kinesisvideo.parser.ebml.MkvTypeInfos;
import com.amazonaws.kinesisvideo.parser.mkv.*;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesisvideo.*;
import com.amazonaws.services.kinesisvideo.model.*;
import com.test.realtrans.auth.AuthHelper;
import com.test.realtrans.streaming.KVSTransactionIdTagProcessor;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility class to interact with KVS streams
 */
public final class KinesisUtils {

    private static final Logger logger = LoggerFactory.getLogger(KinesisUtils.class);

    /**
     * Fetches the next ByteBuffer of size 1024 bytes from the KVS stream by parsing the frame from the MkvElement
     * Each frame has a ByteBuffer having size 1024
     *
     * @param streamingMkvReader
     * @param fragmentVisitor
     * @param tagProcessor
     * @return
     * @throws MkvElementVisitException
     */
    public static ByteBuffer getByteBufferFromStream(StreamingMkvReader streamingMkvReader,
                                                     FragmentMetadataVisitor fragmentVisitor) throws MkvElementVisitException {

        //if (!tagProcessor.shouldStopProcessing()) {
            while (streamingMkvReader.mightHaveNext()) {
                Optional<MkvElement> mkvElementOptional = streamingMkvReader.nextIfAvailable();
                if (mkvElementOptional.isPresent()) {

                    MkvElement mkvElement = mkvElementOptional.get();
                    mkvElement.accept(fragmentVisitor);

                    if (MkvTypeInfos.SIMPLEBLOCK.equals(mkvElement.getElementMetaData().getTypeInfo())) {
                        MkvDataElement dataElement = (MkvDataElement) mkvElement;
                        Frame frame = ((MkvValue<Frame>) dataElement.getValueCopy()).getVal();
                        ByteBuffer audioBuffer = frame.getFrameData();
                        return audioBuffer;
                    }
                }
            }
        //}

        return ByteBuffer.allocate(0);
    }

    /**
     * Fetches ByteBuffer of provided size from the KVS stream by repeatedly calling KVS
     * and concatenating the ByteBuffers to create a single chunk
     *
     * @param streamingMkvReader
     * @param fragmentVisitor
     * @param tagProcessor
     * @param chunkSizeInKB
     * @return
     * @throws MkvElementVisitException
     */
    public static ByteBuffer getByteBufferFromStream(StreamingMkvReader streamingMkvReader,
                                                     FragmentMetadataVisitor fragmentVisitor,
                                                     int chunkSizeInKB) throws MkvElementVisitException {

        List<ByteBuffer> byteBufferList = new ArrayList<ByteBuffer>();

        for (int i = 0; i < chunkSizeInKB; i++) {
            ByteBuffer byteBuffer = KinesisUtils.getByteBufferFromStream(streamingMkvReader, fragmentVisitor);
            if (byteBuffer.remaining() > 0) {
                byteBufferList.add(byteBuffer);
            } else {
                break;
            }
        }

        int length = 0;

        for (ByteBuffer bb : byteBufferList) {
            length += bb.remaining();
        }

        if (length == 0) {
            return ByteBuffer.allocate(0);
        }

        ByteBuffer combinedByteBuffer = ByteBuffer.allocate(length);

        for (ByteBuffer bb : byteBufferList) {
            combinedByteBuffer.put(bb);
        }

        combinedByteBuffer.flip();
        return combinedByteBuffer;
    }

    /**
     * Makes a GetMedia call to KVS and retrieves the InputStream corresponding to the given streamName and startFragmentNum
     *
     * @param streamArn
     * @param region
     * @param startFragmentNum
     * @return
     */
    public static InputStream getInputStreamFromKVS(String streamArn,
                                                    Regions region,
                                                    String startFragmentNum) {
        System.out.println("getInputStreamFromKVS - Validating arn, region and fragment #");

        Validate.notNull(streamArn);
        Validate.notNull(region);
//        Validate.notNull(startFragmentNum);

        System.out.println("getInputStreamFromKVS - Validated successfully arn, region and fragment #");
//        AmazonKinesisVideo amazonKinesisVideo = (AmazonKinesisVideo) AmazonKinesisVideoClientBuilder.standard().build();
//
//        String endPoint = amazonKinesisVideo.getDataEndpoint(new GetDataEndpointRequest()
//                .withAPIName(APIName.GET_MEDIA)
//                .withStreamARN(streamArn)).getDataEndpoint();

//        AmazonKinesisVideoMediaClientBuilder amazonKinesisVideoMediaClientBuilder = AmazonKinesisVideoMediaClientBuilder.standard()
//                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, region.getName()))
//                .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider());
//        AmazonKinesisVideoMedia amazonKinesisVideoMedia = amazonKinesisVideoMediaClientBuilder.build();

        AmazonKinesisVideoAsyncClientBuilder amazonKinesisVideoAsyncClientBuilder = AmazonKinesisVideoAsyncClientBuilder.standard()
                .withRegion(region.getName())
                .withCredentials(new EnvironmentVariableCredentialsProvider());
        final AmazonKinesisVideo frontendClient = amazonKinesisVideoAsyncClientBuilder.build();
        final String endPoint = frontendClient.getDataEndpoint(
                new GetDataEndpointRequest()
                        .withStreamName(streamArn)
                        .withAPIName(APIName.GET_MEDIA)).getDataEndpoint();

        System.out.println("End point is : " + endPoint);

        AmazonKinesisVideoMediaClientBuilder amazonKinesisVideoMediaClientBuilder = AmazonKinesisVideoMediaClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, region.getName()))
                .withCredentials(new EnvironmentVariableCredentialsProvider());

        AmazonKinesisVideoMedia amazonKinesisVideoMedia = amazonKinesisVideoMediaClientBuilder.build();

//        final AmazonKinesisVideoPutMedia dataClient = AmazonKinesisVideoPutMediaClient.builder()
//                .withRegion(region.getName())
//                .withEndpoint(URI.create(endPoint))
//                .withCredentials(AuthHelper.getSystemPropertiesCredentialsProvider())
//                .withConnectionTimeoutInMillis(CONNECTION_TIMEOUT_IN_MILLIS)
//                .build();

        StartSelector startSelector;
        if (startFragmentNum != null)
        {
            startSelector = new StartSelector()
                .withStartSelectorType(StartSelectorType.FRAGMENT_NUMBER)
                .withAfterFragmentNumber(startFragmentNum);
        } else {
            startSelector = new StartSelector().withStartSelectorType(StartSelectorType.EARLIEST);
        }


        GetMediaResult getMediaResult = amazonKinesisVideoMedia.getMedia(new GetMediaRequest()
                .withStreamName(streamArn)
                .withStartSelector(startSelector));

        logger.info("GetMedia called on stream {} response {} requestId {}", streamArn,
                getMediaResult.getSdkHttpMetadata().getHttpStatusCode(),
                getMediaResult.getSdkResponseMetadata().getRequestId());

        return getMediaResult.getPayload();
    }
}
