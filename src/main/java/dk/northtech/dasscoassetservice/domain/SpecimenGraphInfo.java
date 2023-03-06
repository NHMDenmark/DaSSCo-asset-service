package dk.northtech.dasscoassetservice.domain;

import javax.annotation.Nullable;

public record SpecimenGraphInfo(
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
        @Nullable String digitisorName,
        @Nullable String pipelineName,
        String createdDate
) {
    public SpecimenGraphInfo(String instituteName, String instituteOcrText, String specimenName, String specimenMediaSubject, String specimenSpecifySpecId, String specimenSpecifyAttId, String specimenOrigSpecifyMediaName, String assetName, String assetMediaGuid, String assetFileFormat, String assetDateMediaCreated, @Nullable String digitisorName, @Nullable String pipelineName, String createdDate) {
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
        this.digitisorName = digitisorName != null ? digitisorName.replaceAll("\"", "") : null;
        this.pipelineName = pipelineName != null ? pipelineName.replaceAll("\"", "") : null;
        this.createdDate = createdDate.replaceAll("\"", "");
    }
}
