package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.time.LocalDate;
import java.util.UUID;

import uk.gov.justice.domain.annotation.Event;
/**
 *
 * @deprecated
 *
 */
@Deprecated
@SuppressWarnings("squid:S00107")
@Event("progression.events.defendant-updated")
public class DefendantUpdated {

    private final UUID caseId;
    private final UUID defendantId;
    private final Person person;
    private final BailDocument bailDocument;
    private final Interpreter interpreter;
    private final String bailStatus;
    private final LocalDate custodyTimeLimitDate;
    private final String defenceSolicitorFirm;

    public DefendantUpdated(final UUID caseId, final UUID defendantId, final Person person, final BailDocument bailDocument, final Interpreter interpreter, final String bailStatus, final LocalDate custodyTimeLimitDate, final String defenceSolicitorFirm) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.person = person;
        this.bailDocument = bailDocument;
        this.interpreter = interpreter;
        this.bailStatus = bailStatus;
        this.custodyTimeLimitDate = custodyTimeLimitDate;
        this.defenceSolicitorFirm = defenceSolicitorFirm;
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

    public BailDocument getBailDocument() {
        return bailDocument;
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

    public String getDefenceSolicitorFirm() {
        return defenceSolicitorFirm;
    }
}
