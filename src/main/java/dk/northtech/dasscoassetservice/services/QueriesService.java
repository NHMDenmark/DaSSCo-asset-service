package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.AssetGroupRepository;
import dk.northtech.dasscoassetservice.repositories.AssetRepository;
import dk.northtech.dasscoassetservice.repositories.InternalStatusRepository;
import dk.northtech.dasscoassetservice.repositories.QueriesRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueriesService {
    private static final Logger logger = LoggerFactory.getLogger(QueriesService.class);
    private Jdbi jdbi;
    private RightsValidationService rightsValidationService;

    List<String> propertiesTimestamps = Arrays.asList("created_timestamp", "updated_timestamp", "audited_timestamp");
    List<String> propertiesDigitiser = Arrays.asList("asset_created_by", "asset_deleted_by", "asset_updated_by", "audited_by");

    String assetSql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset) ${asset:-}
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                         MATCH (e:Event)<-[:CHANGED_BY]-(a)
                         MATCH (u:User)<-[:INITIATED_BY]-(e)
                         ${assetEvents:-WHERE e.event = 'CREATE_ASSET_METADATA'}
                         MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                         ${instCollAccess:-}
                         OPTIONAL MATCH (p:Pipeline)<-[:USED]-(e) ${pipeline:-}
                         OPTIONAL MATCH (w:Workstation)<-[:USED]-(e) ${workstation:-}
                         OPTIONAL MATCH (s:Specimen)-[sss:USED_BY]->(a) ${specimen:-}
                         OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)

                          RETURN a.asset_guid
                              , a.asset_pid
                              , a.status
                              , a.multi_specimen
                              , a.funding
                              , a.subject
                              , a.payload_type
                              , a.file_formats
                              , a.asset_taken_date
                              , a.internal_status
                              , a.asset_locked
                              , pa.asset_guid AS parent_guid
                              , a.restricted_access
                              , a.tags
                              , a.error_message
                              , a.error_timestamp
                              , collect(s)
                              , i.name AS institution_name
                              , c.name AS collection_name
                              , p.name AS pipeline_name
                              , w.name AS workstation_name
                              , e.timestamp AS creation_date
                              , a.date_asset_finalised
                              , u.name AS user_name
                              , a.date_metadata_taken
                              , a.date_asset_taken
                         LIMIT ${limit:-200}
                      $$)
                    AS (asset_guid agtype
                    , asset_pid agtype
                    , status agtype
                    , multi_specimen agtype
                    , funding agtype
                    , subject agtype
                    , payload_type agtype
                    , file_formats agtype
                    , asset_taken_date agtype
                    , internal_status agtype
                    , asset_locked agtype
                    , parent_guid agtype
                    , restricted_access agtype
                    , tags agtype
                    , error_message agtype
                    , error_timestamp agtype
                    , specimens agtype
                    , institution_name agtype
                    , collection_name agtype
                    , pipeline_name agtype
                    , workstation_name agtype
                    , creation_date agtype
                    , date_asset_finalised agtype
                    , user_name agtype
                    , date_metadata_taken agtype
                    , date_asset_taken agtype);
                  """;

    String assetCountSql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset) ${asset:-}
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                         MATCH (e:Event)<-[:CHANGED_BY]-(a)
                         MATCH (u:User)<-[:INITIATED_BY]-(e)
                         ${assetEvents:-WHERE e.event = 'CREATE_ASSET_METADATA'}
                         MATCH (p:Pipeline)<-[:USED]-(e) ${pipeline:-}
                         MATCH (w:Workstation)<-[:USED]-(e) ${workstation:-}
                         MATCH (i:Institution)<-[:BELONGS_TO]-(a)
                         ${instCollAccess:-}
                         OPTIONAL MATCH (s:Specimen)-[sss:USED_BY]->(a) ${specimen:-}
                         OPTIONAL MATCH (a)-[:CHILD_OF]->(pa:Asset)
                         RETURN count(DISTINCT a) as count
                         LIMIT ${limit:-200}
                      $$)
                    as (count agtype);
                  """;

    @Inject
    public QueriesService(RightsValidationService rightsValidationService, Jdbi jdbi) {
        this.rightsValidationService = rightsValidationService;
        this.jdbi = jdbi;
    }

    public Map<String, List<String>> getNodeProperties() {
        Map<String, List<String>> properties = jdbi.onDemand(QueriesRepository.class).getNodeProperties();
        properties.get("Asset").addAll(propertiesTimestamps);
        properties.get("Asset").addAll(propertiesDigitiser);
        return properties;
    }

    public int getAssetCountFromQuery(List<QueriesReceived> queries, int limit, User user) {
        Set<String> collectionsAccess = null; // only need collection, really, as it's the deepest access check (we check for institute rights in the function if necessary, too)
        if (!checkRights(user)) {
            collectionsAccess = this.rightsValidationService.getCollectionsReadRights(user.roles);
        }
        String query = unwrapQuery(queries, limit, true, collectionsAccess);
        return jdbi.onDemand(QueriesRepository.class).getAssetCountFromQuery(query);
    }

    public List<Asset> getAssetsFromQuery(List<QueriesReceived> queries, int limit, User user) {
        Set<String> collectionsAccess = null; // only need collection, really, as it's the deepest access check (we check for institute rights in the function if necessary, too)
        if (!checkRights(user)) {
            collectionsAccess = this.rightsValidationService.getCollectionsReadRights(user.roles);
        }

        String query = unwrapQuery(queries, limit, false, collectionsAccess);
        if (query == null || StringUtils.isBlank(query)) return new ArrayList<>();

        logger.info("Getting assets from query.");
        System.out.println(query);
        List<Asset> assets = jdbi.onDemand(QueriesRepository.class).getAssetsFromQuery(query);
        // gotta do the following to avoid duplicates in case the query returns an asset with multiple events.
        Map<String, Long> assetCountMap = assets.stream()
                .collect(Collectors.groupingBy(Asset::getAsset_guid, Collectors.counting()));

        Set<String> duplicatedAssetGuids = assetCountMap.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Asset> distinctAssets = new ArrayList<>(new HashSet<>(assets)); // object hash has been set so that not all properties are checked here

        distinctAssets.stream()
                .filter(asset -> duplicatedAssetGuids.contains(asset.asset_guid))
                .forEach(asset -> asset.events = jdbi.onDemand(AssetRepository.class).readEvents_internal(asset.asset_guid));

        return distinctAssets;
    }

    public String unwrapQuery(List<QueriesReceived> queries, int limit, boolean count, Set<String> collectionAccess) {
        for (QueriesReceived received : queries) {
            String finalQuery;
            Map<String, String> whereMap = new HashMap<>();
            whereMap.put("limit", Integer.toString(limit));
            String collectionString = "";
            String instutionString = "";

            for (Query query: received.query) {
                if (query.select.equalsIgnoreCase("Asset")) { // a
                    String eventTimestamps = checkForEventUserProperties(query.where); // cos event timestamps are set as if it belongs to the asset
                    if (!StringUtils.isBlank(eventTimestamps)) {
                        whereMap.put("assetEvents", "\nWHERE (" + eventTimestamps + ")");
                    }
                    String where = joinFields(query.where, "a");
                    if (!StringUtils.isBlank(where)) whereMap.put("asset", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Institution")) { // i
                    instutionString = joinFields(query.where, "i");
                }
                if (query.select.equalsIgnoreCase("Workstation")) { // w
                    String where = joinFields(query.where, "w");
                    if (!StringUtils.isBlank(where)) whereMap.put("workstation", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Pipeline")) { // p
                    String where = joinFields(query.where, "p");
                    if (!StringUtils.isBlank(where)) whereMap.put("pipeline", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Collection"))  { // c
                    collectionString = joinFields(query.where, "c");
                }
                if (query.select.equalsIgnoreCase("Specimen")) { // s
                    String where = joinFields(query.where, "s");
                    if (!StringUtils.isBlank(where)) whereMap.put("specimen", "\nWHERE (" + where + ")");
                }
            }
            if (collectionAccess != null) {
                String collections = String.join(", ", collectionAccess.stream().map(coll -> "'" + coll + "'").toList());
                StringJoiner orJoiner = new StringJoiner(" or ");

                if (!StringUtils.isBlank(instutionString)) {
                    orJoiner.add("(" + instutionString + " AND c.name IN [" + collections + "])");
                }
                if (!StringUtils.isBlank(collectionString)) {
                    orJoiner.add("(" + collectionString + " AND c.name IN [" + collections + "])");
                }
                if (StringUtils.isBlank(instutionString) && StringUtils.isBlank(collectionString)) {
                    orJoiner.add("(c.name IN [" + collections + "])");
                }
                whereMap.put("instCollAccess", "WHERE " + orJoiner.toString());
            } else {
                logger.warn("User does not have access to any collections, and no assets will be returned.");
                return null;
            }

            StringSubstitutor substitutor = new StringSubstitutor(whereMap);
            if (count) finalQuery = substitutor.replace(assetCountSql);
            else finalQuery = substitutor.replace(assetSql);
            return finalQuery;
        }
        return null;
    }

    public String joinFields(List<QueryWhere> wheres, String match) {
        StringJoiner orJoiner = new StringJoiner(" or ");

        for (QueryWhere where : wheres) {
            String property = where.property;
            for (QueryInner inner : where.fields) {
                orJoiner.add(inner.toBasicQueryString(match, property, inner.dataType));
            }
        }
        return orJoiner.toString();
    }

    public String checkForEventUserProperties(List<QueryWhere> wheres) {
        StringJoiner orJoiner = new StringJoiner(" or ");
        StringJoiner createdJoiner = new StringJoiner(" and ");
        StringJoiner updatedJoiner = new StringJoiner(" and ");
        StringJoiner auditedJoiner = new StringJoiner(" and ");
        List<QueryWhere> toRemove = new ArrayList<>();
        

        for (QueryWhere where : wheres) {
            if (propertiesDigitiser.contains(where.property)) {
                if (where.property.equalsIgnoreCase("asset_created_by")) {
                    createdJoiner.add(getInnerQueries(where.fields, "u", "name"));
                    toRemove.add(where);
                }
                if (where.property.equalsIgnoreCase("asset_deleted_by")) {
                    orJoiner.add("(e.event = \"" + DasscoEvent.DELETE_ASSET_METADATA + "\" and " + getInnerQueries(where.fields, "u", "name") + ")");
                    toRemove.add(where);
                }
                if (where.property.equalsIgnoreCase("asset_updated_by")) {
                    updatedJoiner.add(getInnerQueries(where.fields, "u", "name"));
                    toRemove.add(where);
                }
                if (where.property.equalsIgnoreCase("audited_by")) {
                    auditedJoiner.add(getInnerQueries(where.fields, "u", "name"));
                    toRemove.add(where);
                }
            }

            if (propertiesTimestamps.contains(where.property)) {
                if (where.property.equalsIgnoreCase("created_timestamp")) {
                    createdJoiner.add(getInnerQueries(where.fields, "e", "timestamp"));
                    toRemove.add(where);
                }
                if (where.property.equalsIgnoreCase("updated_timestamp")) {
                    updatedJoiner.add(getInnerQueries(where.fields, "e", "timestamp"));
                    toRemove.add(where);
                }
                if (where.property.equalsIgnoreCase("audited_timestamp")) {
                    auditedJoiner.add(getInnerQueries(where.fields, "e", "timestamp"));
                    toRemove.add(where);
                }
            }
        }
        if (createdJoiner.length() > 0) {
            createdJoiner.add("e.event = '" + DasscoEvent.CREATE_ASSET_METADATA + "'");
            orJoiner.add("(" + createdJoiner + ")");
        }
        if (updatedJoiner.length() > 0) {
            updatedJoiner.add("e.event = '" + DasscoEvent.UPDATE_ASSET_METADATA + "'");
            orJoiner.add("(" + updatedJoiner + ")");
        }
        if (auditedJoiner.length() > 0) {
            auditedJoiner.add("e.event = '" + DasscoEvent.AUDIT_ASSET + "'");
            orJoiner.add("(" + auditedJoiner + ")");
        }
        if (!toRemove.isEmpty()) wheres.removeAll(toRemove);
        return orJoiner.toString();
    }

    public String getInnerQueries(List<QueryInner> inners, String match, String property) {
        StringJoiner orJoiner = new StringJoiner(" or ");
        for (QueryInner inner : inners) {
            orJoiner.add(inner.toBasicQueryString(match, property, inner.dataType));
        }
        return orJoiner.toString();
    }

    public SavedQuery saveQuery(SavedQuery savedQuery, String username) {
        return jdbi.onDemand(QueriesRepository.class).saveQuery(savedQuery, username);
    }

    public List<SavedQuery> getSavedQueries(String username) {
        return jdbi.onDemand(QueriesRepository.class).getSavedQueries(username);
    }

    public SavedQuery updateSavedQuery(String prevTitle, SavedQuery newQuery, String username) {
        return jdbi.onDemand(QueriesRepository.class).updateSavedQuery(prevTitle, newQuery, username);
    }

    public String deleteSavedQuery(String title, String username) {
        return jdbi.onDemand(QueriesRepository.class).deleteSavedQuery(title, username);
    }

    public boolean checkRights(User user) {
        Set<String> roles = user.roles;
        if (roles.contains(InternalRole.ADMIN.roleName)
                || roles.contains(InternalRole.SERVICE_USER.roleName)
                || roles.contains(InternalRole.DEVELOPER.roleName)) {
            return true;
        }
        return false;
    }
}
