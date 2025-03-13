package dk.northtech.dasscoassetservice.domain;

import java.util.Objects;

public record Funding(String name) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Funding funding = (Funding) o;
        return Objects.equals(name, funding.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
