package dk.northtech.dasscoassetservice.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

public record InstitutionRoleRestriction(String institution_name, Role role) {

    @JdbiConstructor
    public InstitutionRoleRestriction(String institution_name, Role role) {
        this.institution_name = institution_name();
        this.role = role;
    }
}
