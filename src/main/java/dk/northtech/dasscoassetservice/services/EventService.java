package dk.northtech.dasscoassetservice.services;


import dk.northtech.dasscoassetservice.domain.Event;
import dk.northtech.dasscoassetservice.domain.EventExpanded;
import dk.northtech.dasscoassetservice.domain.PaginatedEventsResponse;
import dk.northtech.dasscoassetservice.repositories.EventRepository;
import jakarta.inject.Inject;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class EventService {

    private final Jdbi jdbi;
    @Inject
    public  EventService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public PaginatedEventsResponse getEvents(String type,
                                             String direction,
                                             int page,
                                             int limit,
                                             String startDate,
                                             String endDate) {

        String dir = "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";

        Instant start = startDate != null && !startDate.isBlank()
                ? Instant.parse(startDate)
                : null;
        Instant end = endDate != null && !endDate.isBlank()
                ? Instant.parse(endDate)
                : null;

        int offset = (Math.max(page, 1) - 1) * limit;

        return jdbi.withExtension(EventRepository.class, repo -> {
            long total = repo.countEvents(type, start, end);

            List<EventExpanded> events = repo
                    .getEvents(type, start, end, limit, offset, dir);

            int totalPages = (int) Math.ceil((double) total / limit);
            Integer next = page < totalPages ? page + 1 : null;
            Integer prev = page > 1 ? page - 1 : null;

            return new PaginatedEventsResponse(total, page, next, prev, events);
        });
    }

    public List<String> getEventTypes() {
        return jdbi.withExtension(EventRepository.class, EventRepository::getEventTypes);
    }


}
