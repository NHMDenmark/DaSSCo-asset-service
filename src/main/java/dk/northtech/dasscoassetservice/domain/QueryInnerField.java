package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

public class QueryInnerField {
    @Schema(description = "The operator type", example = "=")
    public String operator;
    @Schema(description = "The field on the right-hand side of the statement", example = "\"test_name\"")
    public String value;

    public QueryInnerField(String operator, String value) {
        this.operator = operator;
        this.value = value;
    }

    @Override
    public String toString() {
        return "QueryInnerField{" +
                ", operator='" + operator + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

//    public String toBasicQueryString(String match) {
//        match = match.concat(".");
//        if (property.equalsIgnoreCase("file_formats") || property.equalsIgnoreCase("restricted_access")) { // todo should we show restricted_access haha?
//             return "'" + value + "'" + " IN " + match + property;
//        }
////        if (property.equalsIgnoreCase("tags")) {
////            System.out.println("whelp"); // todo
////        }
//        if (property.contains("date") || property.contains("timestamp")) {
//            if (operator.equalsIgnoreCase("range")) {
//                String[] dates = value.split("#"); // if range, the value is {timestampStart}#{timestampEnd}
//                return match + property + " >= " + dates[0] + " and " + match + property + " <= " + dates[1];
//            }
//        }
//        return match + property + " " + operator + " '" + value + "'";
//    }
}
