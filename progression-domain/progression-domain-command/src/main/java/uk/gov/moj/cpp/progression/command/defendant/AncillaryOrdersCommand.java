package uk.gov.moj.cpp.progression.command.defendant;

public class AncillaryOrdersCommand {
    private String details;

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "AncillaryOrdersCommand{" +
                "details='" + details + '\'' +
                '}';
    }
}