package uk.gov.moj.cpp.progression.nows;

public class InvalidNotificationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidNotificationException(String message) {
        super(message);
    }

    public InvalidNotificationException(String message, Exception ex) {
        super(message, ex);
    }
}
