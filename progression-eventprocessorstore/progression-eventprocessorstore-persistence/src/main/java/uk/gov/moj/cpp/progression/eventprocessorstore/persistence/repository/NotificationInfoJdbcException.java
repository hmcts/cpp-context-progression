package uk.gov.moj.cpp.progression.eventprocessorstore.persistence.repository;

public class NotificationInfoJdbcException extends RuntimeException {
    private static final long serialVersionUID = 5934757852541630746L;

    public NotificationInfoJdbcException(String message, final Throwable cause) {
        super(message, cause);
    }
}

