package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CasesReferredToCourtAggregate implements Aggregate {


    private static final Logger LOGGER = LoggerFactory.getLogger(CasesReferredToCourtAggregate.class);
    private static final long serialVersionUID = 101L;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CasesReferredToCourt.class).apply(e -> {
                            //do nothing
                        }
                ),
                when(CourtProceedingsInitiated.class).apply(courtProceedingsInitiated -> {
                    // do nothing
                }),
                otherwiseDoNothing());

    }

    public Stream<Object> referCasesToCourt(final SjpCourtReferral courtReferral) {
        LOGGER.debug("Cases are being referred To Court.");
        return apply(Stream.of(CasesReferredToCourt.casesReferredToCourt().withCourtReferral(courtReferral).build()));
    }


    public Stream<Object> initiateCourtProceedings(CourtReferral courtReferral) {
        LOGGER.info("Court Proceedings being initiated");
        return apply(Stream.of(CourtProceedingsInitiated.courtProceedingsInitiated().withCourtReferral(courtReferral).build()));
    }
}
