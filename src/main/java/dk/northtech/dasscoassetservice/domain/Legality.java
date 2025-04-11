package dk.northtech.dasscoassetservice.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.util.Objects;

public record Legality(Long legality_id,  String copyright, String license, String credit) {
    @JdbiConstructor
    public Legality {
    }

    public Legality( String copyright, String license, String credit) {
        this(null,  copyright, license, credit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Legality legality = (Legality) o;
        return Objects.equals(legality_id, legality.legality_id) && Objects.equals(copyright, legality.copyright) && Objects.equals(license, legality.license) && Objects.equals(credit, legality.credit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(legality_id, copyright, license, credit);
    }
}
