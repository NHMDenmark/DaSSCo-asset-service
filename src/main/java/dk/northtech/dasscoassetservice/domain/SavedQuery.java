package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import static dk.northtech.dasscoassetservice.domain.QueryDataType.DATE;
import static dk.northtech.dasscoassetservice.domain.QueryDataType.LIST;

public class SavedQuery {
    @Schema(description = "The title of the query", example = "Get All Assets")
    public String title;
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

    public SavedQuery(String title, String query) {
        this.title = title;
        this.query = query;
    }

    @Override
    public String toString() {
        return "SavedQuery{" +
                "title='" + title + '\'' +
                ", query='" + query + '\'' +
                '}';
    }
}
