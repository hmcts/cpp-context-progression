package uk.gov.moj.cpp.progression.helper;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingResultUnscheduledListingHelperTest {
    private static final UUID WCPU = UUID.fromString("0d1b161b-d6b0-4b1b-ae08-535864e4f631");
    private static final UUID WCPN = UUID.fromString("ed34136f-2a13-45a4-8d4f-27075ae3a8a9");

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

    @Captor
    private ArgumentCaptor<Set<UUID>> hearingsToBeSendNotificationCaptor;

    @Test
    public void shouldProcessUnscheduledCourtHearingsWhenHasDefendant(){
        final UUID hearing1 = UUID.randomUUID();
        final UUID hearing2 = UUID.randomUUID();
        final UUID hearing3 = UUID.randomUUID();
        final Hearing hearing = Hearing.hearing().withId(hearing1).build();

        when(unscheduledCourtHearingListTransformer.transformHearing(any()))
                .thenReturn(asList(
                        createUnscheduledListingNeeds(hearing1, WCPU),
                        createUnscheduledListingNeeds(hearing2, UUID.randomUUID()),
                        createUnscheduledListingNeeds(hearing3, WCPN)
                ));

        hearingResultUnscheduledListingHelper.processUnscheduledCourtHearings(event, hearing);

        verify(unscheduledCourtHearingListTransformer, times(1)).transformHearing(any());
        verify(listingService, times(1)).listUnscheduledHearings(any(), any());
        verify(progressionService, times(1)).sendUpdateDefendantListingStatusForUnscheduledListing(any(), anyList(), hearingsToBeSendNotificationCaptor.capture());

        final Set<UUID> hearingsToNofify = hearingsToBeSendNotificationCaptor.getValue();
        assertThat(hearingsToNofify.size(), is(2));
        assertThat(hearingsToNofify.contains(hearing1), is(true));
        assertThat(hearingsToNofify.contains(hearing3), is(true));
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
        verify(progressionService, times(0)).sendUpdateDefendantListingStatusForUnscheduledListing(any(), anyList(), anySet());
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

    @Test
    public void shouldRemoveJudicialResultsForUnscheduledHearing(){
        final HearingUnscheduledListingNeeds unscheduledListingNeeds = HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withDefendants(singletonList(Defendant.defendant()
                                .withOffences(singletonList(Offence.offence()
                                        .withJudicialResults(singletonList(JudicialResult.judicialResult().build()))
                                        .build()))
                                .build()))
                        .build()))
                .withCourtApplications(asList(CourtApplication.courtApplication()
                        .withId(UUID.randomUUID())
                        .withJudicialResults(asList(JudicialResult.judicialResult()
                                .withJudicialResultPrompts(asList(JudicialResultPrompt.judicialResultPrompt()
                                        .withLabel("Next hearing in Crown Court")
                                        .withDurationElement(JudicialResultPromptDurationElement.judicialResultPromptDurationElement()
                                                .withPrimaryDurationValue(20)
                                                .build())
                                        .withValue("Date and time to be fixed:Yes\n" +
                                                "Courthouse organisation name:Mold Crown Court\n" +
                                                "Courthouse address line 1:The Law Courts\n")
                                        .build()))
                                .build()))
                        .withType(CourtApplicationType.courtApplicationType()
                                .withLinkType(LinkType.LINKED)
                                .build())
                        .withApplicant(CourtApplicationParty.courtApplicationParty()
                                .withId(UUID.randomUUID())
                                .build())
                        .build()))
                .build();
        final Hearing hearing = hearingResultUnscheduledListingHelper.convertToHearing(unscheduledListingNeeds, null);

        assertThat(hearing.getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getJudicialResults(), is(nullValue()));
        assertThat(hearing.getCourtApplications().get(0).getJudicialResults(), is(nullValue()));

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

    private HearingUnscheduledListingNeeds createUnscheduledListingNeeds(final UUID hearingId, final UUID judicialResultTypeId){
        return HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(asList(Defendant.defendant()
                                .withOffences(asList(Offence.offence()
                                        .withJudicialResults(asList(JudicialResult.judicialResult()
                                                .withJudicialResultTypeId(judicialResultTypeId)
                                                .build()))
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }


}
