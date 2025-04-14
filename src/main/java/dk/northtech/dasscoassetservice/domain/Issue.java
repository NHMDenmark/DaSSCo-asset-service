package dk.northtech.dasscoassetservice.domain;

import org.jdbi.v3.core.mapper.reflect.JdbiConstructor;

import java.time.Instant;
import java.util.Objects;

public record Issue(Integer issue_id, String asset_guid, String category, String name, Instant timestamp, String status, String description, String notes, Boolean solved) {
    @JdbiConstructor
    public Issue {
    }

    public Issue(String asset_guid, String category, String name, Instant timestamp, String status, String description, String notes, Boolean solved) {
        this(null, asset_guid, category, name, timestamp, status, description, notes, solved);
    }
}
