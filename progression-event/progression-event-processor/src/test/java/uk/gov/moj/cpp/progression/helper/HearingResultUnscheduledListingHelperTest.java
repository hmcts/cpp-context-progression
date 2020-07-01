package uk.gov.moj.cpp.progression.helper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;


import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultUnscheduledListingHelperTest {
    @InjectMocks
    private HearingResultUnscheduledListingHelper hearingResultUnscheduledListingHelper;

    @Mock
    private UnscheduledCourtHearingListTransformer unscheduledCourtHearingListTransformer;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private ListingService listingService;

    @Mock
    private  JsonEnvelope event;

    @Test
    public void shouldProcessUnscheduledCourtHearingsWhenHasDefendant(){
        Hearing hearing = Hearing.hearing().build();
        when(unscheduledCourtHearingListTransformer.transformHearing(any()))
                .thenReturn(asList(
                        HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase().build()))
                        .build()));

        hearingResultUnscheduledListingHelper.processUnscheduledCourtHearings(event, hearing);

        verify(unscheduledCourtHearingListTransformer, times(1)).transformHearing(any());
        verify(listingService, times(1)).listUnscheduledHearings(any(), any());
        verify(progressionService, times(1)).sendUpdateDefendantListingStatusForUnscheduledListing(any(), anyList());
    }


    @Test
    public void shouldProcessUnscheduledCourtHearingsWhenStandalone(){
        Hearing hearing = Hearing.hearing().build();
        when(unscheduledCourtHearingListTransformer.transformHearing(any()))
                .thenReturn(asList(
                        HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                                .withCourtApplications(asList(CourtApplication.courtApplication().build()))
                                .build()));

        hearingResultUnscheduledListingHelper.processUnscheduledCourtHearings(event, hearing);

        verify(unscheduledCourtHearingListTransformer, times(1)).transformHearing(any());
        verify(listingService, times(1)).listUnscheduledHearings(any(), any());
        verify(progressionService, times(0)).sendUpdateDefendantListingStatusForUnscheduledListing(any(), anyList());
    }

    @Test
    public void shouldFindUnscheduledFlagInApp(){
        final Hearing hearing = Hearing.hearing()
                .withCourtApplications(asList(
                        CourtApplication.courtApplication()
                                .withJudicialResults(asList(judicialResultWithFlag()))
                                .build())).build();

        boolean actual = hearingResultUnscheduledListingHelper.checksIfUnscheduledHearingNeedsToBeCreated(hearing);
        assertThat(actual, is(true));
    }

    @Test
    public void shouldFindUnscheduledFlagInCase(){
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(asList(createProsecutionCase(judicialResultWithFlag())))
                .build();

        boolean actual = hearingResultUnscheduledListingHelper.checksIfUnscheduledHearingNeedsToBeCreated(hearing);
        assertThat(actual, is(true));
    }

    @Test
    public void shouldNotFindUnscheduledFlagInCase(){
        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(asList(createProsecutionCase(judicialResultWithoutFlag())))
                .build();

        boolean actual = hearingResultUnscheduledListingHelper.checksIfUnscheduledHearingNeedsToBeCreated(hearing);
        assertThat(actual, is(false));
    }

    private JudicialResult judicialResultWithFlag(){
        return JudicialResult.judicialResult().withIsUnscheduled(true).build();
    }

    private JudicialResult judicialResultWithoutFlag(){
        return JudicialResult.judicialResult().withIsUnscheduled(false).build();
    }



    private ProsecutionCase createProsecutionCase(final JudicialResult judicialResult){
        return ProsecutionCase.prosecutionCase()
                .withDefendants(asList(Defendant.defendant()
                        .withOffences(asList(Offence.offence()
                                .withJudicialResults(asList(judicialResult))
                                .build()))
                        .build()))
                    .build();
    }

}
