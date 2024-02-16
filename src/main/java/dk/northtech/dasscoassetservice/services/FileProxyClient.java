package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.webapi.domain.*;
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


    public HttpInfo prepareWorkDir(HttpShareRequest httpShareRequest, User user) {
        Gson gson = new Gson();
        try {
            httpShareRequest.users.add(user.username);
            String json = gson.toJson(httpShareRequest);
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", "Bearer " + user.token).uri(new URI(fileProxyConfiguration.url() + "/shares/assets/"+httpShareRequest.assets.get(0).asset_guid() + "/createShare"))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpClient httpClient = HttpClient.newBuilder().build();
            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = send.body();
            if (send.statusCode() > 199 && send.statusCode() < 300) {
                return gson.fromJson(body, HttpInfo.class);
            }
//            HttpInfo sambaInfo = new HttpInfo();
//            sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;
            if (send.statusCode() == 503) {
                return new HttpInfo("Fileservice is currently unavailable, please try again later", HttpAllocationStatus.UPSTREAM_ERROR);
            } else {
                return new HttpInfo( "FileService encountered an error when attempting to create workdir", HttpAllocationStatus.UPSTREAM_ERROR);
            }
        } catch (Exception e) {
            logger.error("Failed to prepare workdir due to an internal errorin asset service", e);
            return new HttpInfo("Failed to prepare workdir due an internal error, metadata has not been persisted", HttpAllocationStatus.INTERNAL_ERROR);
        }
    }

//    public SambaInfo openSamba(MinimalAsset asset1, User user) {
//        HttpShareRequest httpShareRequest = new HttpShareRequest();
//        httpShareRequest.assets.add(new MinimalAsset(asset1.asset_guid(), asset1.parent_guid()));
//        httpShareRequest.users.add(user.username);
//        return openSamba(httpShareRequest, user);
//    }

    public HttpInfo openHttpShare(MinimalAsset asset1, User user, int allocation) {
        HttpShareRequest httpShareRequest = new HttpShareRequest();
        httpShareRequest.assets.add(asset1);
        httpShareRequest.users.add(user.username);
        httpShareRequest.allocation_mb = allocation;
        return prepareWorkDir(httpShareRequest, user);
    }

//    public SambaInfo openSamba(AssetSmbRequest assetSmbRequest, User user) {
//        if(assetSmbRequest.asset() != null) {
//            Optional<Asset> optionalAsset = assetService.getAsset(assetSmbRequest.asset().asset_guid());
//            if (optionalAsset.isPresent()) {
//                Asset asset1 = optionalAsset.get();
//                assetSmbRequest = new AssetSmbRequest(assetSmbRequest.shareName(), new MinimalAsset(asset1.asset_guid, asset1.parent_guid));
//            } else {
//                 throw new IllegalArgumentException("Asset [" + assetSmbRequest.asset().asset_guid() + "] does not exist");
//            }
//        }
//        return openSamba(user, assetSmbRequest);
//
//    }

//    public SambaInfo openSamba(User user, AssetSmbRequest assetSmbRequest) {
//        Gson gson = new Gson();
//        String json = gson.toJson(assetSmbRequest);
//        SambaInfo sambaInfo = new SambaInfo();
//        try {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .header("Authorization", "Bearer " + user.token)
//                    .header("Content-Type", MediaType.APPLICATION_JSON)
//                    .uri(
//                            new URIBuilder(fileProxyConfiguration.url() + "/samba/openShare")
//                                    .build())
//                    .POST(HttpRequest.BodyPublishers.ofString(json))
//                    .build();
//            HttpClient httpClient = HttpClient.newBuilder().build();
//            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//            String body = send.body();
//            if (send.statusCode() > 199 && send.statusCode() < 300) {
//                return gson.fromJson(body, SambaInfo.class);
//            }
//            sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;
//            if (send.statusCode() == 503) {
//                sambaInfo.sambaRequestStatusMessage = "Service unavailable";
//            } else {
//                logger.error("Failed to open share");
//                sambaInfo.sambaRequestStatusMessage = "Server encountered an error when attempting to open share, please try manually checking out the asset later";
//            }
//            logger.error("Failed to open SMB share, http status code: {}, response body: {}", send.statusCode(), body);
//            return sambaInfo;
//        } catch (Exception e) {
//            sambaInfo.sambaRequestStatus = SambaRequestStatus.INTERNAL_ERROR;
//            sambaInfo.sambaRequestStatusMessage = "Failed to open SMB due to an internal error";
//            logger.error("Failed to get samba share due to an internal error", e);
//            return sambaInfo;
//        }
//    }

    public SambaInfo closeSamba(User user, AssetUpdateRequest assetSmbRequest, boolean syncErda) {
        Gson gson = new Gson();
        String json = gson.toJson(assetSmbRequest);
        SambaInfo sambaInfo = new SambaInfo();
        if(syncErda) {
            if(assetSmbRequest.minimalAsset() == null) {
                throw new IllegalArgumentException("Asset missing");
            }
            Optional<Asset> asset = assetService.getAsset(assetSmbRequest.minimalAsset().asset_guid());
            if(asset.isEmpty()) {
                throw new IllegalArgumentException("Asset not found");
            }
            if(asset.get().asset_locked) {
                throw new DasscoIllegalActionException("Asset locked");
            }
        }
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
                sambaInfo.sambaRequestStatusMessage = "Server encountered an error when attempting to close share, please try manually checking if the share has been closed";
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

//    public SambaInfo disconnectSamba(User user, AssetSmbRequest assetSmbRequest) {
//        HttpShareRequest httpShareRequest = new HttpShareRequest();
//        httpShareRequest.users = Set.of(user.username);
//        Gson gson = new Gson();
//        String json = gson.toJson(assetSmbRequest);
//        SambaInfo sambaInfo = new SambaInfo();
//        try {
//            HttpRequest request = HttpRequest.newBuilder()
//                    .header("Authorization", "Bearer " + user.token).uri(new URI(fileProxyConfiguration.url() + "/samba/disconnectShare"))
//                    .header("Content-Type", MediaType.APPLICATION_JSON)
//                    .POST(HttpRequest.BodyPublishers.ofString(json))
//                    .build();
//            HttpClient httpClient = HttpClient.newBuilder().build();
//            HttpResponse<String> send = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
//            String body = send.body();
//            if (send.statusCode() > 199 && send.statusCode() < 300) {
//                return gson.fromJson(body, SambaInfo.class);
//            }
//            if (send.statusCode() == 503) {
//                sambaInfo.sambaRequestStatus = SambaRequestStatus.UPSTREAM_ERROR;
//                sambaInfo.sambaRequestStatusMessage = "Service unavailable";
//            } else {
//                logger.error("Failed to disconnect share");
//                sambaInfo.sambaRequestStatusMessage = "Server encountered an error when attempting to close share, please try manually checking out the asset later";
//            }
//            logger.error("Failed to disconnect SMB share, http status code: {}, response body: {}", send.statusCode(), body);
//            return sambaInfo;
//        } catch (Exception e) {
//            sambaInfo.sambaRequestStatus = SambaRequestStatus.INTERNAL_ERROR;
//            sambaInfo.sambaRequestStatusMessage = "Failed to disconnect SMB due to an internal error";
//            logger.error("Failed to disconnect samba share due to an internal error", e);
//            return sambaInfo;
//        }
//    }
}
