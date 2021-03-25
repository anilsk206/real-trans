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
public class TranscriptionItem {
    private String start_time;
    private String end_time;
    private String type;
    private List<TranscriptionItemAlternatives> alternatives;
}