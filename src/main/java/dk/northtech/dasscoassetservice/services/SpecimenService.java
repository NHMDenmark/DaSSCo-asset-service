package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.SpecimenGraphInfo;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class SpecimenService {
    private final Jdbi jdbi;
    private static final Logger log = LoggerFactory.getLogger(SpecimenService.class);

    @Inject
    public SpecimenService(DataSource dataSource) {
        this.jdbi = Jdbi.create(dataSource)
                .registerRowMapper(SpecimenGraphInfo.class, (rs, ctx) -> this.setValues(rs));
    }

    public List<SpecimenGraphInfo> getSpecimenData() {
        String extension = "CREATE EXTENSION IF NOT EXISTS age;\n" +
            "LOAD 'age';\n" +
            "SET search_path = ag_catalog, \"$user\", public;";

        jdbi.withHandle(h -> h.execute(extension));

        String sql = "SELECT * from cypher('dassco', $$\n" +
                "        MATCH (i)-[P:PROPERTY_OF]-(s)-[H:HAS_MEDIA]-(a)\n" +
                "        OPTIONAL MATCH (d)-[M:METADATA_CREATED_BY]-(s)\n" +
                "        RETURN i.name, i.ocr_text,\n" +
                "        s.name, s.media_subject, s.specify_specimen_id, s.specify_attachment_id, s.original_specify_media_name,\n" +
                "        a.name, a.media_guid, a.file_format, a.date_media_created,\n" +
                "        d.name, M.created_date\n" +
                "        $$) as (institute_name agtype, institute_ocr_text agtype,\n" +
                "                specimen_name agtype, specimen_media_subject agtype, specimen_specify_spec_id agtype, specimen_specify_att_id agtype, specimen_orig_specify_media_name agtype,\n" +
                "                asset_name agtype, asset_media_guid agtype, asset_file_format agtype, asset_date_media_created agtype, digitisor_name agtype, created_date agtype);";

        return jdbi.withHandle(h ->
                h.setSqlParser(new HashPrefixSqlParser())
                .createQuery(sql)
                .mapTo(SpecimenGraphInfo.class)
                .list()
        );
    }

    public SpecimenGraphInfo setValues(ResultSet rs) {
        try {
            return new SpecimenGraphInfo(
                rs.getString("institute_name").replaceAll("\"", ""),
                rs.getString("institute_ocr_text").replaceAll("\"", ""),
                rs.getString("specimen_name").replaceAll("\"", ""),
                rs.getString("specimen_media_subject").replaceAll("\"", ""),
                rs.getString("specimen_specify_spec_id").replaceAll("\"", ""),
                rs.getString("specimen_specify_att_id").replaceAll("\"", ""),
                rs.getString("specimen_orig_specify_media_name").replaceAll("\"", ""),
                rs.getString("asset_name").replaceAll("\"", ""),
                rs.getString("asset_media_guid").replaceAll("\"", ""),
                rs.getString("asset_file_format").replaceAll("\"", ""),
                rs.getString("asset_date_media_created").replaceAll("\"", ""),
                rs.getString("digitisor_name").replaceAll("\"", ""),
                rs.getString("created_date").replaceAll("\"", "")
            );
        } catch (SQLException e) {
            log.error("Error when setting values for the SpecimenGraph object," + e.getMessage());
            throw new RuntimeException("Error when setting values from ResultSet for SpecimenGraph object,", e);
        }
    }
}
