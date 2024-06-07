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
        return properties;
    }

    public List<Asset> unwrapQuery(List<Query> queries, int limit) {
        String finalQuery;
        Map<String, String> whereMap = new HashMap<>();
        whereMap.put("limit", Integer.toString(limit));

        for (Query query : queries) {
            if (query.select.equalsIgnoreCase("Asset")) { // a
                String assetString = joinFields(query.wheres, "a");
                whereMap.put("asset", assetString);
            }
            if (query.select.equalsIgnoreCase("Institution")) { // i
                whereMap.put("institution", joinFields(query.wheres, "i"));
            }
            if (query.select.equalsIgnoreCase("Workstation")) { // w
                whereMap.put("workstation", joinFields(query.wheres, "w"));
            }
            if (query.select.equalsIgnoreCase("Event")) { // w
                boolean eventTypeSet = query.wheres.stream().anyMatch(w -> w.property.equalsIgnoreCase("name") || w.property.equalsIgnoreCase("event"));
                if (!eventTypeSet) { // otherwise it'll search for all events including the update ones even if they're just searching for a range in the date...
                    query.wheres.add(new QueryField("and", "=", "event", "CREATE_ASSET_METADATA"));
                }
                whereMap.put("event", joinFields(query.wheres, "e"));
            }
            if (query.select.equalsIgnoreCase("Pipeline")) { // p
                whereMap.put("pipeline", joinFields(query.wheres, "p"));
            }
            if (query.select.equalsIgnoreCase("User")) { // u
                whereMap.put("user", joinFields(query.wheres, "u"));
            }
            if (query.select.equalsIgnoreCase("Collection"))  { // c
                whereMap.put("collection", joinFields(query.wheres, "c"));
            }
            if (query.select.equalsIgnoreCase("Specimen")) { // s
                whereMap.put("specimen", joinFields(query.wheres, "s"));
            }
        }
        StringSubstitutor substitutor = new StringSubstitutor(whereMap);
        finalQuery = substitutor.replace(assetSql);
        if (StringUtils.isBlank(finalQuery)) return new ArrayList<>();

        logger.info("Getting assets from query:\n{}", finalQuery);
        List<Asset> assets = jdbi.onDemand(QueriesRepository.class).getAssetsFromQuery(finalQuery);
        return assets;
    }

    public String joinFields(List<QueryField> wheres, String match) {
        StringJoiner andJoiner = new StringJoiner(" and ");
        StringJoiner orJoiner = new StringJoiner(" or ");

        for (QueryField queryField : wheres) {
            if (queryField.type.equalsIgnoreCase("and")) {
                andJoiner.add(queryField.toBasicQueryString(match));
            }
            if (queryField.type.equalsIgnoreCase("or")) {
                orJoiner.add(queryField.toBasicQueryString(match));
            }
        }
        String where = andJoiner.toString();
        if (!StringUtils.isBlank(orJoiner.toString())) {
            where = StringUtils.join(andJoiner, " or ", orJoiner);
        }
        return "\nWHERE (" + where + ")";
    }
}
