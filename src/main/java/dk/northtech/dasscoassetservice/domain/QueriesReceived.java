package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class QueriesReceived {
    @Schema(description = "The id of the overall statement. String for ease of JSON parsing in frontend.", example = "1")
    public String id;
    @Schema(description = "The statements of the entire query", example = "Asset where xyz")
    public List<Query> query;

    public QueriesReceived(String id, List<Query> query) {
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
