package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public class NodeProperty {
    @Schema(description = "The label of the node", example = "Asset")
    public String label;
    @Schema(description = "The properties of the node in cypher format", example = "{\"name\": \"thomas\"}")
    public String properties;

    public NodeProperty(String label, String properties) {
        this.label = label;
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "Temp{" +
                "label='" + label + '\'' +
                ", properties=" + properties +
                '}';
    }
}
