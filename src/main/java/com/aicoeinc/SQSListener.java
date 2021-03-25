package com.aicoeinc;

import com.aicoeinc.kvstranscribestreaming.handler.KVSTranscribeStreamingHandler;
import com.jashmore.sqs.argument.payload.Payload;
import com.jashmore.sqs.processor.argument.Acknowledge;
import com.jashmore.sqs.spring.container.basic.QueueListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SQSListener {
    @Autowired
    KVSTranscribeStreamingHandler handler;

    @QueueListener("${cloud.aws.sqs.endpointUri}")
    public void processMessage(@Payload final String payload, final Acknowledge acknowledge) {
        System.out.println("Pay load is " + payload);
        acknowledge.acknowledgeSuccessful();
        handler.handleRequest(payload);
    }
}