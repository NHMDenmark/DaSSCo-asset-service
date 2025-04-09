package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.repositories.helpers.EventMapper;
import joptsimple.internal.Strings;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeListBuilder;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static dk.northtech.dasscoassetservice.repositories.AssetRepository.READ_WITHOUT_WHERE;



//@Repository
public interface BulkUpdateRepository extends SqlObject {
    //    private Jdbi jdbi;
//    private DataSource dataSource;
    Logger logger = LoggerFactory.getLogger(BulkUpdateRepository.class);

    @CreateSqlObject
    SpecimenRepository createSpecimenRepository();


    //This must be called once per transaction
    default void boilerplate() {
        withHandle(handle -> {
            Connection connection = handle.getConnection();
            try {
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Transaction
    default List<Asset> readMultipleAssets(List<String> assets) {
        boilerplate();
        return readMultipleAssetsInternal(assets);
    }

    static AGEQuery createBulkUpdateSql(List<String> assetList, Asset updatedFields) {

        AgtypeMapBuilder builder = new AgtypeMapBuilder();
        AgtypeListBuilder assetGuidBuilder = new AgtypeListBuilder();
        assetList.forEach(assetGuidBuilder::add);
        builder.add("asset_guids", assetGuidBuilder.build());
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                        MATCH (asset:Asset)
                        WHERE asset.asset_guid IN $asset_guids
                        MATCH (asset)-[existing_has_status:HAS]->(:Status)
                """);
//        if (!Strings.isNullOrEmpty(updatedFields.parent_guid)) {
//            sb.append("""
//                          MATCH (new_parent:Asset{name: $new_parent_guid})
//                    """);
//            builder.add("new_parent_guid", updatedFields.parent_guid);
//        }
        if (!Strings.isNullOrEmpty(updatedFields.status)) {
            sb.append("""
                            MATCH (new_status:Status{name: $new_status})
                    """);
            builder.add("new_status", updatedFields.status);
        }

        sb.append("""
                        OPTIONAL MATCH (digitiser:Digitiser)-[existing_digitised:DIGITISED]->(asset)
                        OPTIONAL MATCH (asset)-[existing_child_of:CHILD_OF]->(existing_parent:Asset)
                        OPTIONAL MATCH (asset)-[existing_has_payload_type:HAS]->(:Payload_type)
                        OPTIONAL MATCH (asset)-[existing_has_subject:HAS]->(:Subject)
                """);
//        if (!Strings.isNullOrEmpty(updatedFields.parent_guid)) {
//
//            sb.append("""
//                            DELETE existing_child_of
//                            MERGE (asset)-[:CHILD_OF]->(new_parent)
//                    """);
//        }
        if (!Strings.isNullOrEmpty(updatedFields.status)) {
            sb.append("""
                            DELETE existing_has_status
                            MERGE (asset)-[:HAS]->(new_status)
                    """);
        }
        if (!Strings.isNullOrEmpty(updatedFields.digitiser)) {
            sb.append("""
                            DELETE existing_digitised
                            MERGE (digitiser:Digitiser{name: $digitiser})
                            MERGE (digitiser)-[:DIGITISED]->(asset)
                    """);
            builder.add("digitiser", updatedFields.digitiser);
        }
        if (!Strings.isNullOrEmpty(updatedFields.subject)) {
            sb.append("""   
                            DELETE existing_has_subject
                            MERGE(new_subject:Subject{name: $new_subject})
                            MERGE (asset)-[:HAS]->(new_subject)
                    """);
            builder.add("new_subject", updatedFields.subject);
        }
        if (!Strings.isNullOrEmpty(updatedFields.payload_type)) {
            sb.append("""
                            DELETE existing_has_payload_type
                            MERGE(new_payload_type:Payload_type{name: $new_payload_type})
                            MERGE (asset)-[:HAS]->(new_payload_type)
                    """);
            builder.add("new_payload_type", updatedFields.payload_type);
        }
        // If asset locked is false it means either that they forgot to add it or that they want to unlock an asset, which they cannot do like this.
        if (updatedFields.asset_locked) {
            sb.append("""
                                SET
                                asset.asset_locked = $asset_locked
                    """);
            builder.add("asset_locked", true);
        }


        sb.append("""
                       
                        $$
                        , #params) as (a agtype);
                """);
        return new AGEQuery(sb.toString(), builder);

    }

    static AGEQuery deleteList(List<String> assetGuids, String relation, String name, String listNode) {
        if (assetGuids.isEmpty()) {
            return null;
        }
        AgtypeMapBuilder builder = new AgtypeMapBuilder();
        AgtypeListBuilder assetGuidBuilder = new AgtypeListBuilder();
        assetGuids.forEach(assetGuidBuilder::add);
        builder.add("asset_guids", assetGuidBuilder.build());
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                        MATCH (asset:Asset)
                        WHERE asset.asset_guid IN $asset_guids
                        MATCH (asset)
                """);
        sb.append(relation)
                .append("(:")
                .append(listNode)
                .append(")\n")
                .append("DELETE ")
                .append(name)
                        .append("""
                                $$
                                , #params) as (a agtype);
                        """);
        return new AGEQuery(sb.toString(), builder);
    }

    //If this query is folded into the createList query as MERGE it will result in the first list item getting duplicated for each asset.
    static AGEQuery ensureExistes(List<String> nameProperties, String listNode) {
        AgtypeMapBuilder builder = new AgtypeMapBuilder();

        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$

                """);
        for(int i = 0 ; i < nameProperties.size(); i++) {
            sb.append("MERGE (").append(":").append(listNode).append("{name: ").append("$l").append(i).append("})\n");
            builder.add("l"+ i, nameProperties.get(i));

        }
         sb.append("""
                                $$
                                , #params) as (a agtype);
                        """);
        return new AGEQuery(sb.toString(), builder);
    }

    static AGEQuery createList(List<String> assetGuids, List<String> nameProperty, String relation, String listNode) {
        if (assetGuids.isEmpty()) {
            return null;
        }
        AgtypeMapBuilder builder = new AgtypeMapBuilder();
        AgtypeListBuilder assetGuidBuilder = new AgtypeListBuilder();
        assetGuids.forEach(assetGuidBuilder::add);
        builder.add("asset_guids", assetGuidBuilder.build());
        StringBuilder sb = new StringBuilder("""
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                        MATCH (asset:Asset)
                        WHERE asset.asset_guid IN $asset_guids
                """);

        String parmName = listNode.toLowerCase();

        for (int i = 0; i < nameProperty.size(); i++) {
            var name = nameProperty.get(i);
            if (!Strings.isNullOrEmpty(name)) {
                sb.append(" \nMATCH (new_").append(parmName).append(i).append(":").append(listNode).append("{name: $new_")
                        .append(parmName)
                        .append(i)
                        .append("})");
                builder.add("new_" + parmName + i, name);
            }
        }
        for (int i = 0; i < nameProperty.size(); i++) {
            var name = nameProperty.get(i);
            if (!Strings.isNullOrEmpty(name)) {
                sb.append("          \nMERGE (asset)")
                        .append(relation)
                        .append("(new_")
                        .append(parmName)
                        .append(i)
                        .append(")");

                builder.add("new_" + parmName + i, name);
            }
        }
                        sb.append("""                               
                                        $$
                                        , #params) as (a agtype);
                                """);
                ;
        return new AGEQuery(sb.toString(), builder);

    }

    @Transaction
    default List<Asset> bulkUpdate(Asset updatedAsset, Event event, List<Asset> assets, List<String> assetList) {
        boilerplate();
        AGEQuery bulkUpdateSql = createBulkUpdateSql(assetList, updatedAsset);
        logger.info("Bulk update SQL: {}", bulkUpdateSql.sql());
        // Update asset metadata:
        executeUpdate(bulkUpdateSql);

        if (updatedAsset.funding != null) {
            AGEQuery ageQuery = deleteList(assetList, "<-[funds_to_delete:FUNDS]-", "funds_to_delete", "Funding_entity");
            System.out.println(ageQuery.sql());
            executeUpdate(ageQuery);
            if (!updatedAsset.funding.isEmpty()) {
                AGEQuery ensureExistsQuery = ensureExistes(updatedAsset.funding, "Funding_entity");
                System.out.println(ensureExistsQuery.sql());
                executeUpdate(ensureExistsQuery);
                AGEQuery funding = createList(assetList, updatedAsset.funding, "<-[:FUNDS]-", "Funding_entity");
                System.out.println(funding.sql());
                executeUpdate(funding);
            }
        }

        // Add Event to every asset:
        // TODO: This is a solution for the bulk update, but it takes individual calls.
//        for (Asset asset : assets) {
//            // Set event (individual calls)
//            setEvent(updatedAsset.updateUser, event, asset);
//            // Connect parent and child (individual calls)
//            connectParentChild(updatedAsset.parent_guid, asset.asset_guid);
//            // Modify tags
//            if (!updatedAsset.tags.isEmpty()) {
//                setTags(asset);
//            }
//        }

        // Return the List of Assets:
        return this.readMultipleAssetsInternal(assetList);
    }


    @Transaction
    default List<Event> readEvents(String guid) {
        boilerplate();
        return readEvents_internal(guid);
    }


    default List<Event> readEvents_internal(String guid) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (e:Event)<-[:CHANGED_BY]-(a:Asset{name: $asset_guid})
                            MATCH (u:User)<-[:INITIATED_BY]-(e)
                            OPTIONAL MATCH (p:Pipeline)<-[:USED]-(e)
                            OPTIONAL MATCH (w:Workstation)<-[:USED]-(e)
                            RETURN e.timestamp
                                , e.event
                                , u.name
                                , p.name
                                , w.name
                        $$
                        , #params) 
                        as (timestamp agtype
                            , event agtype
                            , event_user agtype
                            , pipeline agtype
                            , workstation agtype);
                        """;

        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("asset_guid", guid)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            List<Event> events = handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new EventMapper())
                    .list();

            events.sort(Collections.reverseOrder(Comparator.comparing(event -> event.timestamp)));
            return events;
        });
    }

    default void connectParentChild(String parentGuid, String childGuid) {
        if (Strings.isNullOrEmpty(parentGuid)) {
            return;
        }
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (p:Asset {name: $pGuid})
                            MATCH (c:Asset {name: $cGuid})
                            MERGE (c)-[cf:CHILD_OF]->(p)
                        $$
                        , #params) as (ag agtype);
                        """;
        withHandle(handle -> {

            AgtypeMap parentChildRelation = new AgtypeMapBuilder()
                    .add("pGuid", parentGuid)
                    .add("cGuid", childGuid).build();
            Agtype specimenEdge = AgtypeFactory.create(parentChildRelation);
            handle.createUpdate(sql)
                    .bind("params", specimenEdge)
                    .execute();
            return handle;
        });
    }


    default List<Asset> readMultipleAssetsInternal(List<String> assets) {
        String sql = """
                               SELECT * FROM ag_catalog.cypher(
                             'dassco'
                                 , $$
                                      MATCH (asset:Asset)
                                      WHERE asset.asset_guid IN $asset_guids
                                      """
                     + READ_WITHOUT_WHERE;
        return withHandle(handle -> {
            AgtypeListBuilder assetGuidList = new AgtypeListBuilder();
            assets.forEach(assetGuidList::add);
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("asset_guids", assetGuidList.build())
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new AssetMapper())
                    .list();
        });
    }

    default void internal_deleteAsset(String assetGuid) {
        // Deletes Asset
        String sqlAsset = """
                SELECT * FROM ag_catalog.cypher('dassco'
                , $$
                    MATCH (a:Asset {name: $asset_guid})
                    DETACH DELETE a
                $$
                , #params) as (a agtype);
                """;
        // Deletes orphaned Specimens:
        String sqlSpecimen = """
                SELECT * FROM ag_catalog.cypher('dassco'
                , $$
                    MATCH (s:Specimen)
                    WHERE NOT EXISTS((s)-[:USED_BY]-())
                    DETACH DELETE s
                $$
                ) as (s agtype);
                """;
        // Deletes orphaned Events:
        String sqlEvent = """
                SELECT * FROM ag_catalog.cypher('dassco'
                , $$
                    MATCH (e:Event)
                    WHERE NOT EXISTS((e)-[:CHANGED_BY]-())
                    DETACH DELETE e
                $$
                ) as (e agtype);
                """;

        try {
            withHandle(handle -> {
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("asset_guid", assetGuid);
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sqlAsset)
                        .bind("params", agtype)
                        .execute();
                handle.createUpdate(sqlSpecimen)
                        .execute();
                handle.createUpdate(sqlEvent)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transaction
    default void executeUpdate(AGEQuery ageQuery) {
        try {
            withHandle(handle -> {
                Agtype agtype = AgtypeFactory.create(ageQuery.agtypeMapBuilder().build());
                handle.createUpdate(ageQuery.sql())
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transaction
    default void setTags(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset {name: $asset_guid})
                            
                            SET a.tags = $tags
                        $$
                        , #params) as (a agtype);
                        """;

        try {
            withHandle(handle -> {
                AgtypeMapBuilder tags = new AgtypeMapBuilder();
                asset.tags.entrySet().forEach(tag -> tags.add(tag.getKey(), tag.getValue())); //(tag -> tags.add(tag));
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("asset_guid", asset.asset_guid)
                        .add("tags", tags.build());
                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transaction
    default void setEvent(String user, Event event, Asset asset) {
        boilerplate();
        internal_setEvent(user, event, asset);
    }

    default void internal_setEvent(String user, Event event, Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset {name: $asset_guid})
                            """;
        if (event.pipeline != null) {
            sql += "MATCH (p:Pipeline {name: $pipeline_name}) ";
        }
        if (event.user != null) {

            sql += "MERGE (u:User{user_id: $user, name: $user}) ";
        }
        sql +=
                """
                        MERGE (e:Event{timestamp: $updated_date, event: $event, name: $event})
                        MERGE (a)-[ca:CHANGED_BY]->(e)
                        """;
        if (event.user != null) {
            sql += " MERGE (e)-[pb:INITIATED_BY]->(u) ";
        }
        if (event.pipeline != null) {
            sql += " MERGE (e)-[pu:USED]->(p) ";
        }
        sql +=
                """
                        $$
                        , #params) as (a agtype);
                        """;

        try {
            String finalSql = sql;
            withHandle(handle -> {
                AgtypeMapBuilder builder = new AgtypeMapBuilder()
                        .add("asset_guid", asset.asset_guid)
//                        .add("user", user)
                        .add("event", event.event.name())
                        .add("updated_date", event.timestamp.toEpochMilli());
                if (event.user != null) {
                    builder.add("user", event.user);
                }

                if (event.pipeline != null) {
                    builder.add("pipeline_name", event.pipeline);
                }

                Agtype agtype = AgtypeFactory.create(builder.build());
                handle.createUpdate(finalSql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
