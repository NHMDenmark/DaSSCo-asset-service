package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.PublicationLink;
import dk.northtech.dasscoassetservice.domain.Publisher;
import dk.northtech.dasscoassetservice.repositories.helpers.DBConstants;
import dk.northtech.dasscoassetservice.repositories.helpers.PublicationLinkMapper;
import org.apache.age.jdbc.base.Agtype;
import org.apache.age.jdbc.base.AgtypeFactory;
import org.apache.age.jdbc.base.type.AgtypeMap;
import org.apache.age.jdbc.base.type.AgtypeMapBuilder;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public interface PublisherRepository extends SqlObject {
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
    default void publish(PublicationLink publicationLink) {
        boilerplate();
        internal_publish(publicationLink);
    }
    @Transaction
    default void pull(PublicationLink publicationLink) {
        boilerplate();
        internal_pull(publicationLink);
    }
    @Transaction
    default List<PublicationLink> listPublicationLinks(Publisher publisher) {
        boilerplate();
        return internal_listPublicationLinks(publisher);
    }
    default void internal_publish(PublicationLink publicationLink) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                            MATCH (a:Asset{guid: $asset_guid})   
                            MERGE (p:Publisher{name: $publisher_name})
                            MERGE (a)-[pb:PUBLISHED_BY{link: $link}]->(p)
                            SET pb.timestamp = $publication_timestamp
                        $$
                        , #params) as (a agtype);
                """;

        withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("asset_guid", publicationLink.asset_guid())
                    .add("link", publicationLink.link())
                    .add("publisher_name", publicationLink.publisher_name())
                    .add("publication_timestamp", DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC")).format(publicationLink.timestamp()))
                    .build();
            Agtype agtype = AgtypeFactory.create(params);
            handle.createUpdate(cypher)
                    .bind("params", agtype)
                    .execute();
            return handle;
        });
    }

    default List<PublicationLink> internal_listPublicationLinks(Publisher publisher) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                            MATCH (p:Publisher{name: $publisher_name})
                            MATCH (p)<-[pb:PUBLISHED_BY]-(a:Asset)
                            RETURN a.guid, pb.link, p.name, pb.timestamp
                        $$
                        , #params) as (asset_guid agtype
                        , link agtype
                        , publisher_name agtype
                        , publication_timestamp agtype);
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

    default void internal_pull(PublicationLink publicationLink) {
        String cypher = """
                SELECT * FROM ag_catalog.cypher('dassco'
                         , $$
                            MATCH (a:Asset{guid: $asset_guid})    
                            MATCH (p:Publisher{name: $publisher_name})
                            MATCH (a)-[pb:PUBLISHED_BY{link: $link}]->(p)
                            DELETE pb
                        $$
                        , #params) as (a agtype);
                """;

        withHandle(handle -> {
            AgtypeMap params = new AgtypeMapBuilder()
                    .add("asset_guid", publicationLink.asset_guid())
                    .add("link", publicationLink.link())
                    .add("publisher_name", publicationLink.publisher_name())
                    .build();
            Agtype agtype = AgtypeFactory.create(params);
            handle.createUpdate(cypher)
                    .bind("params", agtype)
                    .execute();
            return handle;
        });
    }
}
