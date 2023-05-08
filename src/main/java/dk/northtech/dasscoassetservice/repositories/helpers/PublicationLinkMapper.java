package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.*;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PublicationLinkMapper implements RowMapper<PublicationLink> {
    @Override
    public PublicationLink map(ResultSet rs, StatementContext ctx) throws SQLException {
        Agtype assetGuid = rs.getObject("asset_guid", Agtype.class);
        Agtype link = rs.getObject("link", Agtype.class);
        Agtype publisherName = rs.getObject("publisher_name", Agtype.class);
        Agtype publicationTimestamp = rs.getObject("publication_timestamp", Agtype.class);
        return new PublicationLink(assetGuid.getString()
                , link.getString()
                , publisherName.getString()
                , dateTimeFormatter.parse(publicationTimestamp.getString(), Instant::from));
    }

    static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.of("UTC"));
}
