package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.QueriesService;
import dk.northtech.dasscoassetservice.services.UserService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/v1/queries")
@Tag(name = "Queries", description = "Endpoints related to querying function for statements")
@SecurityRequirement(name = "dassco-idp")
public class Queries {
    private QueriesService queriesService;
    private UserService userService;

    @Inject
    public Queries(QueriesService queriesService, UserService userService) {
        this.queriesService = queriesService;
        this.userService = userService;
    }


    @GET
    @Path("/nodes")
    @Operation(summary = "Get node properties", description = "Collects all the properties of all the Nodes.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Map<String, List<String>> getNodeProperties() {
        return this.queriesService.getNodeProperties();
    }

    @POST
    @Path("/{limit}")
    @Operation(summary = "Get assets from query", description = """
    Selects all assets based on the received queries.
    This is API is accessed through the Query page in the frontend.
    """)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<Asset> getNodeProperties(QueriesReceived[] queries, @PathParam("limit") int limit, @Context SecurityContext securityContext) {
        User user = userService.from(securityContext);
        return this.queriesService.getAssetsFromQuery(Arrays.asList(queries), limit, user);
    }

    @POST
    @Path("/assetcount/{limit}")
    @Operation(summary = "Get the number of assets for the query", description = "Internal API used to get the count for the number of assets matching a query made on the query page")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public int getAssetCount(QueriesReceived[] queries, @PathParam("limit") int limit, @Context SecurityContext securityContext) {
        User user = userService.from(securityContext);
        if (queries.length == 0) return 0;
        return this.queriesService.getAssetCountFromQuery(Arrays.asList(queries), limit, user);
    }

    @POST
    @Path("/save")
    @Operation(summary = "Save query to user", description = """
        Saves a query and links it to the user.
        This is used through the frontend to save custom queries.
    """)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public SavedQuery saveQuery(SavedQuery savedQuery, @Context SecurityContext securityContext) {
        User user = userService.from(securityContext);
        return this.queriesService.saveQuery(savedQuery, user.username);
    }

    @GET
    @Path("/saved")
    @Operation(summary = "Gets the user's saved queries", description = "Gets the queries the user has saved.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<SavedQuery> getSavedQueries(@Context SecurityContext securityContext) {
        User user = userService.from(securityContext);
        return this.queriesService.getSavedQueries(user.username);
    }

    @POST
    @Path("/saved/update/{title}")
    @Operation(summary = "Updates a user's saved query", description = "Updates a query the user has saved.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public SavedQuery updateSavedQuery(SavedQuery newQuery, @Context SecurityContext securityContext, @PathParam("title") String prevTitle) {
        User user = userService.from(securityContext);
        return this.queriesService.updateSavedQuery(prevTitle, newQuery, user.username);
    }

    @DELETE
    @Path("/saved/{title}")
    @Operation(summary = "Deletes a user's saved query", description = "Deletes the user's query from the title.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, array = @ArraySchema(schema = @Schema(implementation = Specimen.class))))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public String deleteSavedQuery(@Context SecurityContext securityContext, @PathParam("title") String prevTitle) {
        User user = userService.from(securityContext);
        return this.queriesService.deleteSavedQuery(prevTitle, user.username);
    }
}
