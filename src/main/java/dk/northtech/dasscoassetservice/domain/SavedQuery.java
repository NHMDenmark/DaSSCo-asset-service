package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import static dk.northtech.dasscoassetservice.domain.QueryDataType.DATE;
import static dk.northtech.dasscoassetservice.domain.QueryDataType.LIST;

public class SavedQuery {
    @Schema(description = "The title of the query", example = "Get All Assets")
    public String name;
    @Schema(description = "The query itself in JSON format", example = """
            "select": "Asset",
            	"where": [{
            		"property": "name",
            		"fields": [{
                        "operator": "CONTAINS",
                        "value": "hjort",
                        "data_type": "STRING"
            		}]
            	}]
            """)
    public String query;

    public SavedQuery() {
    }

    public SavedQuery(String name, String query) {
        this.name = name;
        this.query = query;
    }

    @Override
    public String toString() {
        return "SavedQuery{" +
                "name='" + name + '\'' +
                ", query='" + query + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavedQuery that = (SavedQuery) o;
        return Objects.equals(name, that.name) && Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, query);
    }
}
