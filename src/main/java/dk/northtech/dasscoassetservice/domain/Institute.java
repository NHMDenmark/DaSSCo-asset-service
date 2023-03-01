package dk.northtech.dasscoassetservice.domain;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.annotation.Nullable;
import java.util.Map;

public class Institute {
    public Long id;
    public String label;
    public String name;
    public String ocrText;
    @Nullable
    public String taxonName;
    @Nullable
    public String geographicRegion;

    @JsonProperty("properties")
    private void unpackNested(Map<String,Object> properties) {
        this.name = (String) properties.get("name");
        this.ocrText = (String) properties.get("ocr_text");
        this.taxonName = (String) properties.get("taxon_name");
        this.geographicRegion = (String) properties.get("geographic_Region");
    }

    public Institute() {
    }

    @Override
    public String toString() {
        return "Institute{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", name='" + name + '\'' +
                ", ocrText='" + ocrText + '\'' +
                ", taxonName='" + taxonName + '\'' +
                ", geographicRegion='" + geographicRegion + '\'' +
                '}';
    }
}
