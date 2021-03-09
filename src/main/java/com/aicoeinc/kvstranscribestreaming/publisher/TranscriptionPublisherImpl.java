package com.aicoeinc.kvstranscribestreaming.publisher;

import com.aicoeinc.kvstranscribestreaming.utils.RealTimeAssistUtils;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

import java.io.StringWriter;
import java.util.List;

public class TranscriptionPublisherImpl implements TranscriptionPublisher {
    private String finalTranscript = "";
    String insights = "";
    @Override
    public void publish(TranscriptEvent e) {
        List<Result> results = e.transcript().results();
        System.out.println("transcribe results size ........"+results.size());
        if(results.size()>0) {
            Result firstResult = results.get(0);
            if (firstResult.alternatives().size() > 0 &&
                    !firstResult.alternatives().get(0).transcript().isEmpty()) {
                String transcript = firstResult.alternatives().get(0).transcript();
                if(!transcript.isEmpty() && !firstResult.isPartial()) {
                    System.out.println("Transcribe output "+transcript);
                    finalTranscript += transcript;
                    // TODO: getRealTimeInsights function makes a POST call to the ASSIST service to get the insights
                    getRealTimeInsights(finalTranscript);

                    // TODO: Send back the FLUX event to the client.
                    // Call a method in RealTimeAssistUtils and pass the insight. That method should send the FLUX
                    // event back to client
                }
            }
        }
    }

    private void getRealTimeInsights(String finalTranscript) {
        final ObjectMapper objectMapper = new ObjectMapper();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        JsonFactory jsonFactory = new JsonFactory();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator jsonGenerator = null;
        String url = "http://localhost:8080/mockAnalysis";
        try{
            jsonGenerator = jsonFactory.createGenerator(stringWriter);
            jsonGenerator.useDefaultPrettyPrinter();
            jsonGenerator.writeStartObject();
            jsonGenerator.writeFieldName("text");
            jsonGenerator.writeString(finalTranscript);
            jsonGenerator.writeEndObject();
            ResponseEntity<String> insightsJson = RealTimeAssistUtils.post(stringWriter.toString(),httpHeaders,url);
            RealTimeAssistUtils.insights.append(insightsJson.getBody());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void publishDone() {
        System.out.println("Transcription Ended");

    }
}
