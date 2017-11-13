package uk.gov.moj.cpp.progression.domain.event.defendant;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.events.interpreter-for-defendant-updated")
public class InterpreterUpdatedForDefendant {

    private final UUID caseId;
    private final UUID defendantId;
    private final Interpreter  interpreter;

    @JsonCreator
    public InterpreterUpdatedForDefendant(
            @JsonProperty(value = "caseId") UUID caseId,
            @JsonProperty(value = "defendantId") UUID defendantId,
            @JsonProperty(value = "interpreter") Interpreter interpreter) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.interpreter = interpreter;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

}
