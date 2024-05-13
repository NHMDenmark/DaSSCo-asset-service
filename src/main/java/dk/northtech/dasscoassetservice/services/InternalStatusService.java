package dk.northtech.dasscoassetservice.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dk.northtech.dasscoassetservice.domain.AssetStatusInfo;
import dk.northtech.dasscoassetservice.domain.Directory;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
import dk.northtech.dasscoassetservice.repositories.DirectoryRepository;
import dk.northtech.dasscoassetservice.repositories.InternalStatusRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class InternalStatusService {
    private static final Logger logger = LoggerFactory.getLogger(InternalStatusService.class);
    private InternalStatusRepository internalStatusRepository;
    private Jdbi jdbi;
    @Inject
    public InternalStatusService(InternalStatusRepository internalStatusRepository, Jdbi jdbi) {
        this.internalStatusRepository = internalStatusRepository;
        this.jdbi = jdbi;
    }

    LoadingCache<String, Map<String, Integer>> cachedInternalStatus = CacheBuilder.newBuilder()
            .expireAfterAccess(24, TimeUnit.HOURS)
            .build(
                    new CacheLoader<String, Map<String, Integer>>() {
                        public Map<String, Integer> load(String key) {
                            Map<String, Integer> statuses = new HashMap<>();

                            if (key.equals("total")) {
                                statuses = getInternalStatusAmt(false).get();
                            } else if (key.equals("daily")) {
                                statuses = getInternalStatusAmt(true).get();
                            }

                            return statuses;
                        }
                    });

    public Map<String, Integer> getCachedStatuses(String key) {
        try {
            return cachedInternalStatus.get(key);
        } catch (ExecutionException e) {
            logger.warn("An error occurred when loading the internal status cache {}", e.getMessage());
            throw new RuntimeException("An error occurred when loading the internal status cache {}", e);
        }
    }

    public List<AssetStatusInfo> getWorkInProgressAssets(boolean onlyFailed) {
        HashMap<String, Integer> guidAllocated = new HashMap<>();
        jdbi.withHandle(h -> {
            DirectoryRepository attach = h.attach(DirectoryRepository.class);
            return attach.getWriteableDirectories();
        }).forEach(x -> guidAllocated.put(x.assetGuid(), x.allocatedStorageMb()));


        return internalStatusRepository.getInprogress().stream()
                .filter(x -> !onlyFailed || x.status() == InternalStatus.ERDA_ERROR)
                .map(assetStatusInfo -> new AssetStatusInfo(assetStatusInfo.asset_guid()
                        , assetStatusInfo.parent_guid()
                        , assetStatusInfo.error_timestamp()
                        , assetStatusInfo.status()
                        , assetStatusInfo.error_message()
                        , guidAllocated.getOrDefault(assetStatusInfo.asset_guid(), null)))
                .collect(Collectors.toList());
    }

    public Optional<AssetStatusInfo> getAssetStatus(String assetGuid) {
        return internalStatusRepository.getAssetStatus(assetGuid);
    }

    public Optional<Map<String, Integer>> getInternalStatusAmt(boolean daily) {
        if (daily) {
            DateTimeFormatter dtf = new DateTimeFormatterBuilder() // need to do it like this to make sure the epochmilli does not involve any hours, minutes, or seconds.
                    .appendPattern("dd-MM-yyyy")
                    .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                    .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                    .toFormatter(Locale.ENGLISH)
                    .withZone(ZoneId.of("UTC"));

            String formattedDateString = dtf.format(Instant.now());
            long epochFormatted = Instant.from(dtf.parse(formattedDateString)).toEpochMilli();
            return internalStatusRepository.getDailyInternalStatusAmt(epochFormatted);
        } else {
            return internalStatusRepository.getTotalInternalStatusAmt();
        }
    }
}
