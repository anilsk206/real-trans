package com.aicoeinc.db;

import com.aicoeinc.model.dbCollections.CallStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallStatusRepository extends MongoRepository<CallStatus, Long> {
    @Query(
            value = "{callId:'?0'}"
            , fields = "{callId:1, callStatus:1}"
            , sort = "{rowCreateTs: -1}"
        )
    List<CallStatus> findByCallId(String callId);
}
