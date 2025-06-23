package dk.northtech.dasscoassetservice.repositories;

import dk.northtech.dasscoassetservice.domain.DasscoFile;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.List;

public interface FileRepository extends SqlObject {
    @SqlUpdate("""
            UPDATE file
            SET file.specify_attachment_id = :specifyAttachmentId
            WHERE file.file_id = :fileId
            """)
    public void setSpecifyAttachmentId(@Bind long fileId, @Bind int specifyAttachmentId);

    @SqlQuery("SELECT * FROM file WHERE asset_guid = :assetGuid AND deleted = false")
    List<DasscoFile> getFilesByAssetGuid(@Bind String assetGuid);
}
