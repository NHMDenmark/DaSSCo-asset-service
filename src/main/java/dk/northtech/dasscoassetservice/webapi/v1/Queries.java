package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.*;
import dk.northtech.dasscoassetservice.services.QueriesService;
import dk.northtech.dasscoassetservice.services.UserService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
    public List<QueryItem> getNodeProperties() {
        return this.queriesService.getNodeProperties();
    }

    @POST
    @Path("/{limit}")
    @Operation(summary = "Get assets from query", description = """
    Selects all assets based on the received queries.
    This API is accessed through the Query page in the frontend.
    Multiple query blocks can be submitted in a single request body.
    Within each `where` clause, multiple `fields` are evaluated as OR conditions, while separate `where` entries are evaluated as AND conditions.
    """)
    @Consumes(APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(
            responseCode = "200",
            content = @Content(
                    mediaType = APPLICATION_JSON,
                    array = @ArraySchema(schema = @Schema(implementation = QueryResultAsset.class)),
                    examples = {
                            @ExampleObject(
                                    name = "Matching assets",
                                    summary = "Example response with one matching asset",
                                    value = """
                                            [
                                              {
                                                "asset_guid": "dassco-asset-0001",
                                                "institution": "Natural History Museum of Denmark",
                                                "collection": "ento-beetles",
                                                "file_formats": ["JPEG", "TIFF"],
                                                "created_date": "2025-01-15T10:15:30Z",
                                                "date_asset_taken": "2024-07-04T12:00:00Z",
                                                "writeAccess": true,
                                                "events": [
                                                  {
                                                    "user": "digitiser@example.org",
                                                    "timestamp": "2025-01-15T10:15:30Z",
                                                    "event": "CREATE_ASSET_METADATA",
                                                    "pipeline": "mass-digitisation-pipeline",
                                                    "change_list": ["created"],
                                                    "bulk_update_uuid": null
                                                  }
                                                ],
                                                "asset_specimen": [
                                                  {
                                                    "specimen_id": 12345,
                                                    "asset_guid": "dassco-asset-0001",
                                                    "specimen_pid": "specimen-pid-12345",
                                                    "asset_specimen_id": 67890,
                                                    "asset_preparation_type": "pinning",
                                                    "specify_collection_object_attachment_id": 4567,
                                                    "asset_detached": false
                                                  }
                                                ]
                                              }
                                            ]
                                            """)
                    }
            )
    )
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public List<QueryResultAsset> getNodeProperties(
            @RequestBody(
                    required = true,
                    description = "One or more query definitions. Each array item represents a query group from the frontend. Use `select` to indicate the entity being filtered, `where` to list filterable properties, and `fields` to define one or more operators for the same property.",
                    content = @Content(
                            mediaType = APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = QueriesReceived.class)),
                            examples = {
                                    @ExampleObject(
                                            name = "Simple asset lookup",
                                            summary = "Find working-copy assets by exact GUID and status",
                                            value = """
                                                    [
                                                      {
                                                        "id": "1",
                                                        "query": [
                                                          {
                                                            "select": "Asset",
                                                            "where": [
                                                              {
                                                                "property": "asset_guid",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "dassco-asset-0001",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "status",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "WORKING_COPY",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    ]
                                                    """),
                                    @ExampleObject(
                                            name = "Advanced asset filters",
                                            summary = "Combine text, list and boolean filters, including OR conditions on the same property",
                                            value = """
                                                    [
                                                      {
                                                        "id": "1",
                                                        "query": [
                                                          {
                                                            "select": "Asset",
                                                            "where": [
                                                              {
                                                                "property": "subject",
                                                                "fields": [
                                                                  {
                                                                    "operator": "contains",
                                                                    "value": "herbarium",
                                                                    "dataType": "STRING"
                                                                  },
                                                                  {
                                                                    "operator": "contains",
                                                                    "value": "pinned insect",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "file_format",
                                                                "fields": [
                                                                  {
                                                                    "operator": "in",
                                                                    "value": "JPEG",
                                                                    "dataType": "LIST"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "legal",
                                                                "fields": [
                                                                  {
                                                                    "operator": "contains",
                                                                    "value": "CC-BY",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "currently_audited",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "true",
                                                                    "dataType": "BOOLEAN"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "multi_specimen",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "true",
                                                                    "dataType": "BOOLEAN"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    ]
                                                    """),
                                    @ExampleObject(
                                            name = "Cross-entity query",
                                            summary = "Filter across institution, collection, specimen, pipeline and asset date taken",
                                            value = """
                                                    [
                                                      {
                                                        "id": "1",
                                                        "query": [
                                                          {
                                                            "select": "Institution",
                                                            "where": [
                                                              {
                                                                "property": "institution",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "Natural History Museum of Denmark",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          },
                                                          {
                                                            "select": "Collection",
                                                            "where": [
                                                              {
                                                                "property": "collection",
                                                                "fields": [
                                                                  {
                                                                    "operator": "starts with",
                                                                    "value": "ento-",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          },
                                                          {
                                                            "select": "Specimen",
                                                            "where": [
                                                              {
                                                                "property": "specimens",
                                                                "fields": [
                                                                  {
                                                                    "operator": "contains",
                                                                    "value": "NHMD-ENT-",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          },
                                                          {
                                                            "select": "Pipeline",
                                                            "where": [
                                                              {
                                                                "property": "pipeline",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "mass-digitisation-pipeline",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          },
                                                          {
                                                            "select": "Asset",
                                                            "where": [
                                                              {
                                                                "property": "date_asset_taken",
                                                                "fields": [
                                                                  {
                                                                    "operator": "between",
                                                                    "value": "1704067200000#1735689599000",
                                                                    "dataType": "DATE"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "parent_guid",
                                                                "fields": [
                                                                  {
                                                                    "operator": "empty",
                                                                    "value": "",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    ]
                                                    """),
                                    @ExampleObject(
                                            name = "Workflow follow-up query",
                                            summary = "Find unpublished assets that still need workflow attention",
                                            value = """
                                                    [
                                                      {
                                                        "id": "1",
                                                        "query": [
                                                          {
                                                            "select": "Asset",
                                                            "where": [
                                                              {
                                                                "property": "internal_status",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "METADATA_RECEIVED",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "make_public",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "false",
                                                                    "dataType": "BOOLEAN"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "push_to_specify",
                                                                "fields": [
                                                                  {
                                                                    "operator": "equal",
                                                                    "value": "false",
                                                                    "dataType": "BOOLEAN"
                                                                  }
                                                                ]
                                                              },
                                                              {
                                                                "property": "specify_attachment_title",
                                                                "fields": [
                                                                  {
                                                                    "operator": "empty",
                                                                    "value": "",
                                                                    "dataType": "STRING"
                                                                  }
                                                                ]
                                                              }
                                                            ]
                                                          }
                                                        ]
                                                      }
                                                    ]
                                                    """)
                            }
                    )
            )
            QueriesReceived[] queries,
            @Parameter(description = "Maximum number of matching assets to return", required = true, example = "100")
            @PathParam("limit") int limit,
            @Context SecurityContext securityContext) {
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
        // The frontend should just use the getNodeProperties and count them
        return this.queriesService.getAssetsFromQuery(Arrays.asList(queries), limit, user).size();
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
