package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Specimen;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface SpecimenRepository extends SqlObject {

    default void persistSpecimens(Asset asset, List<Specimen> specimensToDetach) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (a:Asset {name: $guid})
                            MERGE (s:Specimen{name: $specimen_barcode, specimen_barcode: $specimen_barcode})                       
                            MERGE (s)-[u:USED_BY]->(a)
                            MERGE (s)-[bt:IS_PART_OF]->(c)
                            MERGE (s)-[bts:BELONGS_TO]->(i)
                            SET s.preparation_type = $preparation_type
                                , s.specimen_pid = $specimen_pid
                        $$
                        , #params) as (a agtype);
                """;
        String updateCreatedBy = """
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (a:Asset{name: $guid})
                            MATCH (s:Specimen{name: $specimen_barcode})
                            WHERE NOT EXISTS((s)-[:CREATED_BY]-(:Asset))
                            MERGE (s)-[scb:CREATED_BY]->(a)
                        $$
                        , #params) as (ag agtype);
                """;

        String deleteUsedBy = """
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (a:Asset{name: $guid})
                            MATCH (s:Specimen{name: $specimen_barcode})
                            MATCH (s)-[u:USED_BY]->(a)
                            MATCH (s)-[bt:IS_PART_OF]->(c)
                            MATCH (s)-[bts:BELONGS_TO]->(i)
                            DELETE u
                        $$
                        , #params) as (ag agtype);
                """;
        withHandle(handle -> {
            for (Specimen specimen : asset.specimens) {
                AgtypeMap parms = new AgtypeMapBuilder()
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("guid", asset.asset_guid)
                        .add("specimen_pid", specimen.specimen_pid())
                        .add("preparation_type", specimen.preparation_type())
                        .add("specimen_barcode", specimen.barcode())
                        .build();
                Agtype agtype = AgtypeFactory.create(parms);
                handle.createUpdate(cypher)
                        .bind("params", agtype)
                        .execute();

                AgtypeMap specimenEdgeParam = new AgtypeMapBuilder()
                        .add("guid", asset.asset_guid)
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("specimen_barcode", specimen.barcode())
                        .build();
                Agtype specimenEdge = AgtypeFactory.create(specimenEdgeParam);
                handle.createUpdate(updateCreatedBy)
                        .bind("params", specimenEdge)
                        .execute();

            }
            for(Specimen specimen : specimensToDetach) {
                AgtypeMap deleteEdgeParams = new AgtypeMapBuilder()
                        .add("guid", asset.asset_guid)
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("specimen_barcode", specimen.barcode())
                        .build();
                Agtype deleteSpecimenEdge = AgtypeFactory.create(deleteEdgeParams);
                handle.createUpdate(deleteUsedBy)
                        .bind("params", deleteSpecimenEdge)
                        .execute();
            }
            return handle;
        });
    }

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

    default void updateSpecimen(Specimen specimen) {
        String update = """
                     SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (s:Specimen{name: $specimen_barcode})
                            SET s.specimen_pid = $specimen_pid
                            , s.preparation_type = $preparation_type                  
                            MERGE (s)-[bt:IS_PART_OF]->(c)
                            MERGE (s)-[bts:BELONGS_TO]->(i)
                        $$
                        , #params) as (a agtype);
                """;
        AgtypeMap updateSpecimen = new AgtypeMapBuilder()
                .add("barcode", specimen.barcode())
                .add("specimen_pid", specimen.specimen_pid())
                .add("institution", specimen.institution())
                .add("collection", specimen.collection())
                .add("preparation_type", specimen.preparation_type())
                .build();
        Agtype specimenEdge = AgtypeFactory.create(updateSpecimen);
        withHandle(handle -> {
            handle.createUpdate(update)
                    .bind("params", specimenEdge)
                    .execute();
            return handle;
        });
    }

}
