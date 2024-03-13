package dk.northtech.dasscoassetservice.domain;

import jakarta.annotation.Nullable;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;
import java.util.List;

public record Directory(Long directoryId
        , String uri
        , String node_host
//        , AccessType access
        , Instant creationDatetime
        , int allocatedStorageMb
        , boolean awaitingErdaSync
        , int erdaSyncAttempts
        , String syncUser
        , String syncWorkstation
        , String syncPipeline
        , String assetGuid) {

    @JdbiConstructor
    public Directory {
    }
}
