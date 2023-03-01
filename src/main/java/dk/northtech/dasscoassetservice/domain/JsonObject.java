package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.jdbi.v3.json.Json;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown=true)
public class JsonObject {
    public Institute institute;
    public Asset asset;
    @Nullable public Specimen specimen;

//    @JsonProperty("properties")
//    private void unpackNested(Map<String,Object> properties) {
//        this.name = (String) properties.get("name");
//        this.fileInfo = (String) properties.get("file_info");
//        this.mediaPid = (String) properties.get("media_pid");
//        this.mediaGuid = (String) properties.get("media_guid");
//        this.fileFormat = (String) properties.get("file_format");
//        this.payloadType = (String) properties.get("payload_type");
//        this.dateMediaCreated = (Instant) properties.get("date_media_created");
//    }

    public JsonObject() {
    }

    @Override
    public String toString() {
        return "JsonObject{" +
                "institute=" + institute +
                ", asset=" + asset +
                ", specimen=" + specimen +
                '}';
    }
}
