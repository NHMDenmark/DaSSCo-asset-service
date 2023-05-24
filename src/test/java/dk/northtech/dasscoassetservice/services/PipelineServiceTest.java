package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Pipeline;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

class PipelineServiceTest extends AbstractIntegrationTest{



    @Test
    void persistPipeline() {
        pipelineService.persistPipeline(new Pipeline("persistPipeline", "institution_1"));
        Optional<Pipeline> persistPipeline = pipelineService.findPipeline("persistPipeline");
        assertThat(persistPipeline.isPresent()).isTrue();

    }
}