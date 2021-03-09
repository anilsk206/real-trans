package com.aicoeinc.kvstranscribestreaming.transcribe;

import com.aicoeinc.kvstranscribestreaming.utils.MetricsUtil;
import com.amazonaws.regions.Regions;
import org.apache.commons.lang3.Validate;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.EventStreamAws4Signer;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;
import software.amazon.awssdk.services.transcribestreaming.model.BadRequestException;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionRequest;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Build a client wrapper around the Amazon Transcribe client to retry
 * on an exception that can be retried.
 */
public class TranscribeStreamingRetryClient implements AutoCloseable {

    private static final int DEFAULT_MAX_RETRIES = 10;
    private static final int DEFAULT_MAX_SLEEP_TIME_MILLS = 100;
    private static final Logger log = LoggerFactory.getLogger(TranscribeStreamingRetryClient.class);

    private final TranscribeStreamingAsyncClient client;
    private final MetricsUtil metricsUtil;

    List<Class<?>> nonRetriableExceptions = Arrays.asList(BadRequestException.class);
    private int maxRetries = DEFAULT_MAX_RETRIES;
    private int sleepTime = DEFAULT_MAX_SLEEP_TIME_MILLS;

    /**
     * Create a TranscribeStreamingRetryClient with given credential and configuration
     *
     * @param creds       Creds to use for transcription
     * @param endpoint    Endpoint to use for transcription
     * @param region      Region to use for transcriptions
     * @param metricsUtil
     * @throws URISyntaxException if the endpoint is not a URI
     */
    public TranscribeStreamingRetryClient(AwsCredentialsProvider creds,
                                          String endpoint, Regions region, MetricsUtil metricsUtil) throws URISyntaxException {
        this(TranscribeStreamingAsyncClient.builder()
                .credentialsProvider(creds)
                .overrideConfiguration(
                        c -> c.putAdvancedOption(SdkAdvancedClientOption.SIGNER, EventStreamAws4Signer.create()))
                .endpointOverride(new URI(endpoint))
                .region(Region.of(region.getName()))
                .build(),
                metricsUtil);
    }

    /**
     * Initiate TranscribeStreamingRetryClient with TranscribeStreamingAsyncClient
     *
     * @param client      TranscribeStreamingAsyncClient
     * @param metricsUtil
     */
    public TranscribeStreamingRetryClient(TranscribeStreamingAsyncClient client, MetricsUtil metricsUtil) {
        this.client = client;
        this.metricsUtil = metricsUtil;
    }

    /**
     * Get Max retries
     *
     * @return Max retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Set Max retries
     *
     * @param maxRetries Max retries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Get sleep time
     *
     * @return sleep time between retries
     */
    public int getSleepTime() {
        return sleepTime;
    }

    /**
     * Set sleep time between retries
     *
     * @param sleepTime sleep time
     */
    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    /**
     * Initiate a Stream Transcription with retry.
     *
     * @param request         StartStreamTranscriptionRequest to use to start transcription
     * @param publisher       The source audio stream as Publisher
     * @param responseHandler StreamTranscriptionBehavior object that defines how the response needs to be handled.
     * @return Completable future to handle stream response.
     */

    public CompletableFuture<Void> startStreamTranscription(final StartStreamTranscriptionRequest request,
                                                            final Publisher<AudioStream> publisher,
                                                            final StreamTranscriptionBehavior responseHandler) {

        Validate.notNull(request);
        Validate.notNull(publisher);
        Validate.notNull(responseHandler);

        CompletableFuture<Void> finalFuture = new CompletableFuture<>();

        recursiveStartStream(rebuildRequestWithSession(request), publisher, responseHandler, finalFuture, 0);

        return finalFuture;
    }

    /**
     * Recursively call startStreamTranscription() to be called till the request is completed or till we run out of retries.
     *
     * @param request         StartStreamTranscriptionRequest
     * @param publisher       The source audio stream as Publisher
     * @param responseHandler StreamTranscriptionBehavior object that defines how the response needs to be handled.
     * @param finalFuture     final future to finish on completing the chained futures.
     * @param retryAttempt    Current attempt number
     */
    private void recursiveStartStream(final StartStreamTranscriptionRequest request,
            final Publisher<AudioStream> publisher,
            final StreamTranscriptionBehavior responseHandler,
            final CompletableFuture<Void> finalFuture,
            final int retryAttempt) {
        CompletableFuture<Void> result = client.startStreamTranscription(request, publisher,
                getResponseHandler(responseHandler));
        result.whenComplete((r, e) -> {
            if (e != null) {
                log.debug("Error occured:", e);

                if (retryAttempt <= maxRetries && isExceptionRetriable(e)) {
                    log.debug("Retriable error occurred and will be retried.");
                    log.debug("Sleeping for sometime before retrying...");
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e1) {
                        log.debug("Unable to sleep. Failed with exception: ", e);
                        e1.printStackTrace();
                    }
                    log.debug("Making retry attempt: " + (retryAttempt + 1));
                    recursiveStartStream(request, publisher, responseHandler, finalFuture, retryAttempt + 1);
                } else {
                    metricsUtil.recordMetric("TranscribeStreamError", 1);
                    log.error("Encountered unretriable exception or ran out of retries. ");
                    responseHandler.onError(e);
                    finalFuture.completeExceptionally(e);
                }
            } else {
                metricsUtil.recordMetric("TranscribeStreamSuccess", 1);
                responseHandler.onComplete();
                finalFuture.complete(null);
            }
        });
    }

    private StartStreamTranscriptionRequest rebuildRequestWithSession(StartStreamTranscriptionRequest request) {
        return StartStreamTranscriptionRequest.builder()
                .languageCode(request.languageCode())
                .mediaEncoding(request.mediaEncoding())
                .mediaSampleRateHertz(request.mediaSampleRateHertz())
                .sessionId(UUID.randomUUID().toString())
                .build();
    }

    /**
     * StartStreamTranscriptionResponseHandler implements subscriber of transcript stream
     * Output is printed to standard output
     */
    private StartStreamTranscriptionResponseHandler getResponseHandler(
            StreamTranscriptionBehavior transcriptionBehavior) {
        final StartStreamTranscriptionResponseHandler build = StartStreamTranscriptionResponseHandler.builder()
                .onResponse(r -> {
                    transcriptionBehavior.onResponse(r);
                })
                .onError(e -> {
                    //Do nothing here. Don't close any streams that shouldn't be cleaned up yet.
                })
                .onComplete(() -> {
                    //Do nothing here. Don't close any streams that shouldn't be cleaned up yet.
                })
                .subscriber(event -> {
                    try {
                        transcriptionBehavior.onStream(event);
                    }
                    // We swallow any exception occurred while processing the TranscriptEvent and continue transcribing
                    // Transcribe errors will however cause the future to complete exceptionally and we'll retry (if applicable)
                    catch (Exception e) {
                        log.error("Error happened when transcribing", e);
                    }
                })
                .build();
        return build;
    }

    /**
     * Check if the exception can be retried.
     *
     * @param e Exception that occurred
     * @return True if the exception is retriable
     */
    private boolean isExceptionRetriable(Throwable e) {
        e.printStackTrace();

        return nonRetriableExceptions.contains(e.getClass());
    }

    public void close() {
        this.client.close();
    }
}
