package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.*;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.core.courts.ApplicationReferredToExistingHearing.applicationReferredToExistingHearing;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationCreated.courtApplicationCreated;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.CourtApplicationProceedingsEdited.courtApplicationProceedingsEdited;
import static uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated.courtApplicationProceedingsInitiated;
import static uk.gov.justice.core.courts.CourtApplicationSummonsRejected.courtApplicationSummonsRejected;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.CourtHearingRequest.courtHearingRequest;
import static uk.gov.justice.core.courts.CourtOrderOffence.*;
import static uk.gov.justice.core.courts.HearingResultedApplicationUpdated.hearingResultedApplicationUpdated;
import static uk.gov.justice.core.courts.InitiateCourtHearingAfterSummonsApproved.initiateCourtHearingAfterSummonsApproved;
import static uk.gov.justice.core.courts.MasterDefendant.masterDefendant;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SendNotificationForApplication.sendNotificationForApplication;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsRejectedOutcome.summonsRejectedOutcome;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.EMAIL_ADDRESS;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.processor.CourtApplicationProcessor.PUBLIC_PROGRESSION_EVENTS_BREACH_APPLICATIONS_TO_BE_ADDED_TO_HEARING;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationReferredToExistingHearing;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.BreachApplicationCreationRequested;
import uk.gov.justice.core.courts.BreachApplicationsToBeAddedToHearing;
import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.BreachedApplications;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CreateHearingApplicationRequest;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.InitiateCourtHearingAfterSummonsApproved;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.PublicProgressionCourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.SendNotificationForApplication;
import uk.gov.justice.core.courts.SummonsRejectedOutcome;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.core.courts.SummonsType;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.progression.processor.exceptions.CaseNotFoundException;
import uk.gov.moj.cpp.progression.processor.summons.SummonsHearingRequestService;
import uk.gov.moj.cpp.progression.processor.summons.SummonsRejectedService;
import uk.gov.moj.cpp.progression.service.ListHearingBoxworkService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.SjpService;
import uk.gov.moj.cpp.progression.utils.FileUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@SuppressWarnings({"squid:S1607"})
@ExtendWith(MockitoExtension.class)
public class CourtApplicationProcessorTest {

    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED_APPLICATION_UPDATED = "public.progression.hearing-resulted-application-updated";

    @InjectMocks
    private CourtApplicationProcessor courtApplicationProcessor;

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Mock
    private ListingService listingService;

    @Mock
    private SjpService sjpService;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private NotificationService notificationService = new NotificationService();

    @Mock
    private SummonsHearingRequestService summonsHearingRequestService;

    @Mock
    private SummonsRejectedService summonsRejectedService;

    @Captor
    private ArgumentCaptor<CreateHearingApplicationRequest> captorCreateHearingApplicationRequest;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Captor
    private ArgumentCaptor<Envelope<PublicProgressionCourtApplicationSummonsRejected>> summonsRejectedEnvelopeCaptor;

    @Mock
    private ListHearingBoxworkService listHearingBoxworkService;

    public static Stream<Arguments> applicationSummonsSpecification() {
        return Stream.of(
                // summons code, type, template name, youth defendant, number of documents
                Arguments.of(SummonsTemplateType.BREACH, SummonsType.BREACH),
                Arguments.of(SummonsTemplateType.GENERIC_APPLICATION, SummonsType.APPLICATION)
        );
    }

    @Test
    public void processCourtApplicationCreatedWithoutCpsDefendantId() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");
        final CourtApplicationCreated courtApplicationCreated = courtApplicationCreated()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationCreated);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationCreated.class)).thenReturn(courtApplicationCreated);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("public.progression.defendant-address-changed"));
        assertThat(captor.getAllValues().get(2).payload(), notNullValue());
    }

    @Test
    public void processCourtApplicationCreatedWithCpsDefendantId() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");
        final CourtApplicationCreated courtApplicationCreated = courtApplicationCreated()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationCreated);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationCreated.class)).thenReturn(courtApplicationCreated);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(5)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("public.progression.court-application-created"));
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("progression.command.update-cps-defendant-id"));
        assertThat(captor.getAllValues().get(2).payload(), notNullValue());
        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.update-cps-defendant-id"));
        assertThat(captor.getAllValues().get(3).payload(), notNullValue());
        assertThat(captor.getAllValues().get(3).metadata().name(), is("public.progression.defendant-address-changed"));
        assertThat(captor.getAllValues().get(4).payload(), notNullValue());
        assertThat(captor.getAllValues().get(4).metadata().name(), is("progression.command.list-or-refer-court-application"));
    }


    @Test
    public void processCourtApplicationCreatedWithRemoveCustodialEstablishment() {
        //Given
        final UUID masterDefendantId = randomUUID();
        final UUID caseId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");
        final CourtApplicationCreated courtApplicationCreated = courtApplicationCreated()
                .withCourtApplication(courtApplication()
                        .withApplicant(buildMasterDefendant(masterDefendantId))
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .withApplicationStatus(ApplicationStatus.DRAFT)
                        .withSubject(buildMasterDefendant(masterDefendantId))
                        .withCourtApplicationCases(asList(CourtApplicationCase.courtApplicationCase()
                                .withCaseStatus("INACTIVE")
                                .withProsecutionCaseId(caseId)
                                .build()))
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationCreated);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationCreated.class)).thenReturn(courtApplicationCreated);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(5)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("public.progression.court-application-created"));
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("progression.command.update-cps-defendant-id"));
        assertThat(captor.getAllValues().get(2).payload(), notNullValue());
        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.update-cps-defendant-id"));
        assertThat(captor.getAllValues().get(3).payload(), notNullValue());
        assertThat(captor.getAllValues().get(3).metadata().name(), is("public.progression.defendant-address-changed"));
        assertThat(captor.getAllValues().get(4).payload(), notNullValue());
        assertThat(captor.getAllValues().get(4).metadata().name(), is("progression.command.list-or-refer-court-application"));

    }

    @Test
    public void processCourtApplicationCreatedWithRemoveCustodialEstablishmentForRespondent() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");

        final JsonObject payload = FileUtil.givenPayload("/progression.event.court-application-created_defendant_for_respondent.json");


        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("public.progression.court-application-created"));
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("public.progression.defendant-address-changed"));
        assertThat(captor.getAllValues().get(2).payload(), notNullValue());
        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.list-or-refer-court-application"));

    }

    @Test
    public void processRemoveDefendantCustodialEstablishmentRequested() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");
        final JsonObject payload = FileUtil.givenPayload("/progression.event.court-application-created_defendant_for_respondent.json");
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        //When
        courtApplicationProcessor.processRemoveDefendantCustodialEstablishmentRequested(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("progression.command.remove-defendant-custodial-establishment-from-case"));
    }

    @Test
    public void processRemoveDefendantCustodialEstablishmentRequestedWithMultiCase() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");
        final JsonObject payload = FileUtil.givenPayload("/progression.event.court-application-created_defendant_for_multi-case.json");
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        //When
        courtApplicationProcessor.processRemoveDefendantCustodialEstablishmentRequested(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("progression.command.remove-defendant-custodial-establishment-from-case"));
        assertThat(((JsonObject)captor.getAllValues().get(0).payload()).getString("prosecutionCaseId"), is("2a90f833-052e-4242-92ad-e4cbb75dff83"));
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("progression.command.remove-defendant-custodial-establishment-from-case"));
        assertThat(((JsonObject)captor.getAllValues().get(1).payload()).getString("prosecutionCaseId"), is("9a011446-2096-4c34-8a84-0fe484da9f0a"));

    }

    @Test
    public void processCourtApplicationCreatedWithoutRemoveCustodialEstablishmentForOrganisation() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");

        final JsonObject payload = FileUtil.givenPayload("/progression.event.court-application-created_defendant_for_organisation.json");

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("public.progression.court-application-created"));
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("public.progression.defendant-address-changed"));
        assertThat(captor.getAllValues().get(2).payload(), notNullValue());
        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.list-or-refer-court-application"));

    }

    @Test
    public void processCourtApplicationCreatedWithRemoveCustodialEstablishmentForAppealent() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");

        final JsonObject payload = FileUtil.givenPayload("/progression.event.court-application-created_defendant_is_appealent.json");


        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("public.progression.court-application-created"));
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("public.progression.defendant-address-changed"));
        assertThat(captor.getAllValues().get(2).payload(), notNullValue());
        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.list-or-refer-court-application"));

    }

    @Test
    public void processCourtApplicationCreatedWithRemoveCustodialEstablishmentForApplicant() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-created");

        final JsonObject payload = FileUtil.givenPayload("/progression.event.court-application-created_defendant_is_applicant.json");


        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        //When
        courtApplicationProcessor.processCourtApplicationCreated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        assertThat(captor.getAllValues().get(0).payload(), notNullValue());
        assertThat(captor.getAllValues().get(0).metadata().name(), is("public.progression.court-application-created"));
        assertThat(captor.getAllValues().get(1).payload(), notNullValue());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("public.progression.defendant-address-changed"));
        assertThat(captor.getAllValues().get(2).payload(), notNullValue());
        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.list-or-refer-court-application"));

    }

    @Test
    public void processSendNotificationForApplicationShouldNotThrowException() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.send-notification-for-application-initiated");
        SendNotificationForApplication sendNotificationForApplication = sendNotificationForApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .withIsWelshTranslationRequired(false)
                .withCourtHearing(courtHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre().build())
                        .withEarliestStartDateTime(ZonedDateTime.now())
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(sendNotificationForApplication);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendNotificationForApplication.class)).thenReturn(sendNotificationForApplication);


        courtApplicationProcessor.sendNotificationForApplication(event);
        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(notificationService, times(1)).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

        SendNotificationForApplication sendNotificationForApplicationWelshRequired = sendNotificationForApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .withIsWelshTranslationRequired(true)
                .withCourtHearing(courtHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre().build())
                        .withEarliestStartDateTime(ZonedDateTime.now())
                        .build())
                .build();
        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, times(2)).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

    }

    @Test
    public void processSendNotificationForApplicationShouldNotSendNotificationWhenCourtRoomIsSet() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.send-notification-for-application-initiated");
        SendNotificationForApplication sendNotificationForApplication = sendNotificationForApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .withIsWelshTranslationRequired(false)
                .withCourtHearing(courtHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                        .withEarliestStartDateTime(ZonedDateTime.now())
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(sendNotificationForApplication);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendNotificationForApplication.class)).thenReturn(sendNotificationForApplication);


        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, never()).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, never()).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

    }

    @Test
    public void processSendNotificationForApplicationShouldSendNotificationWhenCourtRoomIsSetAndWeekCommencingDateIsSet() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.send-notification-for-application-initiated");
        SendNotificationForApplication sendNotificationForApplication = sendNotificationForApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .withIsWelshTranslationRequired(false)
                .withCourtHearing(courtHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                        .withEarliestStartDateTime(ZonedDateTime.now())
                        .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate().withStartDate(LocalDate.now()).build())
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(sendNotificationForApplication);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendNotificationForApplication.class)).thenReturn(sendNotificationForApplication);


        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, times(1)).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, times(2)).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

    }

    @Test
    public void processSendNotificationForApplicationShouldSendNotificationWhenCourtRoomIsNotSetAndWeekCommencingDateIsSet() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.send-notification-for-application-initiated");
        SendNotificationForApplication sendNotificationForApplication = sendNotificationForApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .withIsWelshTranslationRequired(false)
                .withCourtHearing(courtHearingRequest()
                        .withCourtCentre(CourtCentre.courtCentre().build())
                        .withEarliestStartDateTime(ZonedDateTime.now())
                        .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate().withStartDate(LocalDate.now()).build())
                        .build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(sendNotificationForApplication);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendNotificationForApplication.class)).thenReturn(sendNotificationForApplication);


        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, times(1)).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, times(2)).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

    }

    @Test
    public void processDoNotSendNotificationForApplicationWhenCourtHearingRequestIsEmpty() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.send-notification-for-application-initiated");
        SendNotificationForApplication sendNotificationForApplication = sendNotificationForApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .withIsWelshTranslationRequired(false)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(sendNotificationForApplication);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendNotificationForApplication.class)).thenReturn(sendNotificationForApplication);

        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, never()).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

    }

    @Test
    public void raisePublicEventWhenWelshTranslationRequired() {
        final UUID masterDefendantId = randomUUID();
        final String applicationReference = STRING.next();
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.send-notification-for-application-initiated");
        SendNotificationForApplication sendNotificationForApplication = sendNotificationForApplication()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(applicationReference)
                        .withApplicant(courtApplicationParty().withId(masterDefendantId).build())
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(buildMasterDefendant(), buildMasterDefendant()))
                        .build())
                .withIsWelshTranslationRequired(true)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(sendNotificationForApplication);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), SendNotificationForApplication.class)).thenReturn(sendNotificationForApplication);

        courtApplicationProcessor.sendNotificationForApplication(event);
        verify(notificationService, never()).sendNotification(any(), any(), anyBoolean(), any(),any(), any(), any());

        verify(sender).send(envelopeCaptor.capture());

        assertThat(envelopeCaptor.getValue().metadata().name(), is("public.progression.welsh-translation-required"));
        assertThat(envelopeCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.welshTranslationRequired.masterDefendantId", equalTo(masterDefendantId.toString())),
                withJsonPath("$.welshTranslationRequired.defendantName", equalTo("")),
                withJsonPath("$.welshTranslationRequired.caseURN", equalTo(applicationReference))
        )));

    }

    private CourtApplicationParty buildMasterDefendant() {
        return buildMasterDefendant(randomUUID());
    }

    private CourtApplicationParty buildMasterDefendant(UUID defendantId) {
        return courtApplicationParty()
                .withMasterDefendant(masterDefendant()
                        .withCpsDefendantId(randomUUID())
                        .withMasterDefendantId(defendantId)
                        .withDefendantCase(singletonList(DefendantCase.defendantCase()
                                .withCaseId(randomUUID())
                                .withDefendantId(defendantId)
                                .build()))
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                        .withCustody("2 years prison")
                                        .withName("John Smith")
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    public void processCourtApplicationSummonsRejected() {
        //Given
        final UUID applicationId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final CourtApplication courtApplication = courtApplication()
                .withId(applicationId)
                .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(prosecutionCaseId).build()))
                .withType(courtApplicationType().withLinkType(LinkType.FIRST_HEARING).build()).build();
        final List<String> reasons = Lists.newArrayList(randomAlphabetic(20), randomAlphabetic(20));
        final SummonsRejectedOutcome summonsRejectedOutcome = summonsRejectedOutcome()
                .withReasons(reasons)
                .withProsecutorEmailAddress("test@test.com")
                .build();
        final CourtApplicationSummonsRejected courtApplicationSummonsRejected = courtApplicationSummonsRejected()
                .withCourtApplication(courtApplication)
                .withCaseIds(singletonList(prosecutionCaseId))
                .withSummonsRejectedOutcome(summonsRejectedOutcome)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationSummonsRejected);

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-rejected");

        final JsonEnvelope event = envelopeFrom(
                metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationSummonsRejected.class)).thenReturn(courtApplicationSummonsRejected);

        //When
        courtApplicationProcessor.courtApplicationSummonsRejected(event);

        //Then
        verify(sender).send(summonsRejectedEnvelopeCaptor.capture());
        final PublicProgressionCourtApplicationSummonsRejected publicEventPayload = summonsRejectedEnvelopeCaptor.getValue().payload();
        assertThat(publicEventPayload.getId(), is(applicationId));
        assertThat(publicEventPayload.getProsecutionCaseId(), is(prosecutionCaseId));
        assertThat(publicEventPayload.getSummonsRejectedOutcome().getReasons().get(0), is(reasons.get(0)));
        assertThat(publicEventPayload.getSummonsRejectedOutcome().getReasons().get(1), is(reasons.get(1)));
        assertThat(publicEventPayload.getSummonsRejectedOutcome().getProsecutorEmailAddress(), is("test@test.com"));
        assertThat(summonsRejectedEnvelopeCaptor.getValue().metadata().name(), is("public.progression.court-application-summons-rejected"));

        verify(summonsRejectedService).sendSummonsRejectionNotification(event, courtApplication, summonsRejectedOutcome);
    }

    @Test
    public void shouldHandleProcessCourtApplicationChangedEventMessage() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.listed-court-application-changed");

        final JsonEnvelope event = envelopeFrom(metadataBuilder, createObjectBuilder().build());

        //When
        courtApplicationProcessor.processCourtApplicationChanged(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().payload(), notNullValue());
    }

    @Test
    public void shouldHandleProcessCourtApplicationUpdatedEventMessage() {
        //Given
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-updated");

        final JsonEnvelope event = envelopeFrom(metadataBuilder, createObjectBuilder().build());

        //When
        courtApplicationProcessor.processCourtApplicationUpdated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(this.sender, times(1)).send(captor.capture());
        final List<Envelope> envelopes = captor.getAllValues();

        final Envelope firstCommandEvent = envelopes.get(0);

        assertThat(firstCommandEvent.metadata().name(), is("public.progression.court-application-updated"));
        assertThat(firstCommandEvent.payload(), notNullValue());
    }

    @Test
    public void shouldCallBoxWorkWhenCourtApplicationInitiatedWithoutProsecutionCase() throws IOException, JSONException {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-boxwork.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1);

        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                )))).build()));


        //When
        courtApplicationProcessor.processBoxWorkApplication(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("hearing.initiate"));
        assertThat(currentEvents.get(1).metadata().name(), is("public.progression.boxwork-application-referred"));

        String expectedPayload = Resources.toString(getResource("expected.progression.event.application-referred-to-boxwork-without-case.json"), defaultCharset());
        expectedPayload = expectedPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1);

        assertEquals(expectedPayload, currentEvents.get(0).payload().toString(), getCustomComparator());
    }

    @Test
    public void shouldThrowExceptionForBoxWorkApplicationWhenProsecutionCaseNotFound() throws IOException {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String caseId_2 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-boxwork-with-multiple-case.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("CASE_ID_2", caseId_2)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1);

        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                )))).build()));

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_2)))
                .thenReturn(Optional.empty());

        //When
        assertThrows(CaseNotFoundException.class, () -> courtApplicationProcessor.processBoxWorkApplication(event));
    }


    @Test
    public void shouldNotThrowExceptionForStandaloneBoxWorkApplicationWhenProsecutionCaseNotFound() throws IOException {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String caseId_2 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.standalone-application-referred-to-boxwork.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("CASE_ID_2", caseId_2)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1);

        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                )))).build()));

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_2)))
                .thenReturn(Optional.empty());

        //When
        courtApplicationProcessor.processBoxWorkApplication(event);
    }

    @Test
    public void shouldCallBoxWorkWhenCourtApplicationInitiated() throws IOException, JSONException {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();
        final String OFFENCE_ID_1 = randomUUID().toString();
        final String OFFENCE_ID_2 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-boxwork.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1);

        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_1))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_2).add("proceedingsConcluded", false)))
                        ))
                ).build()));

        //When
        courtApplicationProcessor.processBoxWorkApplication(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("hearing.initiate"));
        assertThat(currentEvents.get(1).metadata().name(), is("public.progression.boxwork-application-referred"));

        String expectedPayload = Resources.toString(getResource("expected.progression.event.application-referred-to-boxwork.json"), defaultCharset());
        expectedPayload = expectedPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1)
                .replace("OFFENCE_ID_1", OFFENCE_ID_1)
                .replace("OFFENCE_ID_2", OFFENCE_ID_2);

        assertEquals(expectedPayload, currentEvents.get(0).payload().toString(), getCustomComparator());


    }

    @Test
    public void shouldPickupMatchingDefendantFromApplicantIfAnyInCourtApplicationHearingWhenApplicationReferredToCourtHearing() throws IOException, JSONException {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();
        final String OFFENCE_ID_1 = randomUUID().toString();
        final String OFFENCE_ID_2 = randomUUID().toString();

        final String masterDefendantId2 = randomUUID().toString();
        final String OFFENCE_ID_3 = randomUUID().toString();
        final String OFFENCE_ID_4 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-boxwork-with-applicant-as-defendant.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1);
        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_1))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_2).add("proceedingsConcluded", false)))
                        ).add(createObjectBuilder().add("masterDefendantId", masterDefendantId2)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_3))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_4).add("proceedingsConcluded", false)))
                        ))

                ).build()));

        //When
        courtApplicationProcessor.processBoxWorkApplication(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("hearing.initiate"));
        assertThat(currentEvents.get(1).metadata().name(), is("public.progression.boxwork-application-referred"));

        String expectedPayload = Resources.toString(getResource("expected.progression.event.application-referred-to-boxwork-with-applicant-as-defendant.json"), defaultCharset());
        expectedPayload = expectedPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1)
                .replace("OFFENCE_ID_1", OFFENCE_ID_1)
                .replace("OFFENCE_ID_2", OFFENCE_ID_2);


        assertEquals(expectedPayload, currentEvents.get(0).payload().toString(), getCustomComparator());

    }

    @Test
    public void shouldPickupMatchingDefendantFromRespondentsIfAnyInCourtApplicationHearingWhenApplicationReferredToCourtHearing() throws IOException, JSONException {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();
        final String OFFENCE_ID_1 = randomUUID().toString();
        final String OFFENCE_ID_2 = randomUUID().toString();

        final String masterDefendantId2 = randomUUID().toString();
        final String masterDefendantId3 = randomUUID().toString();
        final String masterDefendantId4 = randomUUID().toString();

        final String OFFENCE_ID_3 = randomUUID().toString();
        final String OFFENCE_ID_4 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-boxwork-with-respondent-as-defendant.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID_1", masterDefendantId1)
                .replace("MASTER_DEFENDANT_ID_2", masterDefendantId2)
                .replace("MASTER_DEFENDANT_ID_3", masterDefendantId3);
        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId3)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_1))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_2).add("proceedingsConcluded", false)))
                        ).add(createObjectBuilder().add("masterDefendantId", masterDefendantId4)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_3))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_4).add("proceedingsConcluded", false)))
                        ))

                ).build()));

        //When
        courtApplicationProcessor.processBoxWorkApplication(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("hearing.initiate"));
        assertThat(currentEvents.get(1).metadata().name(), is("public.progression.boxwork-application-referred"));

        String expectedPayload = Resources.toString(getResource("expected.progression.event.application-referred-to-boxwork-with-respondent-as-defendant.json"), defaultCharset());
        expectedPayload = expectedPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID_1", masterDefendantId1)
                .replace("MASTER_DEFENDANT_ID_2", masterDefendantId2)
                .replace("MASTER_DEFENDANT_ID_3", masterDefendantId3)
                .replace("MASTER_DEFENDANT_ID_4", masterDefendantId4)
                .replace("OFFENCE_ID_1", OFFENCE_ID_1)
                .replace("OFFENCE_ID_2", OFFENCE_ID_2);


        assertEquals(expectedPayload, currentEvents.get(0).payload().toString(), getCustomComparator());

    }

    @Test
    public void shouldPickupMatchingDefendantFromRespondentsAndApplicantIfAnyInCourtApplicationHearingWhenApplicationReferredToCourtHearing() throws IOException  {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();
        final String OFFENCE_ID_1 = randomUUID().toString();
        final String OFFENCE_ID_2 = randomUUID().toString();

        final String masterDefendantId2 = randomUUID().toString();
        final String masterDefendantId3 = randomUUID().toString();
        final String masterDefendantId4 = randomUUID().toString();

        final String OFFENCE_ID_3 = randomUUID().toString();
        final String OFFENCE_ID_4 = randomUUID().toString();

        final String OFFENCE_ID_5 = randomUUID().toString();
        final String OFFENCE_ID_6 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-boxwork-with-respondent-and-applicant-as-defendant.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID_1", masterDefendantId1)
                .replace("MASTER_DEFENDANT_ID_2", masterDefendantId2)
                .replace("MASTER_DEFENDANT_ID_3", masterDefendantId3)
                .replace("MASTER_DEFENDANT_ID_4", masterDefendantId4);
        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_1))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_2).add("proceedingsConcluded", false)))
                        ).add(createObjectBuilder().add("masterDefendantId", masterDefendantId2)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_3))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_4).add("proceedingsConcluded", false)))
                        ).add(createObjectBuilder().add("masterDefendantId", masterDefendantId3)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_5))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_6).add("proceedingsConcluded", false)))
                        ))

                ).build()));

        //When
        courtApplicationProcessor.processBoxWorkApplication(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("hearing.initiate"));
        assertThat(currentEvents.get(1).metadata().name(), is("public.progression.boxwork-application-referred"));
        assertThat(currentEvents.get(0).payload().toString(), isJson(anyOf( withJsonPath("$.hearing.prosecutionCases[0].id", equalTo(caseId_1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[*].masterDefendantId", equalTo(masterDefendantId1.toString())),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[*].masterDefendantId", equalTo(masterDefendantId2.toString())),
                withoutJsonPath("$.hearing.prosecutionCases[0].defendants[2].masterDefendantId"))));

    }

    @Test
    public void shouldPickupAllDefendantsFromCaseIfNonMatchingFromApplicantAndRespondentsInCourtApplicationHearingWhenApplicationReferredToCourtHearing() throws IOException  {
        //Given
        final UUID applicationId = randomUUID();

        final String caseId_1 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();
        final String OFFENCE_ID_1 = randomUUID().toString();
        final String OFFENCE_ID_2 = randomUUID().toString();

        final String masterDefendantId2 = randomUUID().toString();
        final String masterDefendantId3 = randomUUID().toString();
        final String masterDefendantId4 = randomUUID().toString();

        final String OFFENCE_ID_3 = randomUUID().toString();
        final String OFFENCE_ID_4 = randomUUID().toString();

        final String OFFENCE_ID_5 = randomUUID().toString();
        final String OFFENCE_ID_6 = randomUUID().toString();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-boxwork");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-boxwork-with-neither-respondent-nor-applicant-as-defendant.json"), defaultCharset());
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID_4", masterDefendantId4);
        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId_1)
                        .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_1))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_2).add("proceedingsConcluded", false)))
                        ).add(createObjectBuilder().add("masterDefendantId", masterDefendantId2)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_3))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_4).add("proceedingsConcluded", false)))
                        ).add(createObjectBuilder().add("masterDefendantId", masterDefendantId3)
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_5))
                                        .add(createObjectBuilder().add("id", OFFENCE_ID_6).add("proceedingsConcluded", false)))
                        ))

                ).build()));

        //When
        courtApplicationProcessor.processBoxWorkApplication(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("hearing.initiate"));
        assertThat(currentEvents.get(1).metadata().name(), is("public.progression.boxwork-application-referred"));
        assertThat(currentEvents.get(0).payload().toString(), isJson(anyOf( withJsonPath("$.hearing.prosecutionCases[0].id", equalTo(caseId_1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[*].masterDefendantId", equalTo(masterDefendantId1.toString())),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[*].masterDefendantId", equalTo(masterDefendantId2.toString())),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[*].masterDefendantId", equalTo(masterDefendantId3.toString())))));

    }


    @Test
    public void shouldProcessEventWhenApplicationReferredToCourtHearing() throws IOException, JSONException {

        final UUID applicationId = randomUUID();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referred-to-court-hearing");

        String inputPayload = Resources.toString(getResource("progression.event.application-referred-to-court-hearing.json"), defaultCharset());
        final String caseId_1 = randomUUID().toString();
        final String masterDefendantId1 = randomUUID().toString();
        final String OFFENCE_ID_1 = randomUUID().toString();
        final String OFFENCE_ID_2 = randomUUID().toString();
        inputPayload = inputPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replaceAll("RANDOM_ARN", STRING.next())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1);

        final JsonObject payload = stringToJsonObjectConverter.convert(inputPayload);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_1)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase",
                        createObjectBuilder().add("id", caseId_1)
                                .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId1)
                                        .add("offences", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                                .add(createObjectBuilder().add("id", OFFENCE_ID_1))
                                                .add(createObjectBuilder().add("id", OFFENCE_ID_2).add("proceedingsConcluded", false)))
                                ))).build()));



        //When
        courtApplicationProcessor.processCourtApplicationReferredToCourtHearing(event);

        //Then
        final ArgumentCaptor<ListCourtHearing> captor = forClass(ListCourtHearing.class);
        verify(listingService).listCourtHearing(any(), captor.capture());
        final ListCourtHearing expectedListCourtHearing = captor.getValue();
        String expectedPayload = Resources.toString(getResource("expected.progression.event.application-referred-to-court-hearing.json"), defaultCharset());
        expectedPayload = expectedPayload.replaceAll("RANDOM_APP_ID", applicationId.toString())
                .replace("CASE_ID_1", caseId_1)
                .replace("MASTER_DEFENDANT_ID", masterDefendantId1)
                .replace("OFFENCE_ID_1", OFFENCE_ID_1)
                .replace("OFFENCE_ID_2", OFFENCE_ID_2);

        assertEquals(expectedPayload, objectToJsonObjectConverter.convert(expectedListCourtHearing).toString(), new CustomComparator(STRICT,
                new Customization("hearings[0].id", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearings[0].courtApplications[0].id", (o1, o2) -> o1 != null && o2 != null)

        ));

        final ArgumentCaptor<Hearing> hearingArgumentCaptor = forClass(Hearing.class);
        verify(progressionService).linkApplicationToHearing(any(JsonEnvelope.class),
                hearingArgumentCaptor.capture(), eq(HearingListingStatus.HEARING_INITIALISED));
        final Hearing hearingArgumentCaptorValue = hearingArgumentCaptor.getValue();
        assertThat(hearingArgumentCaptorValue.getProsecutionCases().get(0).getId().toString(), is(caseId_1));

        verify(progressionService).updateHearingListingStatusToHearingInitiated(any(JsonEnvelope.class), any(Initiate.class));
    }

    @Test
    public void shouldProcessEventWhenApplicationReferredToExistingHearing() {
        final Function<Object, JsonEnvelope> enveloperFunction = mock(Function.class);
        final JsonEnvelope finalEnvelope = mock(JsonEnvelope.class);

        final UUID hearingId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referral-to-existing-hearing");
        final UUID caseId_1 = randomUUID();
        final UUID caseId_2 = randomUUID();
        final ApplicationReferredToExistingHearing applicationReferredToExistingHearing = applicationReferredToExistingHearing()
                .withApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withType(courtApplicationType()
                                .withBreachType(BreachType.COMMISSION_OF_NEW_OFFENCE_BREACH)
                                .build())
                        .withCourtApplicationCases(Lists.newArrayList(courtApplicationCase()
                                        .withProsecutionCaseId(caseId_1)
                                        .withCaseStatus("ACTIVE")
                                        .build(),
                                courtApplicationCase()
                                        .withProsecutionCaseId(caseId_2)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                        .withId(caseId_2)
                        .build())
                .withCourtHearing(courtHearingRequest().withId(hearingId).build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(applicationReferredToExistingHearing);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationReferredToExistingHearing.class)).thenReturn(applicationReferredToExistingHearing);

        courtApplicationProcessor.processCourtApplicationReferredToExistingHearing(event);
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("public.progression.events.hearing-extended"));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).containsKey("prosecutionCases"), is(false));

        verify(progressionService, never()).linkApplicationsToHearing(any(JsonEnvelope.class),
                any(Hearing.class), any(List.class), eq(HearingListingStatus.SENT_FOR_LISTING));

        verify(progressionService).populateHearingToProbationCaseworker(event, hearingId);
    }

    @Test
    public void shouldProcessEventWhenApplicationReferredToExistingHearingGenericBreach() {
        final Function<Object, JsonEnvelope> enveloperFunction = mock(Function.class);
        final JsonEnvelope finalEnvelope = mock(JsonEnvelope.class);

        final UUID hearingId = randomUUID();
        final UUID caseId_1 = randomUUID();
        final UUID caseId_2 = randomUUID();
        final UUID applicantId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final String OFFENCE_ID_1 = randomUUID().toString();
        final String OFFENCE_ID_2 = randomUUID().toString();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-referral-to-existing-hearing");
        final CourtApplicationParty masterDefendant = courtApplicationParty().withId(applicantId).withMasterDefendant(masterDefendant().withMasterDefendantId(masterDefendantId).build()).build();
        final ApplicationReferredToExistingHearing applicationReferredToExistingHearing = applicationReferredToExistingHearing()
                .withApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withType(courtApplicationType()
                                .withBreachType(BreachType.GENERIC_BREACH)
                                .build())
                        .withApplicant(masterDefendant)
                        .withCourtApplicationCases(Lists.newArrayList(courtApplicationCase()
                                        .withProsecutionCaseId(caseId_1)
                                        .withCaseStatus("ACTIVE")
                                        .build(),
                                courtApplicationCase()
                                        .withProsecutionCaseId(caseId_2)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                        .withId(randomUUID())
                        .withSubject(masterDefendant)
                        .build())
                .withCourtHearing(courtHearingRequest().withId(hearingId).build())
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(applicationReferredToExistingHearing);

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationReferredToExistingHearing.class)).thenReturn(applicationReferredToExistingHearing);
        when(progressionService.getHearing(event, hearingId.toString())).thenReturn(Optional.of(createObjectBuilder().add("hearing", createObjectBuilder().
                add("prosecutionCases", Json.createArrayBuilder().add(createObjectBuilder().add("id", caseId_1.toString()).build()).build()).build()).build()));

        when(progressionService.getProsecutionCaseDetailById(any(JsonEnvelope.class), eq(caseId_2.toString())))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                                .add("id", caseId_2.toString())
                                .add("defendants", createArrayBuilder().add(createObjectBuilder().add("masterDefendantId", masterDefendantId.toString())
                                        .add("offences", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                                .add(createObjectBuilder().add("id", OFFENCE_ID_1))
                                                .add(createObjectBuilder().add("id", OFFENCE_ID_2).add("proceedingsConcluded", false)))
                                )))
                        .build()));

        courtApplicationProcessor.processCourtApplicationReferredToExistingHearing(event);
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("public.progression.events.hearing-extended"));
        assertThat(objectToJsonObjectConverter.convert(currentEvents.get(0).payload()).getJsonArray("prosecutionCases").size(), is(1));

        final ArgumentCaptor<Hearing> hearingArgumentCaptor = forClass(Hearing.class);
        verify(progressionService).linkApplicationsToHearing(any(JsonEnvelope.class),
                hearingArgumentCaptor.capture(), any(List.class), eq(HearingListingStatus.SENT_FOR_LISTING));
        final Hearing hearingArgumentCaptorValue = hearingArgumentCaptor.getValue();
        assertThat(hearingArgumentCaptorValue.getProsecutionCases().size(), is(1));
        assertThat(hearingArgumentCaptorValue.getProsecutionCases().get(0).getId(), is(caseId_2));
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsInitiated() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-initiated");

        final UUID oldApplicationId = randomUUID();
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicant(courtApplicationParty().withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(randomUUID())
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("Address1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .withCourtApplicationCases(Arrays.asList(courtApplicationCase()
                                .withProsecutionCaseId(randomUUID()).build()))
                        .build())
                .withOldApplicationId(oldApplicationId)
                .withIsSJP(false)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().add("prosecutionCase", Json.createObjectBuilder().build
                        ()).build()));

        //When
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class)).thenReturn(courtApplicationProceedingsInitiated);

        courtApplicationProcessor.processCourtApplicationInitiated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("progression.command.create-court-application"));
        assertThat(currentEvents.get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(command.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
        assertThat(currentEvents.get(2).metadata().name(), is("public.progression.court-application-proceedings-initiated"));
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsInitiatedWhenLegalEntityIsApplicant() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-initiated");

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicant(courtApplicationParty().withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(randomUUID())
                                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                                .withOrganisation(Organisation.organisation().withAddress(Address.address()
                                                        .withAddress1("Address1")
                                                        .build()).build())
                                                .build())
                                        .build())
                                .build())
                        .withCourtApplicationCases(Arrays.asList(courtApplicationCase()
                                .withProsecutionCaseId(randomUUID()).build()))
                        .build())
                .withIsSJP(false)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().add("prosecutionCase", Json.createObjectBuilder().build
                        ()).build()));

        //When
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class)).thenReturn(courtApplicationProceedingsInitiated);

        courtApplicationProcessor.processCourtApplicationInitiated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("progression.command.create-court-application"));
        assertThat(currentEvents.get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command1 = objectToJsonObjectConverter.convert(currentEvents.get(1).payload());
        assertThat(command1.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command1.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
        assertThat(currentEvents.get(2).metadata().name(), is("public.progression.court-application-proceedings-initiated"));
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsInitiatedWhenDefendantIsRespondent() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-initiated");

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withRespondents(Arrays.asList(courtApplicationParty().withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(randomUUID())
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("Address1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build()))
                        .withCourtApplicationCases(Arrays.asList(courtApplicationCase()
                                .withProsecutionCaseId(randomUUID()).build()))
                        .build())
                .withIsSJP(false)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().add("prosecutionCase", Json.createObjectBuilder().build
                        ()).build()));

        //When
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class)).thenReturn(courtApplicationProceedingsInitiated);

        courtApplicationProcessor.processCourtApplicationInitiated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("progression.command.create-court-application"));
        assertThat(currentEvents.get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(command.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
        assertThat(currentEvents.get(2).metadata().name(), is("public.progression.court-application-proceedings-initiated"));
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsInitiatedWhenApplicantAndRespondentArePresent() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-initiated");
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicant(courtApplicationParty().withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(masterDefendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("Address1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .withRespondents(Arrays.asList(courtApplicationParty().withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(randomUUID())
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("Address2")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build(),
                                courtApplicationParty().withMasterDefendant(masterDefendant()
                                                .withMasterDefendantId(randomUUID())
                                                .withPersonDefendant(PersonDefendant.personDefendant()
                                                        .withPersonDetails(Person.person()
                                                                .withAddress(Address.address()
                                                                        .withAddress1("Address3")
                                                                        .build())
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()))
                        .withCourtApplicationCases(Arrays.asList(courtApplicationCase()
                                .withProsecutionCaseId(caseId).build()))
                        .build())
                .withIsSJP(false)
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCase(any(JsonEnvelope.class), eq(caseId.toString())))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId.toString())
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                .add("masterDefendantId", masterDefendantId.toString())
                                .add("id", masterDefendantId.toString())
                                .add("personDefendant",createObjectBuilder().add("personDetails",
                                                createObjectBuilder().add("address",createObjectBuilder().add("address1","old address")
                                                        .add("postcode","RG1 7DS").build()).build()))
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                )))).build()));

        //When
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class)).thenReturn(courtApplicationProceedingsInitiated);

        courtApplicationProcessor.processCourtApplicationInitiated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(5)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("progression.command.create-court-application"));
        assertThat(currentEvents.get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command1 = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(command1.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command1.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
        assertThat(currentEvents.get(2).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command2 = objectToJsonObjectConverter.convert(captor.getAllValues().get(2).payload());
        assertThat(command2.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command2.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
        assertThat(currentEvents.get(3).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command3 = objectToJsonObjectConverter.convert(captor.getAllValues().get(3).payload());
        assertThat(command3.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command3.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
        assertThat(currentEvents.get(4).metadata().name(), is("public.progression.court-application-proceedings-initiated"));
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsInitiatedWithSjpCase() {
        //Given
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-initiated");

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicant(courtApplicationParty().withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(masterDefendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("Application Address1")
                                                                .withPostcode("RG1 8XD")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .withCourtApplicationCases(Arrays.asList(courtApplicationCase()
                                .withProsecutionCaseId(caseId)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withCaseURN(STRING.next())
                                        .build()).build()))
                        .withId(randomUUID())
                        .build())
                .withIsSJP(true)
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class)).thenReturn(courtApplicationProceedingsInitiated);
        //When
        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().build()));
        final ProsecutionCase sjpCase = prosecutionCase()
                .withDefendants(asList(Defendant.defendant().withMasterDefendantId(masterDefendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("address1")
                                                .withPostcode("RG1 7XD")
                                                .build())
                                        .build())
                                .build()).build()))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("prosecutionCaseIdentifier")
                        .build())
                .build();
        when(sjpService.getProsecutionCase(event, caseId)).thenReturn(sjpCase);

        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().add("prosecutionCase", Json.createObjectBuilder().build
                        ()).build()));

        courtApplicationProcessor.processCourtApplicationInitiated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(4)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        final JsonObject command = objectToJsonObjectConverter.convert(currentEvents.get(0).payload());
        assertThat(command.getJsonObject("prosecutionCase")
                .getJsonArray("defendants").getJsonObject(0)
                .getJsonObject("personDefendant").getJsonObject("personDetails")
                .getJsonObject("address").getString("address1"),is("Application Address1"));
        assertThat(currentEvents.get(2).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command2 = objectToJsonObjectConverter.convert(currentEvents.get(2).payload());
        assertThat(command2.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command2.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsWithAlreadyInitiatedSjpCase() {
        //Given
        final UUID caseId = randomUUID();

        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-initiated");

        final CourtApplicationProceedingsInitiated courtApplicationProceedingsInitiated = courtApplicationProceedingsInitiated()
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicant(courtApplicationParty().withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(randomUUID())
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("Address1")
                                                                .build())
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .withCourtApplicationCases(Arrays.asList(courtApplicationCase()
                                .withProsecutionCaseId(caseId)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                        .withCaseURN(STRING.next())
                                        .build()).build()))
                        .withId(randomUUID())
                        .build())
                .withIsSJP(true)
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsInitiated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class)).thenReturn(courtApplicationProceedingsInitiated);
        //When
        when(progressionService.caseExistsByCaseUrn(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().add("caseId", randomUUID().toString()).build()));
        when(sjpService.getProsecutionCase(event, caseId)).thenReturn(prosecutionCase()
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("prosecutionCaseIdentifier")
                        .build())
                .build());

        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().add("prosecutionCase", Json.createObjectBuilder().build
                        ()).build()));

        courtApplicationProcessor.processCourtApplicationInitiated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(3)).send(captor.capture());
        assertThat(captor.getAllValues().get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(command.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
    }

    @Test
    public void shouldProcessCourtApplicationProceedingsWithCortOrders() {
        //Given
        final UUID caseId = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID defendantId = randomUUID();


        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-initiated");
        //
        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("stub-data/progression.event.court-application-proceedings-initiated.json")
                .replaceAll("%CASEID%", caseId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId.toString())
                .replaceAll("%MASTER_DEFENDANT_ID%", masterDefendantId.toString()));

        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.of
                (createObjectBuilder().add("prosecutionCase", Json.createObjectBuilder().build
                        ()).build()));

        courtApplicationProcessor.processCourtApplicationInitiated(event);

        //Then
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(4)).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(command.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
        assertThat(currentEvents.get(2).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command2 = objectToJsonObjectConverter.convert(captor.getAllValues().get(2).payload());
        assertThat(command2.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command2.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));
    }

    @Test
    public void shouldProcessCourtApplicationWithBoxWorkEdited() {
        final UUID caseId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-edited");
        final UUID hearingId = randomUUID();
        final CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = courtApplicationProceedingsEdited()
                .withCourtApplication(courtApplication()
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(caseId).build()))
                        .withId(randomUUID())
                        .build())
                .withBoxHearing(BoxHearingRequest.boxHearingRequest().withId(hearingId).withApplicationDueDate(LocalDate.now()).
                        withCourtCentre(CourtCentre.courtCentre().withName("Lavender Hill").build()).withJurisdictionType(JurisdictionType.CROWN).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsEdited.class)).thenReturn(courtApplicationProceedingsEdited);
        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.empty());
        courtApplicationProcessor.processCourtApplicationEdited(event);
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(captor.getValue().payload());
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessCourtApplicationWithSummonsEdited() {
        final UUID caseId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-edited");
        final UUID boxWorkHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = courtApplicationProceedingsEdited()
                .withCourtApplication(courtApplication()
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(caseId).build()))
                        .withId(randomUUID())
                        .build())
                .withBoxHearing(BoxHearingRequest.boxHearingRequest().withId(boxWorkHearingId).withApplicationDueDate(LocalDate.now()).
                        withCourtCentre(CourtCentre.courtCentre().withName("Lavender Hill").build()).withJurisdictionType(JurisdictionType.CROWN).build())
                .withCourtHearing(courtHearingRequest().withId(hearingId).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsEdited.class)).thenReturn(courtApplicationProceedingsEdited);
        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.empty());
        courtApplicationProcessor.processCourtApplicationEdited(event);
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());

        assertThat(captor.getAllValues().get(0).metadata().name(), is("progression.command.update-court-application-to-hearing"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(0).payload());
        assertThat(command.getString("hearingId"), is(hearingId.toString()));
        assertThat(command.getJsonObject("courtApplication"), is(notNullValue()));

        assertThat(captor.getAllValues().get(1).metadata().name(), is("public.progression.events.hearing-extended"));
        final JsonObject publicEvent = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(publicEvent.getString("hearingId"), is(boxWorkHearingId.toString()));
    }

    @Test
    public void shouldProcessCourtApplicationEdited() {
        final UUID caseId = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-edited");
        final UUID hearingId = randomUUID();
        final CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = courtApplicationProceedingsEdited()
                .withCourtApplication(courtApplication()
                        .withCourtApplicationCases(singletonList(courtApplicationCase().withProsecutionCaseId(caseId).build()))
                        .withId(randomUUID())
                        .build())
                .withCourtHearing(courtHearingRequest().withId(hearingId).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsEdited.class)).thenReturn(courtApplicationProceedingsEdited);
        when(progressionService.getProsecutionCase(any(), any())).thenReturn(Optional.empty());
        courtApplicationProcessor.processCourtApplicationEdited(event);
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());

        assertThat(captor.getAllValues().get(0).metadata().name(), is("progression.command.update-court-application-to-hearing"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(0).payload());
        assertThat(command.getString("hearingId"), is(hearingId.toString()));
        assertThat(command.getJsonObject("courtApplication"), is(notNullValue()));

        assertThat(captor.getAllValues().get(1).metadata().name(), is("public.progression.events.hearing-extended"));
        final JsonObject publicEvent = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(publicEvent.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessHearingResultedApplicationUpdated() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.hearing-resulted-application-updated");
        final UUID applicationId = randomUUID();
        final HearingResultedApplicationUpdated hearingResultedApplicationUpdated = hearingResultedApplicationUpdated()
                .withCourtApplication(courtApplication().withId(applicationId).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingResultedApplicationUpdated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        courtApplicationProcessor.processHearingResultedApplicationUpdated(event);

        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is(PUBLIC_PROGRESSION_HEARING_RESULTED_APPLICATION_UPDATED));
    }

    @Test
    public void shouldProcessHearingResultedApplicationUpdatedWhenResultedWithListHearingForBoxworkAndFixedDateHearingWithNoCourtRoom() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.hearing-resulted-application-updated");
        final UUID applicationId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withListedStartDateTime(ZonedDateTime.now()).withCourtCentre(CourtCentre.courtCentre().build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();
        final HearingResultedApplicationUpdated hearingResultedApplicationUpdated = hearingResultedApplicationUpdated()
                .withCourtApplication(courtApplication().withId(applicationId).withJudicialResults(singletonList(JudicialResult.judicialResult()
                        .withJudicialResultTypeId(ListHearingBoxworkService.LHBW_RESULT_DEFINITION)
                        .withNextHearing(nextHearing)
                        .build())).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingResultedApplicationUpdated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        when(listHearingBoxworkService.isLHBWResultedAndNeedToSendNotifications(any())).thenReturn(true);
        when(listHearingBoxworkService.getNextHearingFromLHBWResult(any())).thenReturn(nextHearing);

        courtApplicationProcessor.processHearingResultedApplicationUpdated(event);

        notificationService.sendNotification(event, hearingResultedApplicationUpdated.getCourtApplication(), false, nextHearing.getCourtCentre(), nextHearing.getListedStartDateTime(), nextHearing.getJurisdictionType(), false);
        verify(sender).send(any());
    }

    @Test
    public void shouldProcessHearingResultedApplicationUpdatedWhenResultedWithListHearingForBoxworkAndWeekCommencingDateHearing() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.hearing-resulted-application-updated");
        final UUID applicationId = randomUUID();
        final NextHearing nextHearing = NextHearing.nextHearing().withWeekCommencingDate(LocalDate.now())
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .build();
        final HearingResultedApplicationUpdated hearingResultedApplicationUpdated = hearingResultedApplicationUpdated()
                .withCourtApplication(courtApplication().withId(applicationId).withJudicialResults(singletonList(JudicialResult.judicialResult()
                        .withJudicialResultTypeId(ListHearingBoxworkService.LHBW_RESULT_DEFINITION)
                        .withNextHearing(nextHearing)
                        .build())).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(hearingResultedApplicationUpdated);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        when(listHearingBoxworkService.isLHBWResultedAndNeedToSendNotifications(hearingResultedApplicationUpdated.getCourtApplication().getJudicialResults())).thenReturn(true);
        when(listHearingBoxworkService.getNextHearingFromLHBWResult(hearingResultedApplicationUpdated.getCourtApplication().getJudicialResults())).thenReturn(nextHearing);

        courtApplicationProcessor.processHearingResultedApplicationUpdated(event);

        notificationService.sendNotification(event, hearingResultedApplicationUpdated.getCourtApplication(), false, nextHearing.getCourtCentre(), nextHearing.getWeekCommencingDate().atStartOfDay(ZoneOffset.UTC), nextHearing.getJurisdictionType(), false);
        verify(sender).send(any());
    }


    @Test
    public void shouldProcessBreachApplicationsTobeAddedToHearing() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.breach-applications-to-be-added-to-hearing");

        final BreachApplicationsToBeAddedToHearing breachApplicationsToBeAddedToHearing = new BreachApplicationsToBeAddedToHearing(asList(randomUUID()), randomUUID());
        final JsonObject payload = objectToJsonObjectConverter.convert(breachApplicationsToBeAddedToHearing);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);

        courtApplicationProcessor.processBreachApplicationsTobeAddedToHearing(event);

        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is(PUBLIC_PROGRESSION_EVENTS_BREACH_APPLICATIONS_TO_BE_ADDED_TO_HEARING));
    }

    @Test
    public void shouldProcessHearingBreachedApplicationAdded() {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.breach-application-creation-requested");
        final UUID hearingId = UUID.fromString("d89679d9-4179-4783-9646-5036a420caca");
        final UUID masterDefendantId = UUID.fromString("af2d2a3f-949b-4625-aaff-18c32dd7f7ee");
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withProsecutionCases(singletonList(prosecutionCase()
                        .withId(caseId)
                        .withDefendants(Arrays.asList(Defendant.defendant()
                                .withId(masterDefendantId)
                                .withProsecutionCaseId(caseId)
                                .withMasterDefendantId(masterDefendantId)
                                .withOffences(Arrays.asList(Offence.offence()
                                        .withId(offenceId)
                                        .build()))
                                .build(), Defendant.defendant()
                                .withMasterDefendantId(randomUUID())
                                .withOffences(Arrays.asList(Offence.offence()
                                        .withId(randomUUID())
                                        .build()))
                                .build()))
                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                .withProsecutionAuthorityCode(STRING.next())
                                .withProsecutionAuthorityId(randomUUID())
                                .withProsecutionAuthorityReference(STRING.next())
                                .build())
                        .build()))
                .build();
        final BreachApplicationCreationRequested breachApplicationCreationRequested = BreachApplicationCreationRequested.breachApplicationCreationRequested()
                .withHearingId(hearingId)
                .withBreachedApplications(BreachedApplications.breachedApplications().withApplicationType(CourtApplicationType.courtApplicationType()
                        .withBreachType(BreachType.COMMISSION_OF_NEW_OFFENCE_BREACH).build()).build())
                .withMasterDefendantId(masterDefendantId)
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(breachApplicationCreationRequested);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        final JsonObject jsonObjectHearing = createObjectBuilder().add("hearing", objectToJsonObjectConverter.convert(hearing)).build();
        final Optional<JsonObject> optionalJsonObject = Optional.of(jsonObjectHearing);

        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), BreachApplicationCreationRequested.class)).thenReturn(breachApplicationCreationRequested);
        when(progressionService.getHearing(any(JsonEnvelope.class), any(String.class))).thenReturn(optionalJsonObject);

        courtApplicationProcessor.processBreachApplicationCreationRequested(event);

        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final JsonObject actual = objectToJsonObjectConverter.convert(captor.getValue().payload());
        assertThat(actual.getJsonObject("courtApplication"), Matchers.notNullValue());
        assertThat(actual.getJsonObject("courtApplication").getJsonArray("respondents").getJsonObject(0).getJsonObject("masterDefendant").getJsonArray("defendantCase").getJsonObject(0), Matchers.notNullValue());
        assertThat(actual.getJsonObject("courtHearing"), Matchers.notNullValue());
        assertThat(actual.getJsonObject("courtApplication").getJsonArray("courtApplicationCases"), Matchers.notNullValue());
        assertThat(actual.getJsonObject("courtApplication").getJsonArray("courtApplicationCases").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("id"), is(offenceId.toString()));
        assertThat(actual.getJsonObject("courtApplication").getJsonArray("courtApplicationCases").getJsonObject(0).getString("prosecutionCaseId"), is(caseId.toString()));

    }

    @MethodSource("applicationSummonsSpecification")
    @ParameterizedTest
    public void shouldTestInitiateCourtHearingAfterSummonsApproved(final SummonsTemplateType summonsTemplateType, final SummonsType summonsRequired) {
        final UUID masterDefendantId = randomUUID();
        final InitiateCourtHearingAfterSummonsApproved eventPayload = initiateCourtHearingAfterSummonsApproved()
                .withCourtHearing(courtHearingRequest().withId(randomUUID()).withBookingType("Video").withPriority("High").withSpecialRequirements(Arrays.asList("RSZ", "CELL")).build())
                .withApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicant(courtApplicationParty().withMasterDefendant(masterDefendant().withMasterDefendantId(masterDefendantId).build()).build())
                        .withType(courtApplicationType().withSummonsTemplateType(summonsTemplateType).build())
                        .withSubject(courtApplicationParty().withMasterDefendant(masterDefendant().withMasterDefendantId(masterDefendantId).build()).build())
                        .build())
                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                        .withPersonalService(BOOLEAN.next())
                        .withSummonsSuppressed(BOOLEAN.next())
                        .withProsecutorCost(randomAlphanumeric(5))
                        .withProsecutorEmailAddress(EMAIL_ADDRESS.next())
                        .build())
                .build();

        final JsonEnvelope envelope = envelopeFrom(getMetadata("progression.event.initiate-court-hearing-after-summons-approved"), objectToJsonObjectConverter.convert(eventPayload));

        courtApplicationProcessor.initiateCourtHearingAfterSummonsApproved(envelope);

        verify(summonsHearingRequestService).addApplicationRequestToHearing(eq(envelope), captorCreateHearingApplicationRequest.capture());
        final CreateHearingApplicationRequest createHearingApplicationRequestValue = captorCreateHearingApplicationRequest.getValue();
        assertThat(createHearingApplicationRequestValue.getHearingId(), is(eventPayload.getCourtHearing().getId()));
        assertThat(createHearingApplicationRequestValue.getApplicationRequests(), hasSize(1));
        final CourtApplicationPartyListingNeeds applicationSubjectNeeds = createHearingApplicationRequestValue.getApplicationRequests().get(0);
        assertThat(applicationSubjectNeeds.getCourtApplicationId(), is(eventPayload.getApplication().getId()));
        assertThat(applicationSubjectNeeds.getCourtApplicationPartyId(), is(eventPayload.getApplication().getSubject().getId()));
        assertThat(applicationSubjectNeeds.getSummonsRequired(), is(summonsRequired));
        assertThat(applicationSubjectNeeds.getSummonsApprovedOutcome().getPersonalService(), is(eventPayload.getSummonsApprovedOutcome().getPersonalService()));
        assertThat(applicationSubjectNeeds.getSummonsApprovedOutcome().getSummonsSuppressed(), is(eventPayload.getSummonsApprovedOutcome().getSummonsSuppressed()));
        assertThat(applicationSubjectNeeds.getSummonsApprovedOutcome().getProsecutorCost(), is(eventPayload.getSummonsApprovedOutcome().getProsecutorCost()));
        assertThat(applicationSubjectNeeds.getSummonsApprovedOutcome().getProsecutorEmailAddress(), is(eventPayload.getSummonsApprovedOutcome().getProsecutorEmailAddress()));
    }

    @Test
    public void shouldProcessCourtApplicationEdited_addressUpdatedOnApplication() {
        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final UUID masterDefendantId = randomUUID();
        final UUID masterDefendantId2 = randomUUID();
        final UUID defendantId1 = masterDefendantId;
        final UUID defendantId2 = masterDefendantId2;
        final UUID defendantId3 = randomUUID();
        final UUID defendantId4 = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-edited");
        final UUID hearingId = randomUUID();
        final CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = courtApplicationProceedingsEdited()
                .withCourtApplication(courtApplication()
                        .withCourtApplicationCases(Arrays.asList(courtApplicationCase().withProsecutionCaseId(caseId1).build(),courtApplicationCase().withProsecutionCaseId(caseId2).build()))
                        .withApplicant(courtApplicationParty()
                                .withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(masterDefendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("address1")
                                                                .withAddress2("address2")
                                                                .withPostcode("RG1 7SA").build()).build()).build()).build()).build())
                        .withRespondents(asList(courtApplicationParty()
                                .withMasterDefendant(masterDefendant().withMasterDefendantId(masterDefendantId2)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address().withAddress1("Resp Adr 1")
                                                                .withAddress2("Resp Addr 2")
                                                                .withPostcode("RG23 8GH").build()).build()).build()).build()).build()))
                        .withId(randomUUID())
                        .build())
                .withCourtHearing(courtHearingRequest().withId(hearingId).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsEdited.class)).thenReturn(courtApplicationProceedingsEdited);
        when(progressionService.getProsecutionCase(any(JsonEnvelope.class), eq(caseId1.toString())))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId1.toString())
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                        .add("masterDefendantId", masterDefendantId2.toString())
                                        .add("id", defendantId2.toString())
                                        .add("offences", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        ))
                                .add(createObjectBuilder()
                                        .add("masterDefendantId", masterDefendantId.toString())
                                        .add("id", defendantId1.toString())
                                        .add("offences", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        )))).build()));
        when(progressionService.getProsecutionCase(any(JsonEnvelope.class), eq(caseId2.toString())))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId2.toString())
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                .add("masterDefendantId", masterDefendantId2.toString())
                                .add("id", defendantId3.toString())
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                ))
                                .add(createObjectBuilder()
                                        .add("masterDefendantId", masterDefendantId.toString())
                                        .add("id", defendantId4.toString())
                                        .add("offences", createArrayBuilder()
                                                .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                        )))).build()));
        courtApplicationProcessor.processCourtApplicationEdited(event);
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(6)).send(captor.capture());

        assertThat(captor.getAllValues().get(0).metadata().name(), is("progression.command.update-court-application-to-hearing"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(0).payload());
        assertThat(command.getString("hearingId"), is(hearingId.toString()));
        assertThat(command.getJsonObject("courtApplication"), is(notNullValue()));

        assertThat(captor.getAllValues().get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command1 = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(command1.getString("prosecutionCaseId"), is(caseId1.toString()));
        assertThat(command1.getJsonObject("defendant").getString("id").toString(), is(defendantId1.toString()));

        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command2 = objectToJsonObjectConverter.convert(captor.getAllValues().get(2).payload());
        assertThat(command2.getString("prosecutionCaseId"), is(caseId1.toString()));
        assertThat(command2.getJsonObject("defendant").getString("id").toString(), is(defendantId2.toString()));

        assertThat(captor.getAllValues().get(3).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command3 = objectToJsonObjectConverter.convert(captor.getAllValues().get(3).payload());
        assertThat(command3.getString("prosecutionCaseId"), is(caseId2.toString()));
        assertThat(command3.getJsonObject("defendant").getString("id").toString(), is(defendantId4.toString()));

        assertThat(captor.getAllValues().get(4).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command4 = objectToJsonObjectConverter.convert(captor.getAllValues().get(4).payload());
        assertThat(command4.getString("prosecutionCaseId"), is(caseId2.toString()));
        assertThat(command4.getJsonObject("defendant").getString("id").toString(), is(defendantId3.toString()));

        assertThat(captor.getAllValues().get(5).metadata().name(), is("public.progression.events.hearing-extended"));
        final JsonObject publicEvent = objectToJsonObjectConverter.convert(captor.getAllValues().get(5).payload());
        assertThat(publicEvent.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldProcessCourtApplicationEdited_addressUpdatedOnApplicationForCourtOrder() {
        final UUID caseId1 = fromString("e46a2666-dfad-40a1-8038-8d42a8397c82");
        final UUID caseId2 = fromString("ed4b202b-5be8-41f6-aaf2-5281b71f2a8d");
        final UUID masterDefendantId = randomUUID();
        final UUID masterDefendantId2 = randomUUID();
        final UUID id1 = randomUUID();
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.court-application-proceedings-edited");
        final UUID hearingId = randomUUID();
        final CourtApplicationProceedingsEdited courtApplicationProceedingsEdited = courtApplicationProceedingsEdited()
                .withCourtApplication(courtApplication()
                        .withCourtOrder(CourtOrder.courtOrder()
                                .withCourtOrderOffences(Arrays.asList(courtOrderOffence()
                                        .withProsecutionCaseId(caseId1)
                                        .build(), courtOrderOffence()
                                        .withProsecutionCaseId(caseId2)
                                        .build()))
                                .build())
                        .withApplicant(courtApplicationParty()
                                .withMasterDefendant(masterDefendant()
                                        .withMasterDefendantId(masterDefendantId)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address()
                                                                .withAddress1("address1")
                                                                .withAddress2("address2")
                                                                .withPostcode("RG1 7SA").build()).build()).build()).build()).build())
                        .withRespondents(asList(courtApplicationParty()
                                .withMasterDefendant(masterDefendant().withMasterDefendantId(masterDefendantId2)
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withAddress(Address.address().withAddress1("Resp Adr 1")
                                                                .withAddress2("Resp Addr 2")
                                                                .withPostcode("RG23 8GH").build()).build()).build()).build()).build()))
                        .withId(randomUUID())
                        .build())
                .withCourtHearing(courtHearingRequest().withId(hearingId).build())
                .build();
        final JsonObject payload = objectToJsonObjectConverter.convert(courtApplicationProceedingsEdited);
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        when(jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsEdited.class)).thenReturn(courtApplicationProceedingsEdited);
        when(progressionService.getProsecutionCase(any(JsonEnvelope.class), eq(caseId1.toString())))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId1.toString())
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                .add("masterDefendantId", masterDefendantId2.toString())
                                .add("id", id1.toString())
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                )))).build()));
        when(progressionService.getProsecutionCase(any(JsonEnvelope.class), eq(caseId2.toString())))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", createObjectBuilder().add("id", caseId2.toString())
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                .add("masterDefendantId", masterDefendantId2.toString())
                                .add("id", masterDefendantId2.toString())
                                .add("offences", createArrayBuilder()
                                        .add(createObjectBuilder().add("id", randomUUID().toString()).add("proceedingsConcluded", true))
                                )))).build()));
        courtApplicationProcessor.processCourtApplicationEdited(event);
        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(6)).send(captor.capture());

        assertThat(captor.getAllValues().get(0).metadata().name(), is("progression.command.update-court-application-to-hearing"));
        final JsonObject command = objectToJsonObjectConverter.convert(captor.getAllValues().get(0).payload());
        assertThat(command.getString("hearingId"), is(hearingId.toString()));
        assertThat(command.getJsonObject("courtApplication"), is(notNullValue()));

        assertThat(captor.getAllValues().get(1).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command1 = objectToJsonObjectConverter.convert(captor.getAllValues().get(1).payload());
        assertThat(command1.getString("prosecutionCaseId"), is(caseId1.toString()));
        assertThat(command1.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command1.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));

        assertThat(captor.getAllValues().get(2).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command2 = objectToJsonObjectConverter.convert(captor.getAllValues().get(2).payload());
        assertThat(command2.getString("prosecutionCaseId"), is(caseId1.toString()));
        assertThat(command2.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command2.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));

        assertThat(captor.getAllValues().get(3).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command3 = objectToJsonObjectConverter.convert(captor.getAllValues().get(3).payload());
        assertThat(command3.getString("prosecutionCaseId"), is(caseId2.toString()));
        assertThat(command3.getJsonObject("defendant").getString("id"), is(notNullValue()));
        assertThat(command3.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));


        assertThat(captor.getAllValues().get(4).metadata().name(), is("progression.command.update-defendant-address-on-case"));
        final JsonObject command4 = objectToJsonObjectConverter.convert(captor.getAllValues().get(4).payload());
        assertThat(command4.getString("prosecutionCaseId"), is(caseId2.toString()));
        assertThat(command4.getJsonObject("defendant").getString("id"), is(masterDefendantId2.toString()));
        assertThat(command4.getJsonObject("defendant").getString("masterDefendantId"), is(notNullValue()));

        assertThat(captor.getAllValues().get(5).metadata().name(), is("public.progression.events.hearing-extended"));
        final JsonObject publicEvent = objectToJsonObjectConverter.convert(captor.getAllValues().get(5).payload());
        assertThat(publicEvent.getString("hearingId"), is(hearingId.toString()));
    }


    @Test
    public void shouldProcessApplicationDefendantUpdateRequested() throws IOException {
        final MetadataBuilder metadataBuilder = getMetadata("progression.event.application-defendant-update-requested");
        final JsonObject payload = getJsonPayload("progression.event.application-defendant-update-requested.json");
        final JsonEnvelope event = envelopeFrom(metadataBuilder, payload);
        courtApplicationProcessor.processApplicationDefendantUpdateRequested(event);

        final ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender).send(captor.capture());
        final List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), is("progression.command.update.hearing.application.defendant"));
    }

    private JsonObject getJsonPayload(final String fileName) throws IOException {
        final String jsonString = Resources.toString(Resources.getResource(fileName), defaultCharset());
        return Json.createReader(
                        new ByteArrayInputStream(jsonString.getBytes()))
                .readObject();
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("hearing.id", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearing.courtApplications[0].id", (o1, o2) -> o1 != null && o2 != null)
        );
    }

    private MetadataBuilder getMetadata(final String eventName) {
        return metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withUserId(randomUUID().toString());
    }
}
