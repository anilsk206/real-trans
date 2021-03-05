package com.test.realtrans;

import com.test.realtrans.constants.TransConstants;
import com.test.realtrans.transcribing.RealTimeTranscriber;
import com.test.realtrans.utils.AWSUtils;
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
    private final AWSUtils awsUtils;

    public MainController(AWSUtils awsUtils) {
        this.awsUtils = awsUtils;
    }

    @CrossOrigin(allowedHeaders = "*")
    @GetMapping(value = "/event/resources/usage/{id}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getResourceUsage(@PathVariable(required = false) String id) throws Exception {

        Random random = new Random();
        RealTimeTranscriber rtt = new RealTimeTranscriber(awsUtils);
        rtt.initialize();
        System.out.println("param id"+id);
        return Flux.interval(Duration.ofSeconds(1))
                .map(it -> TransConstants.RECOMMENDATIONS[random.nextInt(8)]);

    }
}

