package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.AssetChange;
import dk.northtech.dasscoassetservice.domain.DasscoEvent;
import dk.northtech.dasscoassetservice.domain.Directory;
import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.repositories.AssetChangeRepository;
import dk.northtech.dasscoassetservice.repositories.DirectoryRepository;
import dk.northtech.dasscoassetservice.repositories.EventRepository;
import dk.northtech.dasscoassetservice.repositories.InstitutionRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AssetChangeService {
    private final Jdbi jdbi;

    @Inject
    public AssetChangeService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public int addAssetChange(AssetChange assetChange){
        AssetChangeRepository assetChangeRepository = jdbi.onDemand(AssetChangeRepository.class);
        return assetChangeRepository.create(assetChange);
    }

    public void syncAssetChangesToEvent(DasscoEvent event, Long directory_id, String asset_guid){
        this.jdbi.useTransaction(h -> {
            this.syncAssetChangesToEventWithHandle(event, directory_id, asset_guid, h);
        });
    }
    public void syncAssetChangesToEventWithHandle(DasscoEvent event, Long directory_id, String asset_guid, Handle h){
        AssetChangeRepository assetChangeRepository = h.attach(AssetChangeRepository.class);
        EventRepository eventRepository = h.attach(EventRepository.class);

        Set<AssetChange> assetChanges = assetChangeRepository.getAll(directory_id);
        Map<Long, Long> idCount = assetChanges.stream().collect(Collectors.groupingBy(AssetChange::dassco_user_id, Collectors.counting()));//<id, count>
        Map.Entry<Long, Long> max = idCount.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(null);//<id, count>
        if(max != null){
            Long userId = max.getKey();
            List<String> changes = assetChanges.stream().map(AssetChange::change).collect(Collectors.toList());
            if(!changes.isEmpty()){
                List<Event> events = eventRepository.getAssetEvents(asset_guid);
                var hasEvent = events.stream().anyMatch(e -> e.event.equals(DasscoEvent.CREATE_ASSET));
                eventRepository.insertEvent(asset_guid, hasEvent ? event : DasscoEvent.CREATE_ASSET, Math.toIntExact(userId), null, hasEvent ? null : changes);
                assetChangeRepository.deleteAll(directory_id);
            }
        }
    }
}
