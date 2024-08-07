package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplication;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplicationWithOffenceUnderCase;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplicationWithOffenceUnderCourtOrder;

import uk.gov.justice.core.courts.ApplicationEjected;
import uk.gov.justice.core.courts.ApplicationReferredIgnored;
import uk.gov.justice.core.courts.ApplicationReferredToBoxwork;
import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.ApplicationReferredToCourtHearing;
import uk.gov.justice.core.courts.ApplicationReferredToExistingHearing;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.ConvictionDateAdded;
import uk.gov.justice.core.courts.ConvictionDateRemoved;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiateIgnored;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.EditCourtApplicationProceedings;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SlotsBookedForApplication;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.justice.progression.courts.SendStatdecAppointmentLetter;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationAggregateTest {

    private static final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds().build();
    @InjectMocks
    private ApplicationAggregate aggregate;

    @Before
    public void setUp() {
        aggregate = new ApplicationAggregate();
    }

    @Test
    public void shouldReturnCasesReferredToCourt() {
        final List<Object> eventStream = aggregate.referApplicationToCourt(hearingListingNeeds).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ApplicationReferredToCourt.class)));
    }

    @Test
    public void shouldReturnApplicationStatusChanged() {
        final List<Object> eventStream = aggregate.updateApplicationStatus(randomUUID(), ApplicationStatus.LISTED).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationStatusChanged.class)));
    }

    @Test
    public void shouldReturnCourtApplicationCreated() {
        final LocalDate convictionDate = LocalDate.now();
        final CourtApplication courtApplication = buildCourtapplication(randomUUID(), convictionDate);

        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication)
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationCreated.class)));
    }

    @Test
    public void shouldReturnAddCourtApplicationCase() {
        final List<Object> eventStream = aggregate.addApplicationToCase(courtApplication()
                .withId(randomUUID())
                .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationAddedToCase.class)));
    }

    @Test
    public void shouldReturnHearingApplicationLinkCreated() {
        final List<Object> eventStream = aggregate.createHearingApplicationLink
                (Hearing.hearing().build(), randomUUID(), HearingListingStatus.HEARING_INITIALISED).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingApplicationLinkCreated.class)));
    }

    @Test
    public void shouldReturnApplicationEjected() {
        final List<Object> eventStream = aggregate.ejectApplication(randomUUID(), "Legal").collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ApplicationEjected.class)));
    }

    @Test
    public void shouldNotReturnApplicationEjected() {
        Whitebox.setInternalState(this.aggregate, "applicationStatus", ApplicationStatus.EJECTED);
        final List<Object> eventStream = aggregate.ejectApplication(randomUUID(), "Legal").collect(toList());
        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldReturnCourtApplicationCreatedForCourtProceedings() {
        final List<Object> eventStream = aggregate.initiateCourtApplicationProceedings(InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(randomUUID()).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build(), false, false)
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = (CourtApplicationProceedingsInitiated) eventStream.get(0);
        assertThat(courtApplicationProceedingsInitiated.getClass(), is(equalTo(CourtApplicationProceedingsInitiated.class)));
    }

    @Test
    public void testCourtApplicationCreatedForCourtProceedingsIsIdempotent() {
        InitiateCourtApplicationProceedings initiateCourtProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(randomUUID()).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();
        final List<Object> eventStream = aggregate.initiateCourtApplicationProceedings(initiateCourtProceedings, false, false)
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = (CourtApplicationProceedingsInitiated) eventStream.get(0);
        assertThat(courtApplicationProceedingsInitiated.getClass(), is(equalTo(CourtApplicationProceedingsInitiated.class)));
        aggregate.apply(courtApplicationProceedingsInitiated);
        final List<Object> eventStream2 = aggregate.initiateCourtApplicationProceedings(initiateCourtProceedings, false, false)
                .collect(toList());
        assertThat(eventStream2.size(), is(1));
        final CourtApplicationProceedingsInitiateIgnored courtApplicationProceedingsInitiateIgnored = (CourtApplicationProceedingsInitiateIgnored) eventStream2.get(0);
        assertThat(courtApplicationProceedingsInitiateIgnored.getClass(), is(equalTo(CourtApplicationProceedingsInitiateIgnored.class)));



    }

    @Test
    public void shouldReturnCourtApplicationCreatedForCourtProceedingsWithSjpCase() {
        final List<Object> eventStream = aggregate.initiateCourtApplicationProceedings(InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(randomUUID())
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build(), true, false)
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = (CourtApplicationProceedingsInitiated) eventStream.get(0);
        assertThat(courtApplicationProceedingsInitiated.getClass(), is(equalTo(CourtApplicationProceedingsInitiated.class)));
    }

    @Test
    public void shouldReturnCourtApplicationEditedForCourtProceedings() {
        final List<Object> eventStream = aggregate.editCourtApplicationProceedings(EditCourtApplicationProceedings
                .editCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(randomUUID()).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationProceedingsEdited.class)));
    }

    @Test
    public void shouldReturnAddConvictionDateEventAndUpdateOffenceUnderCourtApplicationCase() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        assertThat(aggregate.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate(), is(nullValue()));


        final List<Object> eventStream = aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());
        assertThat(eventStream.size(), is(1));
        ConvictionDateAdded convictionDateAdded = (ConvictionDateAdded) eventStream.get(0);

        assertThat(convictionDateAdded.getConvictionDate(), is(convictionDate));
        assertThat(convictionDateAdded.getCourtApplicationId(), is(courtApplicationId));
        assertThat(convictionDateAdded.getOffenceId(), is(offenceId));

        assertThat(aggregate.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate(), is(convictionDate));
    }

    @Test
    public void shouldReturnRemovedConvictionDateEventAndUpdateOffenceUnderCourtApplicationCase() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, convictionDate);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        assertThat(aggregate.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate(), is(notNullValue()));


        final List<Object> eventStream = aggregate.removeConvictionDate(courtApplicationId, offenceId).collect(toList());
        assertThat(eventStream.size(), is(1));
        ConvictionDateRemoved convictionDateRemoved = (ConvictionDateRemoved) eventStream.get(0);

        assertThat(convictionDateRemoved.getCourtApplicationId(), is(courtApplicationId));
        assertThat(convictionDateRemoved.getOffenceId(), is(offenceId));

        assertThat(aggregate.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getConvictionDate(), is(nullValue()));
    }

    @Test
    public void shouldReturnAddConvictionDateEventAndUpdateOffenceUnderCourtApplicationCourtOrder() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCourtOrder(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        assertThat(aggregate.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate(), is(nullValue()));


        final List<Object> eventStream = aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());
        assertThat(eventStream.size(), is(1));
        ConvictionDateAdded convictionDateAdded = (ConvictionDateAdded) eventStream.get(0);

        assertThat(convictionDateAdded.getConvictionDate(), is(convictionDate));
        assertThat(convictionDateAdded.getCourtApplicationId(), is(courtApplicationId));
        assertThat(convictionDateAdded.getOffenceId(), is(offenceId));

        assertThat(aggregate.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate(), is(convictionDate));
    }

    @Test
    public void shouldReturnRemovedConvictionDateEventAndUpdateOffenceUnderCourtApplicationCourtOrder() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCourtOrder(courtApplicationId, offenceId, convictionDate);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        assertThat(aggregate.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate(), is(notNullValue()));


        final List<Object> eventStream = aggregate.removeConvictionDate(courtApplicationId, offenceId).collect(toList());
        assertThat(eventStream.size(), is(1));
        ConvictionDateRemoved convictionDateRemoved = (ConvictionDateRemoved) eventStream.get(0);

        assertThat(convictionDateRemoved.getCourtApplicationId(), is(courtApplicationId));
        assertThat(convictionDateRemoved.getOffenceId(), is(offenceId));

        assertThat(aggregate.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getOffence().getConvictionDate(), is(nullValue()));
    }

    @Test
    public void shouldReturnAddConvictionDateEventAndUpdateCourtApplication() {
        final UUID courtApplicationId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplication(courtApplicationId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        assertThat(aggregate.getCourtApplication().getConvictionDate(), is(nullValue()));


        final List<Object> eventStream = aggregate.addConvictionDate(courtApplicationId, null, convictionDate).collect(toList());
        assertThat(eventStream.size(), is(1));
        ConvictionDateAdded convictionDateAdded = (ConvictionDateAdded) eventStream.get(0);

        assertThat(convictionDateAdded.getConvictionDate(), is(convictionDate));
        assertThat(convictionDateAdded.getCourtApplicationId(), is(courtApplicationId));

        assertThat(aggregate.getCourtApplication().getConvictionDate(), is(convictionDate));
    }

    @Test
    public void shouldReturnRemovedConvictionDateEventAndUpdateCourtApplication() {
        final UUID courtApplicationId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplication(courtApplicationId, convictionDate);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        assertThat(aggregate.getCourtApplication().getConvictionDate(), is(notNullValue()));


        final List<Object> eventStream = aggregate.removeConvictionDate(courtApplicationId, null).collect(toList());
        assertThat(eventStream.size(), is(1));
        ConvictionDateRemoved convictionDateRemoved = (ConvictionDateRemoved) eventStream.get(0);

        assertThat(convictionDateRemoved.getCourtApplicationId(), is(courtApplicationId));

        assertThat(aggregate.getCourtApplication().getConvictionDate(), is(nullValue()));
    }

    @Test
    public void shouldReturnCourtApplicationCreatedAndSendStatDecAppointmentLetter() {
        final UUID courtApplicationId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();
        final CourtApplication courtApplication = buildCourtapplication(courtApplicationId, convictionDate);

        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication)
                .withBoxHearing(BoxHearingRequest.boxHearingRequest()
                        .withSendAppointmentLetter(TRUE)
                        .build())
                .withSummonsApprovalRequired(FALSE)
                .build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication)
                .collect(toList());
        assertThat(eventStream.size(), is(2));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(CourtApplicationCreated.class)));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(SendStatdecAppointmentLetter.class)));
    }

    @Test
    public void shouldRaiseHearingDeletedForCourtApplication() {

        final UUID hearingId = randomUUID();
        final UUID courtApplicationId = randomUUID();

        final List<Object> eventStream = aggregate.deleteHearingRelatedToCourtApplication(hearingId, courtApplicationId).collect(toList());

        assertThat(eventStream.size(), is(1));

        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingDeletedForCourtApplication.class)));

        final HearingDeletedForCourtApplication hearingDeletedForCourtApplication = (HearingDeletedForCourtApplication) object;
        assertThat(hearingDeletedForCourtApplication.getHearingId(), is(equalTo(hearingId)));
        assertThat(hearingDeletedForCourtApplication.getCourtApplicationId(), is(equalTo(courtApplicationId)));
    }

    @Test
    public void shouldExtendHearing() {
        final List<Object> eventStream = aggregate.extendHearing(hearingListingNeeds).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingExtended.class)));
    }

    @Test
    public void shouldBookSlotsForApplication() {
        final List<Object> eventStream = aggregate.bookSlotsForApplication(hearingListingNeeds).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(SlotsBookedForApplication.class)));
    }

    @Test
    public void shouldReferApplicationToCourtHearing() {
       aggregate.initiateCourtApplicationProceedings(InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(randomUUID()).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build(), false, false)
                .collect(toList());
        final List<Object> eventStream = aggregate.referApplicationToCourtHearing().collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ApplicationReferredToCourtHearing.class)));
    }

    @Test
    public void shouldHearingResulted() {
        final List<Object> eventStream = aggregate.hearingResulted(buildCourtapplication(randomUUID(), LocalDate.now())).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(HearingResultedApplicationUpdated.class)));
    }

    @Test
    public void shouldIgnoreApplicationReferred() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());

        final List<Object> eventStream = aggregate.referApplication().collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ApplicationReferredIgnored.class)));
    }

    @Test
    public void shouldIgnoreApplicationReferredAndCourtApplicationAddedToCase() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCourtOrder(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withCourtApplication(courtApplication).withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());

        final List<Object> eventStream = aggregate.referApplication().collect(toList());
        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationAddedToCase.class)));
    }

    @Test
    public void shouldReferApplicationToExistingHearing() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest()
                        .withId(randomUUID())
                        .build())
                .withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());

        final List<Object> eventStream = aggregate.referApplication().collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ApplicationReferredToExistingHearing.class)));
    }

    @Test
    public void shouldRejectSummons() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest()
                        .withId(randomUUID())
                        .build())
                .withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());

        final List<Object> eventStream = aggregate.rejectSummons(SummonsRejectedOutcome.summonsRejectedOutcome()
                .build()).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationSummonsRejected.class)));
    }

    @Test
    public void shouldReferApplicationToBoxWork() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withBoxHearing(BoxHearingRequest.boxHearingRequest()
                        .withSendAppointmentLetter(TRUE)
                        .build())
                .withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());

        final List<Object> eventStream = aggregate.referApplication().collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ApplicationReferredToBoxwork.class)));
    }

    @Test
    public void shouldReferApplicationToHearing() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());

        final List<Object> eventStream = aggregate.referApplication().collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(ApplicationReferredToCourtHearing.class)));
    }

    @Test
    public void shoulRecordEmailRequest(){
        final Map<String, String> personalisation = new HashMap<>();
        final List<Notification> notifications = new ArrayList<>();
        notifications.add(new Notification(randomUUID(),randomUUID(),"sendToAddress","replyToAddress", personalisation,"material URL"));
        final List<Object> eventStream = aggregate.recordEmailRequest(randomUUID(),randomUUID(), notifications).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(EmailRequested.class)));
    }

    @Test
    public void shouldRaiseIgnoreEventWhenApplicationNotExist(){
        final SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated = SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                .build();

        final List<Object> eventStream = aggregate.sendNotificationForApplication(sendNotificationForApplicationInitiated).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass().getName(), is("uk.gov.justice.core.courts.SendNotificationForApplicationIgnored"));

    }

    @Test
    public void shouldRaiseIgnoreEventWhenApplicationBoxWorkRequested(){
        aggregate.initiateCourtApplicationProceedings(InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(randomUUID())
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build(), true, false);


        final SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated = SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                .withIsBoxWorkRequest(true)
                .build();

        final List<Object> eventStream = aggregate.sendNotificationForApplication(sendNotificationForApplicationInitiated).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass().getName(), is("uk.gov.justice.core.courts.SendNotificationForApplicationIgnored"));
    }

    @Test
    public void shouldRaiseNotificationEvent(){
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(randomUUID())
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        final SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated = SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                .build();

        final List<Object> eventStream = aggregate.sendNotificationForApplication(sendNotificationForApplicationInitiated).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass().getName(), is("uk.gov.justice.core.courts.SendNotificationForApplicationInitiated"));
        final SendNotificationForApplicationInitiated event =  (SendNotificationForApplicationInitiated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(initiateCourtApplicationProceedings.getCourtApplication().getId()));

    }

}
