package com.aicoeinc.model.insights;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder(alphabetic=true)
public class InsightsRequestTranscript {
    private String jobName;
    private String accountId;
    private String[] entities_to_flag;
    private InsightsRequestTranscriptResults results;
}
