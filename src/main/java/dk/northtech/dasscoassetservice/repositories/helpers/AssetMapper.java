package dk.northtech.dasscoassetservice.repositories.helpers;

import dk.northtech.dasscoassetservice.domain.*;
import org.apache.age.jdbc.base.Agtype;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class AssetMapper implements RowMapper<Asset> {
    @Override
    public Asset map(ResultSet rs, StatementContext ctx) throws SQLException {
        Asset asset = new Asset();
        Agtype guid = rs.getObject("guid", Agtype.class);
        Agtype pid = rs.getObject("pid", Agtype.class);
        Agtype status = rs.getObject("status", Agtype.class);
        Agtype funding = rs.getObject("funding", Agtype.class);
        Agtype subject = rs.getObject("subject", Agtype.class);
        Agtype payloadType = rs.getObject("payload_type", Agtype.class);
        Agtype fileFormats = rs.getObject("file_formats", Agtype.class);
        Agtype assetTakenDate = rs.getObject("asset_taken_date", Agtype.class);
        Agtype internalStatus = rs.getObject("internal_status", Agtype.class);
        Agtype specimenBarcodes = rs.getObject("specimen_barcodes", Agtype.class);
        Agtype institutionName = rs.getObject("institution_name", Agtype.class);
        Agtype collectionName = rs.getObject("collection_name", Agtype.class);
        Agtype pipelineName = rs.getObject("pipeline_name", Agtype.class);

        asset.specimen_barcodes = specimenBarcodes.getList().stream().map(x -> x.toString()).collect(Collectors.toList());
        asset.internal_status = InternalStatus.valueOf(internalStatus.getString());
        asset.guid = guid.getString();
        asset.pid = pid.getString();
        asset.status = AssetStatus.valueOf(status.getString());
        asset.file_formats = fileFormats.getList().stream().map(x -> FileFormat.valueOf(x.toString())).collect(Collectors.toList());
        asset.multi_specimen = asset.specimen_barcodes.size() > 1;
        asset.funding = funding.getString();
        asset.subject = subject.getString();
        asset.payload_type = payloadType.getString();
        System.out.println(assetTakenDate.getString());
        asset.institution = institutionName.getString();
        asset.collection = collectionName.getString();
        asset.pipeline = pipelineName.getString();
        asset.assetLocation = "/" + asset.institution + "/" + asset.collection + "/" + asset.guid;
        return asset;
    }
}
