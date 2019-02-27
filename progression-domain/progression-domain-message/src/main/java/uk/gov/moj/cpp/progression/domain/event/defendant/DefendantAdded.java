package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
/**
 *
 * @deprecated
 *
 */
@Deprecated
@Event("progression.events.defendant-added")
public class DefendantAdded {

    private final UUID caseId;
    private final String caseUrn;

    private final UUID defendantId;

    private UUID personId;

    private final Person person;

    private final String policeDefendantId;

    private List<Offence> offences = new ArrayList<>();



    public DefendantAdded(final UUID caseId,
                          final UUID defendantId,
                          final Person person,
                          final String policeDefendantId,
                          final List<Offence> offences, final String caseUrn) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.person = person;
        this.personId=person!=null?person.getId():null;
        this.policeDefendantId = policeDefendantId;
        this.offences = offences;
        this.caseUrn=caseUrn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Person getPerson() {
        return person;
    }

    public List<Offence> getOffences() {
        return offences;
    }

    public String getPoliceDefendantId() {
        return policeDefendantId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public UUID getPersonId() {
        return personId;
    }


}
