package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public record Institution(
        @Schema(description = "The name of the institution which owns and digitised the specimen", example = "test-institution")
        String name
        ,
        @ArraySchema(schema = @Schema( description = "The roles you need to access assets within this institution",implementation = Role.class))
        List<Role> roleRestriction) {


        public Institution(String name) {
                this(name, new ArrayList<>());
        }
}
