package com.aicoeinc;

import com.aicoeinc.kvstranscribestreaming.handler.KVSTranscribeStreamingHandler;
import com.aicoeinc.kvstranscribestreaming.utils.RealTimeAssistUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class MainController {
    private final RealTimeAssistUtils realTimeAssistUtils;

    public MainController(RealTimeAssistUtils realTimeAssistUtils) {
        this.realTimeAssistUtils = realTimeAssistUtils;
    }

    public String transID = "";

    @CrossOrigin(allowedHeaders = "*")
    @GetMapping(value = "/event/getInsights/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    /*
    public Flux<String> getResourceUsage(@PathVariable(required = false) String id) throws Exception {

        Random random = new Random();
        RealTimeTranscriber rtt = new RealTimeTranscriber(awsUtils);
        rtt.initialize();
        System.out.println("param id"+id);
        return Flux.interval(Duration.ofSeconds(1))
                .map(it -> TransConstants.RECOMMENDATIONS[random.nextInt(8)]);
    }
    */
    public Flux<String> getRealTimeAssist(@PathVariable(required = false) String id) throws Exception {
        System.out.println("UCID : "+ id);
        transID = id;
        realTimeAssistUtils.transcribe(transID).thenAccept(s -> System.out.println("transcribe returned: " + s));
        System.out.println("main thread ............");
        return Flux.interval(Duration.ofSeconds(5))
                .map(it -> RealTimeAssistUtils.insights.toString());
    }

    /*@Async
    public void transcribe() {
        System.out.println("child thread ...................");
        String ucidInfo = getUCIDInfo("");
        KVSTranscribeStreamingHandler handler = new KVSTranscribeStreamingHandler();
        handler.handleRequest(ucidInfo);
    }

    public String getUCIDInfo(String id) {
        // TODO: get the info from DB. For now just return the hardcoded payload as below

        String payload = "{\n" +
                "    \"version\": \"0\",\n" +
                "    \"id\": \"12345678-1234-1234-1234-111122223333\",\n" +
                "    \"detail-type\": \"Chime VoiceConnector Streaming Status\",\n" +
                "    \"source\": \"aws.chime\",\n" +
                "    \"account\": \"111122223333\",\n" +
                "    \"time\": \"2021-03-08T06:00:08Z\",\n" +
                "    \"region\": \"us-east-1\",\n" +
                "    \"resources\": [],\n" +
                "    \"detail\": {\n" +
                "        \"callId\": \"1112-2222-4333\",\n" +
                "        \"direction\": \"Inbound\",\n" +
                "        \"fromNumber\": \"+12065550100\",\n" +
                "        \"inviteHeaders\": {\n" +
                "            \"from\": \"\\\"John\\\" <sip:+12065550100@10.24.34.0>;tag=abcdefg\",\n" +
                "            \"to\": \"<sip:+13605550199@abcdef1ghij2klmno3pqr4.voiceconnector.chime.aws:5060>\",\n" +
                "            \"call-id\": \"1112-2222-4333\",\n" +
                "            \"cseq\": \"101 INVITE\",\n" +
                "            \"contact\": \"<sip:user@10.24.34.0:6090>;\",\n" +
                "            \"content-type\": \"application/sdp\",\n" +
                "            \"content-length\": \"246\"\n" +
                "        },\n" +
                "        \"isCaller\": true,\n" +
                "        \"mediaType\": \"audio/L16\",\n" +
                "        \"sdp\": {\n" +
                "            \"mediaIndex\": 0,\n" +
                "            \"mediaLabel\": \"1\"\n" +
                "        },\n" +
                "        \"siprecMetadata\": \"<&xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"&>;\\r\\n<recording xmlns='urn:ietf:params:xml:ns:recording:1'>\",\n" +
                "        \"startFragmentNumber\": \"91343852333181432392682062635481650698420408598\",\n" +
                "        \"startTime\": \"2021-03-08T06:00:07Z\",\n" +
                "        \"streamArn\": \"arn:aws:kinesisvideo:us-east-1:015156855452:stream/rtttest-connect-rtttester-contact-3f704994-32ed-4e4a-9e4f-280ac05f6eda/1615179364131\",\n" +
                "        \"toNumber\": \"+13605550199\",\n" +
                "        \"transactionId\": \"12345678-1234-1234\",\n" +
                "        \"voiceConnectorId\": \"abcdef1ghij2klmno3pqr4\",\n" +
                "        \"streamingStatus\": \"STARTED\",\n" +
                "        \"version\": \"0\"\n" +
                "    }\n" +
                "}";
        return payload;
    }*/
}


