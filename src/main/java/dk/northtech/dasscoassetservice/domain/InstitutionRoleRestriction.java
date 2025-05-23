package dk.northtech.dasscoassetservice.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

public record InstitutionRoleRestriction(String institution_name, Role role) {

    @JdbiConstructor
    public InstitutionRoleRestriction {
    }
}
