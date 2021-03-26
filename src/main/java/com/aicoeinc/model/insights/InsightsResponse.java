package com.aicoeinc.model.insights;

import lombok.*;

import java.util.Map;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class InsightsResponse {
    private String callStatus;
    private Map<String, String> insights;
}
