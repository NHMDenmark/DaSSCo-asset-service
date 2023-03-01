package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

public class Specimen {
    public Long id;
    public String label;
    public String name;
    @Nullable
    public String fileInfo;
    @Nullable
    public String mediaPid;
    public String mediaGuid;
    public String fileFormat;
    public String payloadType;
    public Instant dateMediaCreated;

    @JsonProperty("properties")
    private void unpackNested(Map<String,Object> properties) {
        this.name = (String) properties.get("name");
        this.fileInfo = (String) properties.get("file_info");
        this.mediaPid = (String) properties.get("media_pid");
        this.mediaGuid = (String) properties.get("media_guid");
        this.fileFormat = (String) properties.get("file_format");
        this.payloadType = (String) properties.get("payload_type");
        this.dateMediaCreated = (Instant) properties.get("date_media_created");
    }

    public Specimen(Long id, String label, String name, @Nullable String fileInfo, @Nullable String mediaPid, String mediaGuid, String fileFormat, String payloadType, Instant dateMediaCreated) {
        this.id = id;
        this.label = label;
        this.name = name;
        this.fileInfo = fileInfo;
        this.mediaPid = mediaPid;
        this.mediaGuid = mediaGuid;
        this.fileFormat = fileFormat;
        this.payloadType = payloadType;
        this.dateMediaCreated = dateMediaCreated;
    }

    @Override
    public String toString() {
        return "Specimen{" +
                "id=" + id +
                ", label='" + label + '\'' +
                ", name='" + name + '\'' +
                ", fileInfo='" + fileInfo + '\'' +
                ", mediaPid='" + mediaPid + '\'' +
                ", mediaGuid='" + mediaGuid + '\'' +
                ", fileFormat='" + fileFormat + '\'' +
                ", payloadType='" + payloadType + '\'' +
                ", dateMediaCreated=" + dateMediaCreated +
                '}';
    }
}
