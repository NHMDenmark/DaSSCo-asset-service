package dk.northtech.dasscoassetservice.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class QueriesService {
    private static final Logger logger = LoggerFactory.getLogger(QueriesService.class);
    private Jdbi jdbi;
    private Jdbi readonlyJdbi;
    private RightsValidationService rightsValidationService;

    LoadingCache<User, Map<String, Set<String>>> accessCache = Caffeine.newBuilder() // <user, <"read", ["collection2"]>>
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build(user -> this.rightsValidationService.getCollectionRights(user.roles));

    List<String> propertiesTimestamps = Arrays.asList("created_timestamp", "updated_timestamp", "audited_timestamp");
    List<String> propertiesDigitiser = Arrays.asList("asset_created_by", "asset_deleted_by", "asset_updated_by", "audited_by");

    String assetSql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                        MATCH (a:Asset) ${asset:-}
                        MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                        MATCH (e:Event)<-[:CHANGED_BY]-(a)
                        MATCH (u:User)<-[:INITIATED_BY]-(e) ${assetEvents:-WHERE e.event = 'CREATE_ASSET_METADATA'}
                        MATCH (i:Institution)<-[:BELONGS_TO]-(a) ${instCollAccess:-}
                        ${optionals}
                      
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
                              , parent.asset_guid AS parent_guid
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
                              , ${writeAccess:-null}
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
                    , date_asset_taken agtype
                    , write_access agtype);
                  """;

    String assetCountSql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset) ${asset:-}
                         ${parentIsNull:-}
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a)
                         MATCH (e:Event)<-[:CHANGED_BY]-(a)
                         MATCH (u:User)<-[:INITIATED_BY]-(e) ${assetEvents:-WHERE e.event = 'CREATE_ASSET_METADATA'}
                         MATCH (i:Institution)<-[:BELONGS_TO]-(a) ${instCollAccess:-}
                         ${optionals}
                         RETURN count(DISTINCT a) as count
                         LIMIT ${limit:-200}
                      $$)
                    as (count agtype);
                  """;

    @Inject
    public QueriesService(RightsValidationService rightsValidationService, @Qualifier("jdbi")Jdbi jdbi, @Qualifier("readonly-jdbi") Jdbi readonlyJdbi) {
        this.rightsValidationService = rightsValidationService;
        this.jdbi = jdbi;
        this.readonlyJdbi = readonlyJdbi;
    }

    public List<QueryItem> getNodeProperties() {
        List<QueryItem> queryItems = new ArrayList<>();
        queryItems.add(new QueryItem("asset", List.of(
                new QueryProperty(QueryItemField.ASSET_GUID.getDisplayName(), "String", QueryItemField.ASSET_GUID.getTableName()),
                new QueryProperty(QueryItemField.ASSET_LOCKED.getDisplayName(), "boolean", QueryItemField.ASSET_LOCKED.getTableName()),
                new QueryProperty(QueryItemField.ASSET_PID.getDisplayName(), "String", QueryItemField.ASSET_PID.getTableName()),
                new QueryProperty(QueryItemField.SUBJECT.getDisplayName(), "String", QueryItemField.SUBJECT.getTableName()),
                new QueryProperty(QueryItemField.SPECIMENS.getDisplayName(), "specimen", QueryItemField.SPECIMENS.getTableName()),
                new QueryProperty(QueryItemField.COLLECTION.getDisplayName(), "collection", QueryItemField.COLLECTION.getTableName()),
                new QueryProperty(QueryItemField.DIGITISER.getDisplayName(), "digitiser", QueryItemField.DIGITISER.getTableName()),
//                new QueryProperty(QueryItemField.V2_FEATURE_EXTERNAL_PUBLISHER.getDisplayName(), "???", QueryItemField.V2_FEATURE_EXTERNAL_PUBLISHER.getTableName()),
                new QueryProperty(QueryItemField.FILE_FORMAT.getDisplayName(), "String", QueryItemField.FILE_FORMAT.getTableName()),
                new QueryProperty(QueryItemField.FUNDING.getDisplayName(), "String", QueryItemField.FUNDING.getTableName()),
                new QueryProperty(QueryItemField.INSTITUTION.getDisplayName(), "institution", QueryItemField.INSTITUTION.getTableName()),
                new QueryProperty(QueryItemField.MULTI_SPECIMEN.getDisplayName(), "boolean", QueryItemField.MULTI_SPECIMEN.getTableName()),
                new QueryProperty(QueryItemField.PARENT_GUID.getDisplayName(), "String", QueryItemField.PARENT_GUID.getTableName()),
                new QueryProperty(QueryItemField.PAYLOAD_TYPE.getDisplayName(), "String", QueryItemField.PAYLOAD_TYPE.getTableName()),
                new QueryProperty(QueryItemField.RESTRICTED_ACCESS.getDisplayName(), "???", QueryItemField.RESTRICTED_ACCESS.getTableName()),
                new QueryProperty(QueryItemField.STATUS.getDisplayName(), "String", QueryItemField.STATUS.getTableName()),
                new QueryProperty(QueryItemField.WORKSTATION.getDisplayName(), "workstation", QueryItemField.WORKSTATION.getTableName()),
                new QueryProperty(QueryItemField.UPDATE_USER.getDisplayName(), "String", QueryItemField.UPDATE_USER.getTableName()),
                new QueryProperty(QueryItemField.PIPELINE.getDisplayName(), "String", QueryItemField.PIPELINE.getTableName()),
                new QueryProperty(QueryItemField.INTERNAL_STATUS.getDisplayName(), "String", QueryItemField.INTERNAL_STATUS.getTableName()),
                new QueryProperty(QueryItemField.MAKE_PUBLIC.getDisplayName(), "boolean", QueryItemField.MAKE_PUBLIC.getTableName()),
                new QueryProperty(QueryItemField.METADATA_SOURCE.getDisplayName(), "String", QueryItemField.METADATA_SOURCE.getTableName()),
                new QueryProperty(QueryItemField.PUSH_TO_SPECIFY.getDisplayName(), "boolean", QueryItemField.PUSH_TO_SPECIFY.getTableName()),
                new QueryProperty(QueryItemField.METADATA_VERSION.getDisplayName(), "String", QueryItemField.METADATA_VERSION.getTableName()),
//                new QueryProperty(QueryItemField.COMPLETE_DIGITISER_LIST.getDisplayName(), "???", QueryItemField.COMPLETE_DIGITISER_LIST.getTableName()),
                new QueryProperty(QueryItemField.CAMERA_SETTING_CONTROL.getDisplayName(), "String", QueryItemField.CAMERA_SETTING_CONTROL.getTableName()),
                new QueryProperty(QueryItemField.MOS_ID.getDisplayName(), "String", QueryItemField.MOS_ID.getTableName()),
                new QueryProperty(QueryItemField.SPECIFY_ATTACHMENT_REMARKS.getDisplayName(), "String", QueryItemField.SPECIFY_ATTACHMENT_REMARKS.getTableName()),
                new QueryProperty(QueryItemField.SPECIFY_ATTACHMENT_TITLE.getDisplayName(), "String", QueryItemField.SPECIFY_ATTACHMENT_TITLE.getTableName()),
                new QueryProperty(QueryItemField.DATE_ASSET_TAKEN.getDisplayName(), "Instant", QueryItemField.DATE_ASSET_TAKEN.getTableName()),
                new QueryProperty(QueryItemField.DATE_ASSET_FINALISED.getDisplayName(), "Instant", QueryItemField.DATE_ASSET_FINALISED.getTableName()),
                new QueryProperty(QueryItemField.DATE_METADATA_INGESTED.getDisplayName(), "Instant", QueryItemField.DATE_METADATA_INGESTED.getTableName())
//                new QueryProperty(QueryItemField.LEGAL.getDisplayName(), "???", QueryItemField.LEGAL.getTableName()),
//                new QueryProperty(QueryItemField.ISSUES.getDisplayName(), "???", QueryItemField.ISSUES.getTableName())
                )));
        queryItems.add(new QueryItem("event", List.of(
//                new QueryProperty("audited", "bool", "event"), // EVENT TYPE
//                new QueryProperty("asset_created_by", "???", "???"), // EVENT USER
                new QueryProperty(QueryItemField.DATE_ASSET_CREATED_ARS.getDisplayName(), "Instant", QueryItemField.DATE_ASSET_CREATED_ARS.getTableName()), // EVENT TYPE + TIME
//                new QueryProperty("asset_updated_by", "string", "???"), // EVENT USER
                new QueryProperty(QueryItemField.DATE_ASSET_UPDATED_ARS.getDisplayName(), "Instant", QueryItemField.DATE_ASSET_UPDATED_ARS.getTableName()), // EVENT TYPE + TIME
//                new QueryProperty("asset_deleted_by", "string", "???"), // EVENT USER
                new QueryProperty(QueryItemField.DATE_ASSET_DELETED_ARS.getDisplayName(), "Instant", QueryItemField.DATE_ASSET_DELETED_ARS.getTableName()), // EVENT TYPE + TIME
//                new QueryProperty("audited_by", "string", "???"), // EVENT USER
                new QueryProperty(QueryItemField.DATE_AUDITED.getDisplayName(), "???", "Instant"), // EVENT TYPE + TIME
//                new QueryProperty("metadata_created_by", "string", "???"), // EVENT USER
                new QueryProperty(QueryItemField.DATE_METADATA_CERATED_ARS.getDisplayName(), "Instant", QueryItemField.DATE_METADATA_CERATED_ARS.getTableName()), // EVENT TYPE + TIME
//                new QueryProperty("metadata_updated_by", "string", "???"), // EVENT USER
                new QueryProperty(QueryItemField.DATE_METADATA_UPDATED_ARS.getDisplayName(), "Instant", QueryItemField.DATE_METADATA_UPDATED_ARS.getTableName()), // EVENT TYPE + TIME
                new QueryProperty(QueryItemField.DATE_PUSHED_TO_SPECIFY.getDisplayName(), "Instant", QueryItemField.DATE_PUSHED_TO_SPECIFY.getTableName()) // EVENT TYPE + TIME
        )));

        return queryItems;
    }

    public int getAssetCountFromQuery(List<QueriesReceived> queries, int limit, User user) {
        Set<String> collectionsAccess = null; // only need collection, really, as it's the deepest access check (we check for institute rights in the function if necessary, too)
        boolean fullAccess = checkRights(user);
        if (!fullAccess) {
            collectionsAccess = accessCache.get(user)
                    .values().stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
        }

        int allAssets = 0;
        for (QueriesReceived received : queries) { // going through all the queries sent (usually just one though.)
            String query = unwrapQuery(received, limit, true, collectionsAccess, fullAccess);
            if (query != null && !StringUtils.isBlank(query)) {
                logger.info("Getting asset count from query.");
                allAssets += readonlyJdbi.onDemand(QueriesRepository.class).getAssetCountFromQuery(query);
            }
        }

        return allAssets;
    }

    public List<QueryResultAsset> getAssetsFromQuery(List<QueriesReceived> queries, int limit, User user) {
        Set<String> collectionsAccess; // only need collection, really, as it's the deepest access check (we check for institute rights in the function if necessary, too)
        Map<String, Set<String>> accessMap = null;
        boolean fullAccess = checkRights(user);
        if (!fullAccess) {
            accessMap = accessCache.get(user);
            collectionsAccess = accessMap // will be empty if the user has no rights to anything
                    .values().stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
        } else {
            collectionsAccess = null;
        }

        Set<String> tablesUsed = new HashSet<>();
        Map<String, Object> paramMap = new HashMap<>();
        AtomicInteger counter = new AtomicInteger();
        String whereFilters = queries.stream().map(queryReceived -> queryReceived.query.stream().map(query -> query.where.stream().map(queryWhere -> {
            var table = query.select;
            var column = queryWhere.property;
            return  "(" + queryWhere.fields.stream().map(queryInner -> {
                var index = counter.getAndIncrement();
                var queryInnerResult = queryInner.toBasicPostgreSQLQueryString(column, table, index);
                var queryInnerResultEntry = queryInnerResult.entrySet().iterator().next();
                paramMap.putAll(queryInnerResultEntry.getValue());
                tablesUsed.add(QueryItemField.fromDisplayName(column).getTableName());
                return queryInnerResultEntry.getKey();
            }).collect(Collectors.joining(" or ")) + ")";
        }).collect(Collectors.joining(" and "))).collect(Collectors.joining(" and "))).collect(Collectors.joining(" and "));

         /*
            left join event using (asset_guid)
            left join digitiser_list using (asset_guid)
            left join dassco_user digitiser_user on digitiser_user.dassco_user_id = digitiser_list.dassco_user_id
            left join asset_funding using (asset_guid)
            left join funding using (funding_id)
            left join parent_child on parent_child.child_guid = asset.asset_guid
            left join workstation using (workstation_id)
            left join pipeline using (pipeline_id)
            left join legality using (legality_id)
            left join issue using (asset_guid)
         */
        StringBuilder leftJoins = new StringBuilder("left join collection using(collection_id) left join asset_specimen using(asset_guid) left join specimen using(specimen_id)");

        for(String tableUsed : tablesUsed) {
            if(tableUsed.equals("event")) {
                leftJoins.append(" left join event using (asset_guid)");
            }
            if(tableUsed.equals("digitiser_user")) {
                leftJoins.append(" left join digitiser_list using (asset_guid)");
                leftJoins.append(" left join dassco_user digitiser_user on digitiser_user.dassco_user_id = digitiser_list.dassco_user_id");
            }
            if(tableUsed.equals("funding")) {
                leftJoins.append(" left join asset_funding using (asset_guid)");
                leftJoins.append(" left join funding using (funding_id)");
            }
            if(tableUsed.equals("parent_child")) {
                leftJoins.append(" left join parent_child on parent_child.child_guid = asset.asset_guid");
            }
            if(tableUsed.equals("workstation")) {
                leftJoins.append(" left join workstation using (workstation_id)");
            }
            if(tableUsed.equals("pipeline")) {
                leftJoins.append(" left join pipeline using (pipeline_id)");
            }
            if(tableUsed.equals("legality")) {
                leftJoins.append(" left join legality using (legality_id)");
            }
            if(tableUsed.equals("issue")) {
                leftJoins.append(" left join issue using (asset_guid)");
            }
        }

        String sql = """
            select
                DISTINCT asset_guid,
                collection.institution_name as institution,
                collection_name as collection,
                file_formats,
                now() as created_date
            from asset
            #LeftJoins#
            #where# #collectionAccess#
            limit :limit
        """
                .replace("#LeftJoins#", leftJoins.toString())
                .replace("#where#", whereFilters.isEmpty() ? "" : "where " + whereFilters)
                .replace("#collectionAccess#", fullAccess ? "" : (whereFilters.isEmpty() ? "where" : " and collection_name in (%s)".formatted(collectionsAccess.stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "(", ")")))));


        return readonlyJdbi.withHandle(h ->
                {
                    List<QueryResultAsset> queryResultAssets = h.createQuery(sql)
                            .bindMap(paramMap)
                            .bind("limit", Math.min(limit, 10000))
                            .mapTo(QueryResultAsset.class)
                            .list();

                    List<String> assetGuids = queryResultAssets.stream().map(QueryResultAsset::asset_guid).toList();

                    Map<String, List<Event>> assetEvents = assetGuids.isEmpty() ? new HashMap<>() : h.createQuery("""
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

                    Map<String, List<Specimen>> assetSpecimens = assetGuids.isEmpty() ? new HashMap<>() : h.createQuery("""
                            select asset_guid, institution_name, collection_name, barcode, specimen_pid, preparation_types, preparation_type, specimen_id, collection_id, specify_collection_object_attachment_id, asset_detached from asset_specimen
                            inner join specimen using (specimen_id)
                            left join collection using (collection_id)
                            where asset_guid in (<assetGuids>)
                            """)
                            .bindList("assetGuids", assetGuids)
                            .execute((statement, ctx) -> {
                                try (ctx; var rs = statement.get().getResultSet()) {
                                    Map<String, List<Specimen>> assetSpecimensTemp = new HashMap<>();
                                    while (rs.next()) {
                                        String assetGuid = rs.getString("asset_guid");
                                        String institutionName = rs.getString("institution_name");
                                        String collectionName = rs.getString("collection_name");
                                        String barcode = rs.getString("barcode");
                                        String specimenPid = rs.getString("specimen_pid");
                                        String preparationTypes = rs.getString("preparation_types");
                                        String preparationType = rs.getString("preparation_type");
                                        int specimenId = rs.getInt("specimen_id");
                                        int collectionId = rs.getInt("collection_id");
                                        Long specifyCollectionObjectAttachmentId = rs.getLong("specify_collection_object_attachment_id");
                                        boolean assetDetached = rs.getBoolean("asset_detached");
                                        Specimen newSpecimen = new Specimen(
                                                institutionName,
                                                collectionName,
                                                barcode,
                                                specimenPid,
                                                new HashSet<>(Arrays.asList(preparationTypes.split(","))),
                                                preparationType,
                                                specimenId,
                                                collectionId,
                                                specifyCollectionObjectAttachmentId,
                                                assetDetached
                                        );
                                        assetSpecimensTemp.computeIfAbsent(assetGuid, k -> new ArrayList<>()).add(newSpecimen);
                                    }
                                    return assetSpecimensTemp;
                                }
                            });

                    return queryResultAssets.stream().map(queryResultAsset ->
                            queryResultAsset
                                    .withEvents(assetEvents.get(queryResultAsset.asset_guid()))
                                    .withSpeciments(assetSpecimens.get(queryResultAsset.asset_guid()))
                    ).toList();
                }
        );
    }

    public List<Asset> handleDuplicatedAssets(List<Asset> originalAssets) {
        // gotta do the following to avoid duplicates in case the query returns an asset with multiple events.
        Map<String, Long> assetCountMap = originalAssets.stream() // counts how many of each guid
                .collect(Collectors.groupingBy(Asset::getAsset_guid, Collectors.counting()));

        Set<String> duplicatedAssetGuids = assetCountMap.entrySet().stream() // collects the guids with more than 1
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<Asset> distinctAssets = new ArrayList<>(new HashSet<>(originalAssets)); // object hash has been set so that not all properties are checked here

        distinctAssets
                .forEach(asset -> asset.events = readonlyJdbi.onDemand(AssetRepository.class).readEvents_internal(asset.asset_guid)); // setting events for the duplicated assets

        return distinctAssets;
    }

    public void applyWriteAccess(Map<String, Set<String>> accessMap, List<Asset> distinctAssets) {
        if (accessMap != null && !accessMap.isEmpty()) {
            distinctAssets.stream()
                    .filter(asset -> accessMap.get("write").contains(asset.collection))
                    .forEach(asset -> asset.writeAccess = true);
        }
    }

    public String unwrapQuery(QueriesReceived queryReceived, int limit, boolean count, Set<String> collectionAccess, boolean fullAccess) {
        String finalQuery;
        Map<String, String> whereMap = new HashMap<>();
        whereMap.put("limit", Integer.toString(limit));
        String collectionString = "";
        String instutionString = "";

        Map<String, String> optionals = getOptionalsMap();

        for (Query query: queryReceived.query) {
            if (query.select.equalsIgnoreCase("Asset")) { // a
                String eventTimestamps = checkForEventUserProperties(query.where); // cos event timestamps are set as if it belongs to the asset

                if (!eventTimestamps.isBlank()) {
                    whereMap.put("assetEvents", "\nWHERE (" + eventTimestamps + ")");
                }

                List<QueryWhere> parentGuidWhere = query.where.stream().filter(q -> q.property.equalsIgnoreCase("parent_guid")).toList();

                if (!parentGuidWhere.isEmpty()) { // Asset query has to look very different if we're querying the parent_guid
                    boolean findNull = false;
                    for (QueryWhere where : parentGuidWhere) {
                        for (QueryInner inner : where.fields) {
                            if (inner.operator.equalsIgnoreCase("empty")) {
                                findNull = true;
                            }
                        }
                    }
                    String childOfWhere = joinFields(parentGuidWhere, "parent");

                    if (!findNull) {
                        whereMap.put("childOfOptional", ""); // no longer optional
                        whereMap.put("childOf", "WHERE (" + childOfWhere + ")");
                        query.where.removeAll(parentGuidWhere);
                    }
                }
                String where = joinFields(query.where, "a");
                if (!where.isBlank()) {
                    if (whereMap.containsKey("asset")) {
                        whereMap.put("asset", whereMap.get("asset").replace(")\n", " ") + "and " + where + ")");
                    } else {
                        whereMap.put("asset", "WHERE (" + where + ")");
                    }
                }
            }
            if (query.select.equalsIgnoreCase("Institution")) { // i
                instutionString = joinFields(query.where, "i");
            }
            if (query.select.equalsIgnoreCase("Workstation")) { // w
                String where = joinFields(query.where, "w");
                if (!where.isBlank()) {
                    whereMap.put("workstation", "WHERE (" + where + ")\n");
                }
            }
            if (query.select.equalsIgnoreCase("Pipeline")) { // p
                String where = joinFields(query.where, "p");
                if (!where.isBlank()) {
                    String pipelineClause = optionals.remove("pipelineOptional");
                    LinkedHashMap<String, String> reorderedOptionals = new LinkedHashMap<>();
                    reorderedOptionals.put("pipelineOptional", pipelineClause);
                    reorderedOptionals.putAll(optionals);
                    optionals = reorderedOptionals;
                    whereMap.put("pipelineOptional", ""); // no longer optional
                    whereMap.put("pipeline", "WHERE (" + where + ")\n");
                }
            }
            if (query.select.equalsIgnoreCase("Collection"))  { // c
                collectionString = joinFields(query.where, "c");
            }
            if (query.select.equalsIgnoreCase("Specimen")) { // s
                String where = joinFields(query.where, "s");
                if (!where.isBlank()) {
                    whereMap.put("specimen", "WHERE (" + where + ")\n");
                }
            }
        }

        if (fullAccess) {
            whereMap.put("writeAccess", "true");
            if (!instutionString.isBlank() || !collectionString.isBlank()) {
                whereMap.put("instCollAccess", "WHERE " + setInstitutionAndCollection(instutionString, collectionString, null));
            }
        } else {
            String collections = String.join(", ", collectionAccess.stream().map(coll -> "'" + coll + "'").toList());
            whereMap.put("instCollAccess", "WHERE " + setInstitutionAndCollection(instutionString, collectionString, collections));
        }

        // Combine the optional clauses into a single string
        StringBuilder optionalsString = new StringBuilder();
        for (String clause : optionals.values()) {
            optionalsString.append(clause).append("\n");
        }


        StringSubstitutor substitutor = new StringSubstitutor(whereMap);
        if (count) {
            // Replace the placeholder with the combined optionals
            String tempAssetCountSql = assetCountSql.replace("${optionals}", optionalsString.toString().trim());
            finalQuery = substitutor.replace(tempAssetCountSql);
        } else {
            // Replace the placeholder with the combined optionals
            String tempAssetSql = assetSql.replace("${optionals}", optionalsString.toString().trim());
            finalQuery = substitutor.replace(tempAssetSql);
        }
        return finalQuery;
    }

    private static Map<String, String> getOptionalsMap() {
        Map<String, String> optionals = new LinkedHashMap<>();
        optionals.put("childOfOptional", "${childOfOptional:-OPTIONAL} MATCH (a)-[:CHILD_OF]->(parent:Asset) ${childOf:-}");
        optionals.put("pipelineOptional", "${pipelineOptional:-OPTIONAL} MATCH (p:Pipeline)<-[:USED]-(e) ${pipeline:-}");
        optionals.put("workstationOptional", "${workstationOptional:-OPTIONAL} MATCH (w:Workstation)<-[:USED]-(e) ${workstation:-}");
        optionals.put("specimenOptional", "${specimenOptional:-OPTIONAL} MATCH (s:Specimen)-[sss:USED_BY]->(a) ${specimen:-}");
        return optionals;
    }

    public String joinFields(List<QueryWhere> wheres, String match) {
        StringJoiner andJoiner = new StringJoiner(" and ");

        for (QueryWhere where : wheres) {
            StringJoiner orJoiner = new StringJoiner(" or ");
            for (QueryInner inner : where.fields) {
                orJoiner.add(inner.toBasicQueryString(match, where.property, inner.dataType));
            }
            andJoiner.add("(" + orJoiner + ")");
        }
        return andJoiner.toString();
    }

    public String setInstitutionAndCollection(String instutionString, String collectionString, String accessString) {
        StringJoiner orJoiner = new StringJoiner(" or ");

        if (!StringUtils.isBlank(instutionString)) {
            orJoiner.add("(" + instutionString + ")");
        }
        if (!StringUtils.isBlank(collectionString)) {
            orJoiner.add("(" + collectionString + ")");
        }

        if (accessString != null) {
            if (!StringUtils.isBlank(instutionString) || !StringUtils.isBlank(collectionString)) {
                return orJoiner.toString().concat(" AND (c.name IN [" + accessString + "])");
            } else { // access is limited
                orJoiner.add("(c.name IN [" + accessString + "])");
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
        return readonlyJdbi.onDemand(QueriesRepository.class).getSavedQueries(username);
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
