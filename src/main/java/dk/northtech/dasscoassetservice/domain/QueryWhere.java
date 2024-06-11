package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class QueryWhere {
    @Schema(description = "The field on the left-hand side of the statement", example = "name")
    public String property;
    @Schema(description = "The and clause for this property", example = "= \"test-name\"")
    public QueryInnerField and;
    @Schema(description = "The or clauses for this property", example = "CONTAINS \"nam\"")
    public List<QueryInnerField> or;

    public QueryWhere(String property, QueryInnerField and, List<QueryInnerField> or) {
        this.property = property;
        this.and = and;
        this.or = or;
    }

    @Override
    public String toString() {
        return "QueryWhere{" +
                "property='" + property + '\'' +
                ", and=" + and +
                ", or=" + or +
                '}';
    }
}
