package dk.northtech.dasscoassetservice.domain;

public class DasscoIllegalActionException extends RuntimeException {
    public DasscoIllegalActionException() {
    }

    public DasscoIllegalActionException(String message) {
        super(message);
    }

    public DasscoIllegalActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
