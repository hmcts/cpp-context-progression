package uk.gov.moj.cpp.progression.domain.event.defendant;


import uk.gov.moj.cpp.progression.domain.StatementOfOffence;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
/**
 * 
 * @deprecated
 *
 */
@Deprecated
@SuppressWarnings("squid:S00107")
public class DefendantOffence extends BaseDefendantOffence implements Serializable {

    private static final long serialVersionUID = 1L;

    private final StatementOfOffence statementOfOffence;

    public DefendantOffence(final UUID id, final String offenceCode, final String wording,
                            final LocalDate startDate, final LocalDate endDate, final Integer count,
                            final LocalDate convictionDate, final StatementOfOffence statementOfOffence) {
        super(id, offenceCode, wording, startDate, endDate, count, convictionDate);
        this.statementOfOffence = statementOfOffence;
    }

    public StatementOfOffence getStatementOfOffence() {
        return statementOfOffence;
    }
}
