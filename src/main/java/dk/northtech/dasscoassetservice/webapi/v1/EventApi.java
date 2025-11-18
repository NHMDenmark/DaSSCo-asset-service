package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.PaginatedEventsResponse;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.services.EventService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/events")
@Tag(name = "Events", description = "Endpoints related to events.")
@SecurityRequirement(name = "dassco-idp")
public class EventApi {

    private final EventService eventService;

    @Inject
    EventApi(EventService eventService) {
        this.eventService = eventService;
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public PaginatedEventsResponse getEvents(@Context SecurityContext securityContext,
                                             @QueryParam("eventType") String type,
                                             @DefaultValue("DESC") @QueryParam("direction") String direction,
                                             @DefaultValue("1") @QueryParam("page") int page,
                                             @DefaultValue("50") @QueryParam("limit") int limit,
                                             @QueryParam("startDate") String startDate,
                                             @QueryParam("endDate") String endDate) {
        if (type == null || type.isBlank()) {
            throw new WebApplicationException("Query parameter 'type' is required", 400);
        }

        return eventService.getEvents(type, direction, page, limit, startDate, endDate);
    }

    @GET
    @Path("types")
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    @Produces(APPLICATION_JSON)
    public List<String> getEventTypes(@Context SecurityContext securityContext) {
        return eventService.getEventTypes();
    }

}
