package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.domain.specifyarssync.SyncParkingSpaceRequest;
import dk.northtech.dasscoassetservice.webapi.domain.HttpAllocationStatus;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import dk.northtech.dasscoassetservice.webapi.domain.HttpShareRequest;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
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
import java.time.LocalDateTime;

@Service
public class FileProxyClient {
    private static final Logger logger = LoggerFactory.getLogger(FileProxyClient.class);
    public FileProxyConfiguration fileProxyConfiguration;
    public AssetService assetService;
    public KeycloakService keycloakService;

    @Inject
    public FileProxyClient(FileProxyConfiguration fileProxyConfiguration, AssetService assetService, KeycloakService keycloakService) {
        this.fileProxyConfiguration = fileProxyConfiguration;
        this.assetService = assetService;
        this.keycloakService = keycloakService;
    }


    public HttpInfo prepareWorkDir(HttpShareRequest httpShareRequest, User user) {
        Gson gson = new Gson();
        try {
            LocalDateTime fileProxyCallStart = LocalDateTime.now();
            logger.info("#4: Call to FileProxy (CreateShareInternal)");
            httpShareRequest.users.add(user.username);
            String json = gson.toJson(httpShareRequest);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + user.token).uri(new URI(fileProxyConfiguration.url() + "/shares/assets/" + httpShareRequest.assets.get(0).asset_guid() + "/createShareInternal"))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            HttpRequest request = requestBuilder.build();
            HttpClient httpClient = HttpClient.newBuilder()
                    .build();
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            LocalDateTime fileProxyCallEnd = LocalDateTime.now();
            logger.info("#4 took {} ms", java.time.Duration.between(fileProxyCallStart, fileProxyCallEnd).toMillis());
            String body = send.body();
            if (send.statusCode() > 199 && send.statusCode() < 300) {
                return gson.fromJson(body, HttpInfo.class);
            }
//            HttpInfo sambaInfo = new HttpInfo();
//            sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;;
            // TODO: If I send 0 Allocation it returns a 400 error: This is not contemplated in the cases here, and it makes the
            // TODO: response HttpInfo just an empty object with null in all the fields.
            // TODO: Should I add a case for when response is 400?
            if (body != null) {
                try {
                    HttpInfo httpInfo = gson.fromJson(body, HttpInfo.class);
                    return httpInfo;
                } catch (Exception e) {
                    // see next catch
                }
                try {
                    DaSSCoError daSSCoError = gson.fromJson(body, DaSSCoError.class);
                    return new HttpInfo(daSSCoError.errorMessage, HttpAllocationStatus.BAD_REQUEST);
                } catch (Exception e) {
                    logger.error("Failed to parse body, content: {}", body);
                }
                ;
            }
            if (send.statusCode() == 503) {
                return new HttpInfo("Fileservice is currently unavailable, please try again later", HttpAllocationStatus.UPSTREAM_ERROR);
            }
            return new HttpInfo("FileService encountered an error when attempting to create workdir", HttpAllocationStatus.UPSTREAM_ERROR);
        } catch (IOException connex) {
            logger.error("Failed to prepare workdir cannot contact fileproxy", connex);
            return new HttpInfo("Failed to prepare workdir cannot contact file-proxy, metadata has not been persisted", HttpAllocationStatus.UPSTREAM_ERROR);
        } catch (URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpInfo openHttpShare(MinimalAsset asset1, User user, int allocation) {
        HttpShareRequest httpShareRequest = new HttpShareRequest();
        httpShareRequest.assets.add(asset1);
        httpShareRequest.users.add(user.username);
        httpShareRequest.allocation_mb = allocation;
        return prepareWorkDir(httpShareRequest, user);
    }

    public void syncParkedFile(SyncParkingSpaceRequest syncParkingSpaceRequest) {
        Gson gson = new Gson();
        try {
            String json = gson.toJson(syncParkingSpaceRequest);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + keycloakService.getUserServiceToken())
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .uri(new URI(fileProxyConfiguration.url() + "/assetfiles/syncparkedfiles/"))
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            HttpRequest request = requestBuilder.build();
            try (HttpClient httpClient = HttpClient.newBuilder()
                    .build();) {
                HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (send.statusCode() == 503) {
                    throw new RuntimeException("File proxy appears to be down, got 503 response");
                }
                if(send.statusCode() > 299) {
                    if(send.body() != null) {
                        System.out.println(send.body());
                    }
                    throw new RuntimeException("Failed to sync parked files, got status " + send.statusCode());
                }
            }


        } catch (URISyntaxException | InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
