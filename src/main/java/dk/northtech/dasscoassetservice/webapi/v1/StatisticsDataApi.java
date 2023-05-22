package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.GraphView;
import dk.northtech.dasscoassetservice.domain.SecurityRoles;
import dk.northtech.dasscoassetservice.domain.StatisticsData;
import dk.northtech.dasscoassetservice.services.StatisticsDataService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections4.map.ListOrderedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/graphdata/")
@SecurityRequirement(name = "dassco-idp")
public class StatisticsDataApi {
    private static final Logger logger = LoggerFactory.getLogger(StatisticsDataApi.class);
    private final StatisticsDataService statisticsDataService;

    private final DateTimeFormatter dailyDateFormatter = new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
            .appendPattern("dd-MMM-yyyy")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .toFormatter(Locale.ENGLISH)
            .withZone(ZoneId.of("UTC"));

    private final DateTimeFormatter monthlyDateFormatter = new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
            .appendPattern("MMM-yyyy")
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .toFormatter(Locale.ENGLISH)
            .withZone(ZoneId.of("UTC"));

    @Inject
    public StatisticsDataApi(StatisticsDataService statisticsDataService) {
        this.statisticsDataService = statisticsDataService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = StatisticsData.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<StatisticsData> getSpecimenData() {
        long year = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toEpochSecond();
        return statisticsDataService.getGraphData(year);
    }

    @GET
    @Path("/daily/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = GraphData.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getGraphDataDaily(@PathParam("timeframe") String timeFrame) {
        Map<String, Map<String, GraphData>> finalData = new ListOrderedMap<>(); // incremental (pr day data): data, exponential (continually adding pr day): data
        Map<String, GraphData> incrData;
        DateTimeFormatter dateFormatter = getDateFormatter("dd-MMM-yyyy");

        if (GraphView.valueOf(timeFrame).equals(GraphView.WEEK)) {
            // For a daily view, a week back
            Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusWeeks(1).toInstant();
            incrData = statisticsDataService.generateIncrData(startDate, Instant.now(), dateFormatter, GraphView.WEEK);
        } else if (GraphView.valueOf(timeFrame).equals(GraphView.MONTH)) {
            // For a daily view, a month back
            Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toInstant();
            incrData = statisticsDataService.generateIncrData(startDate, Instant.now(), dateFormatter, GraphView.MONTH);
        } else {
            logger.warn("Received time frame {} is invalid.", timeFrame);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (incrData.isEmpty()) {
            logger.warn("No data available within the selected time frame.");
            return Response.status(Response.Status.NO_CONTENT).entity("No data available within the selected time frame.").build();
        }

        finalData.put("incremental", incrData);
        return Response.status(Response.Status.OK).entity(finalData).build();
    }

    @GET
    @Path("/year")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getGraphDataYear() {
        // For a monnthly view, a year back
        Map<String, Map<String, GraphData>> finalData = new ListOrderedMap<>(); // incremental (pr day data): data, exponential (continually adding pr day): data
        Instant startDate = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toInstant();
        DateTimeFormatter yearFormatter = getDateFormatter("MMM yyyy");

        Map<String, GraphData> incrData = statisticsDataService.generateIncrData(startDate, Instant.now(), yearFormatter, GraphView.YEAR);
        finalData = statisticsDataService.generateExponData(incrData, yearFormatter);

        if (finalData.get("incremental").isEmpty()) {
            logger.warn("No data available for the past year.");
            return Response.status(Response.Status.NO_CONTENT).entity("No data available for the past year.").build();
        }
        return Response.status(Response.Status.OK).entity(finalData).build();
    }

    @GET
    @Path("/custom")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getGraphDataCustomTimeframe(@QueryParam("view") String view, @QueryParam("start") long startDate, @QueryParam("end") long endDate) {
        // Custom start and end date with either daily or monthly view
        Map<String, GraphData> customData;
        Map<String, Map<String, GraphData>> finalData = new ListOrderedMap<>(); // incremental data: data, exponential data: data
        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        if (GraphView.valueOf(view).equals(GraphView.WEEK) || GraphView.valueOf(view).equals(GraphView.MONTH)) { // every date is shown along x-axis
            DateTimeFormatter dateFormatter = getDateFormatter("dd-MMM-yyyy");
            customData = statisticsDataService.generateIncrData(start, end, dateFormatter, GraphView.WEEK);

            if (customData.isEmpty()) {
                logger.warn("No data available within the selected time frame.");
                return Response.status(Response.Status.NO_CONTENT).entity("No data available within the selected time frame.").build();
            }
            finalData.put("incremental", customData);

            return Response.status(Response.Status.OK).entity(finalData).build();
        } else if (GraphView.valueOf(view).equals(GraphView.YEAR) || GraphView.valueOf(view).equals(GraphView.EXPONENTIAL) ) { // every month is shown along x-axis
            DateTimeFormatter yearFormatter = getDateFormatter("MMM yyyy");

            Map<String, GraphData> incrData = statisticsDataService.generateIncrData(start, end, yearFormatter, GraphView.YEAR);

            if (GraphView.valueOf(view).equals(GraphView.EXPONENTIAL)) { // if they want the line + bar
                finalData = statisticsDataService.generateExponData(incrData, yearFormatter);
                return Response.status(Response.Status.OK).entity(finalData).build();
            }

            if (incrData.isEmpty()) {
                logger.warn("No data available within the selected time frame.");
                return Response.status(Response.Status.NO_CONTENT).entity("No data available within the selected time frame.").build();
            }

            finalData.put("incremental", incrData);
            return Response.status(Response.Status.OK).entity(finalData).build();
        } else {
            logger.warn("View {} is invalid. It has to be either \"daily\" or \"monthly\".", view);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
    }

    public DateTimeFormatter getDateFormatter(String pattern) { // need this as the pattern varies >.>
        return new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));
    }

}
