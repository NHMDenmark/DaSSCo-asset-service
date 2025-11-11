package dk.northtech.dasscoassetservice.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.domain.bulkupdatepayload.*;
import dk.northtech.dasscoassetservice.repositories.*;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.SecurityContext;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BulkUpdateService {

    private static final Logger logger =
            LoggerFactory.getLogger(BulkUpdateService.class);

    private final RightsValidationService rightsValidationService;
    private final AssetService assetService;
    private final UserService userService;
    private final Jdbi jdbi;
    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(10)
            .build();

    @Inject
    public BulkUpdateService(
            Jdbi jdbi,
            RightsValidationService rightsValidationService,
            AssetService assetService,
            UserService userService
    ) {
        this.jdbi = jdbi;
        this.rightsValidationService = rightsValidationService;
        this.assetService = assetService;
        this.userService = userService;
    }

    // Generic caching helper with TTL
    @SuppressWarnings("unchecked")
    private <T> T getCached(String key, java.util.function.Supplier<T> loader) {
        return (T) cache.get(key, k -> {
            logger.debug("Cache miss for '{}', loading from DB", k);
            return loader.get();
        });
    }

    public List<Digitiser> getDigitisers() {
        return getCached("digitisers", () ->
                jdbi.withHandle(h -> h.attach(DigitiserRepository.class).listDigitisers())
        );
    }

    public List<Funding> getFunding() {
        return getCached("funding", () ->
                jdbi.withHandle(h -> h.attach(FundingRepository.class).listFunds())
        );
    }

    public List<String> getSubjects() {
        return getCached("subjects", () ->
                jdbi.withHandle(h ->
                        h.createQuery("SELECT subject FROM subject ORDER BY subject")
                                .mapTo(String.class)
                                .list())
        );
    }

    public List<String> getRoles() {
        return getCached("roles", () ->
                jdbi.withHandle(h ->
                        h.createQuery("SELECT role from role ORDER BY role")
                                .mapTo(String.class)
                                .list())
        );
    }

    public List<String> getIssueCategories() {
        return getCached("issueCategories", () ->
                jdbi.withHandle(h ->
                        h.createQuery("SELECT issue_category from issue_category ORDER BY issue_category")
                                .mapTo(String.class)
                                .list())
        );
    }

    public List<String> getStatuses() {
        return getCached("statuses", () ->
                jdbi.withHandle(h ->
                        h.createQuery("SELECT asset_status FROM asset_status order by asset_status")
                                .mapTo(String.class)
                                .list())
        );
    }

    public List<Map<String, Object>> getGroupedIssues(List<String> assetGuids, SecurityContext securityContext) {
        User user = this.userService.from(securityContext);
        List<Asset> assets = this.assetService.getAssets(assetGuids);
        assets.forEach(asset -> {
            this.rightsValidationService.checkWriteRights(user, asset.institution, asset.collection);
        });
        List<Issue> issues = listIssuesByAssetGuids(assetGuids);

        // Key for grouping
        record IssueKey(
                String category,
                String name,
                String description,
                String status,
                Boolean solved,
                String notes
        ) {
        }

        Map<IssueKey, List<Issue>> grouped = issues.stream()
                .collect(Collectors.groupingBy(
                        i -> new IssueKey(
                                i.category(),
                                i.name(),
                                i.description(),
                                i.status(),
                                i.solved(),
                                i.notes()
                        )
                ));

        // Build a structured list your frontend can easily render
        List<Map<String, Object>> groupedList = new ArrayList<>();
        for (var entry : grouped.entrySet()) {
            IssueKey k = entry.getKey();
            List<Issue> group = entry.getValue();

            groupedList.add(Map.of(
                    "category", k.category(),
                    "name", k.name(),
                    "description", k.description(),
                    "status", k.status(),
                    "solved", k.solved(),
                    "notes", k.notes(),
                    "issueIds", group.stream().map(Issue::issue_id).toList(),
                    "assetGuids", group.stream().map(Issue::asset_guid).distinct().toList(),
                    "count", group.size()
            ));
        }

        return groupedList;
    }

    public List<Issue> listIssuesByAssetGuids(List<String> assetGuids) {
        return jdbi.withHandle(h ->
                h.createQuery("""
                                SELECT issue_id,
                                       asset_guid,
                                       category,
                                       name,
                                       description,
                                       status,
                                       solved,
                                       notes,
                                       timestamp
                                  FROM issue
                                 WHERE asset_guid IN (<assetGuids>)
                                """)
                        .bindList("assetGuids", assetGuids)
                        .mapTo(Issue.class)
                        .list()
        );
    }

    public List<Map<String, Object>> getGroupedDigitisers(List<String> assetGuids, SecurityContext securityContext) {
        User user = this.userService.from(securityContext);
        List<Asset> assets = this.assetService.getAssets(assetGuids);
        assets.forEach(asset -> {
            this.rightsValidationService.checkWriteRights(user, asset.institution, asset.collection);
        });
        List<DigitiserLink> links = jdbi.withHandle(h ->
                h.attach(DigitiserListRepository.class)
                        .listDigitisersByAssetGuids(assetGuids)
        );

        record DigitiserKey(String dassco_user_id, String username) {
        }

        Map<DigitiserKey, List<DigitiserLink>> grouped = links.stream()
                .collect(Collectors.groupingBy(
                        l -> new DigitiserKey(l.dassco_user_id(), l.username())
                ));

        List<Map<String, Object>> groupedList = new ArrayList<>();

        for (var entry : grouped.entrySet()) {
            DigitiserKey key = entry.getKey();
            List<DigitiserLink> list = entry.getValue();

            groupedList.add(Map.of(
                    "dasscoUserId", key.dassco_user_id(),
                    "username", key.username(),
                    "digitiserListIds", list.stream().map(DigitiserLink::digitiser_list_id).toList(),
                    "assetGuids", list.stream().map(DigitiserLink::asset_guid).distinct().toList(),
                    "count", list.size()
            ));
        }

        return groupedList;
    }

    public UUID processBulkUpdate(BulkUpdatePayload payload, SecurityContext securityContext) {
        UUID bulkUpdateUuid = UUID.randomUUID();
        User user = userService.from(securityContext);


        List<Asset> assets = assetService.getAssets(payload.assetGuids());
        if (assets.isEmpty()) {
            throw new NotFoundException("No assets found");
        }
        assets.forEach(asset -> {
            this.rightsValidationService.checkWriteRights(user, asset.institution, asset.collection);
        });


        jdbi.useHandle(handle -> {
            patchAssetFields(handle, payload.assetGuids(), payload.fields(), payload.legality(), bulkUpdateUuid);
            handleIssueActions(handle, payload.assetGuids(), payload.issues(), bulkUpdateUuid);
            handleDigitiserActions(handle, payload.assetGuids(), payload.digitisers(), bulkUpdateUuid);

            if (payload.fields() != null
                    && Boolean.TRUE.equals(payload.fields().get("audited"))) {
                auditAssets(user, new Audit(user.username), assets, handle, bulkUpdateUuid);
            }
            createBulkUpdateEvents(handle, payload.assetGuids(), bulkUpdateUuid);
        });


        return bulkUpdateUuid;
    }

    private void createBulkUpdateEvents(Handle handle,
                                        List<String> assetGuids,
                                        UUID bulkUpdateUuid) {

        if (assetGuids == null || assetGuids.isEmpty()) {
            logger.debug("No assets provided to create bulk‑update events for {}", bulkUpdateUuid);
            return;
        }

        logger.info("Creating {} bulk‑update events (UUID={})", assetGuids.size(), bulkUpdateUuid);

        String sql = """
                INSERT INTO event (asset_guid, event, bulk_update_uuid, timestamp)
                VALUES (:assetGuid, :event, :bulkUpdateUuid, NOW())
                """;

        var batch = handle.prepareBatch(sql);

        for (String assetGuid : assetGuids) {
            batch.bind("assetGuid", assetGuid)
                    .bind("event", DasscoEvent.BULK_UPDATE_ASSET_METADATA.name())
                    .bind("bulkUpdateUuid", bulkUpdateUuid)
                    .add();
        }

        batch.execute();

        logger.info("Created bulk‑update events for {} assets (UUID={})", assetGuids.size(), bulkUpdateUuid);
    }


    private void handleDigitiserActions(Handle handle,
                                        List<String> assetGuids,
                                        DigitiserPatchBlock digitiserPatchBlock,
                                        UUID bulkUpdateUuid) {

        if (digitiserPatchBlock == null) {
            logger.debug("No digitiser actions to process for bulk update {}", bulkUpdateUuid);
            return;
        }

        var digiRepo = handle.attach(DigitiserListRepository.class);

        // ---------- ADD ----------
        if (digitiserPatchBlock.add() != null && !digitiserPatchBlock.add().isEmpty()) {
            logger.info("Bulk update {}: adding {} new digitiser links",
                    bulkUpdateUuid, digitiserPatchBlock.add().size());

            for (DigitiserAddition addition : digitiserPatchBlock.add()) {
                if (addition.dasscoUserId() == null) {
                    logger.warn("Skipping digitiser addition with null user ID: {}", addition);
                    continue;
                }

                // Use provided asset list, fallback to global list if missing
                List<String> targets = (addition.assetGuids() != null && !addition.assetGuids().isEmpty())
                        ? addition.assetGuids()
                        : assetGuids;

                for (String assetGuid : targets) {
                    digiRepo.insertLink(addition.dasscoUserId(), assetGuid);
                }
            }
        }

        // ---------- DELETE ----------
        if (digitiserPatchBlock.delete() != null && !digitiserPatchBlock.delete().isEmpty()) {
            logger.info("Bulk update {}: deleting {} digitiser links",
                    bulkUpdateUuid, digitiserPatchBlock.delete().size());

            handle.createUpdate("DELETE FROM digitiser_list WHERE digitiser_list_id IN (<ids>)")
                    .bindList("ids", digitiserPatchBlock.delete())
                    .execute();
        }

        logger.info("Finished digitiser operations for bulk update {}", bulkUpdateUuid);
    }

    private void handleIssueActions(Handle handle,
                                    List<String> assetGuids,
                                    IssuePatchBlock issuePatchBlock,
                                    UUID bulkUpdateUuid) {

        if (issuePatchBlock == null) {
            logger.debug("No issues to process for bulk update {}", bulkUpdateUuid);
            return;
        }

        var issueRepo = handle.attach(IssueRepository.class);

        // ---------- ADD ----------
        if (issuePatchBlock.add() != null && !issuePatchBlock.add().isEmpty()) {
            logger.info("Bulk update {}: adding {} new issues",
                    bulkUpdateUuid, issuePatchBlock.add().size());

            for (IssueAddition addition : issuePatchBlock.add()) {


                for (String assetGuid : assetGuids) {
                    issueRepo.insert_issue(
                            new Issue(
                                    assetGuid,
                                    addition.category(),
                                    addition.name(),
                                    Instant.now(),
                                    addition.description(),
                                    addition.status(),
                                    addition.notes(),
                                    addition.solved()
                            )
                    );
                }
            }
        }

        // ---------- UPDATE ----------
        if (issuePatchBlock.update() != null && !issuePatchBlock.update().isEmpty()) {
            logger.info("Bulk update {}: updating {} existing issues",
                    bulkUpdateUuid, issuePatchBlock.update().size());

            for (IssueUpdate update : issuePatchBlock.update()) {
                if (update.issueIds() == null || update.issueIds().isEmpty()) {
                    continue;
                }

                Map<String, Object> values = update.values();
                if (values == null || values.isEmpty()) {
                    continue;
                }

                // build a dynamic SET clause from provided keys
                String setClause = values.keySet().stream()
                        .map(k -> k + " = :" + k)
                        .collect(Collectors.joining(", "));

                String sql = "UPDATE issue SET " + setClause + " WHERE issue_id IN (<ids>)";

                var q = handle.createUpdate(sql).bindList("ids", update.issueIds());
                values.forEach(q::bind);
                q.execute();
            }
        }

        // ---------- DELETE ----------
        if (issuePatchBlock.delete() != null && !issuePatchBlock.delete().isEmpty()) {
            logger.info("Bulk update {}: deleting {} issues",
                    bulkUpdateUuid, issuePatchBlock.delete().size());

            handle.createUpdate("DELETE FROM issue WHERE issue_id IN (<ids>)")
                    .bindList("ids", issuePatchBlock.delete())
                    .execute();
        }
        logger.info("Finished issue operations for bulk update {}", bulkUpdateUuid);
    }

    private void patchAssetFields(Handle handle,
                                  List<String> assetGuids,
                                  Map<String, Object> fields,
                                  Optional<Legality> legality,
                                  UUID bulkUpdateUuid) {

        // ✅ Null/empty guard (parentheses fix operator precedence)
        if (((fields == null || fields.isEmpty()) && (legality == null || legality.isEmpty())) ||
                assetGuids == null || assetGuids.isEmpty()) {
            logger.debug("No fields or legality to patch in bulk update {}", bulkUpdateUuid);
            return;
        }

        // ✅ Work on a copy to avoid mutating caller map
        Map<String, Object> sqlFields = new LinkedHashMap<>();
        if (fields != null) {
            sqlFields.putAll(fields);
        }

        // ✅ Remove non‑column or special keys
        sqlFields.remove("audited");

        // ✅ Optionally insert new legality and include legality_id
        legality.ifPresent(l -> {
            Legality created = handle.attach(LegalityRepository.class).insertLegality(l);
            if (created != null && created.legality_id() != null) {
                sqlFields.put("legality_id", created.legality_id());
                logger.info("Bulk update {}: inserted new legality ID {} and linked to assets",
                        bulkUpdateUuid, created.legality_id());
            }
        });

        if (sqlFields.isEmpty()) {
            logger.debug("No patchable fields for bulk update {}", bulkUpdateUuid);
            return;
        }

        // ✅ Construct dynamic SQL safely
        String setClause = sqlFields.keySet().stream()
                .map(k -> k + " = :" + k)
                .collect(Collectors.joining(", "));

        String sql = "UPDATE asset SET " + setClause + " WHERE asset_guid IN (<assetGuids>)";

        var update = handle.createUpdate(sql)
                .bindList("assetGuids", assetGuids);

        sqlFields.forEach(update::bind);

        int affected = update.execute();
        logger.info("Bulk update {}: updated {} asset row(s) with fields {}",
                bulkUpdateUuid, affected, sqlFields.keySet());
    }

    private void auditAssets(User user,
                             Audit audit,
                             List<Asset> assets,
                             Handle handle,
                             UUID bulkUpdateUuid) {

        String sql = """
                INSERT INTO event (asset_guid, event, dassco_user_id, bulk_update_uuid)
                VALUES (:assetGuid, :event, :userId, :bulkUpdateUuid)
                """;

        var batch = handle.prepareBatch(sql);

        for (Asset asset : assets) {
            if (!(InternalStatus.ERDA_SYNCHRONISED.equals(asset.internal_status)
                    || InternalStatus.SPECIFY_SYNCHRONISED.equals(asset.internal_status))) {
                throw new DasscoIllegalActionException(
                        "Asset must be complete before auditing: " + asset.asset_guid);
            }

            if (Objects.equals(asset.digitiser, audit.user())) {
                throw new DasscoIllegalActionException(
                        "Audit cannot be performed by the user who digitized asset: " + asset.asset_guid);
            }

            batch.bind("assetGuid", asset.asset_guid)
                    .bind("event", DasscoEvent.AUDIT_ASSET.name())
                    .bind("userId", user.dassco_user_id)
                    .bind("bulkUpdateUuid", bulkUpdateUuid)
                    .add();
        }

        batch.execute();
        logger.info("Audit: created AUDIT_ASSET events for {} assets under bulk update {}", assets.size(), bulkUpdateUuid);
    }


    public List<Asset> readMultipleAssetsSQL(List<String> assetGuids) {
        return this.assetService.getAssets(assetGuids);
    }

    public String createCSVString(List<Asset> assets) {
        String csv = "";
        if (assets.isEmpty()) {
            return "";
        }
        StringBuilder csvBuilder = new StringBuilder();
        Field[] fields = Asset.class.getDeclaredFields();
        String headers = String.join(",", Arrays.stream(fields).map(Field::getName).collect(Collectors.toList()));
        csvBuilder.append(headers).append("\n");
        for (Asset asset : assets) {
            StringJoiner joiner = new StringJoiner(",");
            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object value = field.get(asset);
                    joiner.add(escapeCsvValue(value != null ? value.toString() : ""));
                } catch (IllegalAccessException e) {
                    joiner.add("");
                }
            }
            csvBuilder.append(joiner).append("\n");
        }
        return csvBuilder.toString();
    }

    public String escapeCsvValue(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}