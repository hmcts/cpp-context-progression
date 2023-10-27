package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.moj.cpp.progression.aggregate.CasesReferredToCourtAggregate;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class CasesReferredToCourtAggregateTest {

    private CasesReferredToCourtAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new CasesReferredToCourtAggregate();
    }

    @Test
    public void shouldReturnCasesReferredToCourt() {
        final SjpCourtReferral courtReferral = new SjpCourtReferral(new ArrayList<ReferredCourtDocument>(),  null, null, new ArrayList<ReferredProsecutionCase>(),null);
        final List<Object> eventStream = aggregate.referCasesToCourt(courtReferral).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CasesReferredToCourt.class)));
    }
}
