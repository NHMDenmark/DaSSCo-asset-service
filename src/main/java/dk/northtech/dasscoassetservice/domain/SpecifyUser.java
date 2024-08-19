package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;

public record SpecifyUser(
        @Schema(description = "Username", example = "moogie")
        String username,
        @Schema(description = "URL to the Specify instance belonging to the institution", example = "https://specify-url.net") // don't actually have the correct urls yet
        String url,
        @Schema(description = "Institution the user is linked to", example = "NNAD")
        String institution){
}
