package com.aicoeinc;

import com.aicoeinc.insights.RealTimeInsights;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class MainController {
    private final RealTimeInsights realTimeInsights;

    public MainController(RealTimeInsights realTimeInsights) {
        this.realTimeInsights = realTimeInsights;
    }

    @CrossOrigin(allowedHeaders = "*")
    @GetMapping(value = "/event/getInsights/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getRealTimeAssist(@PathVariable(required = false) String id) throws Exception {
        System.out.println("UCID : "+ id);

        return Flux.interval(Duration.ofSeconds(3))
                .map(it -> realTimeInsights.getInsightsFor(id));
    }
}


