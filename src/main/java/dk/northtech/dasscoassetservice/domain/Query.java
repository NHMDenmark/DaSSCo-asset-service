package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class Query {
    @Schema(description = "The selection of the statement", example = "\"Institution\"")
    public String select;
    @Schema(description = "The where statements of the query", example = "or name = \"test_name\"")
    public List<QueryField> where;

    public Query(String select, List<QueryField> where) {
        this.select = select;
        this.where = where;
    }

    @Override
    public String toString() {
        return "Query{" +
                "select='" + select + '\'' +
                ", wheres=" + where +
                '}';
    }
}
