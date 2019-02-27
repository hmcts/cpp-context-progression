package uk.gov.moj.cpp.progression.domain.event.completedsendingsheet;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("progression.event.defendant-bail-document-created")
public class DefendantBailDocumentCreated {
    private final UUID caseId;
    private final UUID defendantId;
    private final UUID materialId;
    private final UUID bailDocumentId;

    public DefendantBailDocumentCreated(final UUID caseId, final UUID defendantId, final UUID materialId, final UUID bailDocumentId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.materialId = materialId;
        this.bailDocumentId = bailDocumentId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public UUID getBailDocumentId() {
        return bailDocumentId;
    }
}
