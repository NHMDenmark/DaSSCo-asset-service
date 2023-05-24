package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class PipelineServiceTest extends AbstractIntegrationTest{



    @Test
    void persistPipeline() {
        pipelineService.persistPipeline(new Pipeline("persistPipeline", "institution_1"));
        Optional<Pipeline> persistPipeline = pipelineService.findPipeline("persistPipeline");
        assertThat(persistPipeline.isPresent()).isTrue();

    }
}