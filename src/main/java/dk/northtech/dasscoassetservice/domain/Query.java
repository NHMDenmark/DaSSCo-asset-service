package dk.northtech.dasscoassetservice.domain;

import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Query {
    @Schema(description = "The selection of the statement", example = "\"Institution\"")
    public String select;
    @Schema(description = "The where statements of the query", example = "or name = \"test_name\"")
    public List<QueryField> wheres;

    public Query(String select, List<QueryField> wheres) {
        this.select = select;
        this.wheres = wheres;
    }

    @Override
    public String toString() {
        return "Query{" +
                "select='" + select + '\'' +
                ", wheres=" + wheres +
                '}';
    }
}
