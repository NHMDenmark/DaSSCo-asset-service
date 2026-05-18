package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.configuration.IngestionConfiguration;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class IngestionClient {
    private static final Logger logger = LoggerFactory.getLogger(IngestionClient.class);
    private final IngestionConfiguration ingestionConfiguration;

    @Inject
    public IngestionClient(IngestionConfiguration ingestionConfiguration) {
        this.ingestionConfiguration = ingestionConfiguration;
    }

    public String generateGuid(String institution) {
        if (institution == null || institution.isBlank()) {
            throw new IllegalArgumentException("institution cannot be null or blank");
        }
        try {
            String body = new Gson().toJson(new GuidRequest(institution));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(ingestionConfiguration.url() + "/metadata/guid"))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            try (HttpClient httpClient = HttpClient.newBuilder().build()) {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("Failed to generate guid from ingestion, got status " + response.statusCode());
                }
                String guid = response.body() == null ? "" : response.body().trim();
                if (guid.isEmpty()) {
                    throw new RuntimeException("Failed to generate guid from ingestion, empty response body");
                }
                return guid;
            }
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.error("Failed to generate guid from ingestion service", e);
            throw new RuntimeException("Failed to generate guid from ingestion service", e);
        }
    }

    private record GuidRequest(String institution) {}
}
