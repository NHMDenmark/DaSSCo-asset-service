package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.domain.PublicationLink;
import dk.northtech.dasscoassetservice.domain.Publisher;
import dk.northtech.dasscoassetservice.repositories.PublisherRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class PublicationService {
    public AssetService assetService;
    public Jdbi jdbi;

    @Inject
    public PublicationService(AssetService assetService, Jdbi jdbi) {
        this.assetService = assetService;
        this.jdbi = jdbi;
    }


    public PublicationLink publish(PublicationLink publicationLink) {
        if(Strings.isNullOrEmpty(publicationLink.asset_guid())) {
            throw new IllegalArgumentException("Asset asset_guid cannot be null or empty");
        }
        if(Strings.isNullOrEmpty(publicationLink.link())) {
            throw new IllegalArgumentException("Link cannot be null or empty");
        }
        if(publicationLink.timestamp() == null) {
            publicationLink = new PublicationLink(publicationLink.asset_guid(),publicationLink.link(), publicationLink.publisher_name(), Instant.now());
        }
        jdbi.onDemand(PublisherRepository.class).publish(publicationLink);
        return publicationLink;
    }

    public void delete(PublicationLink publicationLink) {
        if(Strings.isNullOrEmpty(publicationLink.asset_guid())) {
            throw new IllegalArgumentException("Asset asset_guid cannot be null or empty");
        }
        if(Strings.isNullOrEmpty(publicationLink.link())) {
            throw new IllegalArgumentException("Link cannot be null or empty");
        }
        jdbi.onDemand(PublisherRepository.class).delete(publicationLink);
    }

    public List<PublicationLink> list(Publisher publisher) {
        return jdbi.onDemand(PublisherRepository.class).listPublicationLinks(publisher);
    }
}
