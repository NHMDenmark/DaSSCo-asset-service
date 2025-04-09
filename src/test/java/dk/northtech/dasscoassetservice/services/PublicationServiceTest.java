package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled("disabled for now")
public class PublicationServiceTest extends AbstractIntegrationTest{

    // TODO: Publication endpoints don't seem to work properly. They are also hidden in the documentation.
    // I made these tests so if the endpoints are implemented I don't have to make them then:
    @Test
    void testPublish(){
        PublicationLink publicationLink = publicationService.publish(new PublicationLink("testPublicationLink", "test-link", "test-publisher", Instant.now()));
        assertThat(publicationLink.asset_guid()).isEqualTo("testPublicationLink");
        assertThat(publicationLink.link()).isEqualTo("test-link");
        assertThat(publicationLink.publisher_name()).isEqualTo("test-publisher");
    }

    @Test
    void testPublishInvalidAssetGuid(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> publicationService.publish(new PublicationLink("", "", "", Instant.now())));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset asset_guid cannot be null or empty");
    }

    @Test
    void testPublishInvalidLink(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> publicationService.publish(new PublicationLink("testPublicationLink", "", "", Instant.now())));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Link cannot be null or empty");
    }

    @Test
    void testPublishNoTimestamp(){
        PublicationLink publicationLink = publicationService.publish(new PublicationLink("test-publication", "test-link", "test-publisher", null));
        assertThat(publicationLink.asset_guid()).isEqualTo("test-publication");
        assertThat(publicationLink.link()).isEqualTo("test-link");
        assertThat(publicationLink.publisher_name()).isEqualTo("test-publisher");
        assertThat(publicationLink.timestamp()).isNotNull();
    }

    @Test
    void testPullNoAssetGuid(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> publicationService.pull(new PublicationLink("", "", "", null)));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Asset asset_guid cannot be null or empty");
    }

    @Test
    void testPullNoLink(){
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> publicationService.pull(new PublicationLink("test-publication", "", "", null)));
        assertThat(illegalArgumentException).hasMessageThat().isEqualTo("Link cannot be null or empty");
    }
}
