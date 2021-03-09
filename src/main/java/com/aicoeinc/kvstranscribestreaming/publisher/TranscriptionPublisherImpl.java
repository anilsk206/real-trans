package com.aicoeinc.kvstranscribestreaming.publisher;

import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

import java.util.List;

public class TranscriptionPublisherImpl implements TranscriptionPublisher {
    private String finalTranscript = "";
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
                    // String realTimeInsight = getRealTimeInsights(finalTranscript);

                    // TODO: Send back the FLUX event to the client.
                    // Call a method in RealTimeAssistUtils and pass the insight. That method should send the FLUX
                    // event back to client
                }
            }
        }
    }

    @Override
    public void publishDone() {
        System.out.println("Transcription Ended");

    }
}
