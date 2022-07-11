package uk.gov.moj.cpp.progression.aggregate;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.CoreTemplateArguments.toMap;
import static uk.gov.moj.cpp.progression.test.CoreTestTemplates.defaultArguments;

import uk.gov.justice.core.courts.AddBreachApplication;
import uk.gov.justice.core.courts.BreachApplicationCreationRequested;
import uk.gov.justice.core.courts.BreachedApplications;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingExtendedProcessed;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.HearingUpdatedForAllocationFields;
import uk.gov.justice.core.courts.HearingUpdatedWithCourtApplication;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.ListingNumberUpdated;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.UpdateHearingForAllocationFields;
import uk.gov.justice.progression.courts.DeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.ExtendCustodyTimeLimitResulted;
import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.HearingResulted;
import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.progression.courts.VejDeletedHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.staginghmi.courts.UpdateHearingFromHmi;
import uk.gov.moj.cpp.progression.test.CoreTestTemplates;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingAggregateTest {

    @InjectMocks
    private HearingAggregate hearingAggregate;

    @Test
    public void shouldDoCorrectiononHearingDaysWithoutCourtCentre() throws IOException {
        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(randomUUID(), toMap(randomUUID(), singletonList(randomUUID()))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Object response = hearingAggregate.apply(createHearingDaysWithoutCourtCentreCorrected());

        assertThat(response.getClass(), is(CoreMatchers.equalTo(HearingDaysWithoutCourtCentreCorrected.class)));
    }

    @Test
    public void shouldApplyBreachApplicationCreationRequestedEvent() {
        final AddBreachApplication addBreachApplication = AddBreachApplication
                .addBreachApplication()
                .withBreachedApplications(asList(BreachedApplications.breachedApplications()
                                .withApplicationType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .build())
                                .withCourtOrder(CourtOrder.courtOrder()
                                        .withId(randomUUID())
                                        .build())
                                .build(),
                        BreachedApplications.breachedApplications()
                                .withApplicationType(CourtApplicationType.courtApplicationType()
                                        .withId(randomUUID())
                                        .build())
                                .withCourtOrder(CourtOrder.courtOrder()
                                        .withId(randomUUID())
                                        .build())
                                .build()
                ))
                .withMasterDefendantId(randomUUID())
                .withHearingId(randomUUID())
                .build();

        final List<Object> response = hearingAggregate.addBreachApplication(addBreachApplication).collect(Collectors.toList());
        assertThat(response.size(), is(2));
        assertThat(response.get(0).getClass(), is(CoreMatchers.equalTo(BreachApplicationCreationRequested.class)));
    }

    @Test
    public void shouldApplyExtendCustodyTimeLimitResultedEvent() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        final Hearing hearing = CoreTestTemplates.hearing(defaultArguments()
                .setJurisdictionType(JurisdictionType.CROWN)
                .setStructure(toMap(caseId, toMap(defendantId, singletonList(offenceId))))
                .setConvicted(false)).build();

        hearingAggregate.apply(createHearingResulted(hearing));
        final UUID hearingId = hearing.getId();

        final ExtendCustodyTimeLimitResulted extendCustodyTimeLimitResulted = new ExtendCustodyTimeLimitResulted.Builder()
                .withHearingId(hearingId)
                .withCaseId(caseId)
                .withOffenceId(offenceId)
                .withExtendedTimeLimit(LocalDate.now())
                .build();

        hearingAggregate.apply(extendCustodyTimeLimitResulted);

        assertThat(hearingAggregate.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0)
                .getCustodyTimeLimit().getIsCtlExtended(), is(true));
    }

    @Test
    public void shouldNotRaiseHearingPopulateEventWhenAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(true).build()).build())
                        .build())))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        assertThat(events.findFirst().isPresent(), is(false));
        assertThat(hearing.getProsecutionCases().size(), is(1));
        assertThat(hearing.getCourtApplications().size(), is(1));
    }

    @Test
    public void shouldRaiseHearingPopulateEventWhenProsecutionCaseAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(false).build()).build())
                        .build())))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<HearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToProbationCaseWorker()
                .map(HearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(nullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(notNullValue()));
    }

    @Test
    public void shouldRaiseHearingPopulateVejEventWhenProsecutionCaseAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(null)
                .withCourtApplications(null)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<VejHearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToVEP()
                .map(VejHearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(notNullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(nullValue()));
    }

    @Test
    public void shouldRaiseHearingResultedEventWhenHearingResultsAreSaved() {

        final UUID offenceId = randomUUID();
        final Hearing hearing = getHearing(offenceId);
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());

        // hearing resulted without listing number
        final Hearing hearingResult = Hearing.hearing().withValuesFrom(hearing).withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                        .withId(randomUUID())
                        .withIsYouth(true)
                        .withOffences(of(Offence.offence()
                                .withId(offenceId)
                                .build()))
                        .build()
                )))
                .build()
        ))).build();

        final Stream<Object> eventStream = hearingAggregate.saveHearingResult(hearingResult,ZonedDateTime.now(),of(randomUUID()));

        final List<?> eventList = eventStream.collect(toList());
        assertThat(eventList.get(0), instanceOf(HearingResulted.class));
        int listingNumber = ((HearingResulted) eventList.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getListingNumber();
        assertThat(listingNumber,is(1));
    }

    @Test
    public void shouldRaiseHearingPopulateEventWhenCourtApplicationAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(true).build()).build())
                        .build())))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<HearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToProbationCaseWorker()
                .map(HearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(notNullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(nullValue()));
    }

    @Test
    public void shouldRaiseHearingPopulateVejEventWhenCourtApplicationAllDefendantsAreYouth() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(null)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<VejHearingPopulatedToProbationCaseworker> events = hearingAggregate.populateHearingToVEP()
                .map(VejHearingPopulatedToProbationCaseworker.class::cast).collect(toList());

        assertThat(events.get(0).getHearing().getProsecutionCases(), is(notNullValue()));
        assertThat(events.get(0).getHearing().getCourtApplications(), is(nullValue()));
    }


    @Test
    public void shouldNotRaiseHearingPopulateEventWhenBoxWork() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(true)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        assertThat(events.findFirst().isPresent(), is(false));
    }

    @Test
    public void shouldNotRaiseHearingPopulateVejEventWhenBoxWork() {
        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withIsBoxHearing(true)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        assertThat(events.findFirst().isPresent(), is(false));
    }
    @Test
    public void shouldNotRaiseHearingPopulateEventWhenResulted() {

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(false)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.apply(ProsecutionCaseDefendantListingStatusChanged.prosecutionCaseDefendantListingStatusChanged()
                .withHearing(hearing)
                .withHearingListingStatus(HearingListingStatus.HEARING_RESULTED)
                .build());

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        assertThat(events.findFirst().isPresent(), is(false));
    }

    @Test
    public void shouldRaiseHearingTrialVacated() {

        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final List<Object> eventStream = hearingAggregate.hearingTrialVacated(hearingId, randomUUID()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.equalTo(HearingTrialVacated.class)));
        final HearingTrialVacated hearingTrialVacated = (HearingTrialVacated)eventStream.get(0);

        assertThat(hearingTrialVacated.getHearingId(), is(hearingId));

    }

    @Test
    public void shouldRaiseHearingPopulateEventForOnlyAdults() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build(), ProsecutionCase.prosecutionCase()
                        .withId(case2Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build())))
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        final HearingPopulatedToProbationCaseworker event = (HearingPopulatedToProbationCaseworker) events.findFirst().get();
        assertThat(event.getHearing().getProsecutionCases().size(), is(1));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case2Id));
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }

    @Test
    public void shouldRaiseHearingPopulateVejEventWithYouth() {
        final UUID case1Id = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build()
                        )))
                        .build(), ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withIsYouth(true)
                                        .build(),
                                Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build())))
                                        .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        final VejHearingPopulatedToProbationCaseworker event = (VejHearingPopulatedToProbationCaseworker) events.findFirst().get();
        assertThat(event.getHearing().getProsecutionCases().size(), is(2));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case1Id));
        assertThat(prosecutionCase.getDefendants().size(), is(2));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }

    @Test
    public void shouldRaiseDeletedProbationEventWhenHearingDeleted() {

        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.apply(createHearingDeleted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        final DeletedHearingPopulatedToProbationCaseworker event = (DeletedHearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getId(), is(hearingId));
        assertThat(event.getHearing().getProsecutionCases().size(), is(1));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case1Id));
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }

    @Test
    public void shouldRaiseDeletedProbationVejEventWhenHearingDeleted() {

        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.apply(createHearingDeleted(hearing));

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        final VejDeletedHearingPopulatedToProbationCaseworker event = (VejDeletedHearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getId(), is(hearingId));
        assertThat(event.getHearing().getProsecutionCases().size(), is(1));
        final ProsecutionCase prosecutionCase = event.getHearing().getProsecutionCases().get(0);
        assertThat(prosecutionCase.getId(), is(case1Id));
        assertThat(prosecutionCase.getDefendants().size(), is(1));
        assertThat(prosecutionCase.getDefendants().get(0).getId(), is(defendantId));

    }
    @Test
    public void shouldUpdateHearingWithCaseMarkers(){
        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();
        final Marker marker = Marker.marker().withId(randomUUID()).build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.updateCaseMarkers(singletonList(marker),case1Id, hearingId);

        final Stream<Object> events = hearingAggregate.populateHearingToProbationCaseWorker();
        final HearingPopulatedToProbationCaseworker event = (HearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getProsecutionCases().get(0).getCaseMarkers().get(0).getId(), is(marker.getId()));
    }

    @Test
    public void shouldUpdateVejHearingWithCaseMarkers(){
        final UUID case1Id = randomUUID();
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(case1Id)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                        .withLaaApplnReference(LaaReference.laaReference().build())
                                        .build())))
                                .build()
                        )))
                        .build()
                )))
                .build();
        final Marker marker = Marker.marker().withId(randomUUID()).build();

        hearingAggregate.apply(createHearingResulted(hearing));
        hearingAggregate.updateCaseMarkers(singletonList(marker),case1Id, hearingId);

        final Stream<Object> events = hearingAggregate.populateHearingToVEP();
        final VejHearingPopulatedToProbationCaseworker event = (VejHearingPopulatedToProbationCaseworker) events.findFirst().get();

        assertThat(event.getHearing().getProsecutionCases().get(0).getCaseMarkers().get(0).getId(), is(marker.getId()));
    }
    @Test
    public void shouldUpdateApplicationInHearingAndRaiseProbationEvent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withCourtApplications(singletonList(CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()))
                        .build())
                .build());

        final List<Object> events = hearingAggregate.updateApplication(CourtApplication.courtApplication()
                .withId(applicationId)
                .withApplicationReference("B")
                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                .build()).collect(toList());

        assertThat(((HearingUpdatedWithCourtApplication)events.get(0)).getCourtApplication().getApplicationReference(), is("B"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtApplications().get(0).getApplicationReference(), is("B"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getId(), is(hearingId));

    }
    @Test
    public void shouldUpdateHearingForAllocationFieldsAndRaiseProbationEvent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withCourtApplications(asList(CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()))
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                        .withType(HearingType.hearingType().withDescription("Application").build())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withHearingLanguage(HearingLanguage.WELSH)
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                        .build()
                ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields)events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingDays().get(0).getSittingDay() , is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
    }

    @Test
    public void shouldAddNewApplicationToHearingAndRaiseProbationEvent(){
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("A")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()
        ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields)events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingDays().get(0).getSittingDay() , is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtApplications().get(0).getId(), is(applicationId));
    }

    @Test
    public void shouldAddNewApplicationToHearingHasApplicationsAndRaiseProbationEvent(){
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withCourtApplications(Stream.of(CourtApplication.courtApplication()
                                .withId(randomUUID())
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()).collect(toList()))
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("A")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()
        ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields)events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingDays().get(0).getSittingDay() , is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtApplications().get(1).getId(), is(applicationId));
    }

    @Test
    public void shouldUpdateApplicationToHearingHasApplicationsAndRaiseProbationEvent(){
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        ZonedDateTime sittingDay = ZonedDateTime.now();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withCourtApplications(Stream.of(CourtApplication.courtApplication()
                                .withId(randomUUID())
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build(), CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicationReference("C")
                                .withSubject(CourtApplicationParty.courtApplicationParty().build())
                                .build()).collect(toList()))
                        .build())
                .build());
        sittingDay = ZonedDateTime.now().plusDays(1);
        final List<Object> events = hearingAggregate.updateAllocationFields(UpdateHearingForAllocationFields.updateHearingForAllocationFields()
                .withType(HearingType.hearingType().withDescription("Application").build())
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(sittingDay).build()))
                .withHearingLanguage(HearingLanguage.WELSH)
                .withCourtCentre(CourtCentre.courtCentre().withCode("A002").build())
                .withCourtApplication(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("B")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build()
        ).collect(toList());

        assertThat(((HearingUpdatedForAllocationFields)events.get(0)).getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingDays().get(0).getSittingDay() , is(sittingDay));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getType().getDescription(), is("Application"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtCentre().getCode(), is("A002"));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getHearingLanguage(), is(HearingLanguage.WELSH));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtApplications().get(1).getId(), is(applicationId));
        assertThat(((HearingPopulatedToProbationCaseworker)events.get(1)).getHearing().getCourtApplications().get(1).getApplicationReference(), is("B"));
    }

    @Test
    public void shouldUpdateHearingWithNewProsecutionCaseWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                        .withId(defendantId1)
                                                        .build(),
                                                Defendant.defendant()
                                                        .withId(defendantId2)
                                                        .build()))).build()))
                .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId2).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().size(), is(2));
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(2));
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(1).getId(), is(caseId2));
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(1).getDefendants().size(), is(1));
    }

    @Test
    public void shouldUpdateVejHearingWithNewProsecutionCaseWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId1)
                                                .build(),
                                        Defendant.defendant()
                                                .withId(defendantId2)
                                                .build()))).build()))
                        .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId2).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToVEP().collect(toList());
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().size(), is(2));
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(2));
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(1).getId(), is(caseId2));
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(1).getDefendants().size(), is(1));
    }

    @Test
    public void shouldUpdateHearingWithNewDefendantWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                        .withId(defendantId1)
                                                        .build(),
                                                Defendant.defendant()
                                                        .withId(defendantId2)
                                                        .build()))).build()))
                .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToProbationCaseWorker().collect(toList());
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().size(), is(1));
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((HearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(3));
    }

    @Test
    public void shouldUpdateVejHearingWithNewDefendantWhenHearingIsExtended() {
        final UUID hearingId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID caseId1 = randomUUID();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendantId1)
                                                .build(),
                                        Defendant.defendant()
                                                .withId(defendantId2)
                                                .build()))).build()))
                        .build()).build());
        final HearingExtendedProcessed hearingExtendedProcessed = HearingExtendedProcessed.hearingExtendedProcessed()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(caseId1).withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(defendantId3)
                                .build()))).build()))
                        .build())
                .build();
        hearingAggregate.apply(hearingExtendedProcessed);
        final List<Object> objectStream = hearingAggregate.populateHearingToVEP().collect(toList());
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().size(), is(1));
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getId(), is(caseId1));
        assertThat(((VejHearingPopulatedToProbationCaseworker)objectStream.get(0)).getHearing().getProsecutionCases().get(0).getDefendants().size(), is(3));
    }

    @Test
    public void shouldUpdateListingNumbersOfOffencesOfProsecutionCase() {
        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();
        final UUID caseId1 = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withId(caseId1)
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId1)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId2)
                                                        .withListingNumber(11)
                                                        .build())))
                                        .build(),
                                Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offenceId3)
                                                        .build(),
                                                Offence.offence()
                                                        .withId(offenceId4)
                                                        .withListingNumber(13)
                                                        .build())))
                                        .build()
                        )))
                        .build(),
                        ProsecutionCase.prosecutionCase()
                                .withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(randomUUID())
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(randomUUID())
                                                                .withListingNumber(11)
                                                                .build())))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                                .withId(randomUUID())
                                                                .build(),
                                                        Offence.offence()
                                                                .withId(randomUUID())
                                                                .withListingNumber(13)
                                                                .build())))
                                                .build()
                                )))
                                .build()
                )))
                .build();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched().withHearing(hearing).build());
        final List<Object> events = hearingAggregate.updateOffencesWithListingNumber(asList(OffenceListingNumbers.offenceListingNumbers()
                        .withOffenceId(offenceId1).withListingNumber(10).build(),
                OffenceListingNumbers.offenceListingNumbers()
                        .withOffenceId(offenceId3).withListingNumber(12).build()
        )).collect(toList());

        ListingNumberUpdated listingNumberUpdated = (ListingNumberUpdated) events.get(0);

        assertThat(listingNumberUpdated.getHearingId(), is(hearingId));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().size(), is(2));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(0).getOffenceId(), is(offenceId1));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(0).getListingNumber(), is(10));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(1).getOffenceId(), is(offenceId3));
        assertThat(listingNumberUpdated.getOffenceListingNumbers().get(1).getListingNumber(), is(12));
        assertThat(listingNumberUpdated.getProsecutionCaseIds().size(), is(1));
        assertThat(listingNumberUpdated.getProsecutionCaseIds().get(0), is(caseId1));
        assertThat(listingNumberUpdated.getCourtApplicationIds(), is(nullValue()));

    }

    @Test
    public void shouldRaiseUnAllocatedEventWhenStartDateRemoved(){
        final UUID hearingId = randomUUID();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Object> events = hearingAggregate.updateHearing(UpdateHearingFromHmi.updateHearingFromHmi().build()).collect(toList());
        final HearingMovedToUnallocated hearingMovedToUnallocated = (HearingMovedToUnallocated)events.get(0);

        assertThat(hearingMovedToUnallocated.getHearing().getHearingDays(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getJudiciary(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getListingNumber(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getHearingLanguage(), is(HearingLanguage.ENGLISH));
    }

    @Test
    public void shouldRaiseUnAllocatedEventWhenCourtRoomRemoved(){
        final UUID hearingId = randomUUID();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJudiciary(singletonList(JudicialRole.judicialRole().build()))
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Object> events = hearingAggregate.updateHearing(UpdateHearingFromHmi.updateHearingFromHmi()
                .withStartDate(LocalDate.now().toString())
                .build()).collect(toList());
        final HearingMovedToUnallocated hearingMovedToUnallocated = (HearingMovedToUnallocated)events.get(0);

        assertThat(hearingMovedToUnallocated.getHearing().getHearingDays().size(), is(1));
        assertThat(hearingMovedToUnallocated.getHearing().getHearingDays().get(0).getCourtRoomId(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getJudiciary(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getProsecutionCases().get(0).getDefendants().get(0).getOffences().get(0).getListingNumber(), nullValue());
        assertThat(hearingMovedToUnallocated.getHearing().getHearingLanguage(), is(HearingLanguage.ENGLISH));
    }

    @Test
    public void shouldNotRaiseUnAllocatedEventWhenHMINotChanged(){
        final UUID hearingId = randomUUID();

        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(ZonedDateTime.now())
                                .withCourtRoomId(randomUUID()).build()))
                        .withType(HearingType.hearingType().withDescription("Statement").build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("A001").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withProsecutionCases(Lists.newArrayList(ProsecutionCase.prosecutionCase().withId(randomUUID())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(randomUUID())
                                                .withOffences(singletonList(Offence.offence().withListingNumber(1).build()))
                                                .build()))).build()))
                        .build()).build());

        final List<Object> events = hearingAggregate.updateHearing(UpdateHearingFromHmi.updateHearingFromHmi()
                .withStartDate(LocalDate.now().toString())
                .withCourtRoomId(randomUUID())
                .build()).collect(toList());

        assertThat(events.size(), is(0));
    }

    private HearingDeleted createHearingDeleted(final Hearing hearing) {
        return HearingDeleted.hearingDeleted().withHearingId(hearing.getId()).build();
    }

    private HearingResulted createHearingResulted(final Hearing hearing) {
        return HearingResulted.hearingResulted().withHearing(hearing).build();
    }

    private Hearing getHearing(final UUID offenceId){
        return Hearing.hearing()
                .withId(randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(new ArrayList<>(asList(ProsecutionCase.prosecutionCase()
                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                .withId(randomUUID())
                                .withIsYouth(true)
                                .withOffences(of(Offence.offence()
                                        .withId(offenceId)
                                        .withListingNumber(1)
                                        .build()))
                                .build()
                        )))
                        .build()
                )))
                .withCourtApplications(new ArrayList<>(asList(CourtApplication.courtApplication()
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withMasterDefendant(MasterDefendant.masterDefendant().withIsYouth(false).build()).build())
                        .build())))
                .build();
    }


    private HearingDaysWithoutCourtCentreCorrected createHearingDaysWithoutCourtCentreCorrected() {
        return HearingDaysWithoutCourtCentreCorrected.hearingDaysWithoutCourtCentreCorrected()
                .withHearingDays(asList(HearingDay.hearingDay()
                        .withCourtCentreId(UUID.randomUUID())
                        .withCourtRoomId(UUID.randomUUID())
                        .withListedDurationMinutes(30)
                        .withListingSequence(1)
                        .withSittingDay(ZonedDateTime.now())
                        .build()))
                .withId(UUID.randomUUID())
                .build();
    }
}
