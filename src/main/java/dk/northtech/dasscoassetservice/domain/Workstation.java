package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public class Workstation {
    @Schema(description = "The name of the workstation")
    public String workstation_name;

    @Schema(description = "The name of the institution")
    public String institution;
}
