package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class QueriesReceived {
    @Schema(description = "The id of the overall statement", example = "1")
    public Integer id;
    @Schema(description = "The statements of the entire query", example = "Asset where xyz")
    public List<Query> query;

    public QueriesReceived(Integer id, List<Query> query) {
        this.id = id;
        this.query = query;
    }

    @Override
    public String toString() {
        return "QueriesReceived{" +
                "id=" + id +
                ", query=" + query +
                '}';
    }
}
