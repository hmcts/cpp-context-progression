package uk.gov.moj.cpp.progression.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.domain.event.email.PartyType.CASE;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithLegalEntity;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildCourtApplicationPartyWithPersonDefendant;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildDefendantWithLegalEntity;
import static uk.gov.moj.cpp.progression.utils.TestUtils.buildDefendantWithPersonDefendant;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCompanyAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyCompanyEmail;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyPersonAddress;
import static uk.gov.moj.cpp.progression.utils.TestUtils.verifyPersonEmail;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CaseSubjects;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileRetriever;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;
import uk.gov.moj.cpp.progression.value.object.CaseVO;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;
import uk.gov.moj.cpp.progression.value.object.DefendantVO;
import uk.gov.moj.cpp.progression.value.object.EmailTemplateType;
import uk.gov.moj.cpp.progression.value.object.HearingVO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    private UUID caseId;

    private UUID materialId;

    private UUID notificationId;

    private UUID applicationId;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private SystemIdMapperService systemIdMapperService;

    @Mock
    private Sender sender;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private CpsRestNotificationService cpsRestNotificationService;

    @InjectMocks
    private MaterialService materialService;

    @Mock
    private Requester requester;

    @Mock
    private FileRetriever fileRetriever;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> apiNotificationArgumentCaptor;

    private final ZonedDateTime hearingDateTime = ZonedDateTime.of(LocalDate.of(2019, 06, 18), LocalTime.of(14, 45), ZoneId.of("UTC"));

    private CourtCentre courtCentre = CourtCentre.courtCentre().withName("Test Court Centre").withAddress(Address.address()
            .withAddress1("Test Address 1")
            .withAddress2("Test Address 2")
            .withAddress3("Test Address 3")
            .withPostcode("AS1 1DF").build()).build();

    @Before
    public void setUp() {
        this.caseId = randomUUID();
        this.materialId = randomUUID();
        this.notificationId = randomUUID();
        this.applicationId = randomUUID();
        this.materialService = new MaterialService();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldExecutePrint() {
        final UUID notificationId = randomUUID();

        doNothing().when(systemIdMapperService).mapNotificationIdToCaseId(caseId, notificationId);

        notificationService.sendLetter(envelope, notificationId, caseId, null, materialId, false);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.print"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                        withJsonPath("$.materialId", equalTo(materialId.toString())))
                ))));
    }

    @Test
    public void shouldRecordPrintRecordFailure() {
        final UUID notificationId = randomUUID();
        final String failedTime = ZonedDateTimes.toString(new UtcClock().now());
        final String errorMessage = "error message";
        final int statusCode = SC_NOT_FOUND;

        final JsonEnvelope eventEnvelope = envelope()
                .with(metadataBuilder()
                        .withName("public.notificationnotify.events.notification-failed")
                        .withId(randomUUID()))
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(failedTime, "failedTime")
                .withPayloadOf(errorMessage, "errorMessage")
                .withPayloadOf(statusCode, "statusCode")
                .build();

        notificationService.recordNotificationRequestFailure(eventEnvelope, caseId, CASE);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(eventEnvelope).withName("progression.command.record-notification-request-failure"),
                payloadIsJson(allOf(
                        withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.failedTime", equalTo(failedTime)),
                        withJsonPath("$.errorMessage", equalTo(errorMessage)),
                        withJsonPath("$.statusCode", equalTo(statusCode)))
                ))));
    }

    @Test
    public void shouldRecordPrintSuccess() {

        final UUID notificationId = randomUUID();

        final String sentTime = ZonedDateTimes.toString(new UtcClock().now());
        final String completedAt = ZonedDateTimes.toString(new UtcClock().now());

        final JsonEnvelope eventEnvelope = envelope()
                .with(metadataBuilder()
                        .withName("public.notificationnotify.events.notification-sent")
                        .withId(randomUUID()))
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(sentTime, "sentTime")
                .withPayloadOf(completedAt, "completedAt")
                .build();

        notificationService.recordNotificationRequestSuccess(eventEnvelope, caseId, CASE);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(eventEnvelope).withName("progression.command.record-notification-request-success"),
                payloadIsJson(allOf(
                        withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.sentTime", equalTo(sentTime)),
                        withJsonPath("$.completedAt", equalTo(completedAt))
                        )
                ))));
    }

    @Test
    public void shouldRecordSuccess_CompletedTimeNotAvailable() {

        final UUID notificationId = randomUUID();

        final String sentTime = ZonedDateTimes.toString(new UtcClock().now());

        final JsonEnvelope eventEnvelope = envelope()
                .with(metadataBuilder()
                        .withName("public.notificationnotify.events.notification-sent")
                        .withId(randomUUID()))
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(sentTime, "sentTime")
                .build();

        notificationService.recordNotificationRequestSuccess(eventEnvelope, caseId, CASE);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(eventEnvelope).withName("progression.command.record-notification-request-success"),
                payloadIsJson(allOf(
                        withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.sentTime", equalTo(sentTime)),
                        withoutJsonPath("$.completedAt"))
                ))));
    }

    @Test
    public void shouldRecordPrintRequestAcceptedCommand() {

        final ZonedDateTime acceptedTime = ZonedDateTime.now();

        final JsonEnvelope sourceEnvelope = envelope()
                .with(metadataBuilder()
                        .withName("progression.event.print-requested")
                        .withId(randomUUID())
                        .createdAt(acceptedTime)
                )
                .withPayloadOf(notificationId, "notificationId")
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(materialId, "materialId")
                .build();

        notificationService.recordPrintRequestAccepted(sourceEnvelope);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(sourceEnvelope).withName("progression.command.record-notification-request-accepted"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.notificationId", equalTo(notificationId.toString())),
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.acceptedTime", equalTo(ZonedDateTimes.toString(acceptedTime)))
                        )))));

    }

    @Test
    public void sendNotificationForTheApplicant() {
        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(
                                MasterDefendant.masterDefendant().withPersonDefendant(
                                                PersonDefendant.personDefendant().withPersonDetails(
                                                                Person.person().withFirstName("Test").withLastName("Test")
                                                                        .build())
                                                        .build())
                                        .build())
                        .withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.CROWN);
        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.email"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString())))))));
    }

    @Test
    public void sendNotificationForTheApplicantAsOrganisation() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withFirstName("Test").withLastName("Test").build()).build()).build())
                        .withOrganisation(Organisation.organisation().withContact(ContactNumber.contactNumber().withPrimaryEmail("applicant@test.com").build()).build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.email"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString())))))));
    }

    @Test
    public void sendNotificationForTheApplicantAsDefendant() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .build()).build()).build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.email"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString())))))));
    }

    @Test
    public void sendNotificationForTheApplicantAsProsecutingAuthority() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority().withContact(ContactNumber.contactNumber().withPrimaryEmail("applicant@prosecutingauthority.com").build()).build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.email"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString())))))));
    }

    @Test
    public void sendNotificationForTheRespondents() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final List<CourtApplicationParty> respondents = singletonList(
                CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person().withContact(ContactNumber.contactNumber()
                                .withPrimaryEmail("respondents@test.com").build()).build()).build());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withApplicationReference("applicationReference")
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withRespondents(respondents)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(
                                MasterDefendant.masterDefendant().withPersonDefendant(
                                                PersonDefendant.personDefendant().withPersonDetails(
                                                                Person.person().withFirstName("Test").withLastName("Test")
                                                                        .build())
                                                        .build())
                                        .build())                         .withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.CROWN);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        assertThat(this.envelopeArgumentCaptor.getAllValues().get(1), jsonEnvelope(metadata().withName("progression.command.email"), payloadIsJson(allOf(
                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString())),
                withJsonPath("$.notifications[0].personalisation.application_reference", equalTo("applicationReference"))
        ))));
    }

    @Test
    public void sendNotificationForTheThirdParties() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final List<CourtApplicationParty> thirdParties = singletonList(
                CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person().withContact(ContactNumber.contactNumber()
                                .withPrimaryEmail("thirdParties@test.com").build()).build()).build());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withThirdParties(thirdParties)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant().withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().withFirstName("Test").withLastName("Test").build()).build()).build())
                        .withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.CROWN);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        assertThat(this.envelopeArgumentCaptor.getAllValues().get(1), jsonEnvelope(metadata().withName("progression.command.email"), payloadIsJson(allOf(
                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString()))))));
    }

    @Test
    public void sendNotificationForTheRespondentsAsOrganisation() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final List<CourtApplicationParty> respondents = singletonList(
                CourtApplicationParty.courtApplicationParty()
                        .withOrganisation(Organisation.organisation()
                                .withContact(ContactNumber.contactNumber()
                                        .withPrimaryEmail("respondents@organisation.com").build()).build())
                        .build());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withRespondents(respondents)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(
                                MasterDefendant.masterDefendant().withPersonDefendant(
                                        PersonDefendant.personDefendant().withPersonDetails(
                                                        Person.person().withFirstName("Test").withLastName("Test")
                                                                .build())
                                                .build())
                                .build())
                        .withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES);
        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        assertThat(this.envelopeArgumentCaptor.getAllValues().get(1), jsonEnvelope(metadata().withName("progression.command.email"), payloadIsJson(allOf(
                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString()))))));
    }

    @Test
    public void sendNotificationForTheRespondentsAsDefendant() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final List<CourtApplicationParty> respondents = singletonList(
                CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withPersonDetails(Person.person()
                                                .withContact(ContactNumber.contactNumber().withPrimaryEmail("defantant@test.com").build()).build()).build()).build())
                        .build());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withRespondents(respondents)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(
                                MasterDefendant.masterDefendant().withPersonDefendant(
                                        PersonDefendant.personDefendant().withPersonDetails(
                                                Person.person().withFirstName("Test").withLastName("Test")
                                                        .build())
                                                .build())
                                        .build())
                        .withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@defendant.com")
                                                        .build())
                                        .build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        assertThat(this.envelopeArgumentCaptor.getAllValues().get(1), jsonEnvelope(metadata().withName("progression.command.email"), payloadIsJson(allOf(
                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString()))))));
    }


    @Test
    public void sendNotificationForTheRespondentsAsProsecutingAuthority() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final List<CourtApplicationParty> respondents = singletonList(
                CourtApplicationParty.courtApplicationParty()
                        .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority().withContact(ContactNumber.contactNumber().withPrimaryEmail("ProsecutingAuthority@test.com").build()).build())
                        .build());

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withRespondents(respondents)
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(
                                MasterDefendant.masterDefendant().withPersonDefendant(
                                                PersonDefendant.personDefendant().withPersonDetails(
                                                                Person.person().withFirstName("Test").withLastName("Test")
                                                                        .build())
                                                        .build())
                                        .build())                         .withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@prosecutingAuthority.com")
                                                        .build())
                                        .build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.MAGISTRATES);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());

        assertThat(this.envelopeArgumentCaptor.getAllValues().get(1), jsonEnvelope(metadata().withName("progression.command.email"), payloadIsJson(allOf(
                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString())),
                withJsonPath("$.notifications[0].personalisation.time", equalTo("3:45 PM"))
        ))));
    }

    @Test
    public void sendNotificationForTheApplicantOnlyEmailIfBothDetailsAreAvailable() {

        doNothing().when(systemIdMapperService).mapNotificationIdToApplicationId(applicationId, notificationId);

        when(applicationParameters.getApplicationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");

        final CourtApplication courtApplication = CourtApplication.courtApplication()
                .withId(applicationId)
                .withType(CourtApplicationType.courtApplicationType().withSummonsTemplateType(NOT_APPLICABLE).build())
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withMasterDefendant(
                                MasterDefendant.masterDefendant().withPersonDefendant(
                                                PersonDefendant.personDefendant().withPersonDetails(
                                                                Person.person().withFirstName("Test").withLastName("Test")
                                                                        .build())
                                                        .build())
                                        .build())                        .withPersonDetails(
                                Person.person()
                                        .withContact(
                                                ContactNumber.contactNumber()
                                                        .withPrimaryEmail("applicant@test.com")
                                                        .build())
                                        .withAddress(Address.address().withAddress1("address1").withAddress2("address2").withPostcode("CR0 1XG").build())
                                        .build())
                        .build())
                .build();

        notificationService.sendNotification(envelope, notificationId, courtApplication, courtCentre, hearingDateTime, JurisdictionType.CROWN);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("progression.command.email"),
                payloadIsJson(
                        allOf(
                                withJsonPath("$.applicationId", equalTo(applicationId.toString())),
                                withJsonPath("$.notifications[0].notificationId", equalTo(notificationId.toString())))))));

    }

    @Test
    public void getDefendantEmailAddressWithLegalEntityTest() throws Exception {
        MasterDefendant defendantWithLegalEntityMock = buildDefendantWithLegalEntity();
        Optional<String> resultEmailAddress = Whitebox.invokeMethod(notificationService, "getDefendantEmailAddress", defendantWithLegalEntityMock);
        verifyCompanyEmail(resultEmailAddress.get());
    }

    @Test
    public void getDefendantEmailAddressWithPersonDefendantTest() throws Exception {
        MasterDefendant defendantWithPersonDefendantMock = buildDefendantWithPersonDefendant();
        Optional<String> resultEmailAddress = Whitebox.invokeMethod(notificationService, "getDefendantEmailAddress", defendantWithPersonDefendantMock);
        verifyPersonEmail(resultEmailAddress.get());
    }

    @Test
    public void getDefendantAddressWithLegalEntityTest() throws Exception {
        MasterDefendant defendantWithLegalEntityMock = buildDefendantWithLegalEntity();
        Optional<Address> resultAddress = Whitebox.invokeMethod(notificationService, "getDefendantAddress", defendantWithLegalEntityMock);
        verifyCompanyAddress(resultAddress.get());
    }

    @Test
    public void getDefendantAddressWithPersonDefendantTest() throws Exception {
        MasterDefendant defendantWithPersonDefendantMock = buildDefendantWithPersonDefendant();
        Optional<Address> resultAddress = Whitebox.invokeMethod(notificationService, "getDefendantAddress", defendantWithPersonDefendantMock);
        verifyPersonAddress(resultAddress.get());
    }

    @Test
    public void getCourtApplicationPartyEmailAddressTest() throws Exception {
        CourtApplicationParty courtApplicationPartyMock = buildCourtApplicationPartyWithLegalEntity();
        Optional<String> companyEmail = Whitebox.invokeMethod(notificationService, "getCourtApplicationPartyEmailAddress", courtApplicationPartyMock);
        verifyCompanyEmail(companyEmail.get());

        CourtApplicationParty courtApplicationPartyMock1 = buildCourtApplicationPartyWithPersonDefendant();
        Optional<String> personEmail = Whitebox.invokeMethod(notificationService, "getCourtApplicationPartyEmailAddress", courtApplicationPartyMock1);
        verifyPersonEmail(personEmail.get());
    }

    @Test
    public void getApplicantAddressTest() throws Exception {
        CourtApplicationParty courtApplicationPartyMock = buildCourtApplicationPartyWithLegalEntity();
        Optional<Address> companyAddress = Whitebox.invokeMethod(notificationService, "getApplicantAddress", courtApplicationPartyMock);
        verifyCompanyAddress(companyAddress.get());

        CourtApplicationParty courtApplicationPartyMock1 = buildCourtApplicationPartyWithPersonDefendant();
        Optional<Address> personAddress = Whitebox.invokeMethod(notificationService, "getApplicantAddress", courtApplicationPartyMock1);
        verifyPersonAddress(personAddress.get());
    }

    @Test
    public void sendCPSNotificationTest() throws Exception {
        when(applicationParameters.getDefenceDisassociationTemplateId()).thenReturn("47705b45-fbdc-44ec-9fe5-ff89b707e6ce");
        HearingVO hearingVO = HearingVO.builder().courtCenterId(randomUUID()).courtName("CourtName").hearingDate("22-12-2019").build();
        Optional<CaseVO> caseVO = Optional.of(CaseVO.builder().caseId(caseId).caseURN("caseURN").build());
        Optional<DefenceOrganisationVO> defenceOraganisationVO = Optional.of(DefenceOrganisationVO.builder()
                .addressLine1("1 pickwick close")
                .addressLine2("Hounslow Heath")
                .addressLine3("Hounslow")
                .addressLine4("Middlesex")
                .postcode("TW45ED")
                .email("anas.khatri@Gmail.com").phoneNumber("07950564893").name("OrganisationName_MOD").build());

        Optional<DefendantVO> defendantVO = Optional.of(DefendantVO.builder().firstName("firstName").lastName("lastname").legalEntityName("legalEntityName").middleName("S").build());

        CPSNotificationVO cpsNotification = CPSNotificationVO.builder()
                .cpsEmailAddress("mohammed.khatri@hmcts.net")
                .templateType(EmailTemplateType.DISASSOCIATION)
                .defenceOrganisationVO(defenceOraganisationVO)

                .defendantVO(defendantVO)
                .caseVO(caseVO)
                .hearingVO(hearingVO).build();

        notificationService.sendCPSNotification(envelope, cpsNotification);

        verify(this.sender, times(1)).send(this.envelopeArgumentCaptor.capture());
        assertThat(this.envelopeArgumentCaptor.getAllValues().get(0), jsonEnvelope(metadata().withName("progression.command.email"), payloadIsJson(allOf(
                withJsonPath("$.notifications[0].sendToAddress", equalTo("mohammed.khatri@hmcts.net")),
                withJsonPath("$.notifications[0].personalisation.URN", equalTo("caseURN")),
                withJsonPath("$.notifications[0].personalisation.venue_name", equalTo("CourtName")),
                withJsonPath("$.notifications[0].personalisation.hearing_date", equalTo("22-12-2019")),
                withJsonPath("$.notifications[0].personalisation.phone", equalTo("07950564893")),
                withJsonPath("$.notifications[0].personalisation.surname", equalTo("legalEntityName")),
                withJsonPath("$.notifications[0].personalisation.address_line_1", equalTo("1 pickwick close")),
                withJsonPath("$.notifications[0].personalisation.postcode", equalTo("TW45ED")),
                withJsonPath("$.notifications[0].personalisation.address_line_2", equalTo("Hounslow Heath")),
                withJsonPath("$.notifications[0].personalisation.address_line_3", equalTo("Hounslow")),
                withJsonPath("$.notifications[0].personalisation.organisation_name", equalTo("OrganisationName_MOD"))
        ))));
    }

    @Test
    public void sendApiNotificationTest() {
        final MaterialDetails materialDetails = MaterialDetails.materialDetails()
                .withMaterialId(materialId)
                .build();
        List<CaseSubjects> caseSubjects = new ArrayList<>();
        caseSubjects.add(CaseSubjects.caseSubjects()
                .withUrn("caseURN123")
                .withProsecutingAuthorityOUCode("ouCode123")
                .build());
        caseSubjects.add(CaseSubjects.caseSubjects()
                .withUrn("caseURN456")
                .withProsecutingAuthorityOUCode("ouCode456")
                .build());

        notificationService.sendApiNotification(envelope, notificationId, materialDetails, caseSubjects, Arrays.asList("defAsn,defAsn2"),  null);
        verify(cpsRestNotificationService, times(1)).sendMaterial(apiNotificationArgumentCaptor.capture());

        JsonObject jsonObject = new StringToJsonObjectConverter().convert(apiNotificationArgumentCaptor.getValue());
        assertThat(jsonObject.getString("businessEventType"), is("now-generated-for-cps-subscription"));
        assertThat(jsonObject.getJsonArray("cases").size(), is(2));
        assertThat(jsonObject.getJsonArray("additionalDefendantSubject").size(), is(2));
    }

    @Test
    public void sendApiNotificationTest_singleCase() {
        final MaterialDetails materialDetails = MaterialDetails.materialDetails()
                .withMaterialId(materialId)
                .build();
        List<CaseSubjects> caseSubjects = new ArrayList<>();
        caseSubjects.add(CaseSubjects.caseSubjects()
                .withUrn("caseURN123")
                .withProsecutingAuthorityOUCode("ouCode123")
                .build());

        notificationService.sendApiNotification(envelope, notificationId, materialDetails, caseSubjects, Arrays.asList("defAsn"),  null);
        verify(cpsRestNotificationService, times(1)).sendMaterial(apiNotificationArgumentCaptor.capture());

        JsonObject jsonObject = new StringToJsonObjectConverter().convert(apiNotificationArgumentCaptor.getValue());
        assertThat(jsonObject.getJsonObject("subjectDetails").getJsonObject("prosecutionCaseSubject").getJsonString("prosecutingAuthority").getString(), is("ouCode123"));
        assertThat(jsonObject.getJsonArray("cases"), is(nullValue()));
        assertThat(jsonObject.getJsonArray("additionalDefendantSubject"), is(nullValue()));
    }
}
