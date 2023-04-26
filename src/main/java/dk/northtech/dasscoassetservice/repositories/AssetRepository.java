package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Collection;
import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import jakarta.inject.Inject;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeListBuilder;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.core.Jdbi;
import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Repository
public class AssetRepository {
    private Jdbi jdbi;
    private DataSource dataSource;

    private static final String boilerplate =
            "CREATE EXTENSION IF NOT EXISTS age;\n" +
                         "LOAD 'age';\n" +
                         "SET search_path = ag_catalog, \"$user\", public;";
    @Inject
    public AssetRepository(Jdbi jdbi, DataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = jdbi;
    }


    public Asset persistAsset(Asset asset) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MERGE (i:Institution {name: $institution_name})
                            MERGE (c:Collection {name: $collection_name})
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
                            })     
                            MERGE (i)-[:USED_BY]<-(c)
                            MERGE (a)-[:BELONGS_TO]->(i)
                            MERGE (a)-[:IS_PART_OF]->(c)
                            RETURN i
                        $$
                        , #params) as (institution agtype);
                        """;


        try {
            jdbi.withHandle(handle -> {
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);
                AgtypeListBuilder agtypeListBuilder = new AgtypeListBuilder();
                asset.file_formats.forEach(x -> agtypeListBuilder.add(x.name()));
                System.out.println("coll" + asset.collection);
                System.out.println("insst" + asset.institution);
                AgtypeMap parms = new AgtypeMapBuilder()
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("pid", asset.pid)
                        .add("guid", asset.guid)
                        .add("status", asset.status.name())
                        .add("multi_specimen", asset.multi_specimen)
                        .add("funding", asset.funding)
                        .add("subject", asset.subject)
                        .add("payload_type", asset.payload_type)
                        .add("file_formats", agtypeListBuilder.build())
                        .add("asset_taken_date", asset.asset_taken_date != null ? DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(asset.asset_taken_date):null)
                        .build();
                Agtype agtype = AgtypeFactory.create(parms);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                handle.createUpdate(sql)
                        .bind("params", agtype)
                        .execute();
                handle.close();
                return handle;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return asset;
    }

    public List<Collection> listCollections(Institution institution) {
        String sql =
                """
                        SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                             MATCH (c:Collection)-[USED_BY]-(i:Institution)
                             WHERE i.name = $institution_name
                             RETURN c.name
                           $$
                         , #params
                        ) as (collection_name agtype);""";


        try {
            return jdbi.withHandle(handle -> {
                // We have to register the type
                Connection connection = handle.getConnection();
                PgConnection pgConn = connection.unwrap(PgConnection.class);
                pgConn.addDataType("agtype", Agtype.class);

                AgtypeMap name = new AgtypeMapBuilder().add("institution_name", institution.name()).build();
                Agtype agtype = AgtypeFactory.create(name);
                handle.execute(DBConstants.AGE_BOILERPLATE);
                return handle.createQuery(sql).bind("params", agtype)
                        .map((rs, ctx) -> {
                            Agtype collection_name = rs.getObject("collection_name", Agtype.class);
                            return new Collection(collection_name.getString(), institution.name());
                        }).list();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
