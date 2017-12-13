package uk.gov.moj.cpp.progression.domain.event.defendant;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

@Event("progression.events.bail-status-updated-for-defendant")
public class BailStatusUpdatedForDefendant {

    private final UUID caseId;
    private final UUID defendantId;
    private final String bailStatus;
    private final BailDocument bailDocument;
    private final LocalDate custodyTimeLimitDate;

    public BailStatusUpdatedForDefendant(UUID caseId, UUID defendentId, String bailStatus,
                                         BailDocument bailDocument, LocalDate custodyTimeLimitDate) {
        super();
        this.caseId = caseId;
        this.defendantId = defendentId;
        this.bailStatus = bailStatus;
        this.bailDocument = bailDocument;
        this.custodyTimeLimitDate = custodyTimeLimitDate;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public BailDocument getBailDocument() {
        return bailDocument;
    }

    public LocalDate getCustodyTimeLimitDate() {
        return custodyTimeLimitDate;
    }



}
