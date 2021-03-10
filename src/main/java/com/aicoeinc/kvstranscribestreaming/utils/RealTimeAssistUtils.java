package com.aicoeinc.kvstranscribestreaming.utils;


import com.aicoeinc.kvstranscribestreaming.handler.KVSTranscribeStreamingHandler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;

@Component
@Getter
@Setter
public class RealTimeAssistUtils {


    private static final String DEFAULT_REGION = "us-east-1";

    /* max upload bandwidth */
    private static final long MAX_BANDWIDTH_KBPS = 15 * 1024L;

    /* response read timeout */
    private static final int READ_TIMEOUT_IN_MILLIS = 1_000_000;

    /* connect timeout */
    private static final int CONNECTION_TIMEOUT_IN_MILLIS = 10_000;

    public static StringBuffer insights = new StringBuffer();

//    @Autowired
    private static RestTemplate restTemplate = new RestTemplate();

    public static ResponseEntity<String> get(HttpHeaders headers, String url) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    public static ResponseEntity<String> post(String body, HttpHeaders headers, String url) {
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, entity, String.class);
    }

    @Async
    public CompletableFuture<String> transcribe(String id) {
        System.out.println("child thread ...................");
        String ucidInfo = getUCIDInfo(id);
        KVSTranscribeStreamingHandler handler = new KVSTranscribeStreamingHandler();
        String transStatus = handler.handleRequest(ucidInfo);
        return CompletableFuture.completedFuture(transStatus);
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
    }

}
