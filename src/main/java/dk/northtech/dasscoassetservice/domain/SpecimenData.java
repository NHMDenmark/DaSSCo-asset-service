package dk.northtech.dasscoassetservice.domain;

import javax.annotation.Nullable;

public record SpecimenData(
        String instituteName,
        String pipelineName,
        String workstationName,
        String createdDate
) {
    public SpecimenData(String instituteName, String pipelineName, String workstationName, String createdDate) {
        this.instituteName = instituteName.replaceAll("\"", "");
        this.pipelineName = pipelineName != null ? pipelineName.replaceAll("\"", "") : null;
        this.workstationName = workstationName != null ? workstationName.replaceAll("\"", "") : null;
        this.createdDate = createdDate.replaceAll("\"", "");
    }
}
