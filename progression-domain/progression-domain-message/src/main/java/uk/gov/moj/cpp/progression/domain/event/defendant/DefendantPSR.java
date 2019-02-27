package uk.gov.moj.cpp.progression.domain.event.defendant;


import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class DefendantPSR {

    private final UUID defendantId;
    private final Boolean psrIsRequested;

    public DefendantPSR(final UUID defendantId, final Boolean psrIsRequested) {
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
        return "DefendantPSR{" +
                "defendantId=" + defendantId +
                ", psrIsRequested=" + psrIsRequested +
                '}';
    }
}
