package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.Objects;

public record Role(
        @Schema(description = "The role", example = "test-role")
        String name) {
        @Override
        public boolean equals(Object o) {
                if (o == null || getClass() != o.getClass()) return false;
                Role role = (Role) o;
                return Objects.equals(name, role.name);
        }

        @Override
        public int hashCode() {
                return Objects.hashCode(name);
        }
        @JdbiConstructor
        public Role(@Schema(description = "The role", example = "test-role")
                    String name) {
                this.name = name;
        }
}
