package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.BoxHearingRequest.boxHearingRequest;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.HearingResultedUpdateApplication.hearingResultedUpdateApplication;
import static uk.gov.justice.core.courts.InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SendNotificationForApplicationInitiated.sendNotificationForApplicationInitiated;
import static uk.gov.justice.core.courts.SummonsApprovedOutcome.summonsApprovedOutcome;
import static uk.gov.justice.core.courts.SummonsRejectedOutcome.summonsRejectedOutcome;
import static uk.gov.justice.core.courts.SummonsTemplateType.GENERIC_APPLICATION;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.progression.courts.ApproveApplicationSummons.approveApplicationSummons;
import static uk.gov.justice.progression.courts.RejectApplicationSummons.rejectApplicationSummons;
import static uk.gov.justice.progression.courts.SendNotificationForAutoApplication.sendNotificationForAutoApplication;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.AddCourtApplicationToCase;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationDefendantUpdateRequested;
import uk.gov.justice.core.courts.ApplicationReferredToBoxwork;
import uk.gov.justice.core.courts.ApplicationReferredToCourtHearing;
import uk.gov.justice.core.courts.ApplicationReferredToExistingHearing;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BoxHearingRequest;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiateIgnored;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationSummonsApproved;
import uk.gov.justice.core.courts.CourtApplicationSummonsRejected;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.EditCourtApplicationProceedings;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingInitiateEnriched;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.HearingResultedUpdateApplication;
import uk.gov.justice.core.courts.HearingUpdatedWithCourtApplication;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.InitiateCourtHearingAfterSummonsApproved;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SendNotificationForApplicationIgnored;
import uk.gov.justice.core.courts.SendNotificationForApplicationInitiated;
import uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated;
import uk.gov.justice.core.courts.UpdateApplicationDefendant;
import uk.gov.justice.core.courts.UpdateCourtApplicationToHearing;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.progression.courts.HearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.courts.SendNotificationForAutoApplication;
import uk.gov.justice.progression.courts.VejHearingPopulatedToProbationCaseworker;
import uk.gov.justice.progression.event.ApplicationHearingDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;

import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.test.FileUtil;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtApplicationHandlerTest {

    private static final String RESENTENCING_ACTIVATION_CODE = "AJ0001";
    private static final String ORG_OFFENCE_WORDING = "On 12/10/2020 at 10:100am on the corner of the hugh street outside the dog and duck in Croydon you did something wrong";
    private static final String ORG_OFFENCE_WORDING_WELSH = "On 12/10/2020 at 10:100am on the corner of the hugh street outside the";
    private static final String ORG_OFFENCE_CODE = "OFC0001";
    private static final UUID TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER = fromString("8b1cff00-a456-40da-9ce4-f11c20959084");
    private static final String APPLICATION_ID = "applicationId";
    private static final String APPLICATION_REFERRED_TO_COURT_HEARING = "progression.command.application-referred-to-court-hearing";
    private static final String WELSH_WORDING_CLONED_COURT_ORDER_OFFENCE = "Original CaseURN: null, Re-sentenced Original code : OFC0001, Original details: On 12/10/2020 at 10:100am on the corner of the hugh street outside the";
    private static final String ACTIVATION_WORDING_CLONED_COURT_ORDER_OFFENCE = "Activation of a suspended sentence order. Original CaseURN: null, Original code : OFC0001, Original details: On 12/10/2020 at 10:100am on the corner of the hugh street outside the dog and duck in Croydon you did something wrong";
    private static final String WORDING_CLONED_COURT_ORDER_OFFENCE = "Original CaseURN: null, Re-sentenced Original code : OFC0001, Original details: On 12/10/2020 at 10:100am on the corner of the hugh street outside the dog and duck in Croydon you did something wrong";

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CourtApplicationCreated.class,
            CourtApplicationAddedToCase.class,
            CourtApplicationProceedingsInitiated.class,
            ApplicationReferredToBoxwork.class,
            ApplicationReferredToCourtHearing.class,
            ApplicationReferredToExistingHearing.class,
            CourtApplicationProceedingsInitiateIgnored.class,
            CourtApplicationProceedingsEdited.class,
            CourtApplicationSummonsRejected.class,
            CourtApplicationSummonsApproved.class,
            InitiateCourtHearingAfterSummonsApproved.class,
            HearingResultedApplicationUpdated.class,
            HearingUpdatedWithCourtApplication.class,
            HearingPopulatedToProbationCaseworker.class,
            VejHearingPopulatedToProbationCaseworker.class,
            SendNotificationForApplicationInitiated.class,
            SendNotificationForApplicationIgnored.class,
            SendNotificationForAutoApplicationInitiated.class,
            SendNotificationForApplicationIgnored.class,
            CourtApplicationUpdated.class,
            ApplicationHearingDefendantUpdated.class
    );
    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventStream eventStream1;


    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private CourtApplicationHandler courtApplicationHandler;

    @Mock
    private RefDataService referenceDataService;
    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;
    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();
    @Mock
    private ApplicationAggregate applicationAggregate;
    private static final String CONTACT_EMAIL_ADDRESS_PREFIX = STRING.next();
    private static final String EMAIL_ADDRESS_SUFFIX = "@justice.gov.uk";
    private static final String PROSECUTOR_OU_CODE = randomAlphanumeric(8);
    private static final String PROSECUTOR_MAJOR_CREDITOR_CODE = randomAlphanumeric(12);

    private UUID prosecutor1;
    private UUID prosecutor2;
    private String prosecutor2AuthCode;
    private UUID subject;
    private UUID respondent;
    private UUID offenceId;
    private UUID prosecutionCaseId;
    private UUID masterDefendantId1;
    private UUID masterDefendantId2;

    @BeforeEach
    public void setup(){
        prosecutor1 = randomUUID();
        prosecutor2 = randomUUID();
        prosecutor2AuthCode = STRING.next();
        subject = randomUUID();
        respondent = randomUUID();
        offenceId = randomUUID();
        prosecutionCaseId = randomUUID();
        masterDefendantId1= randomUUID();
        masterDefendantId2= randomUUID();

    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-court-application")
                ));
    }

    @Test
    public void shouldHandleNotificationForApplicationCommand() {
        assertThat(new CourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("sendNotificationForApplication")
                        .thatHandles("progression.command.send-notification-for-application")
                ));
    }

    @Test
    public void shouldHandleCommandCourtApplicationAddedToCase() {
        assertThat(new CourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("courtApplicationAddedToCase")
                        .thatHandles("progression.command.add-court-application-to-case")
                ));
    }

    @Test
    public void shouldHandleCommandSendNotificationForAutopplication() {
        assertThat(new CourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("sendNotificationForAutopplication")
                        .thatHandles("progression.command.send-notification-for-auto-application")
                ));
    }


    @Test
    public void shouldHandleCommandForAddBreachApplication() {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        assertThat(new CourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("addBreachApplication")
                        .thatHandles("progression.command.add-breach-application")
                ));
    }

    @Test
    public void shouldProcessCommandCourtApplicationAddedToCase() throws Exception {
        final AddCourtApplicationToCase addCourtApplicationToCase =
                AddCourtApplicationToCase.addCourtApplicationToCase()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID()).build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.add-court-application-to-case")
                .withId(randomUUID())
                .build();

        final Envelope<AddCourtApplicationToCase> envelope = envelopeFrom(metadata, addCourtApplicationToCase);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.courtApplicationAddedToCase(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-added-to-case"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtApplication.id", notNullValue())))

                )
        ));
    }


    @Test
    public void shouldSendNotificationForApplicationCommand() throws Exception {
        final SendNotificationForApplicationInitiated sendNotificationForApplication =
                sendNotificationForApplicationInitiated()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withIsBoxWorkRequest(false)
                        .withIsWelshTranslationRequired(false)
                        .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicationReference("APP00001")
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildCourtApplicationParty(randomUUID()))
                        .withSubject(buildCourtApplicationParty(randomUUID()))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.send-notification-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<SendNotificationForApplicationInitiated> envelope = envelopeFrom(metadata, sendNotificationForApplication);

        courtApplicationHandler.sendNotificationForApplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.send-notification-for-application-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
                                withJsonPath("$.courtHearing", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldSendAutoNotificationForApplicationCommand() throws Exception {
        final SendNotificationForAutoApplication sendNotificationForAutoApplication =
                sendNotificationForAutoApplication()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
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

        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .withCourtApplication(courtApplication()
                        .withId(randomUUID())
                        .withApplicationReference("APP00001")
                        .withType(courtApplicationType()
                                .withProsecutorThirdPartyFlag(false)
                                .withSummonsTemplateType(NOT_APPLICABLE)
                                .build())
                        .withApplicant(buildCourtApplicationParty(randomUUID()))
                        .withSubject(buildCourtApplicationParty(randomUUID()))
                        .withCourtApplicationCases(singletonList(courtApplicationCase()
                                .withIsSJP(false)
                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                .withCaseStatus("ACTIVE")
                                .build()))
                        .build())
                .build());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.send-notification-for-auto-application")
                .withId(randomUUID())
                .build();

        final Envelope<SendNotificationForAutoApplication> envelope = envelopeFrom(metadata, sendNotificationForAutoApplication);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.sendNotificationForAutopplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.send-notification-for-auto-application-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.jurisdictionType", notNullValue()),
                                withJsonPath("$.hearingStartDateTime", notNullValue()),
                                withJsonPath("$.courtCentre", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldRaiseIgnoreNotificationForApplicationCommand() throws Exception {
        final SendNotificationForApplicationInitiated sendNotificationForApplication =
                sendNotificationForApplicationInitiated()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withIsBoxWorkRequest(true)
                        .withIsWelshTranslationRequired(false)
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.send-notification-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<SendNotificationForApplicationInitiated> envelope = envelopeFrom(metadata, sendNotificationForApplication);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new SendNotificationForApplicationIgnored.Builder()
                .withCourtApplication(courtApplication().build())
                .withCourtHearing(CourtHearingRequest.courtHearingRequest().build()));

        courtApplicationHandler.sendNotificationForApplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.send-notification-for-application-ignored"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withoutJsonPath("$.courtApplication.applicationReference"),
                                withJsonPath("$.courtHearing", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldRaiseIgnoreNotificationForApplicationCommandWhenApplicationNotCreated() throws Exception {
        final SendNotificationForApplicationInitiated sendNotificationForApplication =
                sendNotificationForApplicationInitiated()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withIsBoxWorkRequest(false)
                        .withIsWelshTranslationRequired(false)
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.send-notification-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<SendNotificationForApplicationInitiated> envelope = envelopeFrom(metadata, sendNotificationForApplication);

        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        courtApplicationHandler.sendNotificationForApplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.send-notification-for-application-ignored"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withoutJsonPath("$.courtApplication.applicationReference"),
                                withJsonPath("$.courtHearing", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldProcessInitiateCourtProceedingsForApplicationCommand() throws Exception {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtHearing", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.isSJP", is(false)))
                        ).isJson(not(
                                withJsonPath("$.courtApplication.courtApplicationCases", nullValue()))
                        )
                )));
    }

    @Test
    public void shouldProcessInitiateCourtProceedingsForBoxWork() throws Exception {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withBoxHearing(boxHearingRequest().withJurisdictionType(JurisdictionType.CROWN).build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.boxHearing", notNullValue()),
                                withJsonPath("$.boxHearing.id", notNullValue()),
                                withJsonPath("$.isSJP", is(false)),
                                withJsonPath("$.courtApplication.courtApplicationCases", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldProcessInitiateCourtProceedingsForApplicationCommandIsSjp() throws Exception {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withIsSJP(true)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtApplication.courtApplicationCases", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.isSJP", is(true)))
                        )
                )));
    }

    @Test
    public void shouldProcessInitiateCourtProceedingsForApplicationWithInvalidCases() throws Exception {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .withCourtApplicationCases(asList(
                                        courtApplicationCase()
                                                .withIsSJP(true)
                                                .withCaseStatus("ACTIVE")
                                                .build(),
                                        courtApplicationCase()
                                                .withIsSJP(false)
                                                .withCaseStatus("ACTIVE")
                                                .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiate-ignored"),
                        payload()
                                .isJson(allOf(withJsonPath("$.courtApplication.id", notNullValue()))))));
    }

    @Test
    public void shouldProcessEditCourtProceedingsForApplicationCommand() throws Exception {
        final EditCourtApplicationProceedings editCourtApplicationProceedings =
                EditCourtApplicationProceedings.editCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withSummonsApprovalRequired(false)
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.edit-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<EditCourtApplicationProceedings> envelope = envelopeFrom(metadata, editCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.editCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-edited"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtHearing", notNullValue())))

                )
        ));
    }


    @Test
    public void shouldReferApplicationToBoxWorkWhenCourtApplicationCasesIsNotNull() throws Exception {
        final UUID applicationId = randomUUID();
        final JsonObject listOrReferCourtApplication =
                createObjectBuilder().add("id", applicationId.toString()).build();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType()
                                        .withLinkType(LinkType.LINKED)
                                        .build())
                                .withCourtApplicationCases(singletonList(
                                        CourtApplicationCase.courtApplicationCase()
                                                .withIsSJP(false)
                                                .withCaseStatus("ACTIVE")
                                                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                                .withOffences(singletonList(buildOffence(randomUUID(), LocalDate.now())))
                                                .build()
                                ))
                                .build())
                        .withBoxHearing(boxHearingRequest().withId(randomUUID()).build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withSummonsApprovalRequired(false)
                        .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.list-or-refer-court-application")
                .withId(randomUUID())
                .build();

        final Envelope<JsonObject> envelope = envelopeFrom(metadata, listOrReferCourtApplication);

        courtApplicationHandler.listOrReferApplication(envelope);

        final List<JsonEnvelope> eventList = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(eventList.size(), is(2));
        assertThat(eventList.get(0).metadata().name(), is("progression.event.court-application-added-to-case"));
        assertThat(eventList.get(1).metadata().name(), is("progression.event.application-referred-to-boxwork"));
        assertThat(eventList.get(1).payloadAsJsonObject().getJsonObject("application"), notNullValue());
        assertThat(eventList.get(1).payloadAsJsonObject().getJsonObject("boxHearing"), notNullValue());
    }

    @Test
    public void shouldReferApplicationToBoxWorkWhenCourtOrderIsNotNull() throws Exception {
        final UUID applicationId = randomUUID();
        final JsonObject listOrReferCourtApplication =
                createObjectBuilder().add("id", applicationId.toString()).build();
        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType()
                                        .withLinkType(LinkType.LINKED)
                                        .build())
                                .withCourtOrder(
                                        CourtOrder.courtOrder()
                                                .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                                        .withOffence(buildOffence(randomUUID(), LocalDate.now()))
                                                        .build()))
                                                .build()
                                )
                                .build())
                        .withBoxHearing(boxHearingRequest().withId(randomUUID()).build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withSummonsApprovalRequired(false)
                        .build();

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.list-or-refer-court-application")
                .withId(randomUUID())
                .build();

        final Envelope<JsonObject> envelope = envelopeFrom(metadata, listOrReferCourtApplication);

        courtApplicationHandler.listOrReferApplication(envelope);

        final List<JsonEnvelope> eventList = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(eventList.size(), is(2));
        assertThat(eventList.get(0).metadata().name(), is("progression.event.court-application-added-to-case"));
        assertThat(eventList.get(1).metadata().name(), is("progression.event.application-referred-to-boxwork"));
        assertThat(eventList.get(1).payloadAsJsonObject().getJsonObject("application"), notNullValue());
        assertThat(eventList.get(1).payloadAsJsonObject().getJsonObject("boxHearing"), notNullValue());
    }

    @Test
    public void shouldNotReferApplicationToBoxWorkWhenCourtOrderAndCourtApplicationCasesIsNull() throws Exception {
        final UUID applicationId = randomUUID();
        final JsonObject listOrReferCourtApplication =
                createObjectBuilder().add("id", applicationId.toString()).build();
        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType()
                                        .withLinkType(LinkType.LINKED)
                                        .build())
                                .build())
                        .withBoxHearing(boxHearingRequest().withId(randomUUID()).build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .withSummonsApprovalRequired(false)
                        .build();

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.list-or-refer-court-application")
                .withId(randomUUID())
                .build();

        final Envelope<JsonObject> envelope = envelopeFrom(metadata, listOrReferCourtApplication);

        courtApplicationHandler.listOrReferApplication(envelope);

        final List<JsonEnvelope> eventList = verifyAppendAndGetArgumentFrom(eventStream).collect(Collectors.toList());
        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldReferApplicationToExistingHearing() throws Exception {
        final UUID applicationId = randomUUID();
        final JsonObject listOrReferCourtApplication =
                createObjectBuilder().add("id", applicationId.toString()).build();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication().withId(applicationId).withType(courtApplicationType().withLinkType(LinkType.LINKED).build()).build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().withId(randomUUID()).build())
                        .withSummonsApprovalRequired(false)
                        .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);
        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.list-or-refer-court-application")
                .withId(randomUUID())
                .build();

        final Envelope<JsonObject> envelope = envelopeFrom(metadata, listOrReferCourtApplication);

        courtApplicationHandler.listOrReferApplication(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.application-referral-to-existing-hearing"),
                        payload()
                                .isJson(allOf(withJsonPath("$.courtApplication", notNullValue())))
                                .isJson(allOf(withJsonPath("$.courtHearing", notNullValue()))))));
    }

    @Test
    public void shouldHandleInitiateCourtHearingFromHearingResultCommand() {
        assertThat(new CourtApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("initiateCourtHearingFromHearingResult")
                        .thatHandles(APPLICATION_REFERRED_TO_COURT_HEARING)
                ));
    }

    @Test
    public void shouldInitiateCourtHearingFromHearingResult() throws Exception {
        final UUID applicationId = randomUUID();
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID(APPLICATION_REFERRED_TO_COURT_HEARING),
                createObjectBuilder()
                        .add(APPLICATION_ID, applicationId.toString())
                        .build());

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtHearingFromHearingResult(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.application-referred-to-court-hearing"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtHearing", notNullValue()))
                        )
                )));
    }

    @Test
    public void shouldAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_NoExisting3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor1))
                                .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                .build())
                                        .withIsSJP(true)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor2), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(), eq(respondent), any())).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].summonsRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].notificationRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_Existing3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor1))
                                .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                .build())
                                        .withIsSJP(true)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor2), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor3), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(), eq(respondent), any())).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(2)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.thirdParties[1].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor1))
                                .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor2)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .build())
                                        .withIsSJP(true)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor2), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(), eq(respondent), any())).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_Applicant() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID subject = randomUUID();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor2))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor1)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .build())
                                        .withIsSJP(true)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(referenceDataService.getProsecutor(any(), eq(prosecutor2), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor1 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor1 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor1 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_Respondent() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor1))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                                .withRespondents(singletonList(buildCourtApplicationParty(prosecutor2)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .build())
                                        .withIsSJP(true)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor2), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor3), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldNotAddProsecutorFromCaseWhenCourtApplicationProceedingsInitiated_ProsecutorThirdPartyFlagFalse() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor1))
                                .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                .build())
                                        .withIsSJP(true)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor3), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(), eq(respondent), any())).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)))
                        ))
                )
        );
    }

    @Test
    public void shouldAddProsecutorFromCourtOrdersWhenCourtApplicationProceedingsInitiated_NoExisting3rdParty() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();
        final UUID subject = randomUUID();
        final UUID respondent = randomUUID();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor1))
                                .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withCourtOrder(CourtOrder.courtOrder()
                                        .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                                                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                        .withProsecutionAuthorityId(prosecutor2)
                                                        .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                        .build())
                                                .withOffence(Offence.offence().withOffenceCode(STRING.next()).build())
                                                .build()))
                                        .build())
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor2), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(), eq(respondent), any())).thenReturn(of(buildProsecutorQueryResult(respondent, "respondent")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(1)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].summonsRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].notificationRequired", is(true)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldEnrichProsecutorInformationForCourtApplicationParties() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent1 = randomUUID();
        final UUID respondent2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(prosecutor1))
                                .withRespondents(asList(buildCourtApplicationParty(respondent1), buildCourtApplicationParty(respondent2)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                .build())
                                        .withIsSJP(true)
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(referenceDataService.getProsecutor(any(), eq(prosecutor1), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor1, "prosecutor1")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor2), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor2, "prosecutor2")));
        when(referenceDataService.getProsecutor(any(), eq(prosecutor3), any())).thenReturn(of(buildProsecutorQueryResult(prosecutor3, "prosecutor3")));
        when(referenceDataService.getProsecutor(any(), eq(subject), any())).thenReturn(of(buildProsecutorQueryResult(subject, "subject")));
        when(referenceDataService.getProsecutor(any(), eq(respondent1), any())).thenReturn(of(buildProsecutorQueryResult(respondent1, "respondent1")));
        when(referenceDataService.getProsecutor(any(), eq(respondent2), any())).thenReturn(of(buildProsecutorQueryResult(respondent2, "respondent2")));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties.length()", is(2)),
                                withJsonPath("$.courtApplication.thirdParties[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor3.toString())),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.name", is("prosecutor3 Name")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.welshName", is("prosecutor3 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.address.address1", is("prosecutor3 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.thirdParties[1].id", notNullValue()),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityId", is(prosecutor2.toString())),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.name", is("prosecutor2 Name")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.welshName", is("prosecutor2 WelshName")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.address.address1", is("prosecutor2 Address line 1")),
                                withJsonPath("$.courtApplication.thirdParties[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.name", is("prosecutor1 Name")),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.welshName", is("prosecutor1 WelshName")),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.address.address1", is("prosecutor1 Address line 1")),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.prosecutionAuthorityId", is(subject.toString())),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + subject.toString())),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.name", is("subject Name")),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.welshName", is("subject WelshName")),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.address.address1", is("subject Address line 1")),
                                withJsonPath("$.courtApplication.subject.prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.respondents.length()", is(2)),
                                withJsonPath("$.courtApplication.respondents[0].id", notNullValue()),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.prosecutionAuthorityId", is(respondent1.toString())),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + respondent1.toString())),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.name", is("respondent1 Name")),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.welshName", is("respondent1 WelshName")),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.address.address1", is("respondent1 Address line 1")),
                                withJsonPath("$.courtApplication.respondents[0].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)),

                                withJsonPath("$.courtApplication.respondents[1].id", notNullValue()),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.prosecutionAuthorityId", is(respondent2.toString())),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + respondent2.toString())),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.prosecutionAuthorityOUCode", is(PROSECUTOR_OU_CODE)),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.majorCreditorCode", is(PROSECUTOR_MAJOR_CREDITOR_CODE)),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.name", is("respondent2 Name")),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.welshName", is("respondent2 WelshName")),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.address.address1", is("respondent2 Address line 1")),
                                withJsonPath("$.courtApplication.respondents[1].prosecutingAuthority.contact.primaryEmail", is(CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldNotEnrichProsecutorInformationForNonStdIndividualProsecutor() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent1 = randomUUID();
        final UUID respondent2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationPartyIndividual(prosecutor1, "Non STD Individual Prosecutor"))
                                .withRespondents(asList(buildCourtApplicationParty(respondent1), buildCourtApplicationParty(respondent2)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                .build())
                                        .withIsSJP(true)
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.firstName", is("Non STD Individual Prosecutor"))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldNotEnrichProsecutorInformationForNonStdOrganisationProsecutor() throws EventStreamException {

        final UUID prosecutor1 = randomUUID();
        final UUID prosecutor2 = randomUUID();
        final UUID prosecutor3 = randomUUID();
        final UUID subject = randomUUID();
        final UUID respondent1 = randomUUID();
        final UUID respondent2 = randomUUID();
        final String prosecutor2AuthCode = STRING.next();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withApplicationReference(STRING.next())
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(true)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(buildCourtApplicationPartyOrganisation(prosecutor1, "Non STD Organisation Prosecutor"))
                                .withRespondents(asList(buildCourtApplicationParty(respondent1), buildCourtApplicationParty(respondent2)))
                                .withSubject(buildCourtApplicationParty(subject))
                                .withThirdParties(singletonList(buildCourtApplicationParty(prosecutor3)))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                                .withProsecutionAuthorityId(prosecutor2)
                                                .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                                .build())
                                        .withIsSJP(true)
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityId", is(prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.prosecutionAuthorityCode", is("Code_" + prosecutor1.toString())),
                                withJsonPath("$.courtApplication.applicant.prosecutingAuthority.name", is("Non STD Organisation Prosecutor"))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderIsNotSuspendedSentence() throws EventStreamException {
        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        offenceWordingTestForCourtOrder(randomUUID());
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderIsNotSuspendedSentenceForApplicationAddressUpdated() throws EventStreamException {
        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        final ProsecutionCase prosecutionCase = getProsecutionCase(prosecutionCaseId, masterDefendantId1, masterDefendantId2);
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));


        offenceWordingTestForCourtOrderForUpdatedOn(randomUUID());
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderIsSuspendedSentence() throws EventStreamException {

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        offenceWordingTestForCourtOrder(TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER);
    }

    @Test
    public void shouldNotUpdateOffenceDetailsForFirstHearingApplication() throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildFirstHearingApplicationCourtProceedings();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final CourtApplicationCase courtApplicationCase = initiateCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", is(courtApplicationCase.getProsecutionCaseId().toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(courtApplicationCase.getOffences().get(0).getId().toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", is(courtApplicationCase.getOffences().get(0).getOffenceCode())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wording", is(courtApplicationCase.getOffences().get(0).getWording())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wordingWelsh", is(courtApplicationCase.getOffences().get(0).getWordingWelsh()))
                        )))));
    }

    @Test
    public void testCourtProceedingInitiatedForApplicationIsIdempotent() throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildFirstHearingApplicationCourtProceedings();
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final CourtApplicationCase courtApplicationCase = initiateCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0);
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", is(courtApplicationCase.getProsecutionCaseId().toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(courtApplicationCase.getOffences().get(0).getId().toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", is(courtApplicationCase.getOffences().get(0).getOffenceCode())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wording", is(courtApplicationCase.getOffences().get(0).getWording())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wordingWelsh", is(courtApplicationCase.getOffences().get(0).getWordingWelsh()))
                        )))));

        when(eventSource.getStreamById(any())).thenReturn(eventStream1);
        when(aggregateService.get(eventStream1, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream1 = verifyAppendAndGetArgumentFrom(eventStream1);

        assertThat(envelopeStream1,
                streamContaining(
                        jsonEnvelope(metadata().withName("progression.event.court-application-proceedings-initiate-ignored"),
                                payload().isJson(allOf(
                                        withJsonPath("$.courtApplication", notNullValue())
                                )))
                        )

        );


    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtOrderNotExist() throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildInitiateCourtApplicationProceedings(randomUUID(), false, true);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final String expectedWording = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
        final String expectedWordingWelsh = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING_WELSH;
        final UUID offenceId = initiateCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getOffences().get(0).getId();
        final UUID prosecutionCaseId = initiateCourtApplicationProceedings.getCourtApplication().getCourtApplicationCases().get(0).getProsecutionCaseId();
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                hasNoJsonPath("$.courtApplication.courtOrders"),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].isSJP", is(false)),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", is(prosecutionCaseId.toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId.toString())),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", is(RESENTENCING_ACTIVATION_CODE)),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wording", is(expectedWording)),
                                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].wordingWelsh", is(expectedWordingWelsh))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldNotUpdateFutureSummonsHearingWhenCourtHearingIsNotAvailable() throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildInitiateCourtApplicationProceedings(randomUUID(), false, true, true, false);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(not(
                                withJsonPath("$.courtApplication.futureSummonsHearing", nullValue()))))));
    }

    @Test
    public void shouldUpdateFutureSummonsHearingWhenSummonsApprovalRequiredIsTrueForInitiate() throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildInitiateCourtApplicationProceedings(randomUUID(), false, true, true);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.futureSummonsHearing.courtCentre.code", is("COURTCENTER")),
                                withJsonPath("$.courtApplication.futureSummonsHearing.judiciary[0].judicialId", is(initiateCourtApplicationProceedings.getCourtHearing().getJudiciary().get(0).getJudicialId().toString())),
                                withJsonPath("$.courtApplication.futureSummonsHearing.earliestStartDateTime", is("2020-06-26T07:51:00.000Z")),
                                withJsonPath("$.courtApplication.futureSummonsHearing.estimatedMinutes", is(20)),
                                withJsonPath("$.courtApplication.futureSummonsHearing.jurisdictionType", is(JurisdictionType.CROWN.toString())),
                                withJsonPath("$.courtApplication.futureSummonsHearing.weekCommencingDate.duration", is(20))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldUpdateFutureSummonsHearingWhenSummonsApprovalRequiredIsTrueForEdit() throws Exception {
        final UUID applicationId = randomUUID();
        final EditCourtApplicationProceedings editCourtApplicationProceedings =
                EditCourtApplicationProceedings.editCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(applicationId)
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(GENERIC_APPLICATION)
                                        .build())
                                .withApplicant(buildCourtApplicationParty(randomUUID()))
                                .withSubject(buildCourtApplicationParty(randomUUID()))
                                .build())
                        .withCourtHearing(buildCourtHearing(true))
                        .withBoxHearing(boxHearingRequest().build())
                        .withSummonsApprovalRequired(true)
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.edit-court-proceedings-for-application")
                .withId(randomUUID())
                .build();

        final Envelope<EditCourtApplicationProceedings> envelope = envelopeFrom(metadata, editCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.editCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-edited"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.id", is(applicationId.toString())),
                                withJsonPath("$.courtApplication.futureSummonsHearing.courtCentre.code", is("COURTCENTER")),
                                withJsonPath("$.courtApplication.futureSummonsHearing.judiciary[0].judicialId", is(editCourtApplicationProceedings.getCourtHearing().getJudiciary().get(0).getJudicialId().toString())),
                                withJsonPath("$.courtApplication.futureSummonsHearing.earliestStartDateTime", is("2020-06-26T07:51:00.000Z")),
                                withJsonPath("$.courtApplication.futureSummonsHearing.estimatedMinutes", is(20)),
                                withJsonPath("$.courtApplication.futureSummonsHearing.jurisdictionType", is(JurisdictionType.CROWN.toString())),
                                withJsonPath("$.courtApplication.futureSummonsHearing.weekCommencingDate.duration", is(20))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldUpdateOffenceWordingWhenCourtApplicationCasesNotExist() throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildInitiateCourtApplicationProceedings(randomUUID(), true, false);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final String expectedWording = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
        final String expectedWordingWelsh = "Resentenced Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING_WELSH;
        final CourtOrder courtOrder = initiateCourtApplicationProceedings.getCourtApplication().getCourtOrder();
        final UUID offenceId = courtOrder.getCourtOrderOffences().get(0).getOffence().getId();
        final String prosecutor2AuthCode = initiateCourtApplicationProceedings.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.offenceCode", is(RESENTENCING_ACTIVATION_CODE)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wording", is(WORDING_CLONED_COURT_ORDER_OFFENCE)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wordingWelsh", is(WELSH_WORDING_CLONED_COURT_ORDER_OFFENCE)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.id", is(offenceId.toString())),
                                withJsonPath("$.courtApplication.courtOrder.id", is(courtOrder.getId().toString())),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", is(prosecutor2AuthCode)),
                                hasNoJsonPath("$.courtApplication.courtApplicationCases")
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldGenerateSummonsApproved() throws EventStreamException {

        final UUID applicationId = randomUUID();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication().withId(applicationId).withType(courtApplicationType().withLinkType(LinkType.LINKED).build()).build())
                        .withBoxHearing(boxHearingRequest().build())
                        .withSummonsApprovalRequired(true)
                        .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);

        final uk.gov.justice.progression.courts.ApproveApplicationSummons approveApplicationSummons = approveApplicationSummons()
                .withApplicationId(randomUUID())
                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                        .withSummonsSuppressed(false)
                        .withPersonalService(false)
                        .withProsecutorCost("£100.00")
                        .withProsecutorEmailAddress("test@test.com")
                        .build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.court-application-summons-approved")
                .withId(randomUUID())
                .build();

        courtApplicationHandler.courtApplicationSummonsApproved(envelopeFrom(metadata, approveApplicationSummons));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-summons-approved"),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString())),
                                withoutJsonPath("$.caseIds.length()"))))));
    }

    @Test
    public void shouldGenerateInitiateCourtHearingAndSummonsApproved() throws EventStreamException {

        final UUID applicationId = randomUUID();

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication().withId(applicationId).withType(courtApplicationType().withLinkType(LinkType.LINKED).build()).build())
                        .withCourtHearing(new CourtHearingRequest.Builder().build())
                        .withBoxHearing(boxHearingRequest().build())
                        .withSummonsApprovalRequired(true)
                        .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);

        final uk.gov.justice.progression.courts.ApproveApplicationSummons approveApplicationSummons = approveApplicationSummons()
                .withApplicationId(randomUUID())
                .withSummonsApprovedOutcome(summonsApprovedOutcome()
                        .withSummonsSuppressed(false)
                        .withPersonalService(false)
                        .withProsecutorCost("£100.00")
                        .withProsecutorEmailAddress("test@test.com")
                        .build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.court-application-summons-approved")
                .withId(randomUUID())
                .build();
        final Envelope<uk.gov.justice.progression.courts.ApproveApplicationSummons> envelope = envelopeFrom(metadata, approveApplicationSummons);

        courtApplicationHandler.courtApplicationSummonsApproved(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.initiate-court-hearing-after-summons-approved"),
                        payload().isJson(allOf(
                                withJsonPath("$.application.id", is(applicationId.toString()))))),
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-summons-approved"),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationId", is(applicationId.toString())))))
        ));
    }

    @Test
    public void shouldGenerateSummonsRejected() throws EventStreamException {

        final UUID applicationId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();

        final CourtApplicationCase applicationCase = courtApplicationCase().withProsecutionCaseId(prosecutionCaseId).withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(randomAlphabetic(15)).build()).build();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(applicationCase))
                        .withType(courtApplicationType().withLinkType(LinkType.LINKED).build()).build())
                .withBoxHearing(boxHearingRequest().build())
                .withSummonsApprovalRequired(true)
                .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        applicationAggregate.initiateCourtApplicationProceedings(initiateCourtApplicationProceedings, false, false);

        applicationAggregate.createCourtApplication(initiateCourtApplicationProceedings.getCourtApplication(), null);

        final uk.gov.justice.progression.courts.RejectApplicationSummons rejectApplicationSummons = rejectApplicationSummons()
                .withApplicationId(applicationId)
                .withSummonsRejectedOutcome(summonsRejectedOutcome()
                        .withReasons(singletonList("Rejected"))
                        .withProsecutorEmailAddress("test@test.com")
                        .build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.court-application-summons-rejected")
                .withId(randomUUID())
                .build();

        courtApplicationHandler.courtApplicationSummonsRejected(envelopeFrom(metadata, rejectApplicationSummons));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-summons-rejected"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.id", is(applicationId.toString())),
                                withJsonPath("$.summonsRejectedOutcome.reasons", hasItem("Rejected")),
                                withJsonPath("$.caseIds", hasItem(prosecutionCaseId.toString()))
                        )))));
    }

    @Test
    public void shouldGenerateHearingResultedApplicationUpdated() throws EventStreamException {
        final UUID applicationId = randomUUID();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.hearing-resulted-update-application")
                .withId(randomUUID())
                .build();

        final HearingResultedUpdateApplication hearingResultedUpdateApplication = hearingResultedUpdateApplication().withCourtApplication(courtApplication().withId(applicationId).withJudicialResults(asList(JudicialResult.judicialResult().withCategory(JudicialResultCategory.FINAL).build())).build()).build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.hearingResultedUpdateApplication(envelopeFrom(metadata, hearingResultedUpdateApplication));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-resulted-application-updated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.id", is(applicationId.toString())),
                                withJsonPath("$.courtApplication.applicationStatus", is(ApplicationStatus.FINALISED.toString()))
                        )))));
    }

    @Test
    public void shouldUpdateApplicationInHearingAndRaiseProbationEvent() throws EventStreamException {
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();

        final HearingAggregate hearingAggregate = new HearingAggregate();
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
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-court-application-to-hearing")
                .withId(randomUUID())
                .build();
        final UpdateCourtApplicationToHearing hearingUpdateApplication = UpdateCourtApplicationToHearing.updateCourtApplicationToHearing()
                .withHearingId(hearingId)
                .withCourtApplication(courtApplication()
                        .withId(applicationId)
                        .withApplicationReference("B")
                        .withSubject(CourtApplicationParty.courtApplicationParty().build())
                        .build())
                .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        applicationAggregate.apply(new CourtApplicationProceedingsInitiated.Builder()
                .withCourtHearing(new CourtHearingRequest.Builder().build())
                .withBoxHearing(new BoxHearingRequest.Builder().build())
                .build());

        courtApplicationHandler.hearingUpdatedWithApplication(envelopeFrom(metadata, hearingUpdateApplication));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-updated-with-court-application"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.id", is(applicationId.toString())),
                                withJsonPath("$.courtApplication.applicationReference", is("B"))
                        ))),
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.hearing-populated-to-probation-caseworker"),
                        payload().isJson(allOf(
                                withJsonPath("$.hearing.courtApplications[0].id", is(applicationId.toString())),
                                withJsonPath("$.hearing.courtApplications[0].applicationReference", is("B")),
                                withJsonPath("$.hearing.id", is(hearingId.toString()))
                        ))),
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.vej-hearing-populated-to-probation-caseworker"),
                        payload().isJson(allOf(
                                withJsonPath("$.hearing.courtApplications[0].id", is(applicationId.toString())),
                                withJsonPath("$.hearing.courtApplications[0].applicationReference", is("B")),
                                withJsonPath("$.hearing.id", is(hearingId.toString()))
                        )))));

    }

    @Test
    public void shouldProcessInitiateCourtProceedingsForApplicationCommand_updateDefendantUpdatedDateOnApplication() throws Exception {
        final UUID caseId = randomUUID();
        final UUID masterDefendantId1 = randomUUID();
        final UUID masterDefendantId2 = randomUUID();
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings =
                initiateCourtApplicationProceedings()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
                                .withType(courtApplicationType()
                                        .withProsecutorThirdPartyFlag(false)
                                        .withSummonsTemplateType(NOT_APPLICABLE)
                                        .build())
                                .withApplicant(CourtApplicationParty.courtApplicationParty()
                                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                                .withMasterDefendantId(masterDefendantId1)
                                                .withPersonDefendant(PersonDefendant.personDefendant()
                                                        .withPersonDetails(Person.person()
                                                                .withAddress(Address.address()
                                                                        .withAddress1("address1")
                                                                        .withPostcode("TW1 8KS").build()).build()).build()).build()).build())
                                .withSubject(CourtApplicationParty.courtApplicationParty()
                                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                                .withMasterDefendantId(masterDefendantId1)
                                                .withPersonDefendant(PersonDefendant.personDefendant()
                                                        .withPersonDetails(Person.person()
                                                                .withAddress(Address.address()
                                                                        .withAddress1("address1")
                                                                        .withPostcode("TW1 8KS").build()).build()).build()).build()).build())
                                .withRespondents(asList(CourtApplicationParty.courtApplicationParty()
                                        .withMasterDefendant(MasterDefendant.masterDefendant()
                                                .withMasterDefendantId(masterDefendantId2)
                                                .withPersonDefendant(PersonDefendant.personDefendant()
                                                        .withPersonDetails(Person.person()
                                                                .withAddress(Address.address()
                                                                        .withAddress1("address2")
                                                                        .withPostcode("TL1 9AA").build()).build()).build()).build()).build()))
                                .withCourtApplicationCases(singletonList(courtApplicationCase()
                                        .withIsSJP(false)
                                        .withProsecutionCaseIdentifier(prosecutionCaseIdentifier().withCaseURN(STRING.next()).build())
                                        .withProsecutionCaseId(caseId)
                                        .withCaseStatus("ACTIVE")
                                        .build()))
                                .build())
                        .withCourtHearing(CourtHearingRequest.courtHearingRequest().build())
                        .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final ProsecutionCase prosecutionCase = getProsecutionCase(caseId, masterDefendantId1, masterDefendantId2);
        final JsonObject prosecutionCaseJson = objectToJsonObjectConverter.convert(prosecutionCase);
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);
        when(prosecutionCaseQueryService.getProsecutionCase(any(JsonEnvelope.class),any(String.class)))
                .thenReturn(Optional.of(createObjectBuilder().add("prosecutionCase", prosecutionCaseJson).build()));

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.applicant.updatedOn", notNullValue()),
                                withJsonPath("$.courtApplication.subject.updatedOn", nullValue()),
                                withJsonPath("$.courtApplication.respondents[0].updatedOn", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.courtHearing", notNullValue()))
                        ).isJson(allOf(
                                withJsonPath("$.isSJP", is(false)))
                        ).isJson(not(
                                withJsonPath("$.courtApplication.courtApplicationCases", nullValue()))
                        )
                )));
    }

    @Test
    public void shouldUpdateApplicationHearingWithUpdatedDefendant() throws EventStreamException {

        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID masterDefendantId = randomUUID();

        final HearingAggregate hearingAggregate = new HearingAggregate();
        hearingAggregate.apply(HearingInitiateEnriched.hearingInitiateEnriched()
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .withCourtApplications(singletonList(courtApplication()
                                .withId(applicationId)
                                .withApplicationReference("A")
                                .withSubject(CourtApplicationParty.courtApplicationParty().withMasterDefendant(
                                        MasterDefendant.masterDefendant().withMasterDefendantId(masterDefendantId)
                                                .withPersonDefendant(PersonDefendant.personDefendant().withPersonDetails(Person.person().build()).build()).build()
                                ).build())
                                .build()))
                        .build())
                .build());
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.command.update.hearing.application.defendant-test.json")
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%MASTER_DEFENDANT_ID%",masterDefendantId.toString()));

        final ApplicationDefendantUpdateRequested applicationDefendantUpdateRequested = jsonObjectToObjectConverter.convert(jsonObject, ApplicationDefendantUpdateRequested.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update.hearing.application.defendant")
                .withId(randomUUID())
                .build();
        courtApplicationHandler.updateApplicationHearing(envelopeFrom(metadata, applicationDefendantUpdateRequested));

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
            jsonEnvelope(
                    metadata()
                            .withName("progression.event.application-hearing-defendant-updated"),
                    payload().isJson(allOf(
                            withJsonPath("$.hearing", notNullValue()))
                    )),
            jsonEnvelope(
                    metadata()
                            .withName("progression.events.hearing-populated-to-probation-caseworker"),
                    payload().isJson(allOf(
                            withJsonPath("$.hearing", notNullValue()))
                    )),
                jsonEnvelope(
                        metadata()
                                .withName("progression.events.vej-hearing-populated-to-probation-caseworker"),
                        payload().isJson(allOf(
                                withJsonPath("$.hearing", notNullValue()))
                        ))));
    }

    @Test
    public void shouldUpdateCourtApplicationWithUpdatedDefendantInfo() throws EventStreamException {
        final UUID applicationId = randomUUID();
        final JsonObject jsonObject = FileUtil.jsonFromString(FileUtil
                .getPayload("json/progression.command.update-application-defendant-test.json")
                .replaceAll("%APPLICATION_ID%",applicationId.toString()));

        final UpdateApplicationDefendant courtApplication = jsonObjectToObjectConverter.convert(jsonObject, UpdateApplicationDefendant.class);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-application-defendant")
                .withId(randomUUID())
                .build();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
        applicationAggregate.apply(new CourtApplicationUpdated.Builder()
                .withCourtApplication(courtApplication().build()));

        courtApplicationHandler.updateApplicationDefendant(envelopeFrom(metadata, courtApplication));
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        assertThat(envelopeStream, streamContaining(
            jsonEnvelope(
                    metadata()
                            .withName("progression.event.court-application-updated"),
                    payload().isJson(allOf(
                            withJsonPath("$.courtApplication", notNullValue()),
                            withJsonPath("$.courtApplication.id", is(applicationId.toString())))
                    ))

                )
        );
    }

    private void offenceWordingTestForCourtOrder(final UUID judicialResultTypeId) throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildInitiateCourtApplicationProceedings(judicialResultTypeId, true, false);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final String prefix;
        final String expectedWording ;
        final String expectedWordingWelsh;
        if (TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER.equals(judicialResultTypeId)) {
            prefix = "Activation of a suspended sentence order. ";
            expectedWording  =   prefix + "Original CaseURN: null, " +  "Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
            expectedWordingWelsh = prefix + "Original CaseURN: null, " +  "Original code : " + ORG_OFFENCE_CODE + ", Original details: "  + ORG_OFFENCE_WORDING_WELSH;
        } else {
            prefix = "Re-sentenced";
            expectedWording  =   "Original CaseURN: null, " + prefix +  " Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
            expectedWordingWelsh = "Original CaseURN: null, " + prefix + " Original code : " + ORG_OFFENCE_CODE + ", Original details: "  + ORG_OFFENCE_WORDING_WELSH;

        }
        final CourtOrder courtOrder = initiateCourtApplicationProceedings.getCourtApplication().getCourtOrder();
        final UUID offenceId = courtOrder.getCourtOrderOffences().get(0).getOffence().getId();
        final String prosecutor2AuthCode = initiateCourtApplicationProceedings.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.offenceCode", is(RESENTENCING_ACTIVATION_CODE)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wording", is(expectedWording)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wordingWelsh", is(expectedWordingWelsh)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.id", is(offenceId.toString())),
                                withJsonPath("$.courtApplication.courtOrder.id", is(courtOrder.getId().toString())),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", is(prosecutor2AuthCode))
                        )))));
    }

    private void offenceWordingTestForCourtOrderForUpdatedOn(final UUID judicialResultTypeId) throws EventStreamException {
        InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = buildInitiateCourtApplicationProceedings(judicialResultTypeId, true, false);
        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings-for-application")
                .withId(randomUUID())
                .build();
        final Envelope<InitiateCourtApplicationProceedings> envelope = envelopeFrom(metadata, initiateCourtApplicationProceedings);

        courtApplicationHandler.initiateCourtApplicationProceedings(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);
        final String prefix;
        final String expectedWording ;
        final String expectedWordingWelsh;
        if (TYPE_ID_FOR_SUSPENDED_SENTENCE_ORDER.equals(judicialResultTypeId)) {
            prefix = "Activation of a suspended sentence order. ";
            expectedWording  =   prefix + "Original CaseURN: null, " +  "Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
            expectedWordingWelsh = prefix + "Original CaseURN: null, " +  "Original code : " + ORG_OFFENCE_CODE + ", Original details: "  + ORG_OFFENCE_WORDING_WELSH;
        } else {
            prefix = "Re-sentenced";
            expectedWording  =   "Original CaseURN: null, " + prefix +  " Original code : " + ORG_OFFENCE_CODE + ", Original details: " + ORG_OFFENCE_WORDING;
            expectedWordingWelsh = "Original CaseURN: null, " + prefix + " Original code : " + ORG_OFFENCE_CODE + ", Original details: "  + ORG_OFFENCE_WORDING_WELSH;

        }
        final CourtOrder courtOrder = initiateCourtApplicationProceedings.getCourtApplication().getCourtOrder();
        final UUID offenceId = courtOrder.getCourtOrderOffences().get(0).getOffence().getId();
        final String prosecutor2AuthCode = initiateCourtApplicationProceedings.getCourtApplication().getCourtOrder().getCourtOrderOffences().get(0).getProsecutionCaseIdentifier().getProsecutionAuthorityCode();
        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(metadata().withName("progression.event.court-application-proceedings-initiated"),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication", notNullValue()),
                                withJsonPath("$.courtApplication.applicant.updatedOn", notNullValue()),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.offenceCode", is(RESENTENCING_ACTIVATION_CODE)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wording", is(expectedWording)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.wordingWelsh", is(expectedWordingWelsh)),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.id", is(offenceId.toString())),
                                withJsonPath("$.courtApplication.courtOrder.id", is(courtOrder.getId().toString())),
                                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].prosecutionCaseIdentifier.prosecutionAuthorityCode", is(prosecutor2AuthCode))
                        )))));
    }

    private ProsecutionCase getProsecutionCase(final UUID caseId, final UUID defendantId, final UUID defendantId2) {
        return ProsecutionCase.prosecutionCase()
                .withCaseStatus("caseStatus")
                .withId(caseId)
                .withOriginatingOrganisation("originatingOrganisation")
                .withDefendants(asList(Defendant.defendant()
                        .withMasterDefendantId(defendantId)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("old adress one")
                                                .withPostcode("TK1 9MD").build()).build()).build()).build(),
                        Defendant.defendant()
                                .withMasterDefendantId(defendantId2)
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withPersonDetails(Person.person()
                                                .withAddress(Address.address()
                                                        .withAddress1("old adress two")
                                                        .withPostcode("MK1 5PO").build()).build()).build()).build()))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityReference("reference")
                        .withProsecutionAuthorityCode("code")
                        .withProsecutionAuthorityId(randomUUID())
                        .build())
                .build();
    }

    private CourtApplicationParty buildCourtApplicationParty(final UUID prosecutionAuthorityId) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                        .withProsecutionAuthorityCode("Code_" + prosecutionAuthorityId.toString())
                        .build())
                .withMasterDefendant(MasterDefendant.masterDefendant()
                        .withMasterDefendantId(masterDefendantId1)
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withAddress(Address.address()
                                                .withAddress1("new adress one")
                                                .withPostcode("TK2 9MD").build()).build()).build())
                        .build())
                .build();
    }

    private CourtApplicationParty buildCourtApplicationPartyIndividual(final UUID prosecutionAuthorityId, final String firstName) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                        .withProsecutionAuthorityCode("Code_" + prosecutionAuthorityId.toString())
                        .withFirstName(firstName)
                        .build())
                .build();
    }

    private CourtApplicationParty buildCourtApplicationPartyOrganisation(final UUID prosecutionAuthorityId, final String name) {
        return CourtApplicationParty.courtApplicationParty()
                .withId(randomUUID())
                .withProsecutingAuthority(ProsecutingAuthority.prosecutingAuthority()
                        .withProsecutionAuthorityId(prosecutionAuthorityId)
                        .withProsecutionAuthorityCode("Code_" + prosecutionAuthorityId.toString())
                        .withName(name)
                        .build())
                .build();
    }

    private JsonObject buildProsecutorQueryResult(final UUID prosecutorId, final String prosecutor) {
        return createObjectBuilder()
                .add("id", prosecutorId.toString())
                .add("fullName", prosecutor + " Name")
                .add("majorCreditorCode", PROSECUTOR_MAJOR_CREDITOR_CODE)
                .add("oucode", PROSECUTOR_OU_CODE)
                .add("nameWelsh", prosecutor + " WelshName")
                .add("address", createObjectBuilder()
                        .add("address1", prosecutor + " Address line 1")
                        .build())
                .add("informantEmailAddress", prosecutor + EMAIL_ADDRESS_SUFFIX)
                .add("contactEmailAddress", CONTACT_EMAIL_ADDRESS_PREFIX + EMAIL_ADDRESS_SUFFIX)
                .build();
    }

    private InitiateCourtApplicationProceedings buildInitiateCourtApplicationProceedings(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases) {
        return buildInitiateCourtApplicationProceedings(judicialResultTypeId, withCourtOrder, withApplicationCases, false);
    }

    private InitiateCourtApplicationProceedings buildInitiateCourtApplicationProceedings(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases, final boolean withSummonsApprovalRequired) {
        return buildInitiateCourtApplicationProceedings(judicialResultTypeId, withCourtOrder, withApplicationCases, withSummonsApprovalRequired, true);
    }

    private InitiateCourtApplicationProceedings buildInitiateCourtApplicationProceedings(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases, final boolean withSummonsApprovalRequired, final boolean courtHearing) {
        return buildInitiateCourtApplicationProceedings(judicialResultTypeId, withCourtOrder, withApplicationCases, withSummonsApprovalRequired, courtHearing, LinkType.LINKED, "INACTIVE");
    }

    private InitiateCourtApplicationProceedings buildFirstHearingApplicationCourtProceedings() {
        return buildInitiateCourtApplicationProceedings(null, false, true, false, true, LinkType.FIRST_HEARING, "ACTIVE");
    }

    private InitiateCourtApplicationProceedings buildInitiateCourtApplicationProceedings(final UUID judicialResultTypeId, final boolean withCourtOrder, final boolean withApplicationCases, final boolean withSummonsApprovalRequired, final boolean courtHearing, final LinkType linkType, final String caseStatus) {
        final CourtOrder courtOrder;
        if (withCourtOrder) {
            courtOrder = CourtOrder.courtOrder()
                    .withCourtOrderOffences(singletonList(CourtOrderOffence.courtOrderOffence()
                            .withProsecutionCaseId(prosecutionCaseId)
                            .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                                    .withProsecutionAuthorityId(prosecutor2)
                                    .withProsecutionAuthorityCode(prosecutor2AuthCode)
                                    .build())
                            .withOffence(Offence.offence()
                                    .withOffenceCode(ORG_OFFENCE_CODE)
                                    .withWording(ORG_OFFENCE_WORDING)
                                    .withWordingWelsh(ORG_OFFENCE_WORDING_WELSH)
                                    .withId(offenceId)
                                    .build())
                            .build()))
                    .withId(randomUUID())
                    .withJudicialResultTypeId(judicialResultTypeId)
                    .build();
        } else {
            courtOrder = null;
        }
        final List<CourtApplicationCase> courtApplicationCases;
        if (withApplicationCases) {
            courtApplicationCases = singletonList(courtApplicationCase()
                    .withIsSJP(false)
                    .withCaseStatus(caseStatus)
                    .withProsecutionCaseId(randomUUID())
                    .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                            .withProsecutionAuthorityId(randomUUID()).build())
                    .withOffences(singletonList(Offence.offence()
                            .withOffenceCode(ORG_OFFENCE_CODE)
                            .withWording(ORG_OFFENCE_WORDING)
                            .withWordingWelsh(ORG_OFFENCE_WORDING_WELSH)
                            .withId(offenceId)
                            .build()))
                    .build());
        } else {
            courtApplicationCases = null;
        }

        return initiateCourtApplicationProceedings()
                .withCourtApplication(courtApplication()
                        .withApplicationReference(STRING.next())
                        .withId(randomUUID())
                        .withType(courtApplicationType()
                                .withLinkType(linkType)
                                .withProsecutorThirdPartyFlag(true)
                                .withSummonsTemplateType(Boolean.TRUE.equals(withSummonsApprovalRequired) ? GENERIC_APPLICATION : NOT_APPLICABLE)
                                .withResentencingActivationCode(RESENTENCING_ACTIVATION_CODE)
                                .withPrefix("Resentenced")
                                .build())
                        .withApplicant(buildCourtApplicationParty(prosecutor1))
                        .withRespondents(singletonList(buildCourtApplicationParty(respondent)))
                        .withSubject(buildCourtApplicationParty(subject))
                        .withCourtOrder(courtOrder)
                        .withCourtApplicationCases(courtApplicationCases)
                        .build())
                .withBoxHearing(Boolean.TRUE.equals(withSummonsApprovalRequired) ? boxHearingRequest().build() : null)
                .withCourtHearing(buildCourtHearing(courtHearing))
                .build();
    }

    private CourtHearingRequest buildCourtHearing(final boolean courtHearing) {
        if (Boolean.TRUE.equals(courtHearing)) {
            return CourtHearingRequest.courtHearingRequest()
                    .withCourtCentre(CourtCentre.courtCentre().withCode("COURTCENTER").build())
                    .withJudiciary(singletonList(JudicialRole.judicialRole().withJudicialId(randomUUID()).build()))
                    .withEarliestStartDateTime(ZonedDateTime.parse("2020-06-26T07:51Z"))
                    .withEstimatedMinutes(20)
                    .withJurisdictionType(JurisdictionType.CROWN)
                    .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate().withDuration(20).build())
                    .build();
        }
        return null;
    }

    public static Offence buildOffence(final UUID offenceId, final LocalDate convictionDate) {
        return Offence.offence()
                .withId(offenceId)
                .withConvictionDate(convictionDate)
                .build();
    }
}
