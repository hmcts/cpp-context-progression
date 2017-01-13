package uk.gov.moj.cpp.progression.domain.event.defendant;


import java.util.UUID;

public class DefendantPreSentenceReportRequested {

    private final UUID defendantId;
    private final Boolean psrIsRequested;

    public DefendantPreSentenceReportRequested(UUID defendantId, Boolean psrIsRequested) {
        this.defendantId = defendantId;
        this.psrIsRequested = psrIsRequested;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Boolean getPsrIsRequested() {
        return psrIsRequested;
    }

    @Override
    public String toString() {
        return "DefendantPreSentenceReportRequested{" +
                "defendantId=" + defendantId +
                ", psrIsRequested=" + psrIsRequested +
                '}';
    }
}
