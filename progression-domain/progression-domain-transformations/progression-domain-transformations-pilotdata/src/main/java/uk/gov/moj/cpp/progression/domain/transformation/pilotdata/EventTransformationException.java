package uk.gov.moj.cpp.progression.domain.transformation.pilotdata;

public class EventTransformationException extends RuntimeException {
    public EventTransformationException() {
    }

    public EventTransformationException(final String message) {
        super(message);
    }

    public EventTransformationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public EventTransformationException(final Throwable cause) {
        super(cause);
    }

    public EventTransformationException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
