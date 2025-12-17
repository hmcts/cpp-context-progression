package uk.gov.moj.cpp.progression.domain.event.defendant;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
public class DeletedDefendantOffences implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID defendantId;
    private final UUID caseId;
    private final List<UUID> offences;



    public DeletedDefendantOffences(final UUID defendantId, final UUID caseId,
                                    final List<UUID> offences) {
        this.defendantId = defendantId;
        this.caseId = caseId;
        this.offences = offences;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public List<UUID> getOffences() {
        return offences;
    }
}
