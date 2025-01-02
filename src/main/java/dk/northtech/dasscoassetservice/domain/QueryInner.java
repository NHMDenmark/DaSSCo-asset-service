package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import static dk.northtech.dasscoassetservice.domain.QueryDataType.*;

public class QueryInner {
    @Schema(description = "The operator type", example = "=")
    public String operator;
    @Schema(description = "The field on the right-hand side of the statement", example = "\"test_name\"")
    public String value;
    @Schema(description = "The datatype of the value", example = "date")
    public QueryDataType dataType;

    public QueryInner(String operator, String value, QueryDataType dataType) {
        this.operator = operator;
        this.value = value;
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return "QueryInner{" +
                "operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                ", dataType=" + dataType +
                '}';
    }

    public String toBasicQueryString(String match, String property, QueryDataType dataType) {
        match = match.concat(".");

        if (operator.equalsIgnoreCase("equal")) {
            operator = "=";
        }

        if (match.contains("c")) { // is collection name, which means the value is sent as "inst_name.coll_name"
            String[] splitValue = value.split("\\.");
            if (splitValue.length > 1) {
                value = splitValue[1];
            }
        }

        if (dataType.equals(LIST)) {
             return "'" + value + "'" + " IN " + match + property;
        }

        if (dataType.equals(BOOLEAN)) {
             return match + property + operator + value;
        }
//        if (property.equalsIgnoreCase("tags")) {
//            System.out.println("whelp"); // todo
//        }
        if (dataType.equals(DATE)) {
            return switch (operator.toLowerCase()) {
                case "between" -> {
                    String[] dates = value.split("#"); // if between, the value is {timestampStart}#{timestampEnd}
                    yield match + property + " >= " + dates[0] + " and " + match + property + " <= " + dates[1]; // if between, the value is {timestampStart}#{timestampEnd}
                }
                case "before" -> match + property + " <= " + value; // no '' on timestamps
                case "after" -> match + property + " >= " + value; // no '' on timestamps
                default -> match + property + " " + operator + " " + value; // no '' on timestamps
            };
        }

        if (match.contains("parent")) {
            return "toLower(" + match + "asset_guid" + ") " + operator + " toLower('" + value + "')";
        }

        return "toLower(" + match + property + ") " + operator + " toLower('" + value + "')";
    }
}
