package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import static dk.northtech.dasscoassetservice.domain.QueryDataType.DATE;
import static dk.northtech.dasscoassetservice.domain.QueryDataType.LIST;

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
        if (dataType.equals(LIST)) {
             return value + "'" + " IN " + match + property;
        }
//        if (property.equalsIgnoreCase("tags")) {
//            System.out.println("whelp"); // todo
//        }
        if (dataType.equals(DATE)) {
            if (operator.equalsIgnoreCase("range")) {
                String[] dates = value.split("#"); // if range, the value is {timestampStart}#{timestampEnd}
                return match + property + " >= " + dates[0] + " and " + match + property + " <= " + dates[1];
            } else {
                return  match + property + " " + operator + " " + value; // no '' on timestamps
            }
        }
        return "toLower(" + match + property + ") " + operator + " toLower('" + value + "')";
    }
}
