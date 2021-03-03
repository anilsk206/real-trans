package com.test.realtrans;

import com.test.realtrans.constants.TransConstants;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Date;
import java.util.Random;

@RestController
public class MainController {

    @CrossOrigin(allowedHeaders = "*")
    @GetMapping(value = "/event/resources/usage/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getResourceUsage(@PathVariable(required = false) String id) {

        Random random = new Random();

        System.out.println("param id"+id);
        return Flux.interval(Duration.ofSeconds(1))
                .map(it -> TransConstants.RECOMMENDATIONS[random.nextInt(8)]);

    }
}

