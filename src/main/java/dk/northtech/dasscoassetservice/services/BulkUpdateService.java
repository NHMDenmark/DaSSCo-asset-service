package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.DigitiserRepository;
import dk.northtech.dasscoassetservice.repositories.FundingRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BulkUpdateService {

    private static final Logger logger =
            LoggerFactory.getLogger(BulkUpdateService.class);

    private final RightsValidationService rightsValidationService;
    private final AssetService assetService;
    private final Jdbi jdbi;

    @Inject
    public BulkUpdateService(
            Jdbi jdbi,
            RightsValidationService rightsValidationService,
            AssetService assetService
    ) {
        this.jdbi = jdbi;
        this.rightsValidationService = rightsValidationService;
        this.assetService = assetService;
    }

    public List<Digitiser> getDigitisers() {
        return jdbi.withHandle(h -> h.attach(DigitiserRepository.class).listDigitisers());
    }
    public List<Funding> getFunding() {
        return jdbi.withHandle(h -> h.attach(FundingRepository.class).listFunds());
    }
    public List<String> getSubjects() {
        return jdbi.withHandle(h -> h.createQuery("SELECT subject FROM subject ORDER BY subject").mapTo(String.class).list());
    }
    public List<String> getRoles() {
        return jdbi.withHandle(h -> h.createQuery("SELECT role from role ORDER BY role").mapTo(String.class).list());
    }
    public List<String> getIssueCategories() {
        return jdbi.withHandle(h -> h.createQuery("SELECT issue_category from issue_category ORDER BY issue_category").mapTo(String.class).list());
    }

    public List<String> getStatuses() {
        return jdbi.withHandle(h -> h.createQuery("SELECT asset_status FROM asset_status order by asset_status").mapTo(String.class).list());
    }

    public List<Map<String, Object>> getGroupedIssues(List<String> assetGuids) {
        List<Issue> issues = listIssuesByAssetGuids(assetGuids);

        // Key for grouping
        record IssueKey(
                String category,
                String name,
                String description,
                String status,
                Boolean solved,
                String notes
        ) {}

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

    public List<Asset> bulkUpdate(List<String> assetList, Asset updatedAsset, User user) {
        // Placeholder for your future implementation
        logger.info("Bulk update requested for {} assets by user {}", assetList.size(), user.username);
        // TODO: validate rights, update assets, etc.
        return List.of();
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
        String headers = String.join(",",  Arrays.stream(fields).map(Field::getName).collect(Collectors.toList()));
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