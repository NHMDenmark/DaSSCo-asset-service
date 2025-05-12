package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;
import java.util.Objects;

@Schema(description = "URL to external publisher sites to which the asset is published")
public record Publication(
        Long publication_id,
        @Schema(description = "The Global Unique Identifier generated for each asset", example = "ti-a01-202305241657")
        String asset_guid,
        @Schema(description = "Description of the publication")
        String description,
        @Schema(description = "Name of the publisher")
        String name
) {
    @JdbiConstructor
    public Publication {
    }

    public Publication(String asset_guid, String description, String name) {
        this(null, asset_guid, description, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Publication that = (Publication) o;
        return Objects.equals(asset_guid, that.asset_guid) && Objects.equals(description, that.description) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(asset_guid, description, name);
    }
}
