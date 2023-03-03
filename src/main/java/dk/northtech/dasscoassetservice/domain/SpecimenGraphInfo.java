package dk.northtech.dasscoassetservice.domain;

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
        String digitisorName,
        String createdDate
) {
}
