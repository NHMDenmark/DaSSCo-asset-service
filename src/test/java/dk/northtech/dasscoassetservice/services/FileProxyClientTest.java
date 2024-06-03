package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.AssetStatus;
import dk.northtech.dasscoassetservice.domain.MinimalAsset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.webapi.domain.HttpInfo;
import dk.northtech.dasscoassetservice.webapi.domain.HttpShareRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.google.common.truth.Truth.assertThat;

// TODO: Not absolutely sure if I should be testing this class.
// TODO: I figured out that dassco-file-proxy needs to be running to run these tests, so I need to comment the tests out for cleaning package.
public class FileProxyClientTest extends AbstractIntegrationTest{
    /*
    User user = new User("test-suite-service-user");
    HttpClient httpClient = HttpClient.newHttpClient();

    /*
    @Test
    void testPrepareWorkDirAllocationIs0(){
        user.token = getToken();
        MinimalAsset asset = new MinimalAsset("testPrepareWorkdir", "", "institution_2", "i2_c1");
        HttpShareRequest httpShareRequest = new HttpShareRequest();
        httpShareRequest.assets.add(asset);
        httpShareRequest.allocation_mb = 0;
        HttpInfo response = fileProxyClient.prepareWorkDir(httpShareRequest, user);
    }


    @Test
    void testPrepareWorkDir(){
        user.token = getToken();
        Asset asset = new Asset();
        asset.institution = "institution_2";
        asset.asset_guid = "testPrepareWorkDir";
        asset.asset_pid = "pid-testPrepareWorkDir";
        asset.workstation = "i2_w1";
        asset.pipeline = "i2_p1";
        asset.status = AssetStatus.BEING_PROCESSED;
        asset.collection = "i2_c1";
        assetService.persistAsset(asset, user, 1);
        MinimalAsset minimalAsset = new MinimalAsset("testPrepareWorkdir", null, "institution_2", "i2_c1");
        HttpShareRequest httpShareRequest = new HttpShareRequest();
        httpShareRequest.assets.add(minimalAsset);
        httpShareRequest.allocation_mb = 1;
        HttpInfo response = fileProxyClient.prepareWorkDir(httpShareRequest, user);
        assertThat(response.http_allocation_status().httpCode).isEqualTo(200);
        assertThat(response.http_allocation_status().toString()).isEqualTo("SUCCESS");
    }

    // Same method than in the Test-Suite. I needed a token to send in the headers otherwise I didn't get an answer from dassco-file-proxy.
    public String getToken(){
        // Parameters for getting the Token.
        Map<String, String> requestBodyParams = new HashMap<>();
        requestBodyParams.put("client_id", "test-suite-service-user");
        requestBodyParams.put("client_secret", "e7vyJaOF1IGhQdl8QLNrO4nR6y03qTak");
        requestBodyParams.put("grant_type", "client_credentials");
        requestBodyParams.put("scope", "openid");
        String requestBody = requestBodyParams.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce((p1, p2) -> p1 + "&" + p2)
                .orElse("");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8083/realms/dassco/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // Save the Token
                JSONObject jsonResponse = new JSONObject(response.body());
                return jsonResponse.getString("access_token");
            } else {
                System.err.println("Failed to obtain access token. HTTP Status: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    */
}
