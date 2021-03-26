package com.aicoeinc.model.streamingevent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder(alphabetic = true)
public class StreamingStatusEndedOrFailedDetail implements StreamingStatusDetail {

    private static final String currentVersion = "0";

    @NonNull
    private String voiceConnectorId;
    @NonNull
    private String transactionId;
    @NonNull
    private String callId;
    @NonNull
    private Direction direction;
    @NonNull
    private String startTime;
    @NonNull
    private String endTime;
    @NonNull
    private String startFragmentNumber;
    @NonNull
    private String streamArn;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> inviteHeaders;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String siprecMetadata;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String fromNumber;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String toNumber;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean isCaller;
    @NonNull
    private String streamingStatus;

    @Override
    public StreamingStatus getStreamingStatus() {
        return this.streamingStatus.equals("ENDED") ? StreamingStatus.ENDED : StreamingStatus.FAILED;
    }

    @Override
    public String getVersion() {
        return currentVersion;
    }
}

