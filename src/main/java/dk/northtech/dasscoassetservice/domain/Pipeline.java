package dk.northtech.dasscoassetservice.domain;

import io.swagger.annotations.Example;
import io.swagger.v3.oas.annotations.media.Schema;

public class Pipeline {
    @Schema(description = "The name of the pipeline", example = "PIP1")
    public String pipeline_name;
    @Schema(description = "The institution that the pipeline belongs to", example = "NNAD")
    public String institution;
}
