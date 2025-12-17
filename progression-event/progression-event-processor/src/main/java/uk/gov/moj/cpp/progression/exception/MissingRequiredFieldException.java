package uk.gov.moj.cpp.progression.exception;

public class MissingRequiredFieldException extends RuntimeException  {

    public MissingRequiredFieldException(String message) {
        super(String.format("Missing required data : %s",message));
    }


}
