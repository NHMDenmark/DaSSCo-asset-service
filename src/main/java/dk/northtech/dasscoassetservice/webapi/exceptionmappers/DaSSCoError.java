package dk.northtech.dasscoassetservice.webapi.exceptionmappers;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;

public class DaSSCoError {
    @Schema(allowableValues = "Error")
    public final String type = "Error";
    @Schema(allowableValues = "1.0")
    public final String protocolVersion;
    public final DaSSCoErrorCode errorCode;
    public final String errorMessage;

    public DaSSCoError(
            @Nonnull String protocolVersion,
            @Nonnull DaSSCoErrorCode errorCode,
            String errorMessage
    ) {
        this.protocolVersion = protocolVersion;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public DaSSCoError(
            @Nonnull String protocolVersion,
            @Nonnull DaSSCoErrorCode errorCode,
            Throwable t
    ) {
        this(protocolVersion, errorCode, getCauseMessage(t));
    }

    public static String getCauseMessage(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    public static Class<?> getCauseType(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getClass();
    }
}
