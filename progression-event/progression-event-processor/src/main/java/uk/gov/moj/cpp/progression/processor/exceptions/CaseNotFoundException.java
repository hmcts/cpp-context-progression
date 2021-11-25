package uk.gov.moj.cpp.progression.processor.exceptions;

public class CaseNotFoundException extends RuntimeException {

    public CaseNotFoundException(final String message) {
        super(message);
    }
}
