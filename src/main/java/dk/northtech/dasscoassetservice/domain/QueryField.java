package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class QueryField {
    @Schema(description = "The query type", example = "and")
    public String type;
    @Schema(description = "The operator type", example = "=")
    public String operator;
    @Schema(description = "The field on the left-hand side of the statement", example = "name")
    public String property;
    @Schema(description = "The field on the right-hand side of the statement", example = "\"test_name\"")
    public String value;

    public QueryField(String type, String operator, String property, String value) {
        this.type = type;
        this.operator = operator;
        this.property = property;
        this.value = value;
    }

    @Override
    public String toString() {
        return "QueryField{" +
                "type='" + type + '\'' +
                ", operator='" + operator + '\'' +
                ", property='" + property + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public String toBasicQueryString(String match) {
        match = match.concat(".");
        if (property.equalsIgnoreCase("file_formats") || property.equalsIgnoreCase("restricted_access")) { // todo should we show restricted_access haha?
             return "'" + value + "'" + " IN " + match + property;
        }
//        if (property.equalsIgnoreCase("tags")) {
//            System.out.println("whelp"); // todo
//        }
        if (property.contains("date") || property.contains("timestamp")) {
            if (operator.equalsIgnoreCase("range")) {
                String[] dates = value.split("#"); // if range, the value is {timestampStart}#{timestampEnd}
                return match + property + " >= " + dates[0] + " and " + match + property + " <= " + dates[1];
            }
        }
        return match + property + " " + operator + " '" + value + "'";
    }
}
