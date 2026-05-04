package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.amqp.QueueBroadcaster;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifyArsSyncMessage;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SpecifySyncStatus;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncAcknowledge;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncParkingSpaceRequest;
import dk.northtech.dasscoassetservice.repositories.ParkedFileRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class SpecifyArsSyncService {
    private static final Logger log = LoggerFactory.getLogger(SpecifyArsSyncService.class);
    private final AssetService assetService;
    private final UserService userService;
    private final SpecimenService specimenService;
    private final PipelineService pipelineService;
    private final QueueBroadcaster queueBroadcaster;
    private final WorkstationService workstationService;
    private final ExtendableEnumService extendableEnumService;
    private final FileProxyClient fileProxyClient;
    private final Jdbi jdbi;
    public static final String SPECIFY_DEFAULT_CREATION_STATUS = "SPECIFY_CREATED";

    @Inject
    public SpecifyArsSyncService(AssetService assetService
            , UserService userService
            , SpecimenService specimenService
            , PipelineService pipelineService
            , QueueBroadcaster queueBroadcaster
            , WorkstationService workstationService
            , ExtendableEnumService extendableEnumService
            , FileProxyClient fileProxyClient
            , Jdbi jdbi) {
        this.assetService = assetService;
        this.userService = userService;
        this.specimenService = specimenService;
        this.pipelineService = pipelineService;
        this.queueBroadcaster = queueBroadcaster;
        this.workstationService = workstationService;
        this.extendableEnumService = extendableEnumService;
        this.fileProxyClient = fileProxyClient;
        this.jdbi = jdbi;
    }


    public void handleSpecifyUpdate(SpecifyArsSyncMessage specifyArsSyncMessage) {
        try {
            Asset specifyAsset = specifyArsSyncMessage.asset;
            Optional<Asset> existing = assetService.getAsset(specifyAsset.asset_guid);
            User user = new User("dassco-asset-service");
            user = userService.ensureExists(user);
            specifyAsset.digitiser = user.username;

            if (existing.isPresent()) {
                Asset existingAsset = existing.get();
                boolean hasParkedFiles = hasParkedFiles(existingAsset.institution, existingAsset.collection, existingAsset.asset_guid);
                if (hasParkedFiles && existingAsset.asset_locked) {
                    queueBroadcaster.sendSpecifyArsAcknowledge(
                            new SyncAcknowledge(SpecifySyncStatus.FAILED, specifyArsSyncMessage.specifySyncLogId, "Asset is locked, but have parked files", existingAsset.asset_guid));
                    return;
                }

                mapAsset(specifyArsSyncMessage.asset, existingAsset, specifyArsSyncMessage);
                assetService.updateAsset(existingAsset, user);

                checkParkingAndAcknowedge(specifyArsSyncMessage, specifyAsset, existingAsset, hasParkedFiles);
            } else {
                specifyAsset.status = SPECIFY_DEFAULT_CREATION_STATUS;
                specifyAsset.pipeline = specifyAsset.pipeline == null ? "unknown" : specifyAsset.pipeline;
                specifyAsset.workstation = specifyAsset.workstation == null ? "unknown" : specifyAsset.workstation;
                log.info("pipeline is {}", specifyAsset.pipeline);
                log.info("institution is {}", specifyAsset.institution);
                if (pipelineService.findPipelineByInstitutionAndName(specifyAsset.pipeline, specifyAsset.institution).isEmpty()) {
                    pipelineService.persistPipeline(new Pipeline(specifyAsset.pipeline, specifyAsset.institution), specifyAsset.institution);
                }
                if (workstationService.findWorkstation(specifyAsset.workstation, specifyAsset.institution).isEmpty()) {
                    workstationService.createWorkStation(specifyAsset.institution, new Workstation(specifyAsset.workstation, WorkstationStatus.IN_SERVICE, specifyAsset.institution));
                }
                for (AssetSpecimen specimen : specifyArsSyncMessage.asset.asset_specimen) {
                    specimenService.putSpecimen(specimen.specimen, user);
                }
                Set<String> fileFormats = extendableEnumService.getFileFormats();
                for (String fileFormat : specifyArsSyncMessage.asset.file_formats) {
                    if (!fileFormats.contains(fileFormat)) {
                        extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.FILE_FORMAT, fileFormat);
                    }
                }
                if(!extendableEnumService.checkExists(ExtendableEnumService.ExtendableEnum.STATUS, specifyAsset.status)) {
                    extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.STATUS, specifyAsset.status);
                }
                assetService.persistAsset(specifyAsset, user, 122, false);

                boolean hasParkedFiles = hasParkedFiles(specifyAsset.institution, specifyAsset.collection, specifyAsset.asset_guid);
                checkParkingAndAcknowedge(specifyArsSyncMessage, specifyAsset, specifyAsset, hasParkedFiles);
            }
        } catch (Exception e1) {
            log.error(e1.getMessage(), e1);

            queueBroadcaster.sendSpecifyArsAcknowledge(new SyncAcknowledge(SpecifySyncStatus.FAILED, specifyArsSyncMessage.specifySyncLogId, e1.getMessage(), specifyArsSyncMessage.asset != null ? specifyArsSyncMessage.asset.asset_guid : null));
        }
    }

    private boolean checkParkingAndAcknowedge(SpecifyArsSyncMessage specifyArsSyncMessage, Asset specifyAsset, Asset existingAsset, boolean hasParkedFiles) {
        if (!hasParkedFiles) {
            queueBroadcaster.sendSpecifyArsAcknowledge(
                    new SyncAcknowledge(SpecifySyncStatus.SUCCEEDED, specifyArsSyncMessage.specifySyncLogId, null, existingAsset.asset_guid));
            return true;
        }

        acknowledgeIfParkedFileSyncFinished(fileProxyClient.syncParkedFile(
                new SyncParkingSpaceRequest(
                        new MinimalAsset(existingAsset.asset_guid, null, existingAsset.institution, existingAsset.collection),
                        specifyArsSyncMessage.specifySyncLogId)),
                specifyArsSyncMessage.specifySyncLogId, specifyAsset.asset_guid);
        return false;
    }

    private void acknowledgeIfParkedFileSyncFinished(SpecifySyncStatus syncStatus, Long specifySyncLogId, String asset_guid) {
        if (syncStatus != SpecifySyncStatus.STARTED) {
            queueBroadcaster.sendSpecifyArsAcknowledge(new SyncAcknowledge(syncStatus, specifySyncLogId, null, asset_guid));
        }
    }

    private boolean hasParkedFiles(String institution, String collection, String assetGuid) {
        String pathPrefix = String.format("%s/%s/%s/", institution, collection, assetGuid);
        return jdbi.withHandle(handle -> handle.attach(ParkedFileRepository.class).existsByPathPrefix(pathPrefix));
    }

    private void mapAsset(Asset fromSpecify, Asset existing, SpecifyArsSyncMessage specifyArsSyncMessage) {
        for (String s : specifyArsSyncMessage.updatedFields) {
            switch (s) {
                case "${asset_guid}.${file_format}", "${asset_guid}", "${digitiser}": {
                    break;
                }
                case "${file_format}":
                    if (existing.file_formats != null) {
                        // Currently dont delete
                        HashSet<String> strings = new HashSet<>(existing.file_formats);
                        strings.addAll(fromSpecify.file_formats);
                        existing.file_formats = new ArrayList<>(strings);
                    }
                    break;

                case "${asset_pid}":
                    // ARS is master for this, allow only on creation
                    break;

                case "${make_public}":
                    existing.make_public = fromSpecify.make_public;
                    break;

                case "${date_asset_deleted_ars}":
                    if (existing.date_asset_deleted_ars != fromSpecify.date_asset_deleted_ars) {
                        throw new RuntimeException("ARS asset cannot be deleted");
                    }
                    break;

                case "${date_asset_taken}":
                    existing.date_asset_taken = fromSpecify.date_asset_taken;
                    break;

                case "${legality.copyright}":
                    if (fromSpecify.legality != null) {
                        if (existing.legality != null) {
                            existing.legality = new Legality(fromSpecify.legality.copyright(), existing.legality.license(), existing.legality.credit());
                        } else {
                            existing.legality = new Legality(fromSpecify.legality.copyright(), null, null);
                        }
                    }
                    break;

                case "${legality.credit}":
                    if (fromSpecify.legality != null) {
                        if (existing.legality != null) {
                            existing.legality = new Legality(existing.legality.copyright(), existing.legality.license(), fromSpecify.legality.credit());
                        } else {
                            existing.legality = new Legality(null, null, fromSpecify.legality.credit());
                        }

                    }
                    break;

                case "${legality.license}":
                    if (fromSpecify.legality != null) {
                        if (existing.legality != null) {
                            existing.legality = new Legality(existing.legality.copyright(), fromSpecify.legality.license(), existing.legality.credit());
                        } else {
                            existing.legality = new Legality(null, fromSpecify.legality.license(), null);
                        }
                    }
                    break;

                case "${specify_attachment_remarks}":
                    existing.specify_attachment_remarks = fromSpecify.specify_attachment_remarks;
                    break;

                case "${specify_attachment_title}":
                    existing.specify_attachment_title = fromSpecify.specify_attachment_title;
                    break;

                case "${pipeline}":
                    existing.pipeline = fromSpecify.pipeline;
                    break;

                case "${metadata_updated_by}":
                    existing.metadata_updated_by = fromSpecify.metadata_updated_by;
                    break;

                case "${mos_id}":
                    existing.mos_id = fromSpecify.mos_id;
                    break;

                case "${metadata_source}":
                    //only on create
//                    existing.metadata_source = fromSpecify.metadata_source;
                    break;

                case "${metadata_version}":
                    existing.metadata_version = fromSpecify.metadata_version;
                    break;

                case "${camera_setting_control}":
                    existing.camera_setting_control = fromSpecify.camera_setting_control;
                    break;

                case "${workstation}":
                    existing.workstation = fromSpecify.workstation;
                    break;

                case "${date_audited}":
                    existing.date_audited = fromSpecify.date_audited;
                    break;

                case "${status}":
                    existing.status = fromSpecify.status;
                    break;

                case "${institution}":
                    if (!existing.institution.equals(fromSpecify.institution)) {
                        throw new RuntimeException("Institutions cannot be changed through specify-ars-sync");
                    }
                    break;

                case "${collection}":
                    if (!existing.collection.equals(fromSpecify.collection)) {
                        throw new RuntimeException("Collections cannot be changed through specify-ars-sync");
                    }
                    break;

                case "${payload_type}":
                    existing.payload_type = fromSpecify.payload_type;
                    break;

                default:
                    throw new IllegalArgumentException("Unknown property: " + s);
            }
        }
    }
}
