package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Pipeline;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PipelineServiceTest extends AbstractIntegrationTest {

    @Test
    void testPersistPipeline() {
        pipelineService.persistPipeline(new Pipeline("persistPipeline", "institution_1"));
        Optional<Pipeline> persistPipeline = pipelineService.findPipeline("persistPipeline");
        assertThat(persistPipeline.isPresent()).isTrue();
    }

    @Test
    void testPersistPipelineInstitutionDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> pipelineService.persistPipeline(new Pipeline("failed-pipeline", "non-existent-institution")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institute doesnt exist");
    }

    @Test
    void testPersistPipelineAlreadyExists(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> pipelineService.persistPipeline(new Pipeline("i1_p1", "institution_1")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("A pipeline with name [i1_p1] already exists within institution [institution_1]");
    }

    @Test
    void testPersistPipelineNameIsNull(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> pipelineService.persistPipeline(new Pipeline("", "institution_1")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Name cannot be null or empty");
    }

    @Test
    void testListPipelines(){
        List<Pipeline> pipelines = pipelineService.listPipelines(new Institution("institution_1"));
        assertThat(pipelines.size()).isAtLeast(2);
    }

    @Test
    void testListPipelinesInstitutionDoesNotExist(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> pipelineService.listPipelines(new Institution("non-existent-institution")));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Institute does not exist");
    }

    @Test
    void testFindPipeline(){
        Optional<Pipeline> optPipeline = pipelineService.findPipeline("i1_p1");
        assertThat(optPipeline.isPresent()).isTrue();
        Pipeline pipeline = optPipeline.get();
        assertThat(pipeline.name()).isEqualTo("i1_p1");
        assertThat(pipeline.institution()).isEqualTo("institution_1");
    }

    @Test
    void testFindPipelinePipelineDoesNotExist(){
        Optional<Pipeline> optPipeline = pipelineService.findPipeline("non-existent-pipeline");
        assertThat(optPipeline.isPresent()).isFalse();
    }

    @Test
    void testFindPipelineByInstitutionAndName(){
        Optional<Pipeline> optPipeline = pipelineService.findPipelineByInstitutionAndName("i1_p1", "institution_1");
        assertThat(optPipeline.isPresent()).isTrue();
        Pipeline pipeline = optPipeline.get();
        assertThat(pipeline.name()).isEqualTo("i1_p1");
        assertThat(pipeline.institution()).isEqualTo("institution_1");
    }

    @Test
    void testFindPipelineByInstitutionAndNamePipelineDoesNotExist(){
        Optional<Pipeline> optPipeline = pipelineService.findPipelineByInstitutionAndName("non-existent-pipeline", "institution_1");
        assertThat(optPipeline.isPresent()).isFalse();
    }

    @Test
    void testFindPipelineByInstitutionAndNameInstitutionDoesNotExist(){
        Optional<Pipeline> optPipeline = pipelineService.findPipelineByInstitutionAndName("i1_p1", "non-existent-institution");
        assertThat(optPipeline.isPresent()).isFalse();
    }
}
