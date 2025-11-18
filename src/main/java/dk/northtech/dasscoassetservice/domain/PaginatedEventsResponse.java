package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public class PaginatedEventsResponse {
    @Schema(description = "Total number of events matching the filter")
    public long totalCount;

    @Schema(description = "Current page number")
    public int page;

    @Schema(description = "Next page number, if available")
    public Integer nextPage;

    @Schema(description = "Previous page number, if available")
    public Integer previousPage;

    @Schema(description = "List of events for the current page")
    public List<Event> events;

    public PaginatedEventsResponse(long totalCount, int page,
                                   Integer nextPage, Integer previousPage,
                                   List<Event> events) {
        this.totalCount = totalCount;
        this.page = page;
        this.nextPage = nextPage;
        this.previousPage = previousPage;
        this.events = events;
    }
}