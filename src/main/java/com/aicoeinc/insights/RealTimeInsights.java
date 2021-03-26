package com.aicoeinc.insights;

import com.aicoeinc.db.CallStatusRepository;
import com.aicoeinc.db.TranscriptsRepository;
import com.aicoeinc.model.dbCollections.CallStatus;
import com.aicoeinc.model.insights.*;
import com.aicoeinc.model.dbCollections.TranscriptResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Getter
@Setter
public class RealTimeInsights {

    @Autowired
    TranscriptsRepository transcriptsRepository;

    @Autowired
    CallStatusRepository callStatusRepository;

    private static final String DEFAULT_REGION = "us-east-1";

    /* max upload bandwidth */
    private static final long MAX_BANDWIDTH_KBPS = 15 * 1024L;

    /* response read timeout */
    private static final int READ_TIMEOUT_IN_MILLIS = 1_000_000;

    /* connect timeout */
    private static final int CONNECTION_TIMEOUT_IN_MILLIS = 10_000;

    public static String[] entities = {
            "DISCOUNT",
            "PREMIUM",
            "REP_QUESTION",
            "DRIVEWISE",
            "CLIENT_OBJECTION",
            "ASK_FOR_SALE",
            "CUSTOMER_INTENT",
            "COMPETITOR",
            "REP_SUGGEST_MULTILINE"
    };

    public InsightsResponse insights = InsightsResponse.builder()
            .callStatus("Ongoing")
            .insights(new HashMap<>())
            .build();

    //    @Autowired
    private static RestTemplate restTemplate = new RestTemplate();

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ResponseEntity<String> get(HttpHeaders headers, String url) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    public static ResponseEntity<String> post(String body, HttpHeaders headers, String url) {
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, entity, String.class);
    }

    public List<TranscriptResult> getTranscripts(String callId) {
        List<TranscriptResult> transcripts = transcriptsRepository.findByCallId(callId);
        System.out.println("Retrieved " + transcripts.size() + " records from collection!");
        return transcripts;
    }

    public String getInsightsFor(String callId) {
        List<TranscriptResult> callTranscripts = getTranscripts(callId);

        List<CallStatus> callStatusList = callStatusRepository.findByCallId(callId);
        String callStatus = callStatusList.size() > 0 ? callStatusList.get(0).getCallStatus() : "UNKNOWN";

        this.insights.setCallStatus(callStatus);
        InsightsRequest insightsRequest = getInsightRequest(callId, callTranscripts);

        //TODO: Make a call to Insights engine
        String insightsResponse = getInsights(insightsRequest);

        Map<String, String> insights = extractInsights(insightsResponse);

        // For a completed call, show all insights. Otherwise, stream 1 at a time

        if (!callStatus.equals("STARTED")) {
            this.insights.setInsights(insights);
        } else {
            // For this Mock Service, just get a few insights at a time - so that we can show updates in UI
            int insightsAtATime = 1;

            // While this code can be optimized to get the first element instead of creating subLists,
            // keeping this as below to have quick edits if we want to show more than one insights each time.
            List<String> insightsSubset = new ArrayList<String>(insights.keySet());
            Collections.shuffle(insightsSubset);
            List<String> insightsSubsetKeys = insightsAtATime > insightsSubset.size() ?
                    insightsSubset.subList(0, insightsSubset.size()) :
                    insightsSubset.subList(0, insightsAtATime);
            for (String insightsSubsetKey : insightsSubsetKeys) {
                this.insights.getInsights().put(insightsSubsetKey, insights.get(insightsSubsetKey));
            }
        }

        String insightsJson = "";
        try {
            insightsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this.insights);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return insightsJson;
    }

    // Tactical method to get the insights. Needs to be updated with real/mocked service call
    public String getInsights(InsightsRequest insightsRequest) {
        String insights = "";
        try {
            insights = Files.readAllLines(Paths.get("/home/durga/Downloads/model_output1.json"))
                    .stream()
                    .collect(Collectors.joining());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return insights;
    }

    public Map<String, String> extractInsights(String insightsResponse) {
        Map<String, String> insights = new HashMap<>();

        try {
            JsonNode insightsJson = objectMapper.readTree(insightsResponse);
            ArrayNode udpInsights = (ArrayNode) insightsJson.path("udpInsights");
            for (JsonNode udpInsight : udpInsights) {
                insights.put(udpInsight.get("tagName").asText(), udpInsight.get("tagValue").asText());
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return insights;
    }

    public InsightsRequest getInsightRequest(String callId, List<TranscriptResult> results) {
        final List<TranscriptionItem> agentItems = new ArrayList<>();
        final List<TranscriptionItem> customerItems = new ArrayList<>();
        results.stream().forEach(result -> {
            String rttranscript = result.getTranscript();
            String channelId = result.getChannelId();
            try {
                JsonNode transcriptJson = objectMapper.readTree(rttranscript);
                ArrayNode rtitems = (ArrayNode) transcriptJson.path("results").get(0)
                        .path("alternatives").get(0).path("items");

                for (JsonNode item : rtitems) {
                    TranscriptionItem transcriptionItem = TranscriptionItem.builder()
                            .start_time(item.get("startTime").asLong() / 1000 + "")
                            .end_time(item.get("endTime").asLong() / 1000 + "")
                            .alternatives(Arrays.asList(TranscriptionItemAlternatives.builder()
                                    .confidence("1.0")
                                    .content(item.get("content").asText())
                                    .build()))
                            .type(item.get("type").asText())
                            .build();

                    if (channelId.equals("agent")) {
                        agentItems.add(transcriptionItem);
                    } else {
                        customerItems.add(transcriptionItem);
                    }
                }
            } catch (JsonProcessingException e) {
                System.out.println("Error while transforming transcript with start time " + result.getStartTime()
                        + " for channel id " + result.getChannelId());
                e.printStackTrace();
            }
        });

        // Merge all the agent and customer items
        List<TranscriptionItem> allItems = new ArrayList<>();
        Stream.of(agentItems, customerItems).forEach(allItems::addAll);

        // Sort the items by start_time of the item
        List<TranscriptionItem> allItemsSorted = allItems.stream()
                .sorted(Comparator.comparing(TranscriptionItem::getStart_time))
                .collect(Collectors.toList());

        // Create the transcript by
        String transcript = allItemsSorted.stream()
                .map(item -> (item.getType().equals("pronunciation") ? " " : "") + item.getAlternatives().get(0).getContent())
                .collect(Collectors.joining());

        InsightsRequest insightsRequest = InsightsRequest.builder()
                .enrichmentContentId(callId)
                .transcript(InsightsRequestTranscript.builder()
                        .jobName("RealTimeTranscription-" + callId)
                        .entities_to_flag(RealTimeInsights.entities)
                        .accountId("xxxxxxxxx")
                        .results(InsightsRequestTranscriptResults.builder()
                                .transcripts(Arrays.asList(InsightsRequestTranscriptResultsTranscripts.builder()
                                        .transcript(transcript.trim())
                                        .build()))
                                .channel_labels(InsightsRequestTranscriptResultsChannelLabels.builder()
                                        .channels(Arrays.asList(InsightsRequestTranscriptResultsChannelLabelsChannels.builder()
                                                        .channel_label("ch_0")
                                                        .items(agentItems)
                                                        .build(),
                                                InsightsRequestTranscriptResultsChannelLabelsChannels.builder()
                                                        .channel_label("ch_1")
                                                        .items(customerItems)
                                                        .build()
                                        ))
                                        .number_of_channels(2)
                                        .build())
                                .items(allItemsSorted)
                                .build())
                        .build())
                .build();

        try {
            System.out.println(objectMapper.writeValueAsString(insightsRequest));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return insightsRequest;
    }
}
