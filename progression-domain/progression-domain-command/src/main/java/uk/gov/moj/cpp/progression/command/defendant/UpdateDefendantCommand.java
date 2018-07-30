package uk.gov.moj.cpp.progression.command.defendant;


import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;

import java.time.LocalDate;
import java.util.UUID;

@SuppressWarnings("squid:S00107")
public class UpdateDefendantCommand {

    private final UUID caseId;
    private final UUID defendantId;
    private final Person person;
    private final Interpreter interpreter;
    private final String bailStatus;
    private final UUID documentId;
    private final LocalDate custodyTimeLimitDate;
    private final String defenceSolicitorFirm;

    public UpdateDefendantCommand(UUID caseId, UUID defendantId, Person person, Interpreter interpreter, String bailStatus, UUID documentId, LocalDate custodyTimeLimitDate, String defenceSolicitorFirm) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.person = person;
        this.interpreter = interpreter;
        this.bailStatus = bailStatus;
        this.documentId = documentId;
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

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public LocalDate getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }

    public String getDefenceSolicitorFirm() {
        return defenceSolicitorFirm;
    }
}
