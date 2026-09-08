package uk.gov.moj.cpp.progression.listing.domain.exception;

public class DataValidationException extends RuntimeException {
    public DataValidationException(String message) {
        super(message);
    }
}
