package dk.northtech.dasscoassetservice.domain;

import java.time.Instant;

public record PublicationLink(String assetGuid, String link, String publisherName, Instant timestamp) {

}
