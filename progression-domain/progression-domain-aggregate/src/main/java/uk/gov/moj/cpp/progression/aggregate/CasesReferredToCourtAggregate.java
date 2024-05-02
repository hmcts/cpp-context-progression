package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.CasesReferredToCourtV2.casesReferredToCourtV2;
import static  java.util.stream.Stream.of;

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

        if (nonNull(courtReferral.getNextHearing())) {
            return apply(of(casesReferredToCourtV2().withCourtReferral(courtReferral).build()));
        } else {
            return apply(Stream.of(CasesReferredToCourt.casesReferredToCourt().withCourtReferral(courtReferral).build()));
        }
    }


    public Stream<Object> initiateCourtProceedings(final CourtReferral courtReferral) {
        LOGGER.info("Court Proceedings being initiated");
        return apply(Stream.of(CourtProceedingsInitiated.courtProceedingsInitiated().withCourtReferral(courtReferral).build()));
    }
}
