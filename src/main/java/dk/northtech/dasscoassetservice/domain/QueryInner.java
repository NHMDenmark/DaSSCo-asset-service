package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

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

    public Map<String, Map<String, Object>> toBasicPostgreSQLQueryString(String column, String table, int index) {
        String eventfiler = table.equals("event") ? "(#BASE# and %s)".formatted(this.eventTypeByMetadataColumn(column)) : "#BASE#";
        if(table.equals("event") && List.of("after", "before", "between").contains(operator.toLowerCase())) {
            column = "event.timestamp::date"; // move it to QueryItem?
        }else{
            QueryItemField queryItemField = QueryItemField.fromDisplayName(column);
            if(queryItemField != null) {
                column = queryItemField.getFieldName();
            }
        }


        if(operator.equalsIgnoreCase("equal") && ((!table.equals("event")) || !dataType.name().equals("BOOLEAN"))) {
            operator = "=";
            String preparedParam = "%s_%s".formatted(column, index);
            String sql = eventfiler.replace("#BASE#", "%s %s %s".formatted(column, operator, ":" + preparedParam));
            return Map.of(sql, Map.of(preparedParam, value));
        }
        if(operator.equalsIgnoreCase("starts with")) {
            operator = "ILIKE";
            String preparedParam = "%s_%s".formatted(column, index);
            String sql = eventfiler.replace("#BASE#", "%s %s %s || '%%'".formatted(column, operator, ":" + preparedParam));
            return Map.of(sql, Map.of(preparedParam, value));
        }
        if(operator.equalsIgnoreCase("ends with")) {
            operator = "ILIKE";
            String preparedParam = "%s_%s".formatted(column, index);
            String sql = eventfiler.replace("#BASE#", "%s %s '%%' || %s".formatted(column, operator, ":" + preparedParam));
            return Map.of(sql, Map.of(preparedParam, value));
        }
        if(operator.equalsIgnoreCase("contains")) {
            operator = "ILIKE";
            String preparedParam = "%s_%s".formatted(column, index);
            String sql = eventfiler.replace("#BASE#", "%s %s '%%' || %s || '%%'".formatted(column, operator, ":" + preparedParam));
            return Map.of(sql, Map.of(preparedParam, value));
        }
        if(operator.equalsIgnoreCase("empty")) {
            operator = "IS NULL";
            String sql = eventfiler.replace("#BASE#", "%s %s".formatted(column, operator));
            return Map.of(sql, Map.of());
        }
        if(operator.equalsIgnoreCase("in")) {
            operator = "=";
            String preparedParam = "%s_%s".formatted(column, index);
            String sql = eventfiler.replace("#BASE#", "upper(%s) %s any(%s)".formatted(":" + preparedParam, operator, column));
            return Map.of(sql, Map.of(preparedParam, value));
        }
        if(operator.equalsIgnoreCase("after")) {
            operator = "<";
            String preparedParam = "%s_%s".formatted("after", index);
            String sql = eventfiler.replace("#BASE#", "%s %s to_timestamp(%s)".formatted(column, operator, ":" + preparedParam));
            return Map.of(sql, Map.of(preparedParam, (Long.parseLong(value) / 1000)));
        }
        if(operator.equalsIgnoreCase("before")) {
            operator = ">";
            String preparedParam = "%s_%s".formatted("before", index);
            String sql = eventfiler.replace("#BASE#", "%s %s to_timestamp(%s)".formatted(column, operator, ":" + preparedParam));
            return Map.of(sql, Map.of(preparedParam, (Long.parseLong(value) / 1000)));
        }
        if(operator.equalsIgnoreCase("between")) {
            operator = "BETWEEN";
            String preparedParamFirst = "%s1_%s".formatted("start_date", index);
            String preparedParamSecond = "%s2_%s".formatted("end_data", index);
            String[] times = value.split("#");
            if(times.length == 2) {
                var first = Long.parseLong(times[0])/1000;
                var second = Long.parseLong(times[1])/1000;
                String sql = eventfiler.replace("#BASE#", "%s %s to_timestamp(%s) and to_timestamp(%s)".formatted(column, operator, ":" + preparedParamFirst, ":" + preparedParamSecond));
                return Map.of(sql, Map.of(preparedParamFirst, first, preparedParamSecond, second));
            }
        }
        if(table.equals("event")) {
            return Map.of(eventfiler.replace("#BASE# and ", ""), Map.of());
        }

        return Map.of();
    }

    public String toBasicQueryString(String match, String property, QueryDataType dataType) {
        match = match.concat(".");

        if (operator.equalsIgnoreCase("equal")) {
            operator = "=";
        }

        if (operator.equalsIgnoreCase("empty")) {
            if (property.equalsIgnoreCase("parent_guid")) {
                return "NOT EXISTS ((a)-[:CHILD_OF]->(:Asset))";
            }
            operator = " IS NULL";
            return match + property + operator;
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

    public String eventTypeByMetadataColumn(String column) {
        switch (column) {
            case "asset_created_by": return " event = 'CREATE_ASSET'";
            case "asset_updated_by", "date_asset_updated_ars": return " event = 'UPDATE_ASSET'";
            case "audited": return " event %s 'AUDIT_ASSET'".formatted(value.equalsIgnoreCase("true") ? "=" : "!=");
            case "audited_by": return " event = 'AUDIT_ASSET'";
            case "date_asset_created_ars": return "event = 'CREATE_ASSET'";
            case "date_asset_deleted_ars": return " event = 'DELETE_ASSET'";
            case "date_audited": return " event = 'AUDIT_ASSET_METADATA'";
            case "date_metadata_created_ars": return " event = 'CREATE_ASSET_METADATA'";
            case "date_metadata_updated_ars": return " event = 'UPDATE_ASSET_METADATA'";
            case "date_pushed_to_specify": return " event = 'SYNCHRONISE_SPECIFY'";
            case "metadata_created_by": return " event = 'CREATE_ASSET_METADATA'";
            case "metadata_updated_by": return " event = 'UPDATE_ASSET_METADATA'";
            default: return "";
        }
    }
}
