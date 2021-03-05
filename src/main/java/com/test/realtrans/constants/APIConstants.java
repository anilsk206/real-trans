package com.test.realtrans.constants;

public class APIConstants {
    private APIConstants() {
        throw new IllegalStateException("APIConstants class is not supposed to be instantiated!");
    }

    public static final String INVALID_INPUT = "AS-1001";
    public static final String APPLICATION_ERROR = "AS-1002";
    public static final String CALL_REAL_TIME_TRANSCRIPTION = "REAL_TIME_TRANSCRIPTION";

    public static final String[] RECOMMENDATIONS = {"Greet the customer.",
        "Ask for cross sale.",
        "Read the disclaimer in full and get consent.",
        "Congratulate the customer.",
        "Verify what matters more to customer - price or protection?",
        "Spend more time on sale.",
        "Inform the customer about SquareTrade and appliance insurance.",
        "Inform customer about milewise program.",
        "Check if customer wants Road Side Assistance."
    };
}

