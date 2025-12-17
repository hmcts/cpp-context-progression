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
public class BaseDefendantOffences implements Serializable {
    private static final long serialVersionUID = 1L;
    private final UUID defendantId;
    private final UUID caseId;
    private final List<BaseDefendantOffence> offences;

    @SuppressWarnings("squid:S2384")
    public BaseDefendantOffences(final UUID defendantId, final UUID caseId,
                                 final List<BaseDefendantOffence> offences) {
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

    @SuppressWarnings("squid:S2384")
    public List<BaseDefendantOffence> getOffences() {
        return offences;
    }
}
