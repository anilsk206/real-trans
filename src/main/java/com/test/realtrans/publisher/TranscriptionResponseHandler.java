package com.test.realtrans.publisher;

import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

import java.util.List;

public class TranscriptionResponseHandler implements TranscriptionPublisher {
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
                    System.out.println("transcribe output"+transcript);
                   // finalTranscript += transcript;
//                                sendTranscribeData();
                }
            }

        }

    }

    @Override
    public void publishDone() {
        System.out.println("Transcription Ended");

    }
}
