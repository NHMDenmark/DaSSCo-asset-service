package dk.northtech.dasscoassetservice.services;

import com.google.gson.Gson;
import dk.northtech.dasscoassetservice.configuration.FileProxyConfiguration;
import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.webapi.domain.AssetSmbRequest;
import dk.northtech.dasscoassetservice.webapi.domain.SambaInfo;
import dk.northtech.dasscoassetservice.webapi.domain.SambaRequestStatus;
import dk.northtech.dasscoassetservice.webapi.domain.SmbRequest;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

@Service
public class FileProxyClient {
    private static final Logger logger = LoggerFactory.getLogger(FileProxyClient.class);
    public FileProxyConfiguration fileProxyConfiguration;

    @Inject
    public FileProxyClient(FileProxyConfiguration fileProxyConfiguration) {
        this.fileProxyConfiguration = fileProxyConfiguration;
    }

    public SambaInfo openSamba(MinimalAsset asset, User user) {
        Gson gson = new Gson();
        try {
            SmbRequest smbRequest = new SmbRequest();
            smbRequest.assets.add(asset);
            smbRequest.users.add(user.username);
            String json = gson.toJson(smbRequest);
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", user.token).uri(new URI(fileProxyConfiguration.url() + "/opensamba"))
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
                logger.error("Failed to get share");
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
//    public void pauseSamba(AssetSmbRequest assetSmbRequest, MinimalAsset asset, String token, String username) {
//        Gson gson = new Gson();
//        try {
//            SmbRequest smbRequest = new SmbRequest();
//            smbRequest.assets.add(asset);
//            smbRequest.users.add(username);
//            gson.toJson(smbRequest);
//            HttpRequest.newBuilder().header("Authorization", token).uri(new URI("fileProxyConfiguration.url()")).POST(HttpRequest.BodyPublishers.);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to contact fileProxy");
//        }
//    }

    public SambaInfo closeSamba(User user, AssetSmbRequest assetSmbRequest) {
        SmbRequest smbRequest = new SmbRequest();
        smbRequest.users = Arrays.asList(user.username);
        Gson gson = new Gson();
        String json = gson.toJson(assetSmbRequest);
        SambaInfo sambaInfo = new SambaInfo();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", user.token).uri(new URI(fileProxyConfiguration.url() + "/closesamba"))
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
        smbRequest.users = Arrays.asList(user.username);
        Gson gson = new Gson();
        String json = gson.toJson(assetSmbRequest);
        SambaInfo sambaInfo = new SambaInfo();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .header("Authorization", user.token).uri(new URI(fileProxyConfiguration.url() + "/closesamba"))
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
                logger.error("Failed to get share");
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


}
