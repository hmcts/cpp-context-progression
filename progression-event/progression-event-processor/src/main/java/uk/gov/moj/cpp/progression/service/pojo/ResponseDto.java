package uk.gov.moj.cpp.progression.service.pojo;

public class ResponseDto {

    private int statusCode;
    private String messageBody;

    public ResponseDto(final int statusCode, final String messageBody) {
        this.statusCode = statusCode;
        this.messageBody = messageBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessageBody() {
        return messageBody;
    }
}
