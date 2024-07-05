package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.repositories.QueriesRepository;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QueriesService {
    private static final Logger logger = LoggerFactory.getLogger(QueriesService.class);
    private final Jdbi jdbi;

    String assetSql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset) ${asset:-}
                         MATCH (c:Collection)<-[:IS_PART_OF]-(a) ${collection:-}
                         MATCH (e:Event)<-[:CHANGED_BY]-(a) ${event:-WHERE e.event = 'CREATE_ASSET_METADATA'}
                         MATCH (u:User)<-[:INITIATED_BY]-(e) ${user:-}
                         MATCH (p:Pipeline)<-[:USED]-(e) ${pipeline:-}
                         MATCH (w:Workstation)<-[:USED]-(e) ${workstation:-}
                         MATCH (i:Institution)<-[:BELONGS_TO]-(a) ${institution:-}
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
                         , pa.asset_guid 
                         , a.restricted_access
                         , a.tags
                         , a.error_message
                         , a.error_timestamp
                         , collect(s)
                         , i.name
                         , c.name
                         , p.name
                         , w.name
                         , e.timestamp
                         , a.date_asset_finalised
                         , u.name
                         , a.date_metadata_taken
                         , a.date_asset_taken
                         LIMIT ${limit:-200}
                      $$)
                    as (asset_guid agtype
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

    @Inject
    public QueriesService(Jdbi jdbi) {
    this.jdbi = jdbi;
    }

    public Map<String, List<String>> getNodeProperties() {
        Map<String, List<String>> properties = jdbi.onDemand(QueriesRepository.class).getNodeProperties();
        properties.get("Asset").addAll(Arrays.asList("created_timestamp", "updated_timestamp", "audited_timestamp"));
        properties.get("Asset").remove("restricted_access");
        return properties;
    }

    public List<Asset> unwrapQuery(List<QueriesReceived> queries, int limit) {
        List<Asset> allAssets = new ArrayList<>();

        for (QueriesReceived received : queries) {
            String finalQuery;
            Map<String, String> whereMap = new HashMap<>();
            whereMap.put("limit", Integer.toString(limit));

            for (Query query: received.query) {
                if (query.select.equalsIgnoreCase("Asset")) { // a
                    String eventTimestamps = checkForEventTimestamps(query.where); // cos event timestamps are set as if it belongs to the asset
                    if (!StringUtils.isBlank(eventTimestamps)) {
                        whereMap.put("event", "\nWHERE " + eventTimestamps);
                    }
                    String where = joinFields(query.where, "a");
                    if (!StringUtils.isBlank(where)) whereMap.put("asset", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Institution")) { // i
                    String where = joinFields(query.where, "i");
                    if (!StringUtils.isBlank(where)) whereMap.put("institution", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Workstation")) { // w
                    String where = joinFields(query.where, "w");
                    if (!StringUtils.isBlank(where)) whereMap.put("workstation", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Pipeline")) { // p
                    String where = joinFields(query.where, "p");
                    if (!StringUtils.isBlank(where)) whereMap.put("pipeline", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("User")) { // u
                    String where = joinFields(query.where, "u");
                    if (!StringUtils.isBlank(where)) whereMap.put("user", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Collection"))  { // c
                    String where = joinFields(query.where, "c");
                    if (!StringUtils.isBlank(where)) whereMap.put("collection", "\nWHERE (" + where + ")");
                }
                if (query.select.equalsIgnoreCase("Specimen")) { // s
                    String where = joinFields(query.where, "s");
                    if (!StringUtils.isBlank(where)) whereMap.put("specimen", "\nWHERE (" + where + ")");
                }
            }

            StringSubstitutor substitutor = new StringSubstitutor(whereMap);
            finalQuery = substitutor.replace(assetSql);
            if (StringUtils.isBlank(finalQuery)) return new ArrayList<>();

            logger.info("Getting assets from query.");
            System.out.println(finalQuery);
            List<Asset> assets = jdbi.onDemand(QueriesRepository.class).getAssetsFromQuery(finalQuery);
            allAssets.addAll(assets);
        }
        return allAssets;
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

    public String checkForEventTimestamps(List<QueryWhere> wheres) {
        StringJoiner orJoiner = new StringJoiner(" or ");
        List<QueryWhere> toRemove = new ArrayList<>();

        for (QueryWhere where : wheres) {
            if (where.property.equalsIgnoreCase("created_timestamp")) {
                orJoiner.add("(e.event = \"" + DasscoEvent.CREATE_ASSET_METADATA + "\" and " + getInnerQueries(where.fields, "e", "timestamp") + ")");
                toRemove.add(where);
            }
            if (where.property.equalsIgnoreCase("updated_timestamp")) {
                orJoiner.add("(e.event = \"" + DasscoEvent.UPDATE_ASSET_METADATA + "\" and " + getInnerQueries(where.fields, "e", "timestamp") + ")");
                toRemove.add(where);
            }
            if (where.property.equalsIgnoreCase("audited_timestamp")) {
                orJoiner.add("(e.event = \"" + DasscoEvent.AUDIT_ASSET_METADATA + "\" and " + getInnerQueries(where.fields, "e", "timestamp") + ")");
                toRemove.add(where);
            }
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
}
