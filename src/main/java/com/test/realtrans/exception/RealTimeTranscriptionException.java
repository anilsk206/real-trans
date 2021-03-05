package com.test.realtrans.exception;

public class RealTimeTranscriptionException extends RuntimeException {
    private final String errorCode;

    public RealTimeTranscriptionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
