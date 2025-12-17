package uk.gov.moj.cpp.progression.command.defendant;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.gov.moj.cpp.progression.domain.event.defendant.Offence;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class AddDefendant {

    private final UUID caseId;

    private final String caseUrn;

    private final UUID defendantId;

    private final Long version;

    private final Person person;

    private final String policeDefendantId;

    private final List<Offence> offences ;

    public AddDefendant(final UUID caseId, final UUID defendantId, final Long version, final Person person, final String policeDefendantId, final List<Offence> offences, final String caseUrn) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.version = version;
        this.person = person;
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

    public Person getPerson() {
        return person;
    }

    public String getPoliceDefendantId() {
        return policeDefendantId;
    }

    public List<Offence> getOffences() {
        return new ArrayList<>(offences);
    }


}
