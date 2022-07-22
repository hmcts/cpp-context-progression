package uk.gov.moj.cpp.progression.service.exception;

public class SecureConnectionException extends RuntimeException {
    public SecureConnectionException() {
        super("Error setting up secure connection using certificates from azure keystor");
    }

    public SecureConnectionException(String errorMessage) {
        super(errorMessage);
    }

    public SecureConnectionException(String errorMessage, Throwable error) {
        super(errorMessage, error);
    }
}
