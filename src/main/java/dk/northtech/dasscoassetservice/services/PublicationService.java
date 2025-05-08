package dk.northtech.dasscoassetservice.services;

import com.google.common.base.Strings;
import dk.northtech.dasscoassetservice.domain.Publication;
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
    public ExtendableEnumService extendableEnumService;

    @Inject
    public PublicationService(AssetService assetService, Jdbi jdbi, ExtendableEnumService extendableEnumService) {
        this.assetService = assetService;
        this.jdbi = jdbi;
        this.extendableEnumService = extendableEnumService;
    }

    public Publication publish(Publication publicationLink) {
        if (Strings.isNullOrEmpty(publicationLink.asset_guid())) {
            throw new IllegalArgumentException("Asset asset_guid cannot be null or empty");
        }
//          Out for now
//        if(Strings.isNullOrEmpty(publicationLink.description())) {
//            throw new IllegalArgumentException("Link cannot be null or empty");
//        }
        if (!extendableEnumService.checkExists(ExtendableEnumService.ExtendableEnum.EXTERNAL_PUBLISHER, publicationLink.name())) {
            throw new IllegalArgumentException("Publisher doesnt exist");
        }

        jdbi.onDemand(PublisherRepository.class)
                .internal_publish(publicationLink);
        return publicationLink;
    }

    public void delete(Publication publicationLink) {
        jdbi.onDemand(PublisherRepository.class)
                .delete(publicationLink.publication_id());
    }

    public List<Publication> list(String assetGuid) {
        return jdbi.onDemand(PublisherRepository.class).internal_listPublicationLinks(assetGuid);
    }
}
