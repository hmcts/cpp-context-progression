package uk.gov.moj.cpp.progression.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static uk.gov.justice.core.courts.ApplicationStatus.FINALISED;
import static uk.gov.justice.core.courts.ApplicationStatus.LISTED;
import static uk.gov.justice.core.courts.ApplicationStatus.UN_ALLOCATED;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.DefendantCase.defendantCase;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultCategory.INTERMEDIARY;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated.sendNotificationForAutoApplicationInitiated;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.test.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtApplicationWithCustody;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplication;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplicationWithCustodialEstablisment;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplicationWithOffenceUnderCase;
import static uk.gov.moj.cpp.progression.test.TestHelper.buildCourtapplicationWithOffenceUnderCourtOrder;

import uk.gov.justice.core.courts.ApplicationDefenceOrganisationChanged;
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
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiateIgnored;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationStatusUpdated;
import uk.gov.justice.core.courts.CourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefendantTrialRecordSheetRequestedForApplication;
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
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated;
import uk.gov.justice.core.courts.SlotsBookedForApplication;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.justice.progression.courts.SendStatdecAppointmentLetter;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.platform.test.utils.reflection.ReflectionUtil;
import uk.gov.moj.cpp.progression.application.ApplicationCaseDefendantOrganisation;
import uk.gov.moj.cpp.progression.domain.Notification;
import uk.gov.moj.cpp.progression.domain.event.email.EmailRequested;
import uk.gov.moj.cpp.progression.domain.pojo.OrganisationDetails;
import uk.gov.moj.cpp.progression.events.CourtApplicationDocumentUpdated;
import uk.gov.moj.cpp.progression.events.DefenceOrganisationDissociatedForApplicationByDefenceContext;
import uk.gov.moj.cpp.progression.events.DefendantDefenceOrganisationAssociated;
import uk.gov.moj.cpp.progression.laa.LaaRepresentationOrder;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationAggregateTest {

    private static final HearingListingNeeds hearingListingNeeds = HearingListingNeeds.hearingListingNeeds().build();
    @InjectMocks
    private ApplicationAggregate aggregate;

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

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
        assertThat(((CourtApplicationCreated) object).getCourtApplication().getSubject().getMasterDefendant().getPersonDefendant().getCustodialEstablishment(), is(nullValue()));
        assertThat(((CourtApplicationCreated) object).getCourtApplication().getSubject().getMasterDefendant().getMasterDefendantId(), is(courtApplication.getSubject().getMasterDefendant().getMasterDefendantId()));
        assertThat(((CourtApplicationCreated) object).getCourtApplication().getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName(), is(courtApplication.getSubject().getMasterDefendant().getPersonDefendant().getPersonDetails().getFirstName()));

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
    void shouldHearingApplicationLinkCreatedEventIsUpdatedWithApplicationStatus() {
        final UUID applicationId = randomUUID();
        final HearingResultedApplicationUpdated hearingResultedApplicationUpdated = HearingResultedApplicationUpdated.hearingResultedApplicationUpdated().withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withApplicationStatus(FINALISED)
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
                .findFirst().get().getApplicationStatus(), is(FINALISED));
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
    void testCourtApplicationForRepOrder() {
        final UUID applicationId = UUID.randomUUID();
        final UUID subjectId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID organisationId = UUID.randomUUID();

        final UUID offenceId1 = UUID.randomUUID();
        final UUID caseId1 = UUID.randomUUID();
        final UUID defendantId1 = UUID.randomUUID();

        final String APPLICATION_REFERENCE = "APP00001";
        final String LAA_CONTRACT_NUMBER = "LAA1234";
        final String INCORPORATION_NUMBER = "LAAINC1";
        final String ORG_NAME = "Test1";
        final String ORG_ADDRESS1 = "Address1";
        final String STATUS_CODE = "GR";
        final String STATUS = "Refused";
        final String STATUS_DESCRIPTION = "Granted Description";
        final UUID STATUS_ID = randomUUID();
        final LocalDate statusDate = LocalDate.now();

        final List<DefendantCase> defendantCases = new ArrayList<>();
        defendantCases.add(defendantCase()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .build());
        defendantCases.add(defendantCase()
                .withCaseId(caseId1)
                .withDefendantId(defendantId1)
                .build());

        final List<ApplicationCaseDefendantOrganisation> applicationCaseDefendantOrganisationList = new ArrayList<>();
        applicationCaseDefendantOrganisationList.add(ApplicationCaseDefendantOrganisation.applicationCaseDefendantOrganisation()
                .withCaseId(caseId)
                .withDefendantId(defendantId)
                .withOrganisationId(organisationId)
                .build());

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = CourtApplicationProceedingsInitiated
                .courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication().withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withId(subjectId)
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withDefendantCase(defendantCases)
                                        .build())
                                .build())
                        .withCourtApplicationCases(List.of(courtApplicationCase()
                                .withProsecutionCaseId(caseId)
                                .withOffences(singletonList(Offence.offence()
                                        .withId(offenceId)
                                        .build()))
                                .build(), courtApplicationCase()
                                .withProsecutionCaseId(caseId1)
                                .withOffences(singletonList(Offence.offence()
                                        .withId(offenceId1)
                                        .build()))
                                .build()))
                        .build())
                .build();
        aggregate.apply(courtApplicationProceedingsInitiated);
        final UUID orgId = UUID.randomUUID();
        final OrganisationDetails organisationDetails = OrganisationDetails.newBuilder()
                .withId(orgId)
                .withAddressLine1(ORG_ADDRESS1)
                .withName(ORG_NAME)
                .build();

        LaaReference laaReference = LaaReference.laaReference()
                .withStatusCode(STATUS_CODE)
                .withStatusId(STATUS_ID)
                .withStatusDate(statusDate)
                .withStatusDescription(STATUS_DESCRIPTION)
                .withApplicationReference(APPLICATION_REFERENCE)
                .withOffenceLevelStatus(STATUS)
                .withLaaContractNumber(LAA_CONTRACT_NUMBER)
                .withEffectiveStartDate(statusDate)
                .build();

        LaaRepresentationOrder laaRepresentationOrder = LaaRepresentationOrder.laaRepresentationOrder()
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber(LAA_CONTRACT_NUMBER)
                        .withOrganisation(Organisation.organisation()
                                .withName(ORG_NAME)
                                .withIncorporationNumber(INCORPORATION_NUMBER)
                                .build())
                        .build())
                .withLaaReference(laaReference)
                .build();

        List<Object> eventStream2 = aggregate.receiveRepresentationOrderForApplication(applicationId, subjectId, offenceId, laaRepresentationOrder, organisationDetails, applicationCaseDefendantOrganisationList).toList();
        assertThat(eventStream2.size(), is(3));

        eventStream2 = aggregate.receiveRepresentationOrderForApplication(applicationId, subjectId, offenceId, laaRepresentationOrder, organisationDetails, applicationCaseDefendantOrganisationList).toList();
        assertThat(eventStream2.size(), is(1));

        laaReference = LaaReference.laaReference()
                .withStatusCode(STATUS_CODE)
                .withStatusId(STATUS_ID)
                .withStatusDate(statusDate)
                .withStatusDescription("STATUS_DESCRIPTION")
                .withApplicationReference(APPLICATION_REFERENCE)
                .withOffenceLevelStatus(STATUS)
                .withEffectiveStartDate(statusDate)
                .withLaaContractNumber(LAA_CONTRACT_NUMBER)
                .build();

        laaRepresentationOrder = LaaRepresentationOrder.laaRepresentationOrder()
                .withValuesFrom(laaRepresentationOrder)
                .withLaaReference(laaReference)
                .build();

        eventStream2 = aggregate.receiveRepresentationOrderForApplication(applicationId, subjectId, offenceId, laaRepresentationOrder, organisationDetails, applicationCaseDefendantOrganisationList).toList();
        assertThat(eventStream2.size(), is(3));
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
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());
        aggregate.apply(Stream.of(ApplicationReferredToBoxwork.applicationReferredToBoxwork().build()));

        final List<Object> eventStream = aggregate.hearingResulted(buildCourtapplication(applicationId, LocalDate.now())).toList();

        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(LISTED));
    }

    @Test
    public void givenApplicationWasPreviouslyResultedFinalised_whenResultedInSubsequentHearing_shouldHaveNewStatus() {
        final UUID applicationId = randomUUID();

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(buildCourtapplication(applicationId, LocalDate.now())).build());
        aggregate.apply(Stream.of(ApplicationReferredToBoxwork.applicationReferredToBoxwork().build()));
        aggregate.apply(Stream.of(new HearingResultedApplicationUpdated(buildCourtapplication(applicationId, LocalDate.now(), FINALISED))));

        final CourtApplication courtApplication2 = buildCourtapplication(applicationId, LocalDate.now(), List.of(judicialResult()
                .withJudicialResultId(randomUUID())
                .withCategory(INTERMEDIARY).build()));
        final List<Object> eventStream = aggregate.hearingResulted(courtApplication2).toList();

        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(LISTED));
    }

    @Test
    public void givenApplicationWasPreviouslyResultedAndHasStatusFinalised_whenResultedInSubsequentHearing_shouldHaveNewStatus() {
        final UUID applicationId = randomUUID();

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(buildCourtapplication(applicationId, LocalDate.now())).build());
        aggregate.apply(Stream.of(ApplicationReferredToBoxwork.applicationReferredToBoxwork().build()));
        aggregate.apply(Stream.of(new HearingResultedApplicationUpdated(buildCourtapplication(applicationId, LocalDate.now(), FINALISED))));

        final CourtApplication courtApplication2 = buildCourtapplication(applicationId, LocalDate.now(), singletonList(judicialResult()
                .withCategory(INTERMEDIARY)
                .build()));
        final List<Object> eventStream = aggregate.hearingResulted(courtApplication2).toList();

        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(LISTED));
    }

    @Test
    void givenHearingResultedWithFinalResultsShouldChangeApplicationStatusToFinalised() {
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(buildCourtapplication(applicationId, LocalDate.now()))
                        .withJudicialResults(singletonList(judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build()))
                        .build())
                .toList();
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(FINALISED));
    }

    @Test
    void givenApplicationStatusResultedWithFinalResultShouldHaveFinalisedStatus() {
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withJudicialResults(Arrays.asList(judicialResult()
                                .withCategory(INTERMEDIARY)
                                .build(), judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build()))
                        .build())
                .toList();

        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(FINALISED));
    }

    @Test
    void givenApplicationStatusAlreadyFinalisedAndHearingResultedWithFinalShouldHaveFinalisedStatus() {
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());
        aggregate.apply(new HearingResultedApplicationUpdated(courtApplication()
                .withValuesFrom(courtApplication)
                .withApplicationStatus(FINALISED)
                .withJudicialResults(singletonList(judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build())).build()));

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withJudicialResults(Arrays.asList(judicialResult()
                                .withCategory(INTERMEDIARY)
                                .build(), judicialResult()
                                .withCategory(JudicialResultCategory.FINAL)
                                .build()))
                        .build())
                .toList();

        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(FINALISED));
    }

    @Test
    void givenApplicationStatusAlreadyFinalisedAndHearingResultedWithNoJudicialResultRetainStatusOfApplication() {
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());
        aggregate.apply(new HearingResultedApplicationUpdated(courtApplication()
                .withValuesFrom(courtApplication)
                .withApplicationStatus(FINALISED)
                .withJudicialResults(singletonList(judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build())).build()));

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withApplicationStatus(FINALISED)
                        .withJudicialResults(emptyList())
                        .build())
                .toList();

        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(FINALISED));
    }

    @Test
    void givenApplicationStatusAlreadyFinalisedAndApplicationAmendedWithNonFinalJudicialResultsAndHearingResultedShouldRetainApplicationStatusOnRequest() {
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());
        aggregate.apply(new HearingResultedApplicationUpdated(courtApplication()
                .withValuesFrom(courtApplication)
                .withApplicationStatus(FINALISED)
                .withJudicialResults(singletonList(judicialResult()
                        .withCategory(JudicialResultCategory.FINAL)
                        .build())).build()));

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withApplicationStatus(LISTED)
                        .withJudicialResults(singletonList(judicialResult()
                                .withCategory(INTERMEDIARY)
                                .build()))
                        .build())
                .toList();

        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(LISTED));
    }

    @Test
    void shouldHearingResultedWithInProgress() {
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());
        aggregate.apply(Stream.of(ApplicationReferredToBoxwork.applicationReferredToBoxwork().build()));

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withJudicialResults(singletonList(JudicialResult.judicialResult()
                                .withCategory(JudicialResultCategory.ANCILLARY)
                                .build()))
                        .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(LISTED));
    }

    @Test
    void givenApplicationResultedAncillaryResults_shouldHaveApplicationStatusListed() {
        final UUID applicationId = randomUUID();
        final CourtApplication courtApplication = buildCourtapplication(applicationId, LocalDate.now());

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());
        aggregate.apply(Stream.of(ApplicationReferredToBoxwork.applicationReferredToBoxwork().build()));

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withJudicialResults(singletonList(judicialResult()
                                .withCategory(JudicialResultCategory.ANCILLARY)
                                .build()))
                        .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(LISTED));
    }

    @Test
    public void shouldHearingResultedUseOldJudicialResultWhenHearingAlreadyFinalisedAndJudicialResultIsEmpty() {
        final List<JudicialResult> judicialResults = Arrays.asList(judicialResult()
                .withCategory(JudicialResultCategory.FINAL)
                .withJudicialResultId(randomUUID())
                .build());

        final CourtApplication courtApplication = buildCourtapplication(randomUUID(), LocalDate.now(), judicialResults);
        InitiateCourtApplicationProceedings initiateCourtProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();
        aggregate.apply(aggregate.initiateCourtApplicationProceedings(initiateCourtProceedings, false, false));

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(buildCourtapplication(randomUUID(), LocalDate.now()))
                        .withJudicialResults(judicialResults)
                        .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(FINALISED));
        aggregate.apply(eventStream);

        final List<Object> eventStream2 = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withApplicationStatus(FINALISED)
                        .withJudicialResults(null)
                        .build())
                .toList();
        assertThat(eventStream2.size(), is(1));

        final HearingResultedApplicationUpdated secondEvent = (HearingResultedApplicationUpdated) eventStream2.get(0);
        final CourtApplication expectedCourtApplication = courtApplication().withValuesFrom(courtApplication)
                .withApplicationStatus(FINALISED)
                .withJudicialResults(judicialResults)
                .build();
        assertThat(secondEvent.getCourtApplication(), is(expectedCourtApplication));
    }

    @Test
    public void givenApplicationResultedAsFinal_whenTheSameApplicationResultedInSubsequentHearingButOnlyOffencesAreResulted_shouldRetainFinalisedStatusAndJudicialResult() {
        final CourtApplication courtApplication = buildCourtapplication(randomUUID(), LocalDate.now(), emptyList());
        InitiateCourtApplicationProceedings initiateCourtProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();
        aggregate.apply(aggregate.initiateCourtApplicationProceedings(initiateCourtProceedings, false, false));

        final List<JudicialResult> finalJRs = singletonList(judicialResult()
                .withCategory(JudicialResultCategory.FINAL)
                .withJudicialResultId(randomUUID())
                .build());
        final List<Object> eventStream = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(buildCourtapplication(randomUUID(), LocalDate.now()))
                        .withJudicialResults(finalJRs)
                        .build())
                .collect(toList());
        assertThat(eventStream.size(), is(1));
        final HearingResultedApplicationUpdated event = (HearingResultedApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(FINALISED));
        aggregate.apply(eventStream);

        final List<Object> eventStream2 = aggregate.hearingResulted(courtApplication()
                        .withValuesFrom(courtApplication)
                        .withApplicationStatus(UN_ALLOCATED)
                        .withJudicialResults(null)
                        .build())
                .toList();
        assertThat(eventStream2.size(), is(1));

        final HearingResultedApplicationUpdated secondEvent = (HearingResultedApplicationUpdated) eventStream2.get(0);
        final CourtApplication expectedCourtApplication = courtApplication().withValuesFrom(courtApplication)
                .withApplicationStatus(FINALISED)
                .withJudicialResults(finalJRs)
                .build();
        assertThat(secondEvent.getCourtApplication(), is(expectedCourtApplication));
    }

    @Test
    public void shouldHearingResultedRaiseDefendantTrialRecordSheetRequestedEvent() {
        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(getPayload("json/court-application-with-court-order.json"));
        final JsonObject courtApplicationJson = courtApplicationPayload.getJsonObject("courtApplication");
        CourtApplication courtApplication = jsonObjectToObjectConverter.convert(courtApplicationJson, CourtApplication.class);

        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication)
                .toList();
        assertThat(eventStream.size(), is(2));

        assertThat(eventStream.get(0).getClass(), is(equalTo(HearingResultedApplicationUpdated.class)));
        final DefendantTrialRecordSheetRequestedForApplication event = (DefendantTrialRecordSheetRequestedForApplication) eventStream.get(1);
        assertThat(event.getCaseId(), is(courtApplication.getCourtOrder().getCourtOrderOffences().get(0).getProsecutionCaseId()));
        assertThat(event.getOffenceIds(), is(courtApplication.getCourtOrder().getCourtOrderOffences().stream().map(CourtOrderOffence::getOffence).map(Offence::getId).toList()));
        assertThat(event.getCourtApplication(), is(courtApplication));
    }

    @Test
    void shouldHearingResultedRaiseDefendantTrialRecordSheetRequestedEventsWhenMultipleCases() {
        final JsonObject courtApplicationPayload = stringToJsonObjectConverter.convert(getPayload("json/court-application-with-court-order-for-multiple-case.json"));
        final JsonObject courtApplicationJson = courtApplicationPayload.getJsonObject("courtApplication");
        CourtApplication courtApplication = jsonObjectToObjectConverter.convert(courtApplicationJson, CourtApplication.class);
        aggregate.apply(new CourtApplicationCreated.Builder().withCourtApplication(courtApplication).build());

        final List<Object> eventStream = aggregate.hearingResulted(courtApplication).toList();
        assertThat(eventStream.size(), is(3));

        assertThat(eventStream.get(0).getClass(), is(equalTo(HearingResultedApplicationUpdated.class)));
        final DefendantTrialRecordSheetRequestedForApplication event1 = (DefendantTrialRecordSheetRequestedForApplication) eventStream.get(1);
        assertThat(event1.getCaseId(), is(fromString("62a6bcc7-55c4-4cb6-91b8-a918cd37d086")));
        assertThat(event1.getOffenceIds(), is(List.of(fromString("92fd500a-e5c4-4881-8580-fd4754908585"))));
        assertThat(event1.getCourtApplication(), is(courtApplication));

        final DefendantTrialRecordSheetRequestedForApplication event2 = (DefendantTrialRecordSheetRequestedForApplication) eventStream.get(2);
        assertThat(event2.getCaseId(), is(fromString("74672631-fd20-466d-b806-b85a97953476")));
        assertThat(event2.getOffenceIds(), is(List.of(fromString("6073185c-2f85-4c2b-a45c-e1b84e881725"))));
        assertThat(event2.getCourtApplication(), is(courtApplication));
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

        final List<Object> eventStream = aggregate.referApplication().toList();
        assertThat(eventStream.size(), is(1));
        final ApplicationReferredToCourtHearing object = (ApplicationReferredToCourtHearing) eventStream.get(0);
        assertThat(object.getApplication().getApplicationStatus(), is(ApplicationStatus.UN_ALLOCATED));
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
    public void shouldRaiseAutoNotificationEvent() {

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
        final SendNotificationForAutoApplicationInitiated event = (SendNotificationForAutoApplicationInitiated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(initiateCourtApplicationProceedings.getCourtApplication().getId()));
    }

    @Test
    public void shouldRaiseAutoNotificationEventForAmend() {

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
        final SendNotificationForAutoApplicationInitiated event = (SendNotificationForAutoApplicationInitiated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(initiateCourtApplicationProceedings.getCourtApplication().getId()));
        assertThat(event.getIsAmended(), is(initiateCourtApplicationProceedings.getIsAmended()));
        assertThat(event.getIssueDate(), is(orderedDate));

    }

    @Test
    void testRecordLAAReferenceWhenCourtApplicationIsNull() {
        Stream<Object> stream = aggregate.recordLAAReferenceForApplication(randomUUID(), randomUUID(), randomUUID(), Mockito.mock(LaaReference.class));
        assertThat(stream.count(), is(0L));
    }

    @Test
    void testRecordLAAReferenceWhenSubjectIdDoesNotMatch() {
        UUID applicationId = randomUUID();
        UUID subjectId = randomUUID();
        UUID offenceId = randomUUID();
        CourtApplication courtApplication = courtApplication().withId(applicationId)
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build())
                .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .withIsAmended(true)
                .withIssueDate(LocalDate.now())
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        Stream<Object> stream = aggregate.recordLAAReferenceForApplication(applicationId, randomUUID(), offenceId, mock(LaaReference.class));
        assertThat(stream.count(), is(0L));
    }

    @Test
    void testRecordLAAReferenceWhenOffenceNotFound() {
        UUID applicationId = randomUUID();
        UUID subjectId = randomUUID();
        UUID offenceId = randomUUID();
        CourtApplication courtApplication = courtApplication().withId(applicationId)
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build())
                .build();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .withIsAmended(true)
                .withIssueDate(LocalDate.now())
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        Stream<Object> stream = aggregate.recordLAAReferenceForApplication(applicationId, subjectId, offenceId, mock(LaaReference.class));
        assertThat(stream.count(), is(0L));
    }

    @Test
    void testRecordLAAReferenceWhenLaaReferenceIsSame() {
        UUID applicationId = randomUUID();
        UUID subjectId = randomUUID();
        UUID offenceId = randomUUID();
        LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").withStatusDate(LocalDate.now()).build();

        CourtApplication courtApplication = courtApplication().withId(applicationId)
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build())
                .withCourtApplicationCases(singletonList(courtApplicationCase()
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                        .withIsSJP(true)
                        .withOffences(List.of(Offence.offence()
                                .withLaaApplnReference(laaReference)
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();


        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .withIsAmended(true)
                .withIssueDate(LocalDate.now())
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        Stream<Object> stream = aggregate.recordLAAReferenceForApplication(applicationId, subjectId, offenceId, laaReference);
        assertThat(stream.count(), is(0L));
    }

    @Test
    void testRecordLAAReferenceWhenLaaReferenceIsDifferent() {
        UUID applicationId = randomUUID();
        UUID subjectId = randomUUID();
        UUID offenceId = randomUUID();
        LaaReference laaReference = LaaReference.laaReference().withApplicationReference("applicationReference")
                .withStatusCode("statusCode").withStatusDescription("description").withStatusDate(LocalDate.now()).build();

        CourtApplication courtApplication = courtApplication().withId(applicationId)
                .withSubject(CourtApplicationParty.courtApplicationParty().withId(subjectId).build())
                .withCourtApplicationCases(singletonList(courtApplicationCase()
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                        .withIsSJP(true)
                        .withOffences(List.of(Offence.offence()
                                .withLaaApplnReference(laaReference)
                                .withId(offenceId)
                                .build()))
                        .build()))
                .build();


        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication)
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .withIsAmended(true)
                .withIssueDate(LocalDate.now())
                .build();

        aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false);

        Stream<Object> stream = aggregate.recordLAAReferenceForApplication(applicationId, subjectId, offenceId, mock(LaaReference.class));
        assertThat(stream.count(), is(1L));
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
    public void shouldRaiseCourtApplicationUpdated() {

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
        final CourtApplicationUpdated event = (CourtApplicationUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getId(), is(courtApplicationId));
        assertThat(aggregate.getCourtApplication().getSubject().getMasterDefendant().getPersonDefendant().getCustodialEstablishment().getCustody(), is("custody2"));
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void shouldUpdateApplicationStatusWithPatch(final ApplicationStatus applicationStatus) {
        final UUID applicationId = randomUUID();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = InitiateCourtApplicationProceedings
                .initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication().withId(applicationId)
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build()).withIsSJP(true).build())).build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                .withSummonsApprovalRequired(false)
                .build();

        aggregate.apply(aggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, true, false));

        final List<Object> eventStream = aggregate.patchUpdateApplicationStatus(applicationStatus).toList();

        assertThat(eventStream.size(), is(1));
        final CourtApplicationStatusUpdated event = (CourtApplicationStatusUpdated) eventStream.get(0);
        assertThat(event.getCourtApplication().getApplicationStatus(), is(applicationStatus));

        aggregate.apply(event);
        assertThat(aggregate.getApplicationStatus(), is(applicationStatus));
    }

    @Test
    public void shouldRaiseApplicationDefenceOrganisationChangedWhenOrganisationIsAssociatedWithTheDefendant() {

        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID subjectId = randomUUID();

        final DefendantDefenceOrganisationAssociated defendantDefenceOrganisationAssociated = DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                .withDefendantId(defendantId)
                .withOrganisationId(organisationId)
                .build();

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withId(subjectId)
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withMasterDefendantId(defendantId)
                                        .build())
                                .build())
                        .build())
                .build();

        aggregate.apply(courtApplicationProceedingsInitiated);
        aggregate.apply(defendantDefenceOrganisationAssociated);

        final List<Object> eventStream = aggregate.disassociateDefenceOrganisationForApplication(defendantId, organisationId).toList();

        assertThat(eventStream.size(), is(2));
        Object event = eventStream.get(0);
        assertThat(event.getClass(), is(CoreMatchers.equalTo((ApplicationDefenceOrganisationChanged.class))));
        final ApplicationDefenceOrganisationChanged applicationDefenceOrganisationChanged = (ApplicationDefenceOrganisationChanged) event;
        assertThat(applicationDefenceOrganisationChanged.getApplicationId(), is(applicationId));
        assertThat(applicationDefenceOrganisationChanged.getAssociatedDefenceOrganisation(), nullValue());
        assertThat(applicationDefenceOrganisationChanged.getApplicationCaseDefendantOrganisations(), nullValue());
        assertThat(applicationDefenceOrganisationChanged.getSubjectId(), is(subjectId));

        event = eventStream.get(1);
        assertThat(event.getClass(), is(CoreMatchers.equalTo((DefenceOrganisationDissociatedForApplicationByDefenceContext.class))));
        final DefenceOrganisationDissociatedForApplicationByDefenceContext defenceOrganisationDissociatedForApplicationByDefenceContext = (DefenceOrganisationDissociatedForApplicationByDefenceContext) event;
        assertThat(defenceOrganisationDissociatedForApplicationByDefenceContext.getDefendantId(), is(defendantId));
        assertThat(defenceOrganisationDissociatedForApplicationByDefenceContext.getOrganisationId(), is(organisationId));
        assertThat(defenceOrganisationDissociatedForApplicationByDefenceContext.getApplicationId(), is(applicationId));

        assertThat(aggregate.getCourtApplication().getSubject().getAssociatedDefenceOrganisation(), nullValue());
    }


    @Test
    public void shouldNotRaiseApplicationDefenceOrganisationChangedWhenApplicationNotFound() {

        final UUID defendantId = randomUUID();
        final UUID organisationId = randomUUID();

        final List<Object> eventStream = aggregate.disassociateDefenceOrganisationForApplication(defendantId, organisationId).toList();

        assertThat(eventStream.size(), is(0));
    }

    @Test
    public void shouldNotRaiseApplicationDefenceOrganisationChangedWhenNoAssociationForGivenDefendant() {

        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID organisationId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID subjectId = randomUUID();

        final DefendantDefenceOrganisationAssociated defendantDefenceOrganisationAssociated = DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                .withDefendantId(defendantId2)
                .withOrganisationId(organisationId)
                .build();

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withId(subjectId)
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withMasterDefendantId(defendantId2)
                                        .build())
                                .build())
                        .build())
                .build();

        aggregate.apply(courtApplicationProceedingsInitiated);
        aggregate.apply(defendantDefenceOrganisationAssociated);

        final List<Object> eventStream = aggregate.disassociateDefenceOrganisationForApplication(defendantId1, organisationId).toList();

        assertThat(eventStream.size(), is(0));

    }

    @Test
    public void shouldNotRaiseApplicationDefenceOrganisationChangedWhenNoGivenOrganisationIsNotAssociatedForGivenDefendant() {

        final UUID defendantId = randomUUID();
        final UUID organisationId1 = randomUUID();
        final UUID organisationId2 = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID subjectId = randomUUID();

        final DefendantDefenceOrganisationAssociated defendantDefenceOrganisationAssociated = DefendantDefenceOrganisationAssociated.defendantDefenceOrganisationAssociated()
                .withDefendantId(defendantId)
                .withOrganisationId(organisationId1)
                .build();

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withSubject(CourtApplicationParty.courtApplicationParty()
                                .withId(subjectId)
                                .withMasterDefendant(MasterDefendant.masterDefendant()
                                        .withMasterDefendantId(defendantId)
                                        .build())
                                .build())
                        .build())
                .build();

        aggregate.apply(courtApplicationProceedingsInitiated);
        aggregate.apply(defendantDefenceOrganisationAssociated);

        final List<Object> eventStream = aggregate.disassociateDefenceOrganisationForApplication(defendantId, organisationId2).toList();

        assertThat(eventStream.size(), is(0));

    }

}
