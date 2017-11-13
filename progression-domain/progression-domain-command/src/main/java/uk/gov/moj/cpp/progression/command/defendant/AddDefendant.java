package uk.gov.moj.cpp.progression.command.defendant;


import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddDefendant {

    private final UUID caseId;

    private final String caseUrn;

    private final UUID defendantId;

    private final Long version;

    private final UUID personId;

    private final String policeDefendantId;

    private final List<Offence> offences ;

    public AddDefendant(UUID caseId, UUID defendantId, Long version, UUID personId, String policeDefendantId, List<Offence> offences, String caseUrn) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.version = version;
        this.personId = personId;
        this.policeDefendantId = policeDefendantId;
        this.offences = offences != null ? new ArrayList<>(offences) : new ArrayList<>();
        this.caseUrn=caseUrn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getPoliceDefendantId() {
        return policeDefendantId;
    }

    public List<Offence> getOffences() {
        return new ArrayList<>(offences);
    }


}
