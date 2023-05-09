package dk.northtech.dasscoassetservice.domain;

import java.time.Instant;

public record PublicationLink(String asset_guid, String link, String publisher_name, Instant timestamp) {

}
