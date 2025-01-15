package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The data type for the values of the inner where statements of the queries.", example = "date")
public enum QueryDataType {
    DATE
    , NUMBER
    , ENUM
    , STRING
    , LIST
    , BOOLEAN
}
