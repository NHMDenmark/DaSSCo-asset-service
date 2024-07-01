package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class QueryWhere {
    @Schema(description = "The field on the left-hand side of the statement", example = "name")
    public String property;
    @Schema(description = "The initial and possible \"or\" queries for this property", example = "CONTAINS \"nam\" or ENDS WITH \"e\"")
    public List<QueryInner> fields;


    public QueryWhere(String property, List<QueryInner> fields) {
        this.property = property;
        this.fields = fields;
    }

    @Override
    public String toString() {
        return "QueryWhere{" +
                "property='" + property + '\'' +
                ", fields=" + fields +
                '}';
    }
}
