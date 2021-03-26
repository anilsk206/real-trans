package com.aicoeinc.kvstranscribestreaming.publisher;

import com.aicoeinc.db.TranscriptsRepository;
import com.aicoeinc.model.streamingevent.StreamingStatusDetail;
import com.aicoeinc.model.dbCollections.TranscriptResult;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.Validate;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.Transcript;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

import java.util.Date;
import java.util.List;

public class TranscriptionPublisherImpl implements TranscriptionPublisher {
    private final ObjectMapper mapper;
    private final TranscriptsRepository transcriptsCollection;
    private final String transactionId;
    private final String callId;
    private final Boolean isCaller;
    private final String voiceConnector;
    private final String direction;
    private final String fromNumber;
    private final String toNumber;
    private final String channelId;

    public TranscriptionPublisherImpl(StreamingStatusDetail streamingStatusStartedDetail, TranscriptsRepository dbClient) {
        this.transcriptsCollection = Validate.notNull(dbClient);
        this.transactionId = Validate.notNull(streamingStatusStartedDetail.getTransactionId());
        this.callId = streamingStatusStartedDetail.getCallId();
        this.isCaller = streamingStatusStartedDetail.getIsCaller();
        this.voiceConnector = streamingStatusStartedDetail.getVoiceConnectorId();
        this.direction = streamingStatusStartedDetail.getDirection().name();
        this.fromNumber = streamingStatusStartedDetail.getFromNumber();
        this.toNumber = streamingStatusStartedDetail.getToNumber();

        // TODO: Set the channel Id in the transcript result appropriately.
        // In this PoC setup, always customer makes the call. So setting it accordingly!
        this.channelId = isCaller == true ? "agent" : "customer";

        mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public void publish(TranscriptEvent e) {
        List<Result> results = e.transcript().results();
        if(results.size()>0) {
            Result firstResult = results.get(0);
            if (firstResult.alternatives().size() > 0 &&
                    !firstResult.alternatives().get(0).transcript().isEmpty()) {
                String transcript = firstResult.alternatives().get(0).transcript();
                if(!transcript.isEmpty() && !firstResult.isPartial()) {
                    try {
                        transcriptsCollection.insert(getTranscriptResult(e.transcript()));
                    } catch (JsonProcessingException jsonProcessingException) {
                        jsonProcessingException.printStackTrace();
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                    System.out.println("Transcribed text: " + transcript);
                }
            }
        }
    }

    @Override
    public void publishDone() {
        System.out.println("Transcription Ended");
    }

    public TranscriptResult getTranscriptResult(Transcript transcript) throws JsonProcessingException {
        String transcriptJson = mapper.writeValueAsString(transcript);
        System.out.println(transcriptJson);

        return TranscriptResult.builder()
                .transactionId(this.transactionId)
                .callId(this.callId)
                .channelId(this.channelId)
                .startTime((long) (transcript.results().get(0).startTime() * 1000))
                .direction(this.direction)
                .fromNumber(this.fromNumber)
                .isCaller(this.isCaller)
                .toNumber(this.toNumber)
                .voiceConnector(this.voiceConnector)
                .transcript(transcriptJson)
                .rowCreateTs(new Date())
                .build();
    }
}
