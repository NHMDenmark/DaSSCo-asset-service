package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public class PaginatedEventsResponse {
    @Schema(description = "Total number of events matching the filter")
    public long total;

    @Schema(description = "Current page number")
    public int page;

    @Schema(description = "Next page number, if available")
    public Integer nextPage;

    @Schema(description = "Previous page number, if available")
    public Integer previousPage;

    @Schema(description = "List of events for the current page")
    public List<EventExpanded> events;

    public PaginatedEventsResponse(long total, int page,
                                   Integer nextPage, Integer previousPage,
                                   List<EventExpanded> events) {
        this.total = total;
        this.page = page;
        this.nextPage = nextPage;
        this.previousPage = previousPage;
        this.events = events;
    }
}