package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Event("progression.events.defendant-added")
public class DefendantAdded {

    private UUID caseId;
    private String caseUrn;

    private UUID defendantId;

    private UUID personId;

    private Person person;

    private String policeDefendantId;

    private List<Offence> offences = new ArrayList<>();



    public DefendantAdded(UUID caseId,
                          UUID defendantId,
                          Person person,
                          String policeDefendantId,
                          List<Offence> offences, String caseUrn) {
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
