package com.aicoeinc.model.transcript;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Document(collection = "transcripts")
public class TranscriptResult  {
    @Id
    private String id;

    @Indexed
    private String callId;

    @Indexed
    private String channelId;

    @Indexed
    private long startTime;

    private String transactionId;
    private Boolean isCaller;
    private String voiceConnector;
    private String direction;
    private String fromNumber;
    private String toNumber;
    private String transcript;

    @Indexed
    private Date row_create_ts;
}
