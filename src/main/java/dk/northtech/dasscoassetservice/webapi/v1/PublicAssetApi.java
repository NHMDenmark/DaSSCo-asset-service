package dk.northtech.dasscoassetservice.webapi.v1;


import dk.northtech.dasscoassetservice.domain.PublicAsset;
import dk.northtech.dasscoassetservice.services.PublicAssetService;
import dk.northtech.dasscoassetservice.webapi.exceptionmappers.DaSSCoError;
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

    @Inject
    public PublicAssetApi(PublicAssetService publicAssetService) {
        this.publicAssetService = publicAssetService;
    }


    @GET
    @Path("/metadata/{assetGuid}")
    @Operation(summary = "Get asset metadata for external users", description = "Returns limited metadata for external users. Can be used without a token.")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponse(responseCode = "200", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = PublicAsset.class)))
    @ApiResponse(responseCode = "400-599", content = @Content(mediaType = APPLICATION_JSON, schema = @Schema(implementation = DaSSCoError.class)))
    public Optional<PublicAsset> getAssetMetadata(@PathParam("assetGuid") String asset_guid) {
        return this.publicAssetService.getAsset(asset_guid);
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
        Optional<PublicAsset> assetOpt = this.publicAssetService.getAsset(assetGuid);

        if (assetOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Asset not found")
                    .build();
        }

        PublicAsset asset = assetOpt.get();

        // Use StreamingOutput to avoid loading large responses into memory
        StreamingOutput stream = output -> {
            try (
                    BufferedWriter writer =
                            new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))
            ) {
                writer.write("Field,Value\n");

                // Convert each field to a CSV-safe line
                writeLine(writer, "asset_guid", asset.asset_guid());
                writeLine(writer, "asset_pid", asset.asset_pid());
                writeLine(writer, "asset_subject", asset.asset_subject());
                writeLine(writer, "audited", String.valueOf(asset.audited()));
                writeLine(writer, "barcode", join(asset.barcode()));
                writeLine(writer, "camera_setting_control", asset.camera_setting_control());
                writeLine(writer, "collection", asset.collection());
                writeLine(writer, "date_asset_deleted_ars", asset.date_asset_deleted_ars());
                writeLine(writer, "date_asset_taken", asset.date_asset_taken());
                writeLine(writer, "date_audited", asset.date_audited());
                writeLine(writer, "file_formats", join(asset.file_formats()));
                writeLine(writer, "funding", join(asset.funding()));
                writeLine(writer, "institution", asset.institution());
                writeLine(writer, "legality", asset.legality().map(Object::toString).orElse(""));
                writeLine(writer, "metadata_version", asset.metadata_version());
                writeLine(writer, "mime_type", join(asset.mime_type()));
                writeLine(writer, "mos_id", asset.mos_id());
                writeLine(writer, "multi_specimen", String.valueOf(asset.multi_specimen()));
                writeLine(writer, "parent_guids", join(asset.parent_guids()));
                writeLine(writer, "payload_type", asset.payload_type());
                writeLine(writer, "pipeline_name", asset.pipeline_name());
                writeLine(writer, "preparation_type", join(asset.preparation_type()));
                writeLine(writer, "specify_attachment_title", asset.specify_attachment_title());
                writeLine(writer, "specimen_pid", join(asset.specimen_pid()));
            }
        };

        return Response.ok(stream, "text/csv")
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"asset_" + assetGuid + ".csv\""
                )
                .build();
    }

    private void writeLine(Writer writer, String field, String value) throws IOException {
        writer.write(escapeCsv(field));
        writer.write(",");
        writer.write(escapeCsv(value));
        writer.write("\n");
    }

    // Safely join list values (e.g., ["A", "B"]) -> "A; B"
    private String join(List<String> list) {
        return (list == null || list.isEmpty())
                ? ""
                : list.stream().map(this::escapeCsv).collect(Collectors.joining("; "));
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
