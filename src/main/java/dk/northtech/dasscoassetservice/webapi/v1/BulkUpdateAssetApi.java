package dk.northtech.dasscoassetservice.webapi.v1;

import dk.northtech.dasscoassetservice.domain.Digitiser;
import dk.northtech.dasscoassetservice.domain.Funding;
import dk.northtech.dasscoassetservice.domain.bulkupdatepayload.BulkUpdatePayload;
import dk.northtech.dasscoassetservice.services.BulkUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Path("/v1/assets/bulkupdate")
@Tag(name = "Bulk Update Assets", description = "Endpoints related to bulk updating assets.")
@SecurityRequirement(name = "dassco-idp")
public class BulkUpdateAssetApi {
    private final Logger log = LoggerFactory.getLogger(BulkUpdateAssetApi.class);
    private final BulkUpdateService bulkUpdateService;

    @Inject
    BulkUpdateAssetApi(BulkUpdateService bulkUpdateService) {
        this.bulkUpdateService = bulkUpdateService;
    }

    @GET
    @Path("/digitisers")
    @Operation(summary = "List all digitisers",
            description = "Returns a list of known digitisers used during asset digitisation.")
    public List<Digitiser> listDigitisers() {
        return bulkUpdateService.getDigitisers();
    }

    @GET
    @Path("/funding")
    @Operation(summary = "List all funding",
            description = "Returns a list of known funding")
    public List<Funding> listFunding() {
        return bulkUpdateService.getFunding();
    }

    @GET
    @Path("/subjects")
    @Operation(summary = "List all subjects",
            description = "Returns a list of known subjects")
    public List<String> listSubjects() {
        return bulkUpdateService.getSubjects();
    }

    @GET
    @Path("/roles")
    @Operation(summary = "List all roles",
            description = "Returns a list of known roles")
    public List<String> listRoles() {
        return bulkUpdateService.getRoles();
    }

    @GET
    @Path("/issue-categories")
    @Operation(summary = "List all issue categories",
            description = "Returns a list of known issue categories")
    public List<String> listIssueCategories() {
        return bulkUpdateService.getIssueCategories();
    }
    @GET
    @Path("/statuses")
    @Operation(summary = "List all statuses",
            description = "Returns a list of known statuses")
    public List<String> listStatuses() {
        return bulkUpdateService.getStatuses();
    }

    @POST
    @Path("/issues/grouped")
    @Operation(summary = "Get grouped issues for selected assets",
            description = "Returns unique issues aggregated across multiple assets.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupedIssuesForAssets(List<String> assetGuids, @Context SecurityContext securityContext) {
        List<Map<String, Object>> grouped = bulkUpdateService.getGroupedIssues(assetGuids, securityContext);
        return Response.ok(grouped).build();
    }

    @POST
    @Path("/digitisers/grouped")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupedDigitisers(List<String> assetGuids, @Context SecurityContext securityContext) {

        List<Map<String, Object>> grouped = bulkUpdateService.getGroupedDigitisers(assetGuids, securityContext);
        return Response.ok(grouped).build();
    }


    @PATCH
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Perform bulk update on selected assets",
            description = "Applies partial updates, issue changes, and digitiser updates. Returns the generated bulk_update_uuid.")
    public Response bulkUpdate(BulkUpdatePayload payload, @Context SecurityContext securityContext) {
        UUID bulkUpdateUuid = bulkUpdateService.processBulkUpdate(payload, securityContext);
        return Response.ok(Map.of("bulkUpdateUuid", bulkUpdateUuid)).build();
    }

}
