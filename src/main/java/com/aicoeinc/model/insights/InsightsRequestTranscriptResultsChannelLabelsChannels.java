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
public class InsightsRequestTranscriptResultsChannelLabelsChannels {
    private String channel_label;
    private List<TranscriptionItem> items;
}