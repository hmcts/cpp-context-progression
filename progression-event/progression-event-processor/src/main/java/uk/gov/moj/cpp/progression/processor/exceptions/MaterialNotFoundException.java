package uk.gov.moj.cpp.progression.processor.exceptions;

public class MaterialNotFoundException extends RuntimeException {

    public MaterialNotFoundException(final String message) {
        super(message);
    }
}