package com.test.realtrans.transcribing;

import com.amazonaws.kinesisvideo.parser.mkv.StreamingMkvReader;
import com.amazonaws.kinesisvideo.parser.utilities.FragmentMetadataVisitor;
import com.test.realtrans.utils.KinesisUtils;
import org.apache.commons.lang3.Validate;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This Subscription converts audio bytes received from the KVS stream into
 * AudioEvents that can be sent to the Transcribe service. It implements a
 * simple demand system that will read chunks of bytes from a KVS stream using
 * the KVS parser library
 */
public class KVSByteToAudioEventSubscription implements Subscription {

    private static final int CHUNK_SIZE_IN_KB = 4;
    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private AtomicLong demand = new AtomicLong(0);
    private final Subscriber<? super AudioStream> subscriber;
    private final StreamingMkvReader streamingMkvReader;
    private String callId;
    private OutputStream outputStream;
    private final FragmentMetadataVisitor fragmentVisitor;
    private final boolean shouldWriteToOutputStream;

    public KVSByteToAudioEventSubscription(Subscriber<? super AudioStream> s, StreamingMkvReader streamingMkvReader,
                                           String callId, OutputStream outputStream,
                                           FragmentMetadataVisitor fragmentVisitor, boolean shouldWriteToOutputStream) {
        this.subscriber = Validate.notNull(s);
        this.streamingMkvReader = Validate.notNull(streamingMkvReader);
        //this.callId = Validate.notNull(callId);
        this.outputStream = Validate.notNull(outputStream);
        this.fragmentVisitor = Validate.notNull(fragmentVisitor);
        this.shouldWriteToOutputStream = shouldWriteToOutputStream;
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            subscriber.onError(new IllegalArgumentException("Demand must be positive"));
        }

        demand.getAndAdd(n);
        // We need to invoke this in a separate thread because the call to
        // subscriber.onNext(...) is recursive
        executor.submit(() -> {
            try {
                while (demand.get() > 0) {
                    ByteBuffer audioBuffer = KinesisUtils.getByteBufferFromStream(streamingMkvReader, fragmentVisitor, CHUNK_SIZE_IN_KB);

                    if (audioBuffer.remaining() > 0) {

                        AudioEvent audioEvent = audioEventFromBuffer(audioBuffer);
                        subscriber.onNext(audioEvent);

                        if (shouldWriteToOutputStream) {
                            // Write audioBytes to a temporary file as they are received from the stream
                            byte[] audioBytes = new byte[audioBuffer.remaining()];
                            audioBuffer.get(audioBytes);
                            outputStream.write(audioBytes);
                        }

                    } else {
                        subscriber.onComplete();
                        break;
                    }
                    demand.getAndDecrement();
                }
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }

    @Override
    public void cancel() {
        executor.shutdown();
    }

    private AudioEvent audioEventFromBuffer(ByteBuffer bb) {
        return AudioEvent.builder().audioChunk(SdkBytes.fromByteBuffer(bb)).build();
    }
}
