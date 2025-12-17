package uk.gov.moj.cpp.progression.processor.exceptions;

public class CourtApplicationAndCaseNotFoundException extends RuntimeException {

    public CourtApplicationAndCaseNotFoundException(final String message) {
        super(message);
    }
}