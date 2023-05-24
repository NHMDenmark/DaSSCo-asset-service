package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Asset {
    @Schema(description = "???", example = "asdf-1234-3333-1000")
    public String pid;
    @Schema(description = "Unique key for the asset?", example = "asdf1244-233-3")
    public String guid;
    public AssetStatus status;
    @Schema(description = "Basically a multispecimen is a single image (or other type of media) that actually contains multiple specimens in it", example = "true")
    public boolean multi_specimen;
    @Schema(description = "The barcodes of associated specimens")
    public List<String> specimen_barcodes = new ArrayList<>();
    @Schema(description = "A short description of funding source used to create the asset", example = "Funding secured")
    public String funding;
    @Schema(description = "We will need to distinguish between image of a folder, device target, specimen, label etc)", example = "folder")
    public String subject;
    @Schema(description = "image, ct scan, surface scan, document", example = "ct scan")
    public String payload_type;
    @Schema(description = "File format enum, can contain multiple formats")
    public List<FileFormat> file_formats;
    @Schema(description = "Flags if it is possible to edit / delete the media of this asset", example = "false")
    public boolean asset_locked;
    public List<Role> restricted_access = new ArrayList<>();

    @Schema(description = "A dictionary of dynamic properties", example = "ct scan")
    public Map<String, String> tags = new HashMap<>();
    @Schema(description = "audited", example = "Has this asset been audited")
    public boolean audited;

    public Instant created_date;
    public Instant last_updated_date;
    public Instant asset_taken_date;
    public Instant asset_deleted_date;

    //References
    @Schema(description = "The institution", example = "NNAD")
    public String institution;
 //   @Schema(description = "The institution", example = "NNAD")
    public String parent_guid;
    public String collection;
    public String asset_location;
    public InternalStatus internal_status;
    public Instant pushed_to_specify_date;
    public String digitizer;

    public String pipeline;
    public String workstation;
    public String updateUser;
}
