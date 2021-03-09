package com.aicoeinc.streamingeventmodel;

public interface StreamingStatusDetail {
    String getVoiceConnectorId();
    String getTransactionId();
    String getCallId();
    StreamingStatus getStreamingStatus();
    Direction getDirection();
    String getVersion();
    String getStreamArn();
    String getFromNumber();
    String getToNumber();
    Boolean getIsCaller();
}