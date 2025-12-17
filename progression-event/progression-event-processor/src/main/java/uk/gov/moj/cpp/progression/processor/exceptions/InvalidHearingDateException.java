package uk.gov.moj.cpp.progression.processor.exceptions;

public class InvalidHearingDateException extends RuntimeException {

    public InvalidHearingDateException(String message) {
        super(message);
    }

    public InvalidHearingDateException(final String message, final Exception exception) {
        super(message, exception);
    }
}
