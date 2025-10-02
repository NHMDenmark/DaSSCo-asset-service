package dk.northtech.dasscoassetservice.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.cache.DigitiserCache;
import dk.northtech.dasscoassetservice.cache.PayloadTypeCache;
import dk.northtech.dasscoassetservice.cache.PreparationTypeCache;
import dk.northtech.dasscoassetservice.cache.SubjectCache;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.repositories.*;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AssetService {
    private final InstitutionService institutionService;
    private final CollectionService collectionService;
    private final WorkstationService workstationService;
    private final StatisticsDataServiceV2 statisticsDataServiceV2;
    private final FileProxyClient fileProxyClient;
    private final PipelineService pipelineService;
    private final RightsValidationService rightsValidationService;
    private final Jdbi jdbi;
    private final FundingService fundingService;
    private final DigitiserCache digitiserCache;
    private final SubjectCache subjectCache;
    private final PayloadTypeCache payloadTypeCache;
    private final PreparationTypeCache preparationTypeCache;
    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);
    private final FileProxyConfiguration fileProxyConfiguration;
    private final ExtendableEnumService extendableEnumService;
    private final RoleService roleService;
    Cache<String, Instant> assetsGettingCreated;
    private final UserService userService;
    private final SpecimenService specimenService;
    private final AssetSyncService assetSyncService;

    @Inject
    public AssetService(InstitutionService institutionService
            , CollectionService collectionService
            , WorkstationService workstationService
            , @Lazy FileProxyClient fileProxyClient
            , Jdbi jdbi
            , StatisticsDataServiceV2 statisticsDataServiceV2
            , PipelineService pipelineService
            , RightsValidationService rightsValidationService
            , DigitiserCache digitiserCache
            , SubjectCache subjectCache
            , PayloadTypeCache payloadTypeCache
            , PreparationTypeCache preparationTypeCache
            , FileProxyConfiguration fileProxyConfiguration
            , ExtendableEnumService extendableEnumService
            , UserService userService
            , AssetSyncService assetSyncService
            , FundingService fundingService
            , SpecimenService specimenService, RoleService roleService) {
        this.institutionService = institutionService;
        this.collectionService = collectionService;
        this.workstationService = workstationService;
        this.fileProxyClient = fileProxyClient;
        this.statisticsDataServiceV2 = statisticsDataServiceV2;
        this.pipelineService = pipelineService;
        this.jdbi = jdbi;
        this.digitiserCache = digitiserCache;
        this.subjectCache = subjectCache;
        this.payloadTypeCache = payloadTypeCache;
        this.assetSyncService = assetSyncService;
        this.rightsValidationService = rightsValidationService;
        this.preparationTypeCache = preparationTypeCache;
        this.fileProxyConfiguration = fileProxyConfiguration;
        this.extendableEnumService = extendableEnumService;
        this.userService = userService;
        this.fundingService = fundingService;
        this.specimenService = specimenService;
        this.assetsGettingCreated = Caffeine.newBuilder()
                .expireAfterWrite(fileProxyConfiguration.shareCreationBlockedSeconds(), TimeUnit.SECONDS).build();
        this.roleService = roleService;
    }

    void validateAssetFields(Asset asset) {
        if (Strings.isNullOrEmpty(asset.asset_guid)) {
            throw new IllegalArgumentException("asset_guid cannot be null");
        }
        if (Strings.isNullOrEmpty(asset.asset_pid)) {
            throw new IllegalArgumentException("asset_pid cannot be null");
        }
        if (Strings.isNullOrEmpty(asset.status) || !extendableEnumService.getStatuses().contains(asset.status)) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        asset.parent_guids.forEach(parent_guid -> {
            if ("".equals(parent_guid)) {
                throw new IllegalArgumentException("Parent may not be an empty string");
            }
        });
        if (asset.issues != null) {
            asset.issues.forEach(this::validateIssue);
        }
        if (!asset.asset_specimen.isEmpty()) {
            asset.asset_specimen.forEach(specimenService::validateAssetSpecimen);
        }
    }


    void validateIssue(Issue issue) {
        if (Strings.isNullOrEmpty(issue.category())) {
            throw new IllegalArgumentException("Issue category cannot be null");
        }
        if (!extendableEnumService.checkExists(ExtendableEnumService.ExtendableEnum.ISSUE_CATEGORY, issue.category())) {
            throw new IllegalArgumentException("Issue category doesnt exists");
        }
        if (Strings.isNullOrEmpty(issue.status())) {
            throw new IllegalArgumentException("Issue status cannot be null");
        }
        if (issue.solved() == null) {
            throw new IllegalArgumentException("Issue solved cannot be null");
        }
    }

    void validateAndSetCollectionId(Asset asset) {
        Optional<Collection> collectionOpt = collectionService.findCollectionInternal(asset.collection, asset.institution);
        if (collectionOpt.isEmpty()) {
            throw new IllegalArgumentException("Collection doesnt exist");
        }
        asset.collection_id = collectionOpt.get().collection_id();
    }

    void validateNewAssetAndSetIds(Asset asset) {
        if (Strings.isNullOrEmpty(asset.institution)) {
            throw new IllegalArgumentException("Institution cannot be null");
        }
        if (Strings.isNullOrEmpty(asset.collection)) {
            throw new IllegalArgumentException("Collection cannot be null");
        }
        Optional<Institution> ifExists = institutionService.getIfExists(asset.institution);
        if (ifExists.isEmpty()) {
            throw new IllegalArgumentException("Institution doesnt exist");
        }
        if (Strings.isNullOrEmpty(asset.digitiser)) {
            throw new IllegalArgumentException("digitiser cannot be null when creating asset");
        }

    }

    public Optional<Asset> getAsset(String assetGuid) {

//        if (asset.isPresent()) {
//            Asset assetToBeMapped = asset.get();

        return jdbi.withHandle(h -> {
            AssetRepository assetRepository = h.attach(AssetRepository.class);
            Optional<Asset> asset = assetRepository.readAssetInternalNew(assetGuid);
            RoleRepository roleRepository = h.attach(RoleRepository.class);
            if (asset.isEmpty()) {
                return Optional.empty();
            }
            Asset assetToBeMapped = asset.get();
            EventRepository attach = h.attach(EventRepository.class);
            assetToBeMapped.events = attach.getAssetEvents(assetGuid);
            assetToBeMapped.asset_specimen = specimenService.findAssetSpecimens(assetGuid);
            assetToBeMapped.multi_specimen = assetToBeMapped.asset_specimen.size() > 1;
            UserRepository userRepository = h.attach(UserRepository.class);
            assetToBeMapped.complete_digitiser_list = userRepository.getDigitiserList(assetGuid);
            FundingRepository fundingRepository = h.attach(FundingRepository.class);
            IssueRepository issueRepository = h.attach(IssueRepository.class);
            assetToBeMapped.issues = issueRepository.findIssuesByAssetGuid(assetToBeMapped.asset_guid);
            assetToBeMapped.funding = fundingRepository.getAssetFunds(assetToBeMapped.asset_guid).stream().map(Funding::funding).toList();
            assetToBeMapped.parent_guids = assetRepository.getParents(assetToBeMapped.asset_guid);
            assetToBeMapped.role_restrictions = roleRepository.findRoleRestrictions(RestrictedObjectType.ASSET, assetToBeMapped.asset_guid);
            PublisherRepository publisherRepository = h.attach(PublisherRepository.class);
            assetToBeMapped.external_publishers = publisherRepository.internal_listPublicationLinks(assetGuid);
            for (Event event : assetToBeMapped.events) {
                if (DasscoEvent.AUDIT_ASSET.equals(event.event)) {
                    assetToBeMapped.audited = true;
                    if (assetToBeMapped.date_audited == null || event.timestamp.isAfter(assetToBeMapped.date_audited)) {
                        assetToBeMapped.date_audited = event.timestamp;
                        assetToBeMapped.audited_by = event.user;
                    }

                } else if (DasscoEvent.SYNCHRONISE_SPECIFY.equals(event.event) && (assetToBeMapped.date_pushed_to_specify == null || assetToBeMapped.date_pushed_to_specify.isAfter(event.timestamp))) {
                    assetToBeMapped.date_pushed_to_specify = event.timestamp;
                } else if (DasscoEvent.BULK_UPDATE_ASSET_METADATA.equals(event.event)
                           && assetToBeMapped.date_metadata_updated == null) {
                    assetToBeMapped.date_metadata_updated = event.timestamp;
                    assetToBeMapped.metadata_updated_by = event.user;

                } else if (DasscoEvent.UPDATE_ASSET_METADATA.equals(event.event)
                           && (assetToBeMapped.date_metadata_updated == null || event.timestamp.isAfter(assetToBeMapped.date_metadata_updated))) {
                    assetToBeMapped.date_metadata_updated = event.timestamp;
                    assetToBeMapped.metadata_updated_by = event.user;

                } else if (DasscoEvent.CREATE_ASSET_METADATA.equals(event.event)) {
                    if (assetToBeMapped.date_metadata_updated == null) {
                        assetToBeMapped.date_metadata_updated = event.timestamp;
                    }
                    //The pipeline field is always taken from the create event, even if later updates are present with different pipeline
                    assetToBeMapped.pipeline = event.pipeline;
                    assetToBeMapped.metadata_created_by = event.user;
                } else if (DasscoEvent.DELETE_ASSET_METADATA.equals(event.event)) {
                    assetToBeMapped.date_asset_deleted = event.timestamp;
                }
            }

            return Optional.of(assetToBeMapped);
        });

//        }

    }

    public List<Asset> getAssets(List<String> assetGuids){
        return this.jdbi.withHandle(h -> {
                    var assets = h.createQuery("""
                                    SELECT
                                        asset.*
                                        , collection.collection_name
                                        , collection.institution_name
                                        , dassco_user.username AS digitiser
                                        , workstation.workstation_name
                                        , copyright
                                        , license
                                        , credit
                                    FROM asset
                                    LEFT JOIN collection USING(collection_id)
                                    LEFT JOIN workstation USING(workstation_id)
                                    LEFT JOIN legality USING(legality_id)
                                    LEFT JOIN dassco_user ON dassco_user.dassco_user_id = asset.digitiser_id
                                    WHERE asset_guid in (<asset_guids>)
                                    """)
                            .bindList("asset_guids", assetGuids)
                            .map(new AssetMapper())
                            .list();

                    Map<String, List<Event>> assetEvents = h.createQuery("""
                                    select asset_guid, username, timestamp, event, pipeline_name from event
                                    left join dassco_user using (dassco_user_id)
                                    left join pipeline using (pipeline_id)
                                    where asset_guid in (<assetGuids>)
                                    """)
                    .bindList("assetGuids", assetGuids)
                    .execute((statement, ctx) -> {
                        try (ctx; var rs = statement.get().getResultSet()) {
                            Map<String, List<Event>> assetEventsTemp = new HashMap<>();
                            while (rs.next()) {
                                String assetGuid = rs.getString("asset_guid");
                                String username = rs.getString("username");
                                Timestamp timestamp = rs.getTimestamp("timestamp");
                                String event = rs.getString("event");
                                String pipelineName = rs.getString("pipeline_name");
                                Event newEvent = new Event(
                                        username,
                                        timestamp != null ? timestamp.toInstant() : null,
                                        event != null ? DasscoEvent.valueOf(event) : null,
                                        pipelineName
                                );
                                assetEventsTemp.computeIfAbsent(assetGuid, k -> new ArrayList<>()).add(newEvent);
                            }
                            return assetEventsTemp;
                        }
                    });
            Map<String, List<AssetSpecimen>> assetSpecimens = specimenService.getMultiAssetSpecimens(new HashSet<>(assetGuids));
                    Map<String, List<String>> assetCompleteDigitiserList = h.createQuery("""
                            SELECT asset_guid, username
                            FROM digitiser_list
                            LEFT JOIN dassco_user USING (dassco_user_id)
                            WHERE asset_guid in (<assetGuids>)
                            """)
                            .bindList("assetGuids", assetGuids)
                            .execute((statement, ctx) -> {
                                try (ctx; var rs = statement.get().getResultSet()) {
                                    Map<String, List<String>> assetCompleteDigitiserListTemp = new HashMap<>();
                                    while (rs.next()) {
                                        String assetGuid = rs.getString("asset_guid");
                                        String username = rs.getString("username");
                                        assetCompleteDigitiserListTemp.computeIfAbsent(assetGuid, k -> new ArrayList<>()).add(username);
                                    }
                                    return assetCompleteDigitiserListTemp;
                                }
                            });

                    Map<String, List<Issue>> assetIssues = h.createQuery("""
                            SELECT issue_id, asset_guid, category, name, timestamp, status, description, notes, solved
                            FROM issue
                            WHERE asset_guid in (<assetGuids>)
                            """)
                            .bindList("assetGuids", assetGuids)
                            .execute((statement, ctx) -> {
                                try (ctx; var rs = statement.get().getResultSet()) {
                                    Map<String, List<Issue>> assetSpecimensTemp = new HashMap<>();
                                    while (rs.next()) {
                                        int issueId = rs.getInt("issue_id");
                                        String assetGuid = rs.getString("asset_guid");
                                        String category = rs.getString("category");
                                        String name = rs.getString("name");
                                        Timestamp timestamp = rs.getTimestamp("timestamp");
                                        String status = rs.getString("status");
                                        String description = rs.getString("description");
                                        String notes = rs.getString("notes");
                                        boolean solved = rs.getBoolean("solved");
                                        var issue = new Issue(issueId, assetGuid, category, name, timestamp != null ? timestamp.toInstant() : null, status, description, notes, solved);
                                        assetSpecimensTemp.computeIfAbsent(assetGuid, k -> new ArrayList<>()).add(issue);
                                    }
                                    return assetSpecimensTemp;
                                }
                            });

                    Map<String, List<String>> assetFundings = h.createUpdate("""
                            SELECT asset_guid, funding_id, funding
                            FROM asset_funding
                            INNER JOIN funding USING (funding_id)
                            WHERE asset_guid in (<assetGuids>)
                            """).bindList("assetGuids", assetGuids)
                            .execute((statement, ctx) -> {
                                try (ctx; var rs = statement.get().getResultSet()) {
                                    Map<String, List<String>> assetFundingsTemp = new HashMap<>();
                                    while (rs.next()) {
                                        String assetGuid = rs.getString("asset_guid");
                                        String funding = rs.getString("funding");
                                        assetFundingsTemp.computeIfAbsent(assetGuid, k -> new ArrayList<>()).add(funding);
                                    }
                                    return assetFundingsTemp;
                                }
                            });

                    Map<String, Set<String>> assetParent = h.createUpdate("SELECT child_guid, parent_guid FROM parent_child WHERE child_guid in (<assetGuids>)")
                            .bindList("assetGuids", assetGuids)
                            .execute((statement, ctx) -> {
                                try (ctx; var rs = statement.get().getResultSet()) {
                                    Map<String, Set<String>> assetParentTemp = new HashMap<>();
                                    while (rs.next()) {
                                        String assetGuid = rs.getString("child_guid");
                                        String parentGuid = rs.getString("parent_guid");
                                        assetParentTemp.computeIfAbsent(assetGuid, k -> new HashSet<>()).add(parentGuid);
                                    }
                                    return assetParentTemp;
                                }
                            });

            Map<String, List<Publication>> assetPublishers = h.createUpdate("SELECT asset_guid, description, publisher, asset_publisher_id FROM asset_publisher WHERE asset_guid in (<assetGuids>)")
                    .bindList("assetGuids", assetGuids)
                    .execute((statement, ctx) -> {
                        try (ctx; var rs = statement.get().getResultSet()) {
                            Map<String, List<Publication>> assetPublishersTemp = new HashMap<>();
                            while (rs.next()) {
                                String assetGuid = rs.getString("asset_guid");
                                String description = rs.getString("description");
                                String publisher = rs.getString("publisher");
                                Long publisherId = rs.getLong("publisher_id");
                                var newPublisher = new Publication(publisherId, assetGuid, description, publisher);
                                assetPublishersTemp.computeIfAbsent(assetGuid, k -> new ArrayList<>()).add(newPublisher);
                            }
                            return assetPublishersTemp;
                        }
                    });

            return assets.stream().peek(asset -> {
                asset.events = assetEvents.get(asset.asset_guid);
                asset.asset_specimen = assetSpecimens.get(asset.asset_guid);
                asset.complete_digitiser_list = assetCompleteDigitiserList.get(asset.asset_guid);
                asset.issues = assetIssues.get(asset.asset_guid);
                asset.funding = assetFundings.get(asset.asset_guid);
                asset.parent_guids = assetParent.get(asset.asset_guid);
                asset.external_publishers = assetPublishers.get(asset.asset_guid);
                for (Event event : asset.events) {
                    if (DasscoEvent.AUDIT_ASSET.equals(event.event)) {
                        asset.audited = true;
                        if (asset.date_audited == null || event.timestamp.isAfter(asset.date_audited)) {
                            asset.date_audited = event.timestamp;
                            asset.audited_by = event.user;
                        }

                    } else if (DasscoEvent.SYNCHRONISE_SPECIFY.equals(event.event) && (asset.date_pushed_to_specify == null || asset.date_pushed_to_specify.isAfter(event.timestamp))) {
                        asset.date_pushed_to_specify = event.timestamp;
                    } else if (DasscoEvent.BULK_UPDATE_ASSET_METADATA.equals(event.event)
                            && asset.date_metadata_updated == null) {
                        asset.date_metadata_updated = event.timestamp;
                        asset.metadata_updated_by = event.user;

                    } else if (DasscoEvent.UPDATE_ASSET_METADATA.equals(event.event)
                            && (asset.date_metadata_updated == null || event.timestamp.isAfter(asset.date_metadata_updated))) {
                        asset.date_metadata_updated = event.timestamp;
                        asset.metadata_updated_by = event.user;

                    } else if (DasscoEvent.CREATE_ASSET_METADATA.equals(event.event)) {
                        if (asset.date_metadata_updated == null) {
                            asset.date_metadata_updated = event.timestamp;
                        }
                        //The pipeline field is always taken from the create event, even if later updates are present with different pipeline
                        asset.pipeline = event.pipeline;
                        asset.metadata_created_by = event.user;
                    } else if (DasscoEvent.DELETE_ASSET_METADATA.equals(event.event)) {
                        asset.date_asset_deleted = event.timestamp;
                    }
                }
            }).toList();
        });
    }

    void validateAsset(Asset asset) {

        if (asset.parent_guids != null && asset.parent_guids.contains(asset.asset_guid)) {
            throw new IllegalArgumentException("Asset cannot be its own parent");
        }
        Optional<Pipeline> pipelineOpt = pipelineService.findPipelineByInstitutionAndName(asset.pipeline, asset.institution);
        if (pipelineOpt.isEmpty()) {
            throw new IllegalArgumentException("Pipeline doesnt exist in this institution");
        }
        Optional<Workstation> workstationOpt = workstationService.findWorkstation(asset.workstation, asset.institution);
        if (workstationOpt.isEmpty()) {
            throw new IllegalArgumentException("Workstation does not exist");
        }
        Workstation workstation = workstationOpt.get();
        if (workstation.status().equals(WorkstationStatus.OUT_OF_SERVICE)) {
            throw new DasscoIllegalActionException("Workstation [" + workstation.status() + "] is marked as out of service");
        }
        asset.workstation_id = workstation.workstation_id();
        if (asset.file_formats != null && !asset.file_formats.isEmpty()) {
            Set<String> fileFormats = extendableEnumService.getFileFormats();
            for (String s : asset.file_formats) {
                if (!fileFormats.contains(s)) {
                    throw new IllegalArgumentException(s + " is not a valid file format");
                }
            }
        }
        if (asset.external_publishers != null) {
            for (Publication publication : asset.external_publishers) {
                if (!extendableEnumService.checkExists(ExtendableEnumService.ExtendableEnum.EXTERNAL_PUBLISHER, publication.name())) {
                    throw new IllegalArgumentException("Publisher " + publication.name() + " doesnt exist");
                }
            }
        }
        for (String parent_guid : asset.parent_guids) {

            Optional<Asset> parentOpt = getAsset(parent_guid);
            if (parentOpt.isEmpty()) {
                throw new IllegalArgumentException("Parent doesnt exist");
            }
        }

    }

    public Asset persistAsset(Asset asset, User user, int allocation) {
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        validateAssetFields(asset);
        if (assetsGettingCreated.getIfPresent(asset.asset_guid) != null) {
            logger.warn("Same asset uploaded in short time frame, guid: {}", asset.asset_guid);
            throw new DasscoIllegalActionException("Asset " + asset.asset_guid + " is already being processed");
        }
        //Validation
        Optional<Asset> assetOpt = getAsset(asset.asset_guid);
        if (assetOpt.isPresent()) {
            throw new IllegalArgumentException("Asset " + asset.asset_guid + " already exists");
        }
//        });
        if (allocation == 0) {
            throw new IllegalArgumentException("Allocation cannot be 0");
        }

        asset.updateUser = user.username;
        for(AssetSpecimen assetSpecimen : asset.asset_specimen) {
            Optional<Specimen> specimen = specimenService.findSpecimen(assetSpecimen.specimen_pid);
            if (specimen.isEmpty()) {
                throw new IllegalArgumentException("Specimen " + assetSpecimen.specimen_pid + " doesn't exist");
            }
            assetSpecimen.specimen = specimen.get();
        }
        rightsValidationService.requireWriteRights(user, asset);
        validateAndSetCollectionId(asset);
        validateNewAssetAndSetIds(asset);
        ensureValuesExists(asset);
        validateAsset(asset);
        // Create share
        assetsGettingCreated.put(asset.asset_guid, Instant.now());

        LocalDateTime httpInfoStart = LocalDateTime.now();
        logger.info("POSTing asset {} with parent {} to file-proxy", asset.asset_guid, asset.parent_guids);
        Asset resultAsset = jdbi.inTransaction(h -> {
            // Default values on creation
            asset.date_metadata_updated = Instant.now();
            asset.created_date = Instant.now();
            asset.internal_status = InternalStatus.METADATA_RECEIVED;
            AssetRepository assetRepository = h.attach(AssetRepository.class);
            EventRepository eventRepository = h.attach(EventRepository.class);
            UserRepository userRepository = h.attach(UserRepository.class);
            FundingRepository fundingRepository = h.attach(FundingRepository.class);
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);
            IssueRepository issueRepository = h.attach(IssueRepository.class);
            // Handle legality
            if (asset.legality != null) {
                LegalityRepository legalityRepository = h.attach(LegalityRepository.class);
                asset.legality = legalityRepository.insertLegality(asset.legality);
            }
            // Handle the asset itself
            assetRepository.insertBaseAsset(asset);
            // role_restrictions
            if (asset.role_restrictions != null && !asset.role_restrictions.isEmpty()) {
                RoleRepository attach = h.attach(RoleRepository.class);
                attach.setRestrictions(RestrictedObjectType.ASSET, asset.role_restrictions, asset.asset_guid);
            }
            // Handle funds
            for (String funding : asset.funding) {
                Funding funding1 = fundingService.ensureExists(funding);
                fundingRepository.fundAsset(asset.asset_guid, funding1.funding_id());
            }
            // Handle digitiser_list
            for (String s : asset.complete_digitiser_list) {
                User user1 = userService.ensureExists(new User(s));
                userRepository.addDigitiser(asset.asset_guid, user1.dassco_user_id);
            }
            // Handle specimen
            for (AssetSpecimen specimen : asset.asset_specimen) {
                Optional<Specimen> specimensByPID = specimenRepository.findSpecimensByPID(specimen.specimen_pid);
                if (specimensByPID.isEmpty()) {
                    throw new IllegalArgumentException("Specimen " + specimen.specimen_pid + " doesn't exist");
                } else {
                    Specimen existing = specimensByPID.get();
                    specimenRepository.attachSpecimen(asset.asset_guid, specimen.asset_preparation_type, existing.specimen_id());
                }

            }
            //Handle external publishers
            if (asset.external_publishers != null) {
                PublisherRepository publisherRepository = h.attach(PublisherRepository.class);
                // Publication can be POSTED as a list of objects with name only. this ensures the asset_guid is set.
                asset.external_publishers = asset.external_publishers.stream()
                        // Publication can be POSTed as a list of objects with name only. this ensures the asset_guid is set.
                        .map(p -> new Publication(asset.asset_guid, p.description(), p.name()))
                        .map(publisherRepository::internal_publish)
                        .toList();
            }

            //Handle parents
            for (String s : asset.parent_guids) {
                assetRepository.insert_parent_child(asset.asset_guid, s);
            }
            Integer pipelineId = null;
            String pipeline = asset.updating_pipeline != null ? asset.updating_pipeline : asset.pipeline;
            if (pipeline != null) {
                Optional<Pipeline> pipelineByInstitutionAndName = pipelineService.findPipelineByInstitutionAndName(pipeline, asset.institution);
                if (pipelineByInstitutionAndName.isPresent()) {
                    pipelineId = pipelineByInstitutionAndName.get().pipeline_id();
                }
            }
            //Issues
            if (asset.issues != null) {
                for (Issue issue : asset.issues) {
                    issue = new Issue(issue.issue_id()
                            , asset.asset_guid
                            , issue.category()
                            , issue.name()
                            , issue.timestamp() != null ? issue.timestamp() : Instant.now()
                            , issue.status()
                            , issue.description()
                            , issue.notes()
                            , issue.solved());
                    issueRepository.insert_issue(issue);
                }
            }
            //Event
            eventRepository.insertEvent(asset.asset_guid, DasscoEvent.CREATE_ASSET_METADATA, user.dassco_user_id, pipelineId);
//            LocalDateTime createAssetEnd = LocalDateTime.now();
//            logger.info("#5 Creating the asset took {} ms", Duration.between(createAssetStart, createAssetEnd).toMillis());
            // Open share
            try {
//                Observation.createNotStarted("persist:openShareOnFP", observationRegistry)
//                        .observe(() -> {
                asset.httpInfo = openHttpShare(new MinimalAsset(asset.asset_guid, asset.parent_guids, asset.institution, asset.collection), user, allocation);
//                        });
            } catch (Exception e) {
                h.rollback();
                throw new RuntimeException(e);
            }
            LocalDateTime httpInfoEnd = LocalDateTime.now();
            logger.info("#4 HTTPInfo creation took {} ms in total.", Duration.between(httpInfoStart, httpInfoEnd).toMillis());

            if (asset.httpInfo.http_allocation_status() == HttpAllocationStatus.SUCCESS) {

//                LocalDateTime refreshCachedDataStart = LocalDateTime.now();
//                //TEZT
//                Observation.createNotStarted("persist:refresh-statistics-cache", observationRegistry)
//                        .observe(statisticsDataServiceV2::refreshCachedData);
//                LocalDateTime refreshCachedDataEnd = LocalDateTime.now();
//                logger.info("#6 Refreshing the cached data took {} ms", Duration.between(refreshCachedDataStart, refreshCachedDataEnd).toMillis());

//            this.statisticsDataService.addAssetToCache(asset);
                refreshCaches(asset);
            } else {
                // Do not persist asset if share wasnt created
                h.rollback();
                assetsGettingCreated.invalidate(asset.asset_guid);
                return asset;
            }
            //you are here
            return asset;
        });

//        statisticsDataServiceV2.refreshCachedData();
//        this.statisticsDataServiceV2.addAssetToCache(asset);
        assetsGettingCreated.invalidate(asset.asset_guid);
        return resultAsset;
    }

    private void ensureValuesExists(Asset asset) {
        if (!Strings.isNullOrEmpty(asset.digitiser)) {
            User user1 = userService.ensureExists(new User(asset.digitiser));
            asset.digitiser_id = user1.dassco_user_id;
        }
        for (String list_user : asset.complete_digitiser_list) {
            userService.ensureExists(new User(list_user));
        }
        for (String funds : asset.funding) {
            fundingService.ensureExists(funds);
        }
        if(!asset.role_restrictions.isEmpty()) {
            Set<String> roles = roleService.getRoles();
            for(Role role: asset.role_restrictions){
                if(!roles.contains(role.name())){
                    roleService.addRole(role.name());
                }
            }
        }


    }

    public Optional<Asset> getAsset(String assetGuid, User user) {
        LocalDateTime getAssetStart = LocalDateTime.now();
        Optional<Asset> optionalAsset = getAsset(assetGuid);
        LocalDateTime getAssetEnd = LocalDateTime.now();
        logger.info("#4.1.2 Getting complete asset from the DB took {} ms", Duration.between(getAssetStart, getAssetEnd).toMillis());
        if (optionalAsset.isPresent()) {
            Asset found = optionalAsset.get();
            LocalDateTime checkValidationStart = LocalDateTime.now();
            rightsValidationService.checkReadRightsThrowing(user, found);
            LocalDateTime checkValidationEnd = LocalDateTime.now();
            logger.info("#4.1.3 Validating Asset took {} ms", Duration.between(checkValidationStart, checkValidationEnd).toMillis());
        }
        return optionalAsset;
    }

    public void refreshCaches(Asset asset) {
        LocalDateTime cacheStart = LocalDateTime.now();
        if (asset.digitiser != null && !asset.digitiser.isEmpty()) {
            logger.info("Adding Digitiser to Cache if absent in Persist Asset Method");
            digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(asset.digitiser, asset.digitiser));
        }

//        if (asset.specimens != null && !asset.specimens.isEmpty()) {
//            for (Specimen specimen : asset.specimens) {
//                specimen.preparation_types().forEach(preparationTypeCache::putPreparationTypesInCacheIfAbsent);
//            }
//        }

        if (asset.asset_subject != null && !asset.asset_subject.isEmpty()) {
            subjectCache.putSubjectsInCacheIfAbsent(asset.asset_subject);
        }

        if (asset.payload_type != null && !asset.payload_type.isEmpty()) {
            payloadTypeCache.putPayloadTypesInCacheIfAbsent(asset.payload_type);
        }
        LocalDateTime cacheEnd = LocalDateTime.now();
        logger.info("#7 Refreshing dropdown caches took {} ms", Duration.between(cacheStart, cacheEnd).toMillis());
    }

    public List<String> listSubjects() {
        return subjectCache.getSubjects();
    }

    public List<Digitiser> listDigitisers() {
        return digitiserCache.getDigitisers();
    }

    public List<String> listPayloadTypes() {
        return payloadTypeCache.getPayloadTypes();
    }

    public void reloadAssetCache() {
        subjectCache.clearCache();
        Set<String> subjectList = jdbi.withHandle(handle -> {
            AssetRepository assetRepository = handle.attach(AssetRepository.class);
            return extendableEnumService.getSubjects();
        });
        if (!subjectList.isEmpty()) {
            for (String subject : subjectList) {
                this.subjectCache.putSubjectsInCacheIfAbsent(subject);
            }
        }
        payloadTypeCache.clearCache();
//        List<String> payloadTypeList = jdbi.withHandle(handle -> {
//            AssetRepository assetRepository = handle.attach(AssetRepository.class);
//
    }

    //This is here for mocking
    public HttpInfo openHttpShare(MinimalAsset minimalAsset, User updateUser, int allocation) {
        return fileProxyClient.openHttpShare(minimalAsset, updateUser, allocation);
    }


    public Asset updateAsset(Asset updatedAsset, User user) {
        Optional<Asset> assetOpt = getAsset(updatedAsset.asset_guid);
        if (assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset " + updatedAsset.asset_guid + " does not exist");
        }
        rightsValidationService.requireWriteRights(user, assetOpt.get());
//        if (Strings.isNullOrEmpty(updatedAsset.updateUser)) {
//            throw new IllegalArgumentException("Update user must be provided");
//        }
        validateAsset(updatedAsset);
        validateAndSetCollectionId(updatedAsset);
        Asset existing = assetOpt.get();
        if (existing.internal_status.equals(InternalStatus.SPECIFY_SYNC_SCHEDULED)) {
            throw new DasscoIllegalActionException("Asset is synchronising to specify, please await completion");
        }
        Set<String> existingDigitiserList = new HashSet<>(existing.complete_digitiser_list);
        Set<String> existingFunding = new HashSet<>(existing.funding);
        Set<String> newFunding = new HashSet<>(updatedAsset.funding);
        Set<String> newDigitisers = new HashSet<>(updatedAsset.complete_digitiser_list);
        Set<String> existing_parents = new HashSet<>(existing.parent_guids);
//        Set<String> existing_specimens = existing.specimens.stream().map(x -> x.specimen_pid()).collect(Collectors.toSet());
        Set<Publication> existingPublications = new HashSet<>(existing.external_publishers);
        for (String funds : newFunding) {
            fundingService.ensureExists(funds);
        }
        Map<Integer, Issue> existing_issues = new HashMap<>();
        existing.issues.forEach(iss -> existing_issues.put(iss.issue_id(), iss));
        if (updatedAsset.digitiser != null && !updatedAsset.digitiser.equals(existing.digitiser)) {
            User digitiser = userService.ensureExists(new User(updatedAsset.digitiser));
            existing.digitiser_id = digitiser.dassco_user_id;
        }
        ensureValuesExists(updatedAsset);

        Set<String> updatedSpecimenPIDs = updatedAsset.asset_specimen.stream().map(a -> a.specimen_pid).collect(Collectors.toSet());
        List<AssetSpecimen> specimensToDetach = existing.asset_specimen.stream().filter(s -> !updatedSpecimenPIDs.contains(s.specimen_pid)).toList();
        updatedAsset.external_publishers = updatedAsset.external_publishers == null ? new ArrayList<>() : updatedAsset.external_publishers.stream().map(publication -> new Publication(publication.publication_id(), existing.asset_guid, publication.description(), publication.name())).toList();
        existing.collection_id = updatedAsset.collection_id;
//        existing.specimens = updatedAsset.specimens;
        existing.tags = updatedAsset.tags;
        existing.workstation = updatedAsset.workstation;
//        existing.pipeline = updatedAsset.pipeline;
        existing.date_asset_finalised = updatedAsset.date_asset_finalised;
        existing.status = updatedAsset.status;
        if (existing.asset_locked && !updatedAsset.asset_locked) {
            throw new DasscoIllegalActionException("Cannot unlock using updateAsset API, use dedicated API for unlocking");
        }
        existing.asset_locked = updatedAsset.asset_locked;
        existing.asset_subject = updatedAsset.asset_subject;
        existing.restricted_access = updatedAsset.restricted_access;
        existing.file_formats = updatedAsset.file_formats;
        existing.payload_type = updatedAsset.payload_type;
        existing.parent_guids = updatedAsset.parent_guids;
//        existing.updateUser = updatedAsset.updateUser;
        existing.asset_pid = updatedAsset.asset_pid == null ? existing.asset_pid : updatedAsset.asset_pid;
        existing.metadata_version = updatedAsset.metadata_version;
        existing.metadata_source = updatedAsset.metadata_source;
        existing.date_metadata_ingested = updatedAsset.date_metadata_ingested;
        existing.camera_setting_control = updatedAsset.camera_setting_control;
        existing.push_to_specify = updatedAsset.push_to_specify;
        existing.make_public = updatedAsset.make_public;
        existing.digitiser = updatedAsset.digitiser;
        existing.issues = updatedAsset.issues;
        existing.complete_digitiser_list = updatedAsset.complete_digitiser_list;
        existing.funding = updatedAsset.funding;
        existing.mos_id = updatedAsset.mos_id;
        existing.specify_attachment_remarks = updatedAsset.specify_attachment_remarks;
        existing.specify_attachment_title = updatedAsset.specify_attachment_title;
        // Currently we just add new subject types if they do not exist
        if (!Strings.isNullOrEmpty(existing.asset_subject) && !extendableEnumService.getSubjects().contains(existing.asset_subject)) {
            extendableEnumService.persistEnum(ExtendableEnumService.ExtendableEnum.SUBJECT, existing.asset_subject);
        }
        validateAssetFields(existing);
        jdbi.inTransaction(h -> {
            AssetRepository repository = h.attach(AssetRepository.class);
            EventRepository eventRepository = h.attach(EventRepository.class);
            SpecimenRepository specimenRepository = h.attach(SpecimenRepository.class);
            UserRepository userRepository = h.attach(UserRepository.class);
            FundingRepository fundingRepository = h.attach(FundingRepository.class);
            IssueRepository issueRepository = h.attach(IssueRepository.class);
            LegalityRepository legalityRepository = h.attach(LegalityRepository.class);
            PublisherRepository publisherRepository = h.attach(PublisherRepository.class);
            // Handle legality
            Long legalityToDelete = null;
            if (updatedAsset.legality != null) {
                if (existing.legality != null) {
                    Legality legality = new Legality(existing.legality.legality_id(), updatedAsset.legality.copyright(), updatedAsset.legality.license(), updatedAsset.legality.credit());
                    legalityRepository.updateLegality(legality);
                    existing.legality = legality;
                } else {
                    existing.legality = legalityRepository.insertLegality(updatedAsset.legality);
                }
            } else if (existing.legality != null) {
                legalityToDelete = existing.legality.legality_id();
                existing.legality = null;
            }
            //Role restrictions
            if (!Objects.equals(existing.role_restrictions, updatedAsset.role_restrictions) && updatedAsset.role_restrictions != null) {
                RoleRepository attach = h.attach(RoleRepository.class);
                attach.setRestrictions(RestrictedObjectType.ASSET, updatedAsset.role_restrictions, existing.asset_guid);
            }
            repository.update_asset_internal(existing);
            Optional<Pipeline> pipelineByInstitutionAndName = pipelineService.findPipelineByInstitutionAndName(updatedAsset.updating_pipeline, existing.institution);
            eventRepository.insertEvent(existing.asset_guid
                    , DasscoEvent.UPDATE_ASSET_METADATA
                    , user.dassco_user_id
                    , pipelineByInstitutionAndName.map(Pipeline::pipeline_id).orElse(null));
            //Specimens
            List<AssetSpecimen> finalSpecimen = new ArrayList<>();
            for (AssetSpecimen s : specimensToDetach) {
                //If a specify id is connected to the specimen we can first delete the conenction to the asset when specify is synchronized
                if (s.specify_collection_object_attachment_id != null) {
                    specimenRepository.detachSpecimen(existing.asset_guid, s.specimen_id);
                    //makde sure detached specimen is included in specify update
                    s.asset_detached = true;
                    finalSpecimen.add(s);
                } else {
                    specimenRepository.deleteAssetSpecimen(existing.asset_guid, s.specimen_id);
                }
            }

//            Set<String> pids = existing.specimens.stream().map(s -> s.specimen_pid()).collect(Collectors.toSet());
            Map<String, AssetSpecimen> pidExistingSpecimen = existing.asset_specimen.stream().collect(Collectors.toMap(s -> s.specimen_pid, s -> s));
            for (AssetSpecimen s : updatedAsset.asset_specimen) {

                if(!pidExistingSpecimen.containsKey(s.specimen_pid)) {
                    Optional<Specimen> specimen = specimenService.findSpecimen(s.specimen_pid);
                    if(specimen.isPresent()) {
                        s.specimen = specimen.get();
                        specimenRepository.attachSpecimen(updatedAsset.asset_guid, s.asset_preparation_type, specimen.get().specimen_id());
                    } else {
                        //this shouldnt happen
                        throw new RuntimeException("Specimen not found: " + s.specimen_pid);
                    }
                } else {
                    AssetSpecimen existingAssetSpecimen = pidExistingSpecimen.get(s.specimen_pid);
                    if(!existingAssetSpecimen.asset_preparation_type.equals(s.asset_preparation_type)) {
                        specimenRepository.updateAssetSpecimen(updatedAsset.asset_guid, existingAssetSpecimen.specimen_id, existingAssetSpecimen.specify_collection_object_attachment_id,s.asset_preparation_type);
                    }
                }
                finalSpecimen.add(s);
            }
            existing.asset_specimen = finalSpecimen;
            //Handle digitisers
            for (String s : existingDigitiserList) {
                if (!newDigitisers.contains(s)) {
                    User userRoRemove = userService.ensureExists(new User(s));
                    userRepository.removeFromDigitiserList(existing.asset_guid, userRoRemove.dassco_user_id);
                }
            }
            for (String s : newDigitisers) {
                if (!existingDigitiserList.contains(s)) {
                    User userToAdd = userService.ensureExists(new User(s));
                    userRepository.addDigitiser(existing.asset_guid, userToAdd.dassco_user_id);
                }
            }
            //Handle funding
            for (String s : existingFunding) {
                if (!newFunding.contains(s)) {
                    Optional<Funding> fundingIfExists = fundingService.getFundingIfExists(s);
                    fundingIfExists.ifPresent(funding -> fundingRepository.deFundAsset(existing.asset_guid, funding.funding_id()));
                }
            }
            for (String s : newFunding) {
                if (!existingFunding.contains(s)) {
                    Funding funding = fundingService.ensureExists(s);
                    fundingRepository.fundAsset(existing.asset_guid, funding.funding_id());
                }
            }
            //Handle parents
            for (String s : existing_parents) {
                if (!existing.parent_guids.contains(s)) {
                    repository.delete_parent_child(existing.asset_guid, s);
                }
            }
            for (String s : existing.parent_guids) {
                if (!existing_parents.contains(s)) {
                    repository.insert_parent_child(existing.asset_guid, s);
                }
            }
            //Handle publishers
            if (updatedAsset.external_publishers != null) {
                Set<Long> updatedPublishers = new HashSet<>();
                updatedAsset.external_publishers.forEach(extPbl -> {
                    if (extPbl.publication_id() != null) {
                        updatedPublishers.add(extPbl.publication_id());
                        publisherRepository.update(extPbl);
                    } else {
                        publisherRepository.internal_publish(extPbl);
                    }
                });
                existingPublications.forEach(publication -> {
                    if (!updatedPublishers.contains(publication.publication_id())) {
                        publisherRepository.delete(publication.publication_id());
                    }
                });
            }
            //Handle issues
            if (updatedAsset.issues != null) {
                Set<Integer> issue_ids = new HashSet<>();
                for (Issue issue : existing.issues) {
                    //Ensure time is set and asset id is correct
                    issue = new Issue(issue.issue_id()
                            , existing.asset_guid
                            , issue.category()
                            , issue.name()
                            , issue.timestamp() != null ? issue.timestamp() : Instant.now()
                            , issue.status()
                            , issue.description()
                            , issue.notes()
                            , issue.solved());
                    if (issue.issue_id() != null) {
                        issue_ids.add(issue.issue_id());
                        issueRepository.updateIssue(issue);
                    } else {
                        issueRepository.insert_issue(issue);
                    }
                }
                //remove remaining
                for (int i : existing_issues.keySet()) {
                    if (!issue_ids.contains(i)) {
                        issueRepository.deleteIssue(i);
                    }
                }
                if (legalityToDelete != null) {
                    legalityRepository.deleteLegality(legalityToDelete);
                }
            }
            return h;
        });

        this.assetSyncService.checkAndSync(existing);

        //TODO fix queries
//        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Update Asset Method");
//        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(updatedAsset.digitiser, updatedAsset.digitiser));

        return existing;
    }

    public void setIds(Asset asset) {
        Integer pipelineId = null;
        if (asset.pipeline != null) {
            Optional<Pipeline> pipelineByInstitutionAndName = pipelineService.findPipelineByInstitutionAndName(asset.pipeline, asset.institution);
            if (pipelineByInstitutionAndName.isPresent()) {
                pipelineId = pipelineByInstitutionAndName.get().pipeline_id();
            }
        }
        asset.updating_pipeline_id = pipelineId;
        Optional<Collection> collectionInternal = collectionService.findCollectionInternal(asset.collection, asset.institution);
        collectionInternal.ifPresent(collection -> asset.collection_id = collection.collection_id());

    }

    public boolean completeAsset(AssetUpdateRequest assetUpdateRequest, User user) {
        Optional<Asset> optAsset = getAsset(assetUpdateRequest.minimalAsset().asset_guid());
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = InternalStatus.ERDA_SYNCHRONISED;
        asset.error_message = null;
        asset.error_timestamp = null;
        Optional<Pipeline> optPipl = pipelineService.findPipelineByInstitutionAndName(assetUpdateRequest.pipeline(), asset.institution);


        jdbi.withHandle(h -> {
            AssetRepository assetRepository = h.attach(AssetRepository.class);
            EventRepository eventRepository = h.attach(EventRepository.class);
            assetRepository.updateAssetStatus(asset);
            eventRepository.insertEvent(asset.asset_guid
                    , DasscoEvent.CREATE_ASSET
                    , user.dassco_user_id
                    , optPipl.map(Pipeline::pipeline_id).orElse(null));
            return h;
        });

        //WP5a
//        statisticsDataServiceV2.refreshCachedData();


//        if (assetUpdateRequest.digitiser() != null && !assetUpdateRequest.digitiser().isEmpty()) {
//            logger.info("Adding Digitiser to Cache if absent in Complete Asset Method");
//            digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(assetUpdateRequest.digitiser(), assetUpdateRequest.digitiser()));
//        }
        return true;
    }

    public boolean auditAsset(User user, Audit audit, String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (Strings.isNullOrEmpty(audit.user())) {
            throw new IllegalArgumentException("Audit must have a user!");
        }
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        rightsValidationService.checkReadRightsThrowing(user, asset);
        // Asset cannot have work in progress when auditing
        if (!(InternalStatus.ERDA_SYNCHRONISED.equals(asset.internal_status) || InternalStatus.SPECIFY_SYNCHRONISED.equals(asset.internal_status))) {
            throw new DasscoIllegalActionException("Asset must be complete before auditing");
        }
        if (Objects.equals(asset.digitiser, audit.user())) {
            throw new DasscoIllegalActionException("Audit cannot be performed by the user who digitized the asset");
        }
        Event event = new Event(audit.user(), Instant.now(), DasscoEvent.AUDIT_ASSET, null);
        jdbi.onDemand(EventRepository.class).insertEvent(asset.asset_guid, DasscoEvent.AUDIT_ASSET, user.dassco_user_id, null);

        logger.info("Adding Digitiser to Cache if absent in Audit Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(audit.user(), audit.user()));

        return true;
    }

    public List<Event> getEvents(String assetGuid, User user) {
        //TODO find a way to check rights witout loading entire asset
        Optional<Asset> assetOpt = this.getAsset(assetGuid);
        if (assetOpt.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist");
        }
        Asset asset = assetOpt.get();
        rightsValidationService.checkReadRightsThrowing(user, asset);
        return jdbi.onDemand(EventRepository.class).getAssetEvents(assetGuid);
    }


    // The upload of files to file proxy is completed. The asset is now awaiting ERDA synchronization.
    public boolean completeUpload(AssetUpdateRequest assetSmbRequest, User user) {
        if (assetSmbRequest.minimalAsset() == null) {
            throw new IllegalArgumentException("Asset cannot be null");
        }
        Optional<Asset> optAsset = getAsset(assetSmbRequest.minimalAsset().asset_guid());
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }

        // Mark as asset received
        Asset asset = optAsset.get();
        rightsValidationService.requireWriteRights(user, asset);
        if (asset.asset_locked) {
            throw new DasscoIllegalActionException("Asset is locked");
        }
        asset.internal_status = InternalStatus.ASSET_RECEIVED;
        // Close samba and sync ERDA;
        jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
        statisticsDataServiceV2.refreshCachedData();

        logger.info("Adding Digitiser to Cache if absent in Complete Upload Asset Method");
        digitiserCache.putDigitiserInCacheIfAbsent(new Digitiser(user.username, user.username));
        // when asset is completed, it's going to be synced to Specify
        assetSyncService.sendAssetToQueue(new ARSUpdate(asset));
        return true;
    }

    private static final Set<InternalStatus> permitted_statuses = Set.of(InternalStatus.ERDA_FAILED, InternalStatus.SPECIFY_SYNC_FAILED, InternalStatus.ASSET_RECEIVED);
    private static final Set<InternalStatus> errorStatuses = Set.of(InternalStatus.ERDA_FAILED, InternalStatus.SPECIFY_SYNC_FAILED);

    public boolean setAssetStatus(String assetGuid, String status, String errorMessage) {
        InternalStatus assetStatus = null;
        try {
            assetStatus = InternalStatus.valueOf(status);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        if (!permitted_statuses.contains(assetStatus)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.internal_status = assetStatus;
        asset.error_message = errorMessage;
        if (errorStatuses.contains(assetStatus)) {
            asset.error_timestamp = Instant.now();
        }
        jdbi.onDemand(AssetRepository.class)
                .updateAssetNoEvent(asset);

        statisticsDataServiceV2.refreshCachedData();
        return true;
    }

    public void deleteAssetMetadata(String assetGuid, User user) {
        // Check that the asset exists:
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }

        // Close the share if open:
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fileProxyConfiguration.url() + "/shares/assets/" + assetGuid + "/deleteShare"))
                .header("Authorization", "Bearer " + user.token)
                .DELETE()
                .build();


        try (HttpClient httpClient = createHttpClient();) {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 404) {
                Asset asset = optAsset.get();
                rightsValidationService.requireWriteRights(user, asset.institution, asset.collection);
                jdbi.onDemand(AssetRepository.class).deleteAsset(assetGuid);
                // Make sure deleted funding is removed from cache
                fundingService.forceRefreshCache();
                // Refresh cache:
                reloadAssetCache();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteAsset(String assetGuid, User user) {
        String userId = user.username;
        if (Strings.isNullOrEmpty(userId)) {
            throw new IllegalArgumentException("User is null");
        }
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        rightsValidationService.checkRightsAsset(user, asset, true);
        if (asset.asset_locked) {
            throw new DasscoIllegalActionException("Asset is locked");
        }
        if (asset.date_asset_deleted != null) {
            throw new IllegalArgumentException("Asset is already deleted");
        }

        jdbi.onDemand(EventRepository.class).insertEvent(asset.asset_guid, DasscoEvent.DELETE_ASSET_METADATA, user.dassco_user_id, null);

        Optional<AssetSpecimen> specimenWithSpecifyId = asset.asset_specimen.stream().filter(specimen -> specimen.specify_collection_object_attachment_id != null).findAny();
        if (specimenWithSpecifyId.isPresent()) {
            assetSyncService.syncAsset(asset.asset_guid);
        }

        return true;
    }

    public boolean unlockAsset(String assetGuid) {
        Optional<Asset> optAsset = getAsset(assetGuid);
        if (optAsset.isEmpty()) {
            throw new IllegalArgumentException("Asset doesnt exist!");
        }
        Asset asset = optAsset.get();
        asset.asset_locked = false;
        jdbi.onDemand(AssetRepository.class).updateAssetNoEvent(asset);
        return true;
    }

    // For testing
    public HttpClient createHttpClient() {
        return HttpClient.newHttpClient();
    }

}
