package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.domain.aggregate.Aggregate;

import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CasesReferredToCourtAggregate implements Aggregate {


    private static final Logger LOGGER = LoggerFactory.getLogger(CasesReferredToCourtAggregate.class);
    private static final long serialVersionUID = 171032067089771351L;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CasesReferredToCourt.class).apply(e -> {
                            //do nothing
                        }
                ), otherwiseDoNothing());

    }

    public Stream<Object> referCasesToCourt(final SjpCourtReferral courtReferral) {
        LOGGER.debug("cases is being refered To Court .");
        return apply(Stream.of(CasesReferredToCourt.casesReferredToCourt().withCourtReferral(courtReferral).build()));
    }


}
