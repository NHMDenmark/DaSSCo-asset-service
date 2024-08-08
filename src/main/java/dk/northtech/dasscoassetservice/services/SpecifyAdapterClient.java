package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.configuration.SpecifyAdapterConfiguration;
import dk.northtech.dasscoassetservice.domain.AdapterReturnObj;
import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import dk.northtech.dasscoassetservice.webapi.domain.HttpShareRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class SpecifyAdapterClient {
    private static final Logger logger = LoggerFactory.getLogger(SpecifyAdapterClient.class);
    public SpecifyAdapterConfiguration specifyAdapterConfiguration;

    @Inject
    public SpecifyAdapterClient(SpecifyAdapterConfiguration specifyAdapterConfiguration) {
        this.specifyAdapterConfiguration = specifyAdapterConfiguration;
    }

    public int sendAssets(String jsonAsset, User user) {
        System.out.println("trying to send :0");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + user.token)
                    .uri(new URI(specifyAdapterConfiguration.url() + "/assets/"))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonAsset))
                    .build();
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = send.body();
            System.out.println("has been sent?");


            System.out.println(body);

            if (body != null) System.out.println(body);

            return send.statusCode();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send asset to the Specify adapter.", e);
        }
    }
}
