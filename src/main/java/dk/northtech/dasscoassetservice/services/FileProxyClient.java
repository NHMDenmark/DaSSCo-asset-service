package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.webapi.domain.AssetSmbRequest;
import dk.northtech.dasscoassetservice.webapi.domain.SambaInfo;
import dk.northtech.dasscoassetservice.webapi.domain.SambaRequestStatus;
import dk.northtech.dasscoassetservice.webapi.domain.SmbRequest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.Set;

@Service
public class FileProxyClient {
    private static final Logger logger = LoggerFactory.getLogger(FileProxyClient.class);
    public FileProxyConfiguration fileProxyConfiguration;
    public AssetService assetService;

    @Inject
    public FileProxyClient(FileProxyConfiguration fileProxyConfiguration, AssetService assetService) {
        this.fileProxyConfiguration = fileProxyConfiguration;
        this.assetService = assetService;
    }

    public SambaInfo openSamba(SmbRequest smbRequest, User user) {
        Gson gson = new Gson();
        try {
            smbRequest.users.add(user.username);
            String json = gson.toJson(smbRequest);
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + user.token).uri(new URI(fileProxyConfiguration.url() + "/samba/createShare"))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = send.body();
            if (send.statusCode() > 199 && send.statusCode() < 300) {
                return gson.fromJson(body, SambaInfo.class);
            }
            SambaInfo sambaInfo = new SambaInfo();
            sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;
            if (send.statusCode() == 503) {
                sambaInfo.sambaRequestStatusMessage = "Shares are temporarily unavailable, please try manually checking out the asset later";
            } else {
                sambaInfo.sambaRequestStatusMessage = "Server encountered an error when attempting to create share, please try manually checking out the asset later";
            }
            logger.error("Failed to get smb share from file-proxy, http status code: {}, response code: {}", send.statusCode(), body);
            return sambaInfo;
        } catch (Exception e) {
            SambaInfo sambaInfo = new SambaInfo();
            sambaInfo.sambaRequestStatus = SambaRequestStatus.INTERNAL_ERROR;
            sambaInfo.sambaRequestStatusMessage = "Failed to get samba share, please try manually checking out the asset";
            logger.error("Failed to get samba share due to an internal error", e);
            return sambaInfo;
        }
    }

    public SambaInfo openSamba(MinimalAsset asset1, User user) {
        SmbRequest smbRequest = new SmbRequest();
        smbRequest.assets.add(new MinimalAsset(asset1.guid(), asset1.parent_guid()));
        smbRequest.users.add(user.username);
        return openSamba(smbRequest, user);
    }

    public SambaInfo openSamba(AssetSmbRequest assetSmbRequest, User user) {
        if(assetSmbRequest.asset() != null) {
            Optional<Asset> optionalAsset = assetService.getAsset(assetSmbRequest.asset().guid());
            if (optionalAsset.isPresent()) {
                Asset asset1 = optionalAsset.get();
                assetSmbRequest = new AssetSmbRequest(assetSmbRequest.shareName(), new MinimalAsset(asset1.asset_guid, asset1.parent_guid));
            } else {
                 throw new IllegalArgumentException("Asset [" + assetSmbRequest.asset().guid() + "] does not exist");
            }
        }
        return openSamba(user, assetSmbRequest);

    }

    public SambaInfo openSamba(User user, AssetSmbRequest assetSmbRequest) {
        Gson gson = new Gson();
        String json = gson.toJson(assetSmbRequest);
        SambaInfo sambaInfo = new SambaInfo();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + user.token)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .uri(
                            new URIBuilder(fileProxyConfiguration.url() + "/samba/openShare")
                                    .build())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = send.body();
            if (send.statusCode() > 199 && send.statusCode() < 300) {
                return gson.fromJson(body, SambaInfo.class);
            }
            sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;
            if (send.statusCode() == 503) {
                sambaInfo.sambaRequestStatusMessage = "Service unavailable";
            } else {
                logger.error("Failed to open share");
                sambaInfo.sambaRequestStatusMessage = "Server encountered an error when attempting to open share, please try manually checking out the asset later";
            }
            logger.error("Failed to close SMB share, http status code: {}, response body: {}", send.statusCode(), body);
            return sambaInfo;
        } catch (Exception e) {
            sambaInfo.sambaRequestStatus = SambaRequestStatus.INTERNAL_ERROR;
            sambaInfo.sambaRequestStatusMessage = "Failed to open SMB due to an internal error";
            logger.error("Failed to get samba share due to an internal error", e);
            return sambaInfo;
        }
    }

    public SambaInfo closeSamba(User user, AssetUpdateRequest assetSmbRequest, boolean syncErda) {
        Gson gson = new Gson();
        String json = gson.toJson(assetSmbRequest);
        SambaInfo sambaInfo = new SambaInfo();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization","Bearer " + user.token)
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .uri(
                            new URIBuilder(fileProxyConfiguration.url() + "/samba/closeShare")
                                    .addParameter("syncERDA", String.valueOf(syncErda))
                                    .build())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = send.body();
            if (send.statusCode() > 199 && send.statusCode() < 300) {
                return gson.fromJson(body, SambaInfo.class);
            }
            sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;
            if (send.statusCode() == 503) {
                sambaInfo.sambaRequestStatusMessage = "Service unavailable";
            } else {
                logger.error("Failed to close share");
                sambaInfo.sambaRequestStatusMessage = "Server encountered an error when attempting to close share, please try manually checking out the asset later";
            }
            logger.error("Failed to close SMB share, http status code: {}, response body: {}", send.statusCode(), body);
            return sambaInfo;
        } catch (Exception e) {
            sambaInfo.sambaRequestStatus = SambaRequestStatus.INTERNAL_ERROR;
            sambaInfo.sambaRequestStatusMessage = "Failed to close SMB due to an internal error";
            logger.error("Failed to get samba share due to an internal error", e);
            return sambaInfo;
        }
    }

    public SambaInfo disconnectSamba(User user, AssetSmbRequest assetSmbRequest) {
        SmbRequest smbRequest = new SmbRequest();
        smbRequest.users = Set.of(user.username);
        Gson gson = new Gson();
        String json = gson.toJson(assetSmbRequest);
        SambaInfo sambaInfo = new SambaInfo();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + user.token).uri(new URI(fileProxyConfiguration.url() + "/samba/disconnectShare"))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = send.body();
            if (send.statusCode() > 199 && send.statusCode() < 300) {
                return gson.fromJson(body, SambaInfo.class);
            }
            if (send.statusCode() == 503) {
                sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;
                sambaInfo.sambaRequestStatusMessage = "Service unavailable";
            } else {
                logger.error("Failed to disconnect share");
                sambaInfo.sambaRequestStatusMessage = "Server encountered an error when attempting to close share, please try manually checking out the asset later";
            }
            logger.error("Failed to disconnect SMB share, http status code: {}, response body: {}", send.statusCode(), body);
            return sambaInfo;
        } catch (Exception e) {
            sambaInfo.sambaRequestStatus = SambaRequestStatus.INTERNAL_ERROR;
            sambaInfo.sambaRequestStatusMessage = "Failed to disconnect SMB due to an internal error";
            logger.error("Failed to disconnect samba share due to an internal error", e);
            return sambaInfo;
        }
    }
}
