package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.StatisticsDataService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static dk.northtech.dasscoassetservice.domain.GraphType.exponential;
import static dk.northtech.dasscoassetservice.domain.GraphType.incremental;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

// Hidden for now
@Hidden
@Component
@Path("/v1/graphdata/")
@Tag(name = "Statistics Data", description = "Endpoints related to statistics data.")
@SecurityRequirement(name = "dassco-idp")
public class StatisticsDataApi {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDataApi.class);
    private final StatisticsDataService statisticsDataService;

    @Inject
    public StatisticsDataApi(StatisticsDataService statisticsDataService) {
        this.statisticsDataService = statisticsDataService;
    }

    // TODO: I need access to some documentation to be able to understand these endpoints.
    // TODO: No mention of it on Confluence.

    @GET
    @Operation(summary = "Get Graph Data", description = "")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = StatisticsData.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<StatisticsData> getSpecimenData() {
        long year = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toEpochSecond();
        return statisticsDataService.getGraphData(year);
    }

    @GET
    @Operation(summary = "Get Graph Data during timeframe", description = "")
    @Path("/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = GraphData.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getGraphData(@PathParam("timeframe") String timeFrame) {
        // {incremental (pr day data): data, exponential (continually adding pr day): data}
        Map<GraphType, Map<String, GraphData>> finalData;

        if (EnumUtils.isValidEnum(GraphView.class, timeFrame)) {
            logger.info("Getting data for time frame {}.", timeFrame);
            finalData = statisticsDataService.getCachedGraphData(GraphView.valueOf(timeFrame));
        } else {
            logger.warn("Received time frame {} is invalid.", timeFrame);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (finalData.isEmpty()) {
            logger.info("No data available within the selected time frame.");
            return Response.status(Response.Status.NO_CONTENT).entity("No data available within the selected time frame.").build();
        }

        return Response.status(Response.Status.OK).entity(finalData).build();
    }

    @GET
    @Operation(summary = "Get Custom Graph Data", description = "")
    @Path("/custom")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getGraphDataCustomTimeframe(@QueryParam("view") String view, @QueryParam("start") long startDate, @QueryParam("end") long endDate) {
        // Custom start and end date with either daily or monthly view
        Map<String, GraphData> customData;
        Map<GraphType, Map<String, GraphData>> finalData = new ListOrderedMap<>(); // incremental data: data, exponential data: data
        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        if (GraphView.valueOf(view).equals(GraphView.WEEK) || GraphView.valueOf(view).equals(GraphView.MONTH)) { // every date is shown along x-axis
            DateTimeFormatter dateFormatter = statisticsDataService.getDateFormatter("dd-MMM-yyyy");
            customData = statisticsDataService.generateIncrData(start, end, dateFormatter, GraphView.WEEK);

            if (customData.isEmpty()) {
                logger.warn("No data available within the selected time frame.");
                return Response.status(Response.Status.NO_CONTENT).entity("No data available within the selected time frame.").build();
            }
            logger.info("Data has been gathered for time frame {} to {}.", start, end);
            finalData.put(incremental, customData);

            return Response.status(Response.Status.OK).entity(finalData).build();
        } else if (GraphView.valueOf(view).equals(GraphView.YEAR) || GraphView.valueOf(view).equals(GraphView.EXPONENTIAL) ) { // every month is shown along x-axis
            DateTimeFormatter yearFormatter = statisticsDataService.getDateFormatter("MMM yyyy");
            Map<String, GraphData> incrData = statisticsDataService.generateIncrData(start, end, yearFormatter, GraphView.YEAR);

            if (GraphView.valueOf(view).equals(GraphView.EXPONENTIAL)) { // if they want the line + bar
                Map<String, GraphData> exponData = statisticsDataService.generateExponData(incrData, yearFormatter);

                finalData.put(incremental, incrData);
                finalData.put(exponential, exponData);
                return Response.status(Response.Status.OK).entity(finalData).build();
            }

            if (incrData.isEmpty()) {
                logger.warn("No data available within the selected time frame.");
                return Response.status(Response.Status.NO_CONTENT).entity("No data available within the selected time frame.").build();
            }

            finalData.put(incremental, incrData);
            return Response.status(Response.Status.OK).entity(finalData).build();
        } else {
            logger.warn("View {} is invalid. It has to be either \"daily\" or \"monthly\".", view);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    @GET
    @Path("/refreshcache")
    @Produces(TEXT_PLAIN)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER, SecurityRoles.SERVICE})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = TEXT_PLAIN))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = TEXT_PLAIN))
    public Response refreshGraphCache() {
        statisticsDataService.refreshCachedData();
        return Response.status(Response.Status.OK).entity("Cache has been refreshed").build();
    }

}
