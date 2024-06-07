package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;
import java.util.List;

public record Directory(
        @Schema(description = "ID for the Directory", example = "")
        Long directoryId,
        @Schema(description = "Uniform Resource Identifier for the directory.", example = "")
        String uri,
        @Schema(description = "", example = "")
        String node_host,
        //AccessType access,
        @Schema(description = "Date and time of creation", example = "2023-05-24T00:00:00.000Z")
        Instant creationDatetime,
        @Schema(description = "Allocation in memory for the directory, in MB", example = "10")
        int allocatedStorageMb,
        @Schema(description = "Shows if the directory is awaiting synchronizing with ERDA", example = "false")
        boolean awaitingErdaSync,
        @Schema(description = "Number of attempts to synchronize with ERDA", example = "0")
        int erdaSyncAttempts,
        @Schema(description = "User attempting the synchronization", example = "THBO")
        String syncUser,
        @Schema(description = "Workstation attempting the synchronization", example = "ti-ws-01")
        String syncWorkstation,
        @Schema(description = "Pipeline attempting the synchronization", example = "ti-p1")
        String syncPipeline,
        @Schema(description = "The Global Unique Identifier generated for each asset", example = "ti-a01-202305241657")
        String assetGuid) {

    @JdbiConstructor
    public Directory {
    }
}
