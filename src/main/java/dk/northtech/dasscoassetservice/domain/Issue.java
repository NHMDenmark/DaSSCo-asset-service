package dk.northtech.dasscoassetservice.domain;

import java.util.Objects;

public record Issue(String issue) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Issue issue1 = (Issue) o;
        return Objects.equals(issue, issue1.issue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issue);
    }
}
