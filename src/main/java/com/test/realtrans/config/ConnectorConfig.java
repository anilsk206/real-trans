package com.test.realtrans.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

public class ConnectorConfig {
    @Bean
    public RestTemplate restTemplate(){
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname,sslSession) -> hostname.equals("localhost"));
        return new RestTemplate();
    }
}
