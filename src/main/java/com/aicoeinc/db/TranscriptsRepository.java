package com.aicoeinc.db;

import com.aicoeinc.model.transcript.TranscriptResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptsRepository extends MongoRepository<TranscriptResult, Long> {
    @Query(
            value = "{callId:'?0'}",
            //fields = "{channelId, startTime, transcript}",
            sort = "{startTime: 1}"
        )
    List<TranscriptResult> findByCallId(String callId);
}
