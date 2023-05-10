package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.GraphData;
import dk.northtech.dasscoassetservice.domain.GraphTimeFrame;
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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.antlr.v4.misc.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

    @Inject
    public StatisticsDataApi(StatisticsDataService statisticsDataService) {
        this.statisticsDataService = statisticsDataService;
    }

//    @GET
//    @Produces(MediaType.APPLICATION_JSON)
//    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
//    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = StatisticsData.class)))
//    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
//    public List<StatisticsData> getSpecimenData() {
//        long year = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toEpochSecond();
//        return statisticsDataService.getGraphData(year);
//    }

    @GET
    @Path("/daily/{timeframe}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = GraphData.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getGraphDataWeek(@PathParam("timeframe") String timeFrame) {
        Map<String, GraphData> incrData;

        if (GraphTimeFrame.valueOf(timeFrame).equals(GraphTimeFrame.WEEK)) {
            // For a daily view, a week back
            long days = ZonedDateTime.now(ZoneOffset.UTC).minusDays(7).toEpochSecond();
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
            incrData = statisticsDataService.generateIncrData(days, dayFormatter);
        } else if (GraphTimeFrame.valueOf(timeFrame).equals(GraphTimeFrame.MONTH)) {
            // For a daily view, a month back
            long days = ZonedDateTime.now(ZoneOffset.UTC).minusMonths(1).toEpochSecond();
            DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneId.of("UTC"));
            incrData = statisticsDataService.generateIncrData(days, dayFormatter);
        } else {
            logger.warn("Received time frame {} is invalid.", timeFrame);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (incrData.isEmpty()) {
            logger.warn("No data available within the selected time frame.");
            return Response.status(Response.Status.NO_CONTENT).entity("No data available within the selected time frame.").build();
        }

        return Response.status(Response.Status.OK).entity(incrData).build();
    }

    @GET
    @Path("/year")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({SecurityRoles.ADMIN, SecurityRoles.DEVELOPER})
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = List.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getGraphDataYear() {
        // For a monnthly view, a year back
        long year = ZonedDateTime.now(ZoneOffset.UTC).minusYears(1).toEpochSecond();
        DateTimeFormatter yearFormatter = new DateTimeFormatterBuilder() // default day and hour as the pattern is only month and year
                .appendPattern("MMM yyyy")
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ENGLISH)
                .withZone(ZoneId.of("UTC"));

        Map<String, GraphData> incrData = statisticsDataService.generateIncrData(year, yearFormatter);
        List<Map<String, GraphData>> yearData = statisticsDataService.generateExponData(incrData, yearFormatter); // 0 -> incr 1 -> expon

        if (yearData.get(0).isEmpty()) {
            logger.warn("No data available for the past year.");
            return Response.status(Response.Status.NO_CONTENT).entity("No data available for the past year.").build();
        }
        return Response.status(Response.Status.OK).entity(yearData).build();
    }

}
