package dk.northtech.dasscoassetservice.domain;

import javax.annotation.Nullable;

public record SpecimenData(
        String instituteName,
        String instituteOcrText,
        String specimenName,
        String specimenMediaSubject,
        String specimenSpecifySpecId,
        String specimenSpecifyAttId,
        String specimenOrigSpecifyMediaName,
        String assetName,
        String assetMediaGuid,
        String assetFileFormat,
        String assetDateMediaCreated,
        @Nullable String pipelineName,
        @Nullable String workstationName,
        String createdDate
) {
    public SpecimenData(String instituteName, String instituteOcrText, String specimenName, String specimenMediaSubject, String specimenSpecifySpecId, String specimenSpecifyAttId, String specimenOrigSpecifyMediaName, String assetName, String assetMediaGuid, String assetFileFormat, String assetDateMediaCreated, @Nullable String pipelineName, @Nullable String workstationName, String createdDate) {
        this.instituteName = instituteName.replaceAll("\"", "");
        this.instituteOcrText = instituteOcrText.replaceAll("\"", "");
        this.specimenName = specimenName.replaceAll("\"", "");
        this.specimenMediaSubject = specimenMediaSubject.replaceAll("\"", "");
        this.specimenSpecifySpecId = specimenSpecifySpecId.replaceAll("\"", "");
        this.specimenSpecifyAttId = specimenSpecifyAttId.replaceAll("\"", "");
        this.specimenOrigSpecifyMediaName = specimenOrigSpecifyMediaName.replaceAll("\"", "");
        this.assetName = assetName.replaceAll("\"", "");
        this.assetMediaGuid = assetMediaGuid.replaceAll("\"", "");
        this.assetFileFormat = assetFileFormat.replaceAll("\"", "");
        this.assetDateMediaCreated = assetDateMediaCreated.replaceAll("\"", "");
        this.pipelineName = pipelineName != null ? pipelineName.replaceAll("\"", "") : null;
        this.workstationName = workstationName != null ? workstationName.replaceAll("\"", "") : null;
        this.createdDate = createdDate.replaceAll("\"", "");
    }
}
