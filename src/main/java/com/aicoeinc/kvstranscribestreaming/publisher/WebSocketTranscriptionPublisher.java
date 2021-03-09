package com.aicoeinc.kvstranscribestreaming.publisher;

import com.aicoeinc.streamingeventmodel.StreamingStatusDetail;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApi;
import com.amazonaws.services.apigatewaymanagementapi.AmazonApiGatewayManagementApiClientBuilder;
import com.amazonaws.services.apigatewaymanagementapi.model.GoneException;
import com.amazonaws.services.apigatewaymanagementapi.model.PostToConnectionRequest;
import com.amazonaws.services.apigatewaymanagementapi.model.PostToConnectionResult;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.transcribestreaming.model.Result;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;

//import static com.amazonaws.kvstranscribestreaming.constants.WebSocketMappingDDBConstants.CONNECTION_ID;
//import static com.amazonaws.kvstranscribestreaming.constants.WebSocketMappingDDBConstants.NUMBER;

/**
 * Implemention of publisher to transmit transcription from backend to client through API Gateway web socket.
 *
 * Steps:
 * 1. Get connection id from web socket mapping table to generate endpoint url. Publisher will keep trying to get connection id until it is
 * available in the table.
 * 2. POST transcription from AWS Transcribe to the endpoint.
 */
public class WebSocketTranscriptionPublisher implements TranscriptionPublisher {

//    private static final Logger logger = LoggerFactory.getLogger(WebSocketTranscriptionPublisher.class);
//    private static final String WEBSOCKET_MAPPING_TABLE_NAME = System.getenv("WEBSOCKET_MAPPING_TABLE_NAME");
//    private static final String TRANSCRIBE_API_GATEWAY_APIID = System.getenv("TRANSCRIBE_API_GATEWAY_APIID");
//    private static final String TRANSCRIBE_API_GATEWAY_STAGE = System.getenv("TRANSCRIBE_API_GATEWAY_STAGE");
//    private static final Regions REGION = Regions.fromName(System.getenv("AWS_REGION"));
//    private static final String API_GATEWAY_ENDPOINT = "https://" + TRANSCRIBE_API_GATEWAY_APIID + ".execute-api." + REGION.getName()
//            + ".amazonaws.com/" + TRANSCRIBE_API_GATEWAY_STAGE;
//    private static final String WEB_SOCKET_PUBLISHER_PREFIX = "WebSocketPublisher:";
//
//    //private final DynamoDB dynamoDB;
//    private final AmazonApiGatewayManagementApi apigatewayClient;
//    private final AWSCredentialsProvider credentialsProvider;
//    private final StreamingStatusDetail detail;
//
//    private String connectionId = null;
//
//    public WebSocketTranscriptionPublisher(final DynamoDB dynamoDB,
//                                           final StreamingStatusDetail detail,
//                                           final AWSCredentialsProvider credentialsProvider
//    ) {
//        //this.dynamoDB = dynamoDB;
//        this.detail = detail;
//        this.credentialsProvider = credentialsProvider;
//        AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(API_GATEWAY_ENDPOINT, REGION.getName());
//        this.apigatewayClient = AmazonApiGatewayManagementApiClientBuilder.standard()
//                .withCredentials(credentialsProvider)
//                .withEndpointConfiguration(endpointConfiguration)
//                .build();
//    }
//
//    /**
//     * Publish transcription to client by posting to an established web socket connection.
//     */
//    @Override
//    public void publish(TranscriptEvent event) {
//        List<Result> results = event.transcript().results();
//        if (results.size() > 0) {
//            Result result = results.get(0);
//            if (!result.isPartial()) {
//                try {
//                    logger.info("{} transcription event is {}", WEB_SOCKET_PUBLISHER_PREFIX, event.toString());
//
//                    if(getConnectionId() == null) {
//                        logger.info("{} connection id is null. Waiting for updating connection Id", WEB_SOCKET_PUBLISHER_PREFIX);
//                        return;
//                    }
//                    PostToConnectionRequest request = new PostToConnectionRequest().withConnectionId(this.connectionId).withData(StandardCharsets.UTF_8.encode(buildTranscription(result)));
//                    PostToConnectionResult postResult = apigatewayClient.postToConnection(request);
//                    logger.info("{} connection id is {}, post to connection result is {}", WEB_SOCKET_PUBLISHER_PREFIX, this.connectionId, postResult.toString());
//
//                    // No need to handle http response.
//                } catch(GoneException e) {
//                    logger.error("{} the connection with the provided id no longer exists. Refreshing connection id, message: {}", WEB_SOCKET_PUBLISHER_PREFIX, e.getMessage(), e);
//                    this.connectionId = null;
//                } catch (Exception e) {
//                    logger.error("{} publish encountered exception, error message: {}", WEB_SOCKET_PUBLISHER_PREFIX, e.getMessage(), e);
//                }
//            }
//        }
//    }
//
//    /**
//     * Publish done signal to client by posting to an established web socket connection.
//     */
//    @Override
//    public void publishDone() {
//        if(detail.getConnectionId() == null) {
//            logger.info("{} failed to get the connection id ", WEB_SOCKET_PUBLISHER_PREFIX);
//            return;
//        }
//
//        try {
//            String endedMsg = String.format("=== Transcription Ended for call %s in stream %s ===", this.detail.getCallId(), this.detail.getStreamArn());
//            PostToConnectionRequest postRequest = new PostToConnectionRequest().withConnectionId(this.connectionId).withData(StandardCharsets.UTF_8.encode(endedMsg));
//            PostToConnectionResult postResult = apigatewayClient.postToConnection(postRequest);
//
//            logger.info("{} post to connection result is {}", WEB_SOCKET_PUBLISHER_PREFIX, postResult.toString());
//        } catch (Exception e) {
//            // Don't have to handle any exception since this is the last POST that is sent to the endpoint.
//            logger.error("{} publish done encountered exception, error message: {}", WEB_SOCKET_PUBLISHER_PREFIX, e.getMessage(), e);
//        }
//    }
//
//    /*
//    private String getConnectionId() {
//        if(this.connectionId == null) {
//            GetItemSpec fromNumberSpec = new GetItemSpec()
//                    .withPrimaryKey(NUMBER, detail.getFromNumber())
//                    .withConsistentRead(true)
//                    .withProjectionExpression(CONNECTION_ID);
//
//            GetItemSpec toNumberSpec = new GetItemSpec()
//                    .withPrimaryKey(NUMBER, detail.getToNumber())
//                    .withConsistentRead(true)
//                    .withProjectionExpression(CONNECTION_ID);
//
//            Item fromNumberItem = getDDBClient().getTable(WEBSOCKET_MAPPING_TABLE_NAME).getItem(fromNumberSpec),
//                    toNumberItem = getDDBClient().getTable(WEBSOCKET_MAPPING_TABLE_NAME).getItem(toNumberSpec);
//
//            if (fromNumberItem != null && fromNumberItem.hasAttribute(CONNECTION_ID)) {
//                this.connectionId = (String) fromNumberItem.get(CONNECTION_ID);
//                logger.info("{} connection is associated with from number {} and id {}, starting transmission", WEB_SOCKET_PUBLISHER_PREFIX, detail.getFromNumber(), this.connectionId);
//                return this.connectionId;
//            }
//
//            if (toNumberItem != null && toNumberItem.hasAttribute(CONNECTION_ID)) {
//                this.connectionId = (String) toNumberItem.get(CONNECTION_ID);
//                logger.info("{} connection is associated with to number {} and id {}, starting transmission", WEB_SOCKET_PUBLISHER_PREFIX, detail.getToNumber(), this.connectionId);
//                return this.connectionId;
//            }
//
//            logger.info("{} cannot get connection id associated with number {} or number {} in dynamodb table. ", WEB_SOCKET_PUBLISHER_PREFIX, detail.getFromNumber(), detail.getToNumber());
//        }
//
//        return this.connectionId;
//    }
//
//    private DynamoDB getDDBClient() {
//        return this.dynamoDB;
//    }
//    */
//    private String buildTranscription(Result result) {
//        NumberFormat nf = NumberFormat.getInstance();
//        nf.setMinimumFractionDigits(3);
//        nf.setMaximumFractionDigits(3);
//
//        String callerLabel = String.format("Caller(%s)", detail.getFromNumber()), calleeLabel = String.format("Callee(%s)", detail.getToNumber());
//        return String.format("Thread %s %d: [%s, %s] %s - %s",
//                Thread.currentThread().getName(),
//                System.currentTimeMillis(),
//                nf.format(result.startTime()),
//                nf.format(result.endTime()),
//                this.detail.getIsCaller() == Boolean.TRUE ? callerLabel : calleeLabel,
//                result.alternatives().get(0).transcript());
//    }

    public void publish(TranscriptEvent event) {

    }

    public void publishDone() {

    }
}
