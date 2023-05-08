package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.PublicationLink;
import dk.northtech.dasscoassetservice.domain.Publisher;
import dk.northtech.dasscoassetservice.repositories.helpers.PublicationLinkMapper;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public interface PublisherRepository extends SqlObject {
    default void publish(PublicationLink publicationLink) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                            MATCH (a:asset{guid: $asset_guid})   
                            MERGE (p:Publisher{name: $publisher_name})
                            MERGE (a)-[pb:PUBLISHED_BY{link: $link, timestamp: $publication_timestamp}]->(p)
                        $$
                        , #params) as (a agtype);
                """;

        withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("asset_guid", publicationLink.assetGuid())
                    .add("link", publicationLink.link())
                    .add("publisher_name", publicationLink.publisherName())
                    .add("publication_timestamp", DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(publicationLink.timestamp()))
                    .build();
            Agtype agtype = AgtypeFactory.create(params);
            handle.createUpdate(cypher)
                    .bind("params", agtype)
                    .execute();
            return handle;
        });
    }

    default List<PublicationLink> listPublicationLinks(Publisher publisher) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                            MATCH (p:Publisher{name: $publisher_name})
                            MATCH (p)<-[pb:PUBLISHED_BY]-(a:Asset)
                            RETURN a.guid, pb.link, p.name, pb.timestamp
                        $$
                        , #params) as (asset_guid agtype
                        , publication_timestamp agtype
                        , publisher_name agtyper
                        , link agtype);
                """;

        return withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("publisher_name", publisher.name())
                    .build();
            Agtype agtype = AgtypeFactory.create(params);
            return handle.createQuery(cypher)
                    .bind("params", agtype)
                    .map(new PublicationLinkMapper())
                    .list();
        });
    }

    default void pull(PublicationLink publicationLink) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                            MATCH (a:asset{guid: $asset_guid})    
                            MATCH (p:Publisher{name: $publisher_name})
                            MATCH (a)-[pb:PUBLISHED_BY{link: $link}]->(p)
                            DELETE pb
                        $$
                        , #params) as (a agtype);
                """;

        withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("asset_guid", publicationLink.assetGuid())
                    .add("link", publicationLink.link())
                    .add("publisher_name", publicationLink.publisherName())
                    .add("publication_timestamp", DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(publicationLink.timestamp()))
                    .build();
            Agtype agtype = AgtypeFactory.create(params);
            handle.createUpdate(cypher)
                    .bind("params", agtype)
                    .execute();
            return handle;
        });
    }
}
