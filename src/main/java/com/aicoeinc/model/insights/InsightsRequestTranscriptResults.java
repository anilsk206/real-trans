package com.aicoeinc.model.insights;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder(alphabetic=true)
public class InsightsRequestTranscriptResults {
    private List<InsightsRequestTranscriptResultsTranscripts> transcripts;
    private InsightsRequestTranscriptResultsChannelLabels channel_labels;
    private List<TranscriptionItem> items;
}