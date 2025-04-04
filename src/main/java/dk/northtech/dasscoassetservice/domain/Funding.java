package dk.northtech.dasscoassetservice.domain;

import java.util.Objects;

public record Funding(String funding, Integer funding_id) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Funding funding = (Funding) o;
        return Objects.equals(funding, funding.funding);
    }

    @Override
    public String toString() {
        return "Funding{" +
               "funding='" + funding + '\'' +
               ", funding_id=" + funding_id +
               '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(funding);
    }
}
