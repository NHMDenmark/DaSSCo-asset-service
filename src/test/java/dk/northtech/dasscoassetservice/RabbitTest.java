package dk.northtech.dasscoassetservice;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitTest {
    @Test
    void test() {
        ConnectionFactory factory = new ConnectionFactory();
// "guest"/"guest" by default, limited to localhost connections
        factory.setPassword("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICIxQ2dnUkdjY1l1ZmRYUENPdy1OZHRmNHFIZ2ZtbmV3bFh0b2JJYUVKbWVRIn0.eyJleHAiOjE3MjI1ODg5NzAsImlhdCI6MTcyMjU4ODY3MCwianRpIjoiNGQ2MmY1OTMtODllMi00MWVlLTgyMjktODJlNzdhZWJjNWUxIiwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgzL3JlYWxtcy9yYWJiaXRtcSIsImF1ZCI6WyJyYWJiaXQtdGVzdCIsImFjY291bnQiXSwic3ViIjoiOTM2OWU2M2MtYTg4Yi00NGJmLTgxNmQtOTNiYjc1YTQ5NTEzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoicmFiYml0LXRlc3QiLCJhY3IiOiIxIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtcmFiYml0bXEiLCJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJyYWJiaXRtcSBwcm9maWxlIGVtYWlsIiwiY2xpZW50SG9zdCI6IjE3Mi4xOC4wLjEiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsImNsaWVudElkIjoicmFiYml0LXRlc3QiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJzZXJ2aWNlLWFjY291bnQtcmFiYml0LXRlc3QiLCJjbGllbnRBZGRyZXNzIjoiMTcyLjE4LjAuMSIsInJhYmJpdG1xIjoicmFiYml0bXEifQ.JxxokcyCi1izD4PrxEPkmoh5FOYhN_sCSgBl7wVGkc2DEXcgiUUOuYdvEZekMs2CW3pXtQaBjX08YYYs146zpeHHm-ixQUE6eLmC1yP9B3lwmj0n30nO3pDGQJAUbhxUSra0Y1GZ8KhYCRyI--vB7STiIUP9sbQRBV1FFYN6Y8sKZJx_uxGL3KDog74ax5j36gLhSAF4fpm-LK11Vj8mR6qmepZiNe4pc81QuhXcerrKlgbrep1rxZMzp7Vi4wyQrvX1tYInGImzeVGr_3oyoHTV05QS2-9FCPQvaEe0Sg1H3J8kpb4VeDTdpvcR6fgRDLjDSS20w9LXTOsxbeqOqb8eExyQGiGC8RlidsQ-UqPMYURQnLFmZ-Xe6fT0mppUyGkhRb5zza2ctihHGYOdo35ymapHpKwBx-8nJw9-U4iLJSvFQ1sNhwJfXaAZsg3UW_Qu-FIEwLzwBgqU9e6hoOecXmuBX0MEzik_r61VKeP4vGFLm-8W0zqRckOHHSSpK41RsuM6KRZOpNzqfFsuRldzuTlIKDHWfF2xgJaD4XuQQ_8OaaWEQ95LN6OcKZVlEtsF7fl2cvImdkLNSohc8BkBRBWnhOdgR3lrhbQTclxibMLJ7j4EWIJ2qwB2JTSA5TmxHgfmzQp-Lwkxo5kMcDN9QyDIV0q__XPIPyDXC40");
        factory.setVirtualHost("/");
        factory.setHost("localhost");
        factory.setPort(5672);

        try {
            Connection conn = factory.newConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
