package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.Specimen;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;

import java.util.List;

public interface SpecimenRepository extends SqlObject {

    default void persistSpecimens(Asset asset) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (i:Institution {name: $institution_name})
                            MATCH (c:Collection {name: $collection_name})
                            MATCH (a:Asset {name: $guid})
                            MERGE (s:Specimen{name: $specimen_barcode, barcode: $specimen_barcode})                       
                            MERGE (s)-[u:USED_BY]->(a)
                            MERGE (s)-[bt:BELONGS_TO]->(c)
                            MERGE (s)-[bts:BELONGS_TO]->(i)
                        $$
                        , #params) as (a agtype);
                """;
        String updateCreatedBy = """
                SELECT * FROM ag_catalog.cypher('dassco'
                        , $$
                            MATCH (a:Asset{name: $guid})
                            MATCH (s:Specimen{name: $specimen_barcode})
                            WHERE NOT EXISTS((s)-[:CREATED_BY]-(:Asset))
                            MERGE (s)-[scb:CREATED_BY]->(a)
                        $$
                        , #params) as (ag agtype);
                """;
        withHandle(handle -> {
            for(String specimenBarcode: asset.specimen_barcodes) {
                System.out.println("PERSISTING " + specimenBarcode);
                AgtypeMap parms = new AgtypeMapBuilder()
                        .add("institution_name", asset.institution)
                        .add("collection_name", asset.collection)
                        .add("guid", asset.guid)
                        .add("specimen_barcode", specimenBarcode).build();
                Agtype agtype = AgtypeFactory.create(parms);
                handle.createUpdate(cypher)
                        .bind("params", agtype)
                        .execute();

                AgtypeMap specimenEdgeParam = new AgtypeMapBuilder()
                        .add("guid", asset.guid)
                        .add("specimen_barcode", specimenBarcode).build();
                Agtype specimenEdge = AgtypeFactory.create(specimenEdgeParam);
                handle.createUpdate(updateCreatedBy)
                        .bind("params", specimenEdge)
                        .execute();

            }
            return handle;
        });
    }
}
