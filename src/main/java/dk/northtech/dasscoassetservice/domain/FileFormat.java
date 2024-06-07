package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The format of the asset", example = "JPEG")
public enum FileFormat {
    TIF
    , JPEG
    , RAW
    , RAF
    , CR3
    , DNG
    , TXT
}
