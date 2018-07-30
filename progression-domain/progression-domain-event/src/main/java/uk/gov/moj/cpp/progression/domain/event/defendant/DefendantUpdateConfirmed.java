package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;

@Event("progression.events.defendant-update-confirmed")
public class DefendantUpdateConfirmed {

    private final UUID caseId;
    private final UUID defendantId;
    private final Person person;
    private final Interpreter interpreter;
    private final String bailStatus;
    private final LocalDate custodyTimeLimitDate;
    private final String defenceOrganisation;

    public DefendantUpdateConfirmed(final UUID caseId, final UUID defendantId, final Person person,
                    final Interpreter interpreter, final String bailStatus,
                    final LocalDate custodyTimeLimitDate, final String defenceOrganisation) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.person = person;
        this.interpreter = interpreter;
        this.bailStatus = bailStatus;
        this.custodyTimeLimitDate = custodyTimeLimitDate;
        this.defenceOrganisation = defenceOrganisation;
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

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public LocalDate getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public String getDefenceOrganisation() {
        return defenceOrganisation;
    }
}
