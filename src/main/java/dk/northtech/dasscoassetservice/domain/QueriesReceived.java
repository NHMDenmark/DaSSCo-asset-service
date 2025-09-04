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

    public String toPostgreSQL(int limit, boolean count, Set<String> collectionAccess, boolean fullAccess){
        AtomicInteger counter = new AtomicInteger();
        Map<String, String> params = new HashMap<>();
        return this.query.stream().map(query ->
            query.where.stream().map(queryWhere -> {
                var table = query.select;
                var column = queryWhere.property;
                return "(" + queryWhere.fields.stream().map(queryInner -> {
                    var index = counter.getAndIncrement();
                    params.put("%s__%s".formatted(column, index), queryInner.value);
                    return queryInner.toBasicPostgreSQLQueryString(column, table, index);
                }).collect(Collectors.joining(" or ")) + ")";
            }
        ).collect(Collectors.joining(" and "))).collect(Collectors.joining(" and "));
    }
}
