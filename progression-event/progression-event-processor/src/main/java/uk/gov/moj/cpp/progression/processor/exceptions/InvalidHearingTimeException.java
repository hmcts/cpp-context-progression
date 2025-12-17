package uk.gov.moj.cpp.progression.processor.exceptions;

public class InvalidHearingTimeException extends RuntimeException {

    public InvalidHearingTimeException(final String message) {
        super(message);
    }

    public InvalidHearingTimeException(final String message, final Exception e) {
        super(message, e);
    }
}
