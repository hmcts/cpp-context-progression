package uk.gov.moj.cpp.progression.service.exception;


public class DocumentGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DocumentGenerationException() {
    }

    public DocumentGenerationException(final Throwable cause) {
        super(cause);
    }
}
