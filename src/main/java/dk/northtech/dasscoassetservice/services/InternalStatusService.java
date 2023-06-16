package dk.northtech.dasscoassetservice.services;

import dk.northtech.dasscoassetservice.domain.Institution;
import dk.northtech.dasscoassetservice.domain.InternalStatus;
import dk.northtech.dasscoassetservice.repositories.InstitutionRepository;
import dk.northtech.dasscoassetservice.repositories.InternalStatusRepository;
import jakarta.inject.Inject;
import joptsimple.internal.Strings;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class InternalStatusService {

    private InternalStatusRepository internalStatusRepository;

    @Inject
    public InternalStatusService(InternalStatusRepository internalStatusRepository) {
        this.internalStatusRepository = internalStatusRepository;
    }

    public Optional<Map<String, Integer>> getInternalStatusAmt() {
        DateTimeFormatter dtf = new DateTimeFormatterBuilder() // need to do it like this to make sure the epochmilli does not involve any hours, minutes, or seconds.
                .appendPattern("dd-MM-yyyy")
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));

        String formattedDateString = dtf.format(Instant.now());
        long epochFormatted = Instant.from(dtf.parse(formattedDateString)).toEpochMilli();

        return internalStatusRepository.getInternalStatusAmt(epochFormatted);
    }
}
