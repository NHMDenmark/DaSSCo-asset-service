package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

// TODO: Add @Schema information.
public record AssetV1(
        @Schema(description = "", example = "CP0002637_L_selago_Fuji_ICC")
        String originalMedia,
        @Schema(description = "", example = "1970-01-01T00:00:00.000Z")
        Instant originalMediaTaken,
        @Schema(description = "", example = "Justin Hungerford")
        String digitiser,
        @Schema(description = "The name of the workstation used to do the imaging", example = "ti-ws1")
        String workstationName,
        @Schema(description = "The name of the pipeline that sent a create, update or delete request to the storage service", example = "ti-p1")
        String pipelineName,
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String institution,
        @Schema(description = "The collection name within the institution that holds the specimen", example = "test-collection")
        String collection,
        @Schema(description = "", example = "1970-01-01T00:00:00.000Z")
        Instant dateMediaCreated,
        @Schema(description = "", example = "PIPEHERB0001")
        String mediaCreatedBy,
        @Schema(description = "", example = "[\"1970-01-01T00:00:00.000Z\"]")
        List<Instant> dateMediaUpdated,
        @Schema(description = "", example = "[\"PIPEHERB0001\"]")
        List<String> mediaUpdatedBy,
        @Schema(description = "", example = "1970-01-01T00:00:00.000Z")
        String dateMediaDeleted,
        @Schema(description = "", example = "PIPEHERB0001")
        String mediaDeletedBy,
        @Schema(description = "", example = "1970-01-01T00:00:00.000Z")
        Instant dateMetadataCreated,
        @Schema(description = "", example = "[\"PIPEHERB0001\"]")
        List<String> metadataCreatedBy,
        @Schema(description = "", example = "[\"1970-01-01T00:00:00.000Z\"]")
        List<Instant> dateMetadataUpdated,
        @Schema(description = "", example = "[\"PIPEHERB0001\"]")
        List<String> metadataUpdatedBy,
        @Schema(description = "Records if the asset has been manually audited", example = "yes")
        String audited,
        @Schema(description = "", example = "Chelsea Graham")
        String auditedBy,
        @Schema(description = "", example = "1970-01-01T00:00:00.000Z")
        Instant auditedDate,
        @Schema(description = "", example = "archive")
        String status,
        @Schema(description = "", example = "")
        String storageLocation,
        @Schema(description = "", example = "")
        String parent,
        @Schema(description = "", example = "")
        String originalParent,
        @Schema(description = "", example = "7e7-1-02-11-21-25-1-01-001-05a8c7-00000000")
        String relatedMedia,
        @Schema(description = "", example = "no")
        String mutispecimenStatus,
        @Schema(description = "", example = "")
        String otherMultispecimen,
        @Schema(description = "", example = "CP0002637")
        String barcode,
        @Schema(description = "", example = "")
        String specimenPid,
        @Schema(description = "", example = "ae1fcf25-7e94-4506-8d64-5c54d69fa900")
        String specifySpecimenId,
        @Schema(description = "", example = "b33ea887-11ab-43b9-a562-44fdfe32af8e")
        String specifyAttachmentId,
        @Schema(description = "", example = "7e7-1-02-11-21-25-1-01-001-05a8cb-00000000")
        String mediaGuid,
        @Schema(description = "", example = "")
        String mediaPid,
        @Schema(description = "", example = "")
        String externalLink,
        @Schema(description = "", example = "image")
        String payloadType,
        @Schema(description = "The format of the asset", example = "tif")
        String fileFormat,
        @Schema(description = "", example = "")
        String fileInfo,
        @Schema(description = "", example = "")
        String accessLevel,
        @Schema(description = "The way that the specimen has been prepared (pinned insect or mounted on a slide)", example = "")
        String preparationType,
        @Schema(description = "", example = "FLORA DANICA EXSICCATA Lycopodium selago L. Jyll Silkeborg Vesterskov YII 1904 leg. M. Lorenzen.")
        String ocrText,
        @Schema(description = "", example = "")
        String geographicRegion,
        @Schema(description = "", example = "")
        String taxonName,
        @Schema(description = "", example = "")
        String typeStatus,
        @Schema(description = "", example = "")
        String specimenStorageLocation,
        @Schema(description = "A short description of funding source used to create the asset", example = "Hundredetusindvis af dollars")
        String funding,
        @Schema(description = "", example = "NHMD")
        String copyrightOwner,
        @Schema(description = "", example = "Attribution 4.0 International (CC BY 4.0)")
        String license,
        @Schema(description = "", example = "")
        String embargoType,
        @Schema(description = "", example = "")
        String embargoNotes,
        @Schema(description = "", example = "[]")
        List<String> equipmentDetails,
        @Schema(description = "", example = "")
        String exposureTime,
        @Schema(description = "", example = "")
        String fNumber,
        @Schema(description = "", example = "")
        String focalLength,
        @Schema(description = "", example = "")
        String isoSetting,
        @Schema(description = "", example = "")
        String whiteBalance,
        @Schema(description = "", example = "https://specify-attachments.science.ku.dk/fileget?coll=NHMD+Vascular+Plants&type=O&filename=sp68923230029256349442.att.jpg&downloadname=NHMD-679283.jpg&token=d545c06844d5b1fae60be67316374bce%3A1674817928")
        String originalSpecifyMediaName,
        @Schema(description = "", example = "specimen")
        String mediaSubject,
        @Schema(description = "", example = "[]")
        List<String> notes,
        @Schema(description = "", example = "no")
        String pushAssetToSpecify,
        @Schema(description = "", example = "yes")
        String pushMetadataToSpecify
){
    
}
