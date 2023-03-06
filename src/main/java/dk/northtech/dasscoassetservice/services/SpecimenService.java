package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.SpecimenGraphInfo;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.HashPrefixSqlParser;
import org.jdbi.v3.core.statement.StatementContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SpecimenService {
    private final Jdbi jdbi;
    private static final Logger log = LoggerFactory.getLogger(SpecimenService.class);

    @Inject
    public SpecimenService(DataSource dataSource) {
        this.jdbi = Jdbi.create(dataSource)
                .registerRowMapper((ConstructorMapper.factory(SpecimenGraphInfo.class)));
    }

    public List<SpecimenGraphInfo> specimenDataByInstitute() {
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

    public List<SpecimenGraphInfo> specimenDataByPipeline() {
        String extension = "CREATE EXTENSION IF NOT EXISTS age;\n" +
            "LOAD 'age';\n" +
            "SET search_path = ag_catalog, \"$user\", public;";

        jdbi.withHandle(h -> h.execute(extension));

        String sql = "SELECT * from cypher('dassco', $$\n" +
                "        MATCH (s)-[H:HAS_MEDIA]-(a)-[M:MEDIA_CREATED_BY]-(p)\n" +
                "        OPTIONAL MATCH (i)-[P:PROPERTY_OF]-(s)\n" +
                "        OPTIONAL MATCH (d)-[MD:METADATA_CREATED_BY]-(s)\n" +
                "        RETURN i.name, i.ocr_text,\n" +
                "        s.name, s.media_subject, s.specify_specimen_id, s.specify_attachment_id, s.original_specify_media_name,\n" +
                "        a.name, a.media_guid, a.file_format, a.date_media_created,\n" +
                "        p.name, MD.created_date\n" +
                "        $$) as (institute_name agtype, institute_ocr_text agtype,\n" +
                "                specimen_name agtype, specimen_media_subject agtype, specimen_specify_spec_id agtype, specimen_specify_att_id agtype, specimen_orig_specify_media_name agtype, \n" +
                "                asset_name agtype, asset_media_guid agtype, asset_file_format agtype, asset_date_media_created agtype, \n" +
                "                pipeline_name agtype, created_date agtype)";

        return jdbi.withHandle(h ->
                h.setSqlParser(new HashPrefixSqlParser())
                .createQuery(sql)
                .mapTo(SpecimenGraphInfo.class)
                .list()
        );
    }
}
