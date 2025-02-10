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
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated.sendNotificationForAutoApplicationInitiated;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplication;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplicationWithCustodialEstablisment;
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
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.DeleteCourtApplicationHearingRequested;
import uk.gov.justice.core.courts.EditCourtApplicationProceedings;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.core.courts.SlotsBookedForApplication;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.justice.progression.courts.SendStatdecAppointmentLetter;
import uk.gov.moj.cpp.platform.test.utils.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtApplicationWithCustody;
import uk.gov.moj.cpp.progression.events.CourtApplicationDocumentUpdated;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationAggregateTest {

    private static final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds().build();
    @InjectMocks
    private ApplicationAggregate aggregate;

    @BeforeEach
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

        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication, null)
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationCreated.class)));
    }

    @Test
    public void shouldReturnCourtApplicationCreatedWithoutCustodialEstablishment() {
        final LocalDate convictionDate = LocalDate.now();
        final UUID courtApplicationId = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();
        final CourtApplication courtApplication = buildCourtapplicationWithCustodialEstablisment(courtApplicationId, convictionDate, masterDefendantId);

        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication, null)
                .collect(toList());
        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationCreated.class)));
        assertThat(((CourtApplicationCreated)object).getCourtApplication().getSubject().getMasterDefendant().getPersonDefendant().getCustodialEstablishment(), is(nullValue()));
        assertThat(((CourtApplicationCreated)object).getCourtApplication().getSubject().getMasterDefendant().getMasterDefendantId(), is(courtApplication.getSubject().getMasterDefendant().getMasterDefendantId()));
        assertThat(((CourtApplicationCreated)object).getCourtApplication().getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName(), is(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName()));

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
    public void shouldHearingApplicationLinkCreatedEventIsUpdatedWithApplicationStatus() {
        final UUID applicationId = randomUUID();
        final HearingResultedApplicationUpdated hearingResultedApplicationUpdated = HearingResultedApplicationUpdated.hearingResultedApplicationUpdated().withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withApplicationStatus(ApplicationStatus.FINALISED)
                        .build())
                .build();
        aggregate.apply(Stream.of(hearingResultedApplicationUpdated));

        final Hearing hearing = Hearing.hearing()
                .withCourtApplications(Arrays.asList(
                        courtApplication().withId(applicationId).withApplicationStatus(ApplicationStatus.IN_PROGRESS).build(),
                        courtApplication().withId(randomUUID()).withApplicationStatus(ApplicationStatus.IN_PROGRESS).build()))
                .build();
        final List<Object> eventStream = aggregate.createHearingApplicationLink
                (hearing, applicationId, HearingListingStatus.HEARING_INITIALISED).collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingApplicationLinkCreated hearingApplicationLinkCreated = (HearingApplicationLinkCreated) eventStream.get(0);
        assertThat(hearingApplicationLinkCreated.getHearing().getCourtApplications().stream()
                .filter(c -> c.getId().equals(applicationId))
                .findFirst().get().getApplicationStatus(), is(ApplicationStatus.FINALISED));
        assertThat(hearingApplicationLinkCreated.getHearing().getCourtApplications().stream()
                .filter(c -> !c.getId().equals(applicationId))
                .findFirst().get().getApplicationStatus(), is(ApplicationStatus.IN_PROGRESS));
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
        ReflectionUtil.setField(this.aggregate, "applicationStatus", ApplicationStatus.EJECTED);
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
                .build(), null)
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
                        .withVirtualAppointmentTime(ZonedDateTime.now())
                        .build())
                .withSummonsApprovalRequired(FALSE)
                .build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication, null)
                .collect(toList());
        assertThat(eventStream.size(), is(2));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(CourtApplicationCreated.class)));
        final Object object2 = eventStream.get(1);
        assertThat(object2.getClass(), is(equalTo(SendStatdecAppointmentLetter.class)));
    }

    @Test
    public void shouldReturnCourtApplicationDocumentWhenOldApplicationIsExistsDuringCourtApplicationIsCreated() {
        final LocalDate convictionDate = LocalDate.now();
        final UUID oldApplicationId = randomUUID();
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, convictionDate);

        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication, oldApplicationId)
                .collect(toList());
        assertThat(eventStream.size(), is(2));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CourtApplicationCreated.class)));

        final CourtApplicationDocumentUpdated courtApplicationDocumentUpdated = (CourtApplicationDocumentUpdated) eventStream.get(1);
        assertThat(courtApplicationDocumentUpdated.getApplicationId(), is(applicationId));
        assertThat(courtApplicationDocumentUpdated.getOldApplicationId(), is(oldApplicationId));
    }

    @Test
    public void shouldReturnCourtApplicationCreatedButNoSendStatDecAppointmentLetterWhenNonVirtualHearing() {
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

        final List<Object> eventStream = aggregate.createCourtApplication(courtApplication, null)
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object1 = eventStream.get(0);
        assertThat(object1.getClass(), is(equalTo(CourtApplicationCreated.class)));
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
        aggregate.apply(Stream.of(ApplicationReferredToBoxwork.applicationReferredToBoxwork().build()));
        final List<Object> eventStream = aggregate.hearingResulted(buildCourtapplication(randomUUID(), LocalDate.now())).collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(ApplicationStatus.IN_PROGRESS));
    }

    @Test
    public void shouldHearingResultedWithFinalised() {
        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(buildCourtapplication(randomUUID(), LocalDate.now()))
                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build()))
                        .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(ApplicationStatus.FINALISED));
    }

    @Test
    public void shouldHearingResultedWithInProgress() {
        aggregate.apply(Stream.of(ApplicationReferredToBoxwork.applicationReferredToBoxwork().build()));
        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(buildCourtapplication(randomUUID(), LocalDate.now()))
                        .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                .withCategory(JudicialResultCategory.ANCILLARY)
                                .build()))
                        .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(ApplicationStatus.IN_PROGRESS));
    }

    @Test
    public void shouldHearingResultedUseOldJudicialResultWhenHearingAlreadyFinalisedAndJudicialResultIsEmpty() {
        final List<JudicialResult> judicialResults = Arrays.asList(JudicialResult.judicialResult()
                .withCategory(JudicialResultCategory.FINAL)
                .withJudicialResultId(randomUUID())
                .build());
        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(buildCourtapplication(randomUUID(), LocalDate.now()))
                        .withJudicialResults(judicialResults)
                        .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(ApplicationStatus.FINALISED));
        aggregate.apply(eventStream);

        final CourtApplication courtApplication = buildCourtapplication(randomUUID(), LocalDate.now());
        final List<Object> eventStream2 = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withApplicationStatus(ApplicationStatus.FINALISED)
                        .withJudicialResults(null)
                        .build())
                .collect(toList());
        assertThat(eventStream2.size(), is(1));

        final HearingResultedApplicationUpdated secondEvent = (HearingResultedApplicationUpdated) eventStream2.get(0);
        final CourtApplication expectedCourtApplication = courtApplication().withValuesFrom(courtApplication)
                .withApplicationStatus(ApplicationStatus.FINALISED)
                .withJudicialResults(judicialResults)
                .build();
        assertThat(secondEvent.getCourtApplication(), is(expectedCourtApplication));

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
    public void shouldReferApplicationToBoxWorkWhenCourtApplicationCasesIsNotNull() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, null, true, false);
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
    public void shouldReferApplicationToBoxWorkWhenCourtApplicationCasesIsNullButCourtOrderIsNotNull() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, offenceId, LocalDate.now(), false, true);
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
    public void shouldIgnoreReferApplicationToBoxWorkWhenCourtApplicationCasesAndCourtOrderIsNull() {
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        CourtApplication courtApplication = buildCourtapplicationWithOffenceUnderCase(courtApplicationId, null, null, false, false);
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withBoxHearing(BoxHearingRequest.boxHearingRequest()
                        .withSendAppointmentLetter(TRUE)
                        .build())
                .withSummonsApprovalRequired(false).build();
        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        aggregate.addConvictionDate(courtApplicationId, offenceId, convictionDate).collect(toList());

        final List<Object> eventStream = aggregate.referApplication().collect(toList());
        assertThat(eventStream.size(), is(0));
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
    public void shoulRecordEmailRequest() {
        final Map<String, String> personalisation = new HashMap<>();
        final List<Notification> notifications = new ArrayList<>();
        notifications.add(new Notification(randomUUID(), randomUUID(), "sendToAddress", "replyToAddress", personalisation, "material URL"));
        final List<Object> eventStream = aggregate.recordEmailRequest(randomUUID(), randomUUID(), notifications).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(EmailRequested.class)));
    }

    @Test
    public void shouldRaiseIgnoreEventWhenApplicationNotExist() {
        final SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated = SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                .build();

        final List<Object> eventStream = aggregate.sendNotificationForApplication(sendNotificationForApplicationInitiated).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass().getName(), is("uk.gov.justice.core.courts.SendNotificationForApplicationIgnored"));

    }

    @Test
    public void shouldRaiseIgnoreEventWhenApplicationBoxWorkRequested() {
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
    public void shouldRaiseNotificationEvent() {
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
        final SendNotificationForApplicationInitiated event = (SendNotificationForApplicationInitiated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(initiateCourtApplicationProceedings.getCourtApplication().getId()));

    }

    @Test
    public void shouldRaiseAutoNotificationEvent(){

        final UUID applicationId = randomUUID();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(applicationId)
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        final SendNotificationForAutoApplicationInitiated sendNotificationForAutoApplicationInitiated =
                sendNotificationForAutoApplicationInitiated()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("COURTCENTER").build())
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingStartDateTime("2020-06-26T07:51Z")
                        .build();

        final List<Object> eventStream = aggregate.sendNotificationForAutoApplication(sendNotificationForAutoApplicationInitiated.getCourtApplication(),
                sendNotificationForAutoApplicationInitiated.getCourtCentre(), sendNotificationForAutoApplicationInitiated.getJurisdictionType()
        , sendNotificationForAutoApplicationInitiated.getHearingStartDateTime()).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass().getName(), is("uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated"));
        final SendNotificationForAutoApplicationInitiated event =  (SendNotificationForAutoApplicationInitiated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(initiateCourtApplicationProceedings.getCourtApplication().getId()));
    }

    @Test
    public void shouldRaiseAutoNotificationEventForAmend(){

        final UUID applicationId = randomUUID();
        final LocalDate orderedDate = LocalDate.now();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(applicationId)
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .withIsAmended(true)
                .withIssueDate(orderedDate)
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        final SendNotificationForAutoApplicationInitiated sendNotificationForAutoApplicationInitiated =
                sendNotificationForAutoApplicationInitiated()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtCentre(CourtCentre.courtCentre().withCode("COURTCENTER").build())
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingStartDateTime("2020-06-26T07:51Z")
                        .withIssueDate(orderedDate)
                        .build();

        final List<Object> eventStream = aggregate.sendNotificationForAutoApplication(sendNotificationForAutoApplicationInitiated.getCourtApplication(),
                sendNotificationForAutoApplicationInitiated.getCourtCentre(), sendNotificationForAutoApplicationInitiated.getJurisdictionType()
                , sendNotificationForAutoApplicationInitiated.getHearingStartDateTime()).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass().getName(), is("uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated"));
        final SendNotificationForAutoApplicationInitiated event =  (SendNotificationForAutoApplicationInitiated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(initiateCourtApplicationProceedings.getCourtApplication().getId()));
        assertThat(event.getIsAmended(), is(initiateCourtApplicationProceedings.getIsAmended()));
        assertThat(event.getIssueDate(), is(orderedDate));

    }

    @Test
    public void shouldDeleteCourtApplication() {
        final UUID courtApplicationId = randomUUID();
        final UUID hearingId = randomUUID();
        InitiateCourtApplicationProceedings initiateCourtProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(courtApplicationId).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().withId(hearingId).build())
                .withSummonsApprovalRequired(false)
                .build();
        final List<Object> eventStream = aggregate.initiateCourtApplicationProceedings(initiateCourtProceedings, false, false)
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = (CourtApplicationProceedingsInitiated) eventStream.get(0);
        assertThat(courtApplicationProceedingsInitiated.getClass(), is(equalTo(CourtApplicationProceedingsInitiated.class)));
        aggregate.apply(courtApplicationProceedingsInitiated);

        final UUID seedingHearingId = randomUUID();
        final List<Object> deleteCourtApplicationEventStream = aggregate.deleteCourtApplication(courtApplicationId, seedingHearingId).collect(toList());

        assertThat(deleteCourtApplicationEventStream.size(), is(1));
        final DeleteCourtApplicationHearingRequested event = (DeleteCourtApplicationHearingRequested) deleteCourtApplicationEventStream.get(0);
        assertThat(event.getApplicationId(), is(courtApplicationId));
        assertThat(event.getSeedingHearingId(), is(seedingHearingId));
        assertThat(event.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldRaiseCourtApplicationUpdated(){

        final LocalDate convictionDate = LocalDate.now();
        final UUID courtApplicationId = randomUUID();
        final UUID offenceId = randomUUID();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(courtApplicationId)
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        final SendNotificationForApplicationInitiated sendNotificationForApplicationInitiated = SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated()
                .build();

        CourtApplication courtApplication1 = buildCourtApplicationWithCustody(courtApplicationId, offenceId, convictionDate, "custody2");

        final List<Object> eventStream = aggregate.updateApplicationDefendant(courtApplication1).collect(toList());
        assertThat(eventStream.size(), is(1));
        assertThat(eventStream.get(0).getClass().getName(), is("uk.gov.justice.core.courts.CourtApplicationUpdated"));
        final CourtApplicationUpdated event =  (CourtApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(courtApplicationId));
        assertThat(aggregate.getCourtApplication().getSubject().getMasterDefendant().getPersonDefendant().getCustodialEstablishment().getCustody(), is("custody2"));
    }


}
