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

public class InsightsRequest {
    private String enrichmentContentId;
    private InsightsRequestTranscript transcript;
}