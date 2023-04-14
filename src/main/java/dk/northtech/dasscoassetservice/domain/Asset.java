package dk.northtech.dasscoassetservice.domain;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Asset {
    public String asset_pid;
    public String asset_guid;
    public AssetStatus asset_status;
    public List<String> specimen_barcodes;
    public String funding;
    public String asset_subject;
    public String payload_type;
    public List<FileFormat> file_formats;
    public String asset_locked;
    public List<Role> restricted_access;
    public Map<String, String> tags = new HashMap<>();
    public boolean audited;
    public Instant asset_taken_date;

    //References
    public String institution;
    public String collection;
    public String pipeline;
    public String workstation;
}
