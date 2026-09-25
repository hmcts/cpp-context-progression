package uk.gov.moj.cpp.progression.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.NextHearing.nextHearing;


import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.CasesReferredToCourtV2;

import uk.gov.justice.core.courts.ReferredCourtDocument;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.core.courts.SjpCourtReferral;
import uk.gov.justice.core.courts.TypeOfList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CasesReferredToCourtAggregateTest {

    private CasesReferredToCourtAggregate aggregate;

    @BeforeEach
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

    @Test
    public void shouldProcessInitiateCourtProceedings(){
        final CourtReferral courtReferral = CourtReferral.courtReferral().build();
        final List<Object> eventStream = aggregate.initiateCourtProceedings(courtReferral).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtProceedingsInitiated.class)));
    }

    @Test
    public void shouldAddTypeOfListToCourtProceedingsInitiated(){
        final CourtReferral courtReferral = CourtReferral.courtReferral().build();
        final TypeOfList typeOfList = TypeOfList.typeOfList().withId(UUID.randomUUID()).withDescription("Bench Warrant").build();

        final List<Object> eventStream = aggregate.initiateCourtProceedings(courtReferral, typeOfList).collect(toList());

        assertThat(eventStream.size(), is(1));
        assertThat(((CourtProceedingsInitiated) eventStream.get(0)).getTypeOfList(), is(typeOfList));
    }

    @Test
    public void shouldNotProcessInitiateCourtProceedingsItAlreadyInitiated(){
        final CourtReferral courtReferral = CourtReferral.courtReferral().build();
        List<Object> eventStream = aggregate.initiateCourtProceedings(courtReferral).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CourtProceedingsInitiated.class)));

        eventStream = aggregate.initiateCourtProceedings(courtReferral).collect(toList());
        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldReturnCasesReferredToCourtV2() {
        final SjpCourtReferral courtReferralV2 = new SjpCourtReferral(new ArrayList<ReferredCourtDocument>(),  null,nextHearing().build(), new ArrayList<ReferredProsecutionCase>(),null);
        final List<Object> eventStream = aggregate.referCasesToCourt(courtReferralV2).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(CasesReferredToCourtV2.class)));
    }
}
