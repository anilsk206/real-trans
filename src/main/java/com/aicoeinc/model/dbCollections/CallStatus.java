package com.aicoeinc.model.dbCollections;

import lombok.*;
import org.hibernate.validator.constraints.UniqueElements;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Document(collection = "call_status")
public class CallStatus {
    @Id
    private String id;

    @Indexed
    private String callId;
    private String callStatus;
    private String startTime;
    private String endTime;
    @Indexed
    private Date rowCreateTs;
}
