package dk.northtech.dasscoassetservice.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Exceptions thrown for Dassco Actions (such as assets being locked, workstations out of service, etc")
public class DasscoIllegalActionException extends RuntimeException {
    private String body;

    public DasscoIllegalActionException() {
    }

    public DasscoIllegalActionException(String message) {
        super(message);
    }

    public DasscoIllegalActionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DasscoIllegalActionException(String message, String body) {
        super(message);
        this.body = body;
    }

    public String body() {
        return body;
    }
}
