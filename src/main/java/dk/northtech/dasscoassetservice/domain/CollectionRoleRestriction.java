package dk.northtech.dasscoassetservice.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

public record CollectionRoleRestriction(int collection_id, Role role) {

    @JdbiConstructor
    public CollectionRoleRestriction(int collection_id, Role role) {
        this.collection_id = collection_id;
        this.role = role;
    }
}
