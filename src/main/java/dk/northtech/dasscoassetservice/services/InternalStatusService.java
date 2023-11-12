package dk.northtech.dasscoassetservice.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dk.northtech.dasscoassetservice.domain.AssetError;
import dk.northtech.dasscoassetservice.repositories.InternalStatusRepository;
import jakarta.inject.Inject;
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

@Service
public class InternalStatusService {
    private static final Logger logger = LoggerFactory.getLogger(InternalStatusService.class);
    private InternalStatusRepository internalStatusRepository;

    @Inject
    public InternalStatusService(InternalStatusRepository internalStatusRepository) {
        this.internalStatusRepository = internalStatusRepository;
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

    public List<AssetError> getFailedAssets() {
        return internalStatusRepository.getFailed();
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
