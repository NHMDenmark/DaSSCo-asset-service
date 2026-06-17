package dk.northtech.dasscoassetservice.webapi.v1;


import dk.northtech.dasscoassetservice.domain.PublicAsset;
import dk.northtech.dasscoassetservice.domain.Asset;
import dk.northtech.dasscoassetservice.domain.User;
import dk.northtech.dasscoassetservice.services.AssetService;
import dk.northtech.dasscoassetservice.services.PublicAssetService;
import dk.northtech.dasscoassetservice.services.RightsValidationService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/extern")
@Tag(name = "PublicAssets", description = "Endpoints related to public assets.")
@SecurityRequirement(name = "dassco-idp")
public class PublicAssetApi {

    private final PublicAssetService publicAssetService;
    private final AssetService assetService;
    private final RightsValidationService rightsValidationService;

    @Inject
    public PublicAssetApi(PublicAssetService publicAssetService, AssetService assetService, RightsValidationService rightsValidationService) {
        this.publicAssetService = publicAssetService;
        this.assetService = assetService;
        this.rightsValidationService = rightsValidationService;
    }


    @GET
    @Path("/metadata/{assetGuid}")
    @Operation(summary = "Get asset metadata for external users", description = "Returns limited metadata for external users. Can be used without a token.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = PublicAsset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Response getAssetMetadata(@PathParam("assetGuid") String asset_guid) {
        Response accessError = validateExternalReadAccess(asset_guid);
        if (accessError != null) {
            return accessError;
        }
        return this.publicAssetService.getAsset(asset_guid)
                .map(asset -> Response.ok(asset).build())
                .orElseGet(() -> notFound(asset_guid));
    }

    @GET
    @Path("/metadata/{assetGuid}/csv")
    @Produces("text/csv")
    @Operation(
            summary = "Download asset metadata as CSV",
            description = "Generates and streams a CSV file containing metadata for the specified asset. Accessible without authentication."
    )
    @ApiResponse(
            responseCode = "200",
            description = "CSV file successfully generated",
            content = @Content(mediaType = "text/csv")
    )
    @ApiResponse(
            responseCode = "404",
            description = "Asset not found",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DaSSCoError.class))
    )
    public Response downloadAssetMetadataAsCsv(
            @Parameter(description = "GUID of the asset", required = true)
            @PathParam("assetGuid") String assetGuid
    ) {
        Response accessError = validateExternalReadAccess(assetGuid);
        if (accessError != null) {
            return accessError;
        }

        Optional<PublicAsset> assetOpt = this.publicAssetService.getAsset(assetGuid);
        if (assetOpt.isEmpty()) {
            return notFound(assetGuid);
        }

        PublicAsset asset = assetOpt.get();

        // Use StreamingOutput to avoid loading large responses into memory
        StreamingOutput stream = output -> {
            try (
                    BufferedWriter writer =
                            new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))
            ) {
                writeCsvRow(writer,
                        "asset_guid",
                        "asset_pid",
                        "asset_subject",
                        "audited",
                        "barcode",
                        "camera_setting_control",
                        "collection",
                        "date_asset_deleted_ars",
                        "date_asset_taken",
                        "date_audited",
                        "file_formats",
                        "funding",
                        "institution",
                        "legality",
                        "metadata_version",
                        "mime_type",
                        "mos_id",
                        "multi_specimen",
                        "parent_guids",
                        "payload_type",
                        "pipeline_name",
                        "preparation_type",
                        "specify_attachment_title",
                        "specimen_pid"
                );

                writeCsvRow(writer,
                        asset.asset_guid(),
                        asset.asset_pid(),
                        asset.asset_subject(),
                        String.valueOf(asset.audited()),
                        join(asset.barcode()),
                        asset.camera_setting_control(),
                        asset.collection(),
                        asset.date_asset_deleted_ars(),
                        asset.date_asset_taken(),
                        asset.date_audited(),
                        join(asset.file_formats()),
                        join(asset.funding()),
                        asset.institution(),
                        asset.legality().map(Object::toString).orElse(""),
                        asset.metadata_version(),
                        join(asset.mime_type()),
                        asset.mos_id(),
                        String.valueOf(asset.multi_specimen()),
                        join(asset.parent_guids()),
                        asset.payload_type(),
                        asset.pipeline_name(),
                        join(asset.preparation_type()),
                        asset.specify_attachment_title(),
                        join(asset.specimen_pid())
                );
            }
        };

        return Response.ok(stream, "text/csv")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"asset_" + assetGuid + ".csv\""
                )
                .build();
    }

    private Response validateExternalReadAccess(String assetGuid) {
        Optional<Asset> assetOpt = assetService.getAsset(assetGuid);
        if (assetOpt.isEmpty()) {
            return notFound(assetGuid);
        }

        User anonymous = new User("anonymous");
        if (!rightsValidationService.checkRightsAsset(anonymous, assetOpt.get(), false)) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(new DaSSCoError(
                            "1.0",
                            DaSSCoErrorCode.FORBIDDEN,
                            "This asset is not available for external viewing."
                    ))
                    .build();
        }

        return null;
    }

    private Response notFound(String assetGuid) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new DaSSCoError(
                        "1.0",
                        DaSSCoErrorCode.NOT_FOUND,
                        "Asset not found: " + assetGuid
                ))
                .build();
    }

    private void writeCsvRow(Writer writer, String... values) throws IOException {
        writer.write(
                java.util.Arrays.stream(values)
                        .map(this::escapeCsv)
                        .collect(Collectors.joining(","))
        );
        writer.write("\n");
    }

    // Safely join list values (e.g., ["A", "B"]) -> "A; B"
    private String join(List<String> list) {
        return (list == null || list.isEmpty())
                ? ""
                : String.join("; ", list);
    }

    // Escape potential commas, quotes, or newlines in CSV
    private String escapeCsv(String value) {
        if (value == null) return "";
        String result = value.replace("\"", "\"\"");
        if (result.contains(",") || result.contains("\n")) {
            result = "\"" + result + "\"";
        }
        return result;
    }
}
