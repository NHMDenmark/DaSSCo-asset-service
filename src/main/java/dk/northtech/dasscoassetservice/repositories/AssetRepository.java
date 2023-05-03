package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.repositories.helpers.AssetMapper;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeListBuilder;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;
import org.jdbi.v3.sqlobject.SqlObject;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.beans.Transient;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

//@Repository
public interface AssetRepository extends SqlObject {
//    private Jdbi jdbi;
//    private DataSource dataSource;

    @CreateSqlObject
    SpecimenRepository createSpecimenRepository();

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
    default Asset createAsset(Asset asset) {
        boilerplate();
        persistAsset(asset);
        createSpecimenRepository().persistSpecimens(asset);
        connectParentChild(asset.parent_guid, asset.guid);
        return asset;
    }

    @Transaction
    default Optional<Asset> readAsset(String assetId) {
        boilerplate();
        return readAssetInternal(assetId);
    }

    default Optional<Asset> readAssetInternal(String assetGuid) {
        String sql = """
                SELECT * FROM ag_catalog.cypher(
                'dassco'
                    , $$
                         MATCH (a:Asset{name: $guid})
                         MATCH (c:Collection)<-[IS_PART_OF]-(a)
                         MATCH (p:Pipeline)<-[CREATED_BY]-(a)
                         MATCH (i:Institution)<-[BELONGS_TO]-(a)
                         OPTIONAL MATCH (s:Specimen)-[sss:USED_BY]->(:Asset{name: 'bestific'})
                         RETURN a.guid
                         , a.pid
                         , a.status
                         , a.multi_specimen
                         , a.funding, a.subject
                         , a.payload_type
                         , a.file_formats
                         , a.asset_taken_date
                         , a.internal_status
                         , collect(s.name)
                         , i.name
                         , c.name
                         , p.name
                      $$
                    , #params)
                    as (guid agtype
                    , pid agtype
                    , status agtype
                    , multi_specimen agtype
                    , funding agtype
                    , subject agtype
                    , payload_type agtype
                    , file_formats agtype
                    , asset_taken_date agtype
                    , internal_status agtype
                    , specimen_barcodes agtype
                    , institution_name agtype
                    , collection_name agtype
                    , pipeline_name agtype);
                  """;
        return withHandle(handle -> {
            AgtypeMap agParams = new AgtypeMapBuilder()
                    .add("guid", assetGuid)
                    .build();
            Agtype agtype = AgtypeFactory.create(agParams);
            return handle.createQuery(sql)
                    .bind("params", agtype)
                    .map(new AssetMapper())
                    .findOne();
        });
    }

    default void deleteParentRelation(String childGuid) {

    }
    default void connectParentChild(String parentGuid, String childGuid) {
        if(Strings.isNullOrEmpty(parentGuid)) {
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
    default Asset persistAsset(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (w:Workstation {name: $workstation_name})
                            MATCH (p:Pipeline {name: $pipeline_name})
                            MERGE (a:Asset {name: $guid
                                , pid: $pid
                                , guid: $guid
                                , status: $status
                                , multi_specimen: $multi_specimen
                                , funding: $funding
                                , subject: $subject
                                , payload_type: $payload_type
                                , file_formats: $file_formats
                                , asset_taken_date: $asset_taken_date
                                , internal_status: $internal_status
                            })    
                            MERGE (a)-[cb:CREATED_BY{timestamp: $created_date}]->(p)
                            MERGE (a)-[bt:BELONGS_TO]->(i)
                            MERGE (w)-[sa:STATIONED_AT]->(i)
                            MERGE (p)-[ub:USED_BY]->(i)
                            MERGE (a)-[ipf:IS_PART_OF]->(c)
                        $$
                        , #params) as (a agtype);
                        """;
        try {
            withHandle(handle -> {
                AgtypeListBuilder agtypeListBuilder = new AgtypeListBuilder();
                asset.file_formats.forEach(x -> agtypeListBuilder.add(x.name()));
                AgtypeMap parms = new AgtypeMapBuilder()
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("workstation_name", asset.workstation)
                        .add("pipeline_name", asset.pipeline)
                        .add("pid", asset.pid)
                        .add("guid", asset.guid)
                        .add("status", asset.status.name())
                        .add("multi_specimen", asset.multi_specimen)
                        .add("funding", asset.funding)
                        .add("subject", asset.subject)
                        .add("payload_type", asset.payload_type)
                        .add("file_formats", agtypeListBuilder.build())
                        .add("asset_taken_date", asset.asset_taken_date != null ? DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(asset.asset_taken_date) : null)
                        .add("created_date", DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(asset.created_date))
                        .add("internal_status", asset.internal_status.name())
                        .add("parent_id",asset.parent_guid)
                        .build();
                Agtype agtype = AgtypeFactory.create(parms);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return asset;
    }


//    public List<Collection> listCollections(Institution institution) {
//        String sql =
//                """
//                        SELECT * FROM ag_catalog.cypher('dassco'
//                         , $$
//                             MATCH (c:Collection)-[USED_BY]-(i:Institution)
//                             WHERE i.name = $institution_name
//                             RETURN c.name
//                           $$
//                         , #params
//                        ) as (collection_name agtype);""";
//
//
//        try {
//            withHandle(handle -> {
//                // We have to register the type
//                Connection connection = handle.getConnection();
//                PgConnection pgConn = connection.unwrap(PgConnection.class);
//                pgConn.addDataType("agtype", Agtype.class);
//
//                AgtypeMap name = new AgtypeMapBuilder().add("institution_name", institution.name()).build();
//                Agtype agtype = AgtypeFactory.create(name);
//                handle.execute(DBConstants.AGE_BOILERPLATE);
//                return handle.createQuery(sql).bind("params", agtype)
//                        .map((rs, ctx) -> {
//                            Agtype collection_name = rs.getObject("collection_name", Agtype.class);
//                            return new Collection(collection_name.getString(), institution.name());
//                        }).list();
//            });
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
}
