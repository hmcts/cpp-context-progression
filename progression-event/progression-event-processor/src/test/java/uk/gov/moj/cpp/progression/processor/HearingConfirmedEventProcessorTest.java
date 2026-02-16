package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ApplicationStatus.LISTED;
import static uk.gov.justice.core.courts.CivilOffence.civilOffence;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationCase.courtApplicationCase;
import static uk.gov.justice.core.courts.CourtApplicationType.courtApplicationType;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated.sendNotificationForAutoApplicationInitiated;
import static uk.gov.justice.core.courts.SummonsTemplateType.NOT_APPLICABLE;
import static uk.gov.justice.hearing.courts.Initiate.initiate;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ConfirmedDefendant;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedOffence;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantRequestFromCurrentHearingToExtendHearingCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ExtendHearingDefendantRequestUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingConfirmed;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListCourtHearing;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.core.courts.SendNotificationForAutoApplicationInitiated;
import uk.gov.justice.core.courts.UpdateHearingForPartialAllocation;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.listing.courts.ListNextHearings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.featurecontrol.FeatureControlGuard;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.helper.HearingNotificationHelper;
import uk.gov.moj.cpp.progression.processor.exceptions.CourtApplicationAndCaseNotFoundException;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.CalendarService;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.ListingService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.PartialHearingConfirmService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.dto.HearingNotificationInputData;
import uk.gov.moj.cpp.progression.utils.FileUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class HearingConfirmedEventProcessorTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @InjectMocks
    private HearingConfirmedEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private HearingConfirmed hearingConfirmed;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private PartialHearingConfirmService partialHearingConfirmService;

    @Mock
    private ListingService listingService;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private Function<Object, JsonEnvelope> publicEnveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private HearingListingNeeds hearingListingNeeds;
    @Mock
    private CourtApplication courtApplication;
    @Mock
    private JsonObject jsonObject;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;
    @Captor
    private ArgumentCaptor<JsonObject> envelopeCaptor;
    @Mock
    private HearingNotificationHelper hearingNotificationHelper;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private FeatureControlGuard featureControlGuard;

    @Mock
    private NotificationService notificationService = new NotificationService();

    @Captor
    private ArgumentCaptor<HearingNotificationInputData> hearingInputDataEnvelopeCaptor;

    @Mock
    private RefDataService referenceDataService;
    @Mock
    private Requester requester;
    @Mock
    private CalendarService calendarService;
    @Mock
    private DocumentGeneratorService documentGeneratorService;
    @Spy
    private Logger logger;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessage() throws Exception {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final UUID hearingId = randomUUID();
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(UUID.randomUUID())
                        .build()))
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("Trial")
                                .build())
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());

        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(any());
        verify(progressionService).prepareSummonsData(any(JsonEnvelope.class), any());
    }

    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessage_SendOnlinePleaWhenOPAFeatureON() throws Exception {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID courtCentreId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final UUID hearingId = randomUUID();
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("First hearing")
                        .build())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(UUID.randomUUID())
                        .build()))
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(featureControlGuard.isFeatureEnabled("OPA")).thenReturn(true);
        final JsonObject sampleJsonObject = createObjectBuilder().add("oucodeL3Name", "oucodeL3Name").build();
        when(referenceDataService.getOrganisationUnitById(courtCentreId, envelope, requester)).thenReturn(Optional.of(sampleJsonObject));
        final JsonObject sampleJsonObject2 = createObjectBuilder().add("isWelsh", false).build();
        when(referenceDataService.getCourtCentreWithCourtRoomsById(courtCentreId, envelope, requester)).thenReturn(Optional.of(sampleJsonObject2));
        when(calendarService.plusWorkingDays(LocalDate.now(), 11L, requester)).thenReturn(LocalDate.now().plusDays(60));
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now().plusDays(40)).build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("First hearing")
                                .build())
                        .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).build())
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withPersonDefendant(PersonDefendant.personDefendant()
                                                .withPersonDetails(Person.person()
                                                        .withFirstName("John")
                                                        .withLastName("Wick")
                                                        .withAddress(Address.address()
                                                                .withAddress1("Flat 2")
                                                                .withAddress2("9 Russell St").build()).build()).build())
                                        .withOffences(singletonList(Offence.offence()
                                                .withModeOfTrial("Either Way")
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().withCaseURN("caseUrn").build())
                                .build()))
                        .build());

        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(any());
        verify(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        verify(documentGeneratorService,times(1)).generatePostalDocumentForOpa(any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessage_DoNotSendOnlinePleaWhenOPAFeatureOFF() throws Exception {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final UUID hearingId = randomUUID();
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withCourtApplications(Arrays.asList(CourtApplication.courtApplication().build()))
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(UUID.randomUUID())
                        .build()))
                .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(featureControlGuard.isFeatureEnabled("OPA")).thenReturn(false);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("Trial")
                                .build())
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withIsCivil(false)
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(),any())).thenReturn(hearingInProgression);

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(any());
        verify(progressionService, times(1)).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        verify(documentGeneratorService,times(0)).generatePostalDocumentForOpa(any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessageAndForBulkCaseDoesNotPrepareSummons(){
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withIsGroupProceedings(true)
                .withType(HearingType.hearingType().withDescription("Plea").withId(randomUUID()).build())
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withProsecutionCases(Arrays.asList(prosecutionCase))
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withIsCivil(true)
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .withType(HearingType.hearingType().withId(randomUUID()). withDescription("Crime").build())
                        .build());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(any());
        verify(progressionService, never()).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
    }

    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessageWhenPartialHearingConfirm() throws Exception {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withIsCivil(false)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(randomUUID())
                                        .build()))
                                .build()))
                        .build()))
                .build();

        final JsonObject hearingJson = createHearingJson(objectToJsonObjectConverter.convert(hearing));

        ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);
        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(createProsecutionCase(offenceId, defendantId, caseId));

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(partialHearingConfirmService.getDifferences(confirmedHearing, hearing)).thenReturn(deltaProsecutionCases);
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(hearing);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.assign-defendant-request-from-current-hearing-to-extend-hearing")).thenReturn(enveloperFunction);
        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = buildUpdateHearingForPartialAllocation(hearingId);
        final ListCourtHearing listCourtHearing = buildListCourtHearing(randomUUID());
        when(partialHearingConfirmService.transformToUpdateHearingForPartialAllocation(hearingId, deltaProsecutionCases)).thenReturn(updateHearingForPartialAllocation);
        when(partialHearingConfirmService.transformToListCourtHearing(eq(deltaProsecutionCases), any(), any())).thenReturn(listCourtHearing);
        when(progressionService.retrieveHearing(envelope, hearingId)).thenReturn(hearing);

        eventProcessor.processEvent(envelope);

        verify(progressionService).updateHearingForPartialAllocation(envelope, updateHearingForPartialAllocation);
        verify(listingService).listCourtHearing(envelope, listCourtHearing);


    }


    @Test
    public void shouldHandleHearingConfirmedWithCasesEventMessageWhenPartialHearingConfirmWithSeedingHearing() throws Exception {
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID seedingHearing1Id = randomUUID();
        final UUID seedingHearing2Id = randomUUID();
        final String sittingDay = "2025-05-01";
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearing1Id).withSittingDay(sittingDay).build();
        final SeedingHearing seedingHearing2 = SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearing2Id).withSittingDay(sittingDay).build();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offence1Id,seedingHearing);
        final UUID hearingId = randomUUID();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withSeedingHearing(seedingHearing)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withIsCivil(false)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(Arrays.asList(Offence.offence()
                                                .withId(offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),
                                        Offence.offence()
                                                .withId(offence2Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),
                                        Offence.offence()
                                                .withId(offence3Id)
                                                .withSeedingHearing(seedingHearing2)
                                                .build()))
                                .build()))
                        .build()))
                .build();

        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();

        final List<ProsecutionCase> deltaProsecutionCases = Arrays.asList(ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(Arrays.asList(Offence.offence()
                                        .withId(offence2Id)
                                        .withSeedingHearing(seedingHearing)
                                        .build(),
                                Offence.offence()
                                        .withId(offence3Id)
                                        .withSeedingHearing(seedingHearing2)
                                        .build()))
                        .build()))
                .build());

        final UpdateHearingForPartialAllocation updateHearingForPartialAllocation = buildUpdateHearingForPartialAllocation(hearingId);
        final ListNextHearings listNextHearings = buildListNextHearings(randomUUID(), seedingHearing1Id);
        final Map<SeedingHearing, List<ProsecutionCase>> relatedSeedingHearingsProsecutionCasesMap = new HashMap<>();
        relatedSeedingHearingsProsecutionCasesMap.put(seedingHearing2, Arrays.asList(ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(singletonList(Defendant.defendant()
                        .withId(defendantId)
                        .withOffences(singletonList(Offence.offence().withId(offence3Id).withSeedingHearing(seedingHearing2).build()))
                        .build()))
                .build()));
        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        doNothing().when(progressionService).prepareSummonsData(any(), any());
        when(partialHearingConfirmService.getDifferences(confirmedHearing, hearing)).thenReturn(deltaProsecutionCases);
        when(partialHearingConfirmService.transformToUpdateHearingForPartialAllocation(hearingId, deltaProsecutionCases)).thenReturn(updateHearingForPartialAllocation);
        when(partialHearingConfirmService.transformToListNextCourtHearing(any(), any(), any(), eq(seedingHearing))).thenReturn(listNextHearings);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(hearing);
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command-link-prosecution-cases-to-hearing")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.assign-defendant-request-from-current-hearing-to-extend-hearing")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "public.progression.seeding-hearing-updated-with-next-hearings")).thenReturn(publicEnveloperFunction);
        when(progressionService.retrieveHearing(envelope, hearingId)).thenReturn(hearing);

        eventProcessor.processEvent(envelope);

        verify(progressionService).updateHearingForPartialAllocation(envelope, updateHearingForPartialAllocation);
        verify(listingService).listNextCourtHearings(envelope, listNextHearings);
        verify(sender, times(3)).send(finalEnvelope);
        verify(publicEnveloperFunction, times(2)).apply(envelopeCaptor.capture());
        assertThat(envelopeCaptor.getAllValues().size(), is(2));
        envelopeCaptor.getAllValues().stream().filter(event -> event.getString("seedingHearingId").equals(seedingHearing1Id.toString()))
                .forEach(event -> {
                    assertThat(event.getString("hearingDay"), is(sittingDay));
                    assertThat(event.getString("seedingHearingId"), is(seedingHearing1Id.toString()));
                    assertThat(event.containsKey("oldNextHearingId"), is(false));
                    assertThat(event.getString("newNextHearingId"), is(listNextHearings.getHearings().get(0).getId().toString()));
                });

        envelopeCaptor.getAllValues().stream().filter(event -> event.getString("seedingHearingId").equals(seedingHearing2Id.toString()))
                .forEach(event -> {
                    assertThat(event.getString("hearingDay"), is(sittingDay));
                    assertThat(event.getString("seedingHearingId"), is(seedingHearing2Id.toString()));
                    assertThat(event.getString("oldNextHearingId"), is(hearingId.toString()));
                    assertThat(event.getString("newNextHearingId"), is(listNextHearings.getHearings().get(0).getId().toString()));
                });

    }

    @Test
    public void shouldProcessHearingConfirmedForExtendHearing() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final UUID hearingId = randomUUID();
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withExistingHearingId(randomUUID())
                .build();

        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(randomUUID()).build())).build();

        hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingConfirmed).thenReturn(hearing);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearing);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.of(Json.
                createObjectBuilder().add("hearing", Json.createObjectBuilder().build())
                .add("hearingListingStatus", "HEARING_INITIALISED")
                .build()));

        when(enveloper.withMetadataFrom(envelope, "progression.command.extend-hearing")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.prepare-summons-data-for-extended-hearing")).thenReturn(enveloperFunction);
        when(progressionService.transformHearingToHearingListingNeeds(any(Hearing.class), any(UUID.class))).thenReturn(hearingListingNeeds);
        when(partialHearingConfirmService.getDifferences(any(), any())).thenReturn(new ArrayList<>());
        when(progressionService.transformToHearingFrom(any(), any())).thenReturn(Hearing.hearing().build());

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(any());
        verify(objectToJsonObjectConverter, times(2)).convert(any());
        verify(progressionService).transformHearingToHearingListingNeeds(any(), any());
        verify(progressionService).transformToHearingFrom(any(), any());
    }

    @Test
    public void shouldNotHandleAndThrowExceptionForHearingConfirmedIfProsecutionCaseIsNotThereYet() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        ProsecutionCase prosecutionCase = createProsecutionCase(offenceId, defendantId, caseId);

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);

        assertThrows(CourtApplicationAndCaseNotFoundException.class, () -> eventProcessor.processEvent(envelope));
    }

    @Test
    public void shouldProcessHearingConfirmedForFullyExtendHearing() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final UUID hearingId = randomUUID();
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withExistingHearingId(randomUUID())
                .withFullExtension(Boolean.TRUE)
                .build();

        final Hearing hearing = Hearing.hearing()
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase().withId(randomUUID()).build())).build();

        hearingListingNeeds = HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(hearingConfirmed).thenReturn(hearing);
        when(progressionService.getHearing(any(), any())).thenReturn(Optional.of(Json.
                createObjectBuilder().add("hearing", Json.createObjectBuilder().build())
                .add("hearingListingStatus", "HEARING_INITIALISED")
                .build()));


        when(enveloper.withMetadataFrom(envelope, "progression.command.extend-hearing")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, "progression.command.prepare-summons-data-for-extended-hearing")).thenReturn(enveloperFunction);

        when(progressionService.transformHearingToHearingListingNeeds(any(Hearing.class), any(UUID.class))).thenReturn(hearingListingNeeds);
        when(partialHearingConfirmService.getDifferences(any(), any())).thenReturn(new ArrayList<>());
        when(progressionService.transformToHearingFrom(any(), any())).thenReturn(Hearing.hearing().build());

        when(progressionService.retrieveHearing(envelope, hearingId)).thenReturn(hearing);

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(any());
        verify(objectToJsonObjectConverter, times(2)).convert(any());
        verify(progressionService).transformHearingToHearingListingNeeds(any(), any());
        verify(progressionService).transformToHearingFrom(any(), any());
        verify(progressionService).sendListingCommandToDeleteHearing(eq(envelope), eq(hearingId));
    }

    @Test
    public void shouldProcessExtendHearingDefendantRequestCreated() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withExistingHearingId(randomUUID())
                .build();

        final ExtendHearingDefendantRequestCreated extendHearingDefendantRequestCreated = ExtendHearingDefendantRequestCreated.extendHearingDefendantRequestCreated()
                .withConfirmedHearing(confirmedHearing)
                .build();

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(extendHearingDefendantRequestCreated);
        when(enveloper.withMetadataFrom(envelope, "progression.command.extend-hearing-defendant-request-update-requested")).thenReturn(enveloperFunction);

        eventProcessor.processExtendHearingDefendantRequestCreated(envelope);

        verify(sender).send(any());
        verify(objectToJsonObjectConverter).convert(any());
    }

    @Test
    public void shouldProcessExtendHearingDefendantRequestUpdated() {
        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(caseId, defendantId, offenceId, null);
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .withExistingHearingId(randomUUID())
                .build();

        final ExtendHearingDefendantRequestUpdated extendHearingDefendantRequestUpdated = ExtendHearingDefendantRequestUpdated.extendHearingDefendantRequestUpdated()
                .withConfirmedHearing(confirmedHearing)
                .build();


        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(extendHearingDefendantRequestUpdated);
        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));

        eventProcessor.processExtendHearingDefendantRequestUpdated(envelope);

        verify(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        verify(jsonObjectToObjectConverter).convert(any(), any());
    }

    @Test
    public void shouldHandleHearingConfirmedWithApplicationsEventMessage() {
        final UUID hearingId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withIsCivil(false)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(randomUUID())
                                        .withListingNumber(1)
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final JsonObject hearingJson = createHearingJson(objectToJsonObjectConverter.convert(hearing));
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withCourtApplicationIds(singletonList(randomUUID()))
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(hearing);
        doNothing().when(progressionService).updateCourtApplicationStatus(any(), any(), anyList(), any());
        when(enveloper.withMetadataFrom(envelope, "progression.command-enrich-hearing-initiate")).thenReturn(enveloperFunction);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class))
                .thenReturn(hearingConfirmed);
        when(progressionService.retrieveHearing(envelope, hearingId)).thenReturn(hearing);

        eventProcessor.processEvent(envelope);

        verify(sender).send(finalEnvelope);
        verify(progressionService).updateCourtApplicationStatus(any(), any(), anyList(), any());
    }

    @Test
    public void shouldHandleHearingConfirmedWithCaseAndApplicationsEventMessage() {
        final UUID hearingId = randomUUID();
        final ConfirmedProsecutionCase confirmedProsecutionCase = createConfirmedProsecutionCase(randomUUID(), randomUUID(), randomUUID(), null);
        final List<UUID> courtApplicationIds = singletonList(randomUUID());
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(hearingId)
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withCourtApplicationIds(courtApplicationIds)
                .withProsecutionCases(singletonList(confirmedProsecutionCase))
                .build();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing().build();

        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                .withType(HearingType.hearingType()
                        .withId(randomUUID())
                        .withDescription("Trial")
                        .build())
                .withSeedingHearing(seedingHearing)
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(randomUUID())
                        .withIsCivil(true)
                        .withDefendants(singletonList(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(singletonList(Offence.offence()
                                        .withId(randomUUID())
                                        .withCivilOffence(civilOffence().withIsExParte(true).build())
                                        .build()))
                                .build()))
                        .build()))
                .build();
        final JsonObject hearingJson = createHearingJson(objectToJsonObjectConverter.convert(hearing));

        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class))
                .thenReturn(hearingConfirmed);
        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);

        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(confirmedHearing, envelope, seedingHearing)).thenReturn(hearing);
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(envelope, hearingId)).thenReturn(hearing);

        eventProcessor.processEvent(envelope);

        verify(sender, times(2)).send(finalEnvelope);
        verify(progressionService).updateCourtApplicationStatus(envelope, hearing, courtApplicationIds, LISTED);
        verify(progressionService).prepareSummonsData(envelope, confirmedHearing);
    }

    @Test
    public void shouldCallInitiateHearing() throws Exception {
        final Initiate arbitraryInitiateObj = initiate().withHearing(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                                .withIsCivil(false)
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(UUID.randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(UUID.randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build()
        ).build();
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        //When
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), Initiate.class))
                .thenReturn(arbitraryInitiateObj);


        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);

        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);
        when(enveloper.withMetadataFrom(envelope, HearingConfirmedEventProcessor.PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT))
                .thenReturn(enveloperFunction);

        eventProcessor.processHearingInitiatedEnrichedEvent(envelope);

        //Then
        verify(sender, times(2)).send(finalEnvelope);
    }

    @Test
    public void shouldProcessDefendantRequestFromCurrentHearingToExtendHearingCreated() {
        final UUID currentHearingId = randomUUID();
        final UUID extendHearingId = randomUUID();
        final UUID defendantId = randomUUID();

        final DefendantRequestFromCurrentHearingToExtendHearingCreated event = DefendantRequestFromCurrentHearingToExtendHearingCreated.defendantRequestFromCurrentHearingToExtendHearingCreated()
                .withExtendHearingId(extendHearingId)
                .withCurrentHearingId(currentHearingId)
                .withDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .build()))
                .build();

        final JsonObject payload = objectToJsonObjectConverter.convert(event);
        final JsonEnvelope eventJson = envelopeFrom(metadataWithRandomUUID("progression.event.defendant-request-from-current-hearing-to-extend-hearing-created"), payload);

        when(jsonObjectToObjectConverter.convert(any(JsonObject.class), any())).thenReturn(event);

        eventProcessor.processDefendantRequestFromCurrentHearingToExtendHearingCreated(eventJson);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.assign-defendant-request-to-extend-hearing"));
        assertThat(commandEvent.payload().toString(), isJson(anyOf(
                withJsonPath("$.hearingId", equalTo(extendHearingId)),
                withJsonPath("$.defendantRequests[0]", notNullValue()))));

    }

    @Test
    public void shouldHandleHearingConfirmed_NotificationSendTrue() {

        setField(this.eventProcessor, "jsonObjectConverter", new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper()));
        setField(this.eventProcessor, "objectToJsonObjectConverter", new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()));

        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("public.listing.hearing-confirmed.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_TYPE%", "Plea")
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder()
                        .withName("public.listing.hearing-confirmed")
                        .withId(randomUUID())
                        .build(),
                payload);

        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(UUID.randomUUID())
                        .build()))
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(("e4648583-eb0f-438e-aab5-5eff29f3f7b4"));
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("First hearing")
                                .build())
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);
        eventProcessor.processEvent(jsonEnvelope);

        verify(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        verify(hearingNotificationHelper, times(1)).sendHearingNotificationsToRelevantParties(any(), hearingInputDataEnvelopeCaptor.capture());
        HearingNotificationInputData inputData = hearingInputDataEnvelopeCaptor.getValue();
        assertThat(inputData.getHearingId(), is(hearingId));
        assertThat(inputData.getHearingType(), is("Plea"));
    }


    @Test
    public void shouldHandleHearingConfirmedforAutApplication_NotificationSendTrue() {

        setField(this.eventProcessor, "jsonObjectConverter", new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper()));
        setField(this.eventProcessor, "objectToJsonObjectConverter", new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()));

        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("public.listing.hearing-confirmed.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_TYPE%", "Plea")
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder()
                        .withName("public.listing.hearing-confirmed")
                        .withId(randomUUID())
                        .build(),
                payload);

        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(UUID.randomUUID())
                        .build()))
                .withCourtApplications(singletonList(courtApplication()
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
                        .build()))
                .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentre(CourtCentre.courtCentre().withCode("COURTCENTER").build())
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(("e4648583-eb0f-438e-aab5-5eff29f3f7b4"));
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .withCourtApplications(singletonList(courtApplication()
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
                                .build()))
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withCourtCentre(CourtCentre.courtCentre().withCode("COURTCENTER").build())
                        .withType(HearingType.hearingType().withId(randomUUID()). withDescription("Crime").build())
                        .build());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);

        eventProcessor.processEvent(jsonEnvelope);

        verify(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));

        verify(hearingNotificationHelper, times(1)).sendHearingNotificationsToRelevantParties(any(), hearingInputDataEnvelopeCaptor.capture());
        HearingNotificationInputData inputData = hearingInputDataEnvelopeCaptor.getValue();
        assertThat(inputData.getHearingId(), is(hearingId));
        assertThat(inputData.getHearingType(), is("Plea"));

        verify(this.sender, times(3)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> commandEvents = this.senderJsonEnvelopeCaptor.getAllValues();
        assertThat(commandEvents.size(), is(3));
    }

    @Test
    public void shouldSendNotificationForAutoApplication() throws Exception {
        final SendNotificationForAutoApplicationInitiated sendNotificationForAutoApplicationInitiated =
                sendNotificationForAutoApplicationInitiated()
                        .withCourtApplication(courtApplication()
                                .withId(randomUUID())
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
                        .withIssueDate(LocalDate.now())
                        .build();
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        //When
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), SendNotificationForAutoApplicationInitiated.class))
                .thenReturn(sendNotificationForAutoApplicationInitiated);

        eventProcessor.sendNotificationForAutoApplication(envelope);

        //Then
        verify(notificationService, times(1)).sendNotificationForAutoApplication(eq(envelope), eq(sendNotificationForAutoApplicationInitiated));
    }


    @Test
    public void shouldHandleHearingConfirmedForApplication_NotificationSendTrue() {

        setField(this.eventProcessor, "jsonObjectConverter", new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper()));
        setField(this.eventProcessor, "objectToJsonObjectConverter", new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()));

        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID caseId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("public.listing.hearing-confirmed-inactive-case-application.json")
                .replaceAll("%APPLICATION_ID%", applicationId.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_TYPE%", "Plea"));

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder()
                        .withName("public.listing.hearing-confirmed")
                        .withId(randomUUID())
                        .build(),
                payload);

        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        JsonObject prosecutionCaseJson2 = createObjectBuilder()
                .add("prosecutionCase", prosecutionCaseJson).build();

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(hearingId)
                .withCourtApplications(singletonList(CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase()
                                .withProsecutionCaseId(caseId)
                                .withCaseStatus("INACTIVE")
                                .withOffences(singletonList(Offence.offence()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build()))
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(caseId)
                        .build()))
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        when(progressionService.getProsecutionCaseById(any(), any())).thenReturn(prosecutionCaseJson2);
        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(applicationParameters.getNotifyHearingTemplateId()).thenReturn(("e4648583-eb0f-438e-aab5-5eff29f3f7b4"));
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(hearingId)
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withType(HearingType.hearingType().withId(randomUUID()).withDescription("First hearing").build())
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase().withId(caseId)
                                .withDefendants(singletonList(Defendant.defendant().withId(defendantId)
                                        .withOffences(singletonList(Offence.offence().withId(offenceId)
                                                .build())).build())).build()))
                        .build());

        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);

        eventProcessor.processEvent(jsonEnvelope);

        verify(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        verify(hearingNotificationHelper, times(1)).sendHearingNotificationsToRelevantParties(any(), hearingInputDataEnvelopeCaptor.capture());
        HearingNotificationInputData inputData = hearingInputDataEnvelopeCaptor.getValue();
        assertThat(inputData.getHearingId(), is(hearingId));
        assertThat(inputData.getHearingType(), is("Plea"));
        assertTrue(inputData.getCaseIds().contains(caseId));
        assertTrue(inputData.getDefendantIds().contains(defendantId));
        assertTrue(inputData.getDefendantOffenceListMap().get(defendantId).contains(offenceId));
    }

    @Test
    public void shouldHandleHearingConfirmed_NotificationSendFalse() {

        setField(this.eventProcessor, "jsonObjectConverter", new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper()));
        setField(this.eventProcessor, "objectToJsonObjectConverter", new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper()));

        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();

        final JsonObject payload = FileUtil.jsonFromString(FileUtil.getPayload("public.listing.hearing-confirmed-notification-send-false.json")
                .replaceAll("%CASE_ID%", caseId.toString())
                .replaceAll("%HEARING_ID%", hearingId.toString())
                .replaceAll("%HEARING_TYPE%", "Plea")
                .replaceAll("%DEFENDANT_ID%", defendantId.toString()));

        JsonEnvelope jsonEnvelope = envelopeFrom(metadataBuilder()
                        .withName("public.listing.hearing-confirmed")
                        .withId(randomUUID())
                        .build(),
                payload);

        JsonObject prosecutionCaseJson = createProsecutionCaseJson(offenceId, defendantId, caseId);
        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                        .withId(UUID.randomUUID())
                        .build()))
                .build();

        final JsonObject hearingInProgressionJson = createHearingJson(objectToJsonObjectConverter.convert(hearingInProgression));

        doNothing().when(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withType(HearingType.hearingType()
                                .withId(randomUUID())
                                .withDescription("First hearing")
                                .build())
                        .withProsecutionCases(singletonList(ProsecutionCase.prosecutionCase()
                                .withDefendants(singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);

        eventProcessor.processEvent(jsonEnvelope);

        verify(progressionService).prepareSummonsData(any(JsonEnvelope.class), any(ConfirmedHearing.class));
        verify(hearingNotificationHelper, never()).sendHearingNotificationsToRelevantParties(any(), hearingInputDataEnvelopeCaptor.capture());
    }


    private ConfirmedProsecutionCase createConfirmedProsecutionCase(final UUID prosecutionCaseId, final UUID defendantId, final UUID offenceId, final SeedingHearing seedingHearing) {
        return ConfirmedProsecutionCase.confirmedProsecutionCase()
                .withDefendants(singletonList(createConfirmedDefendant(defendantId, offenceId, seedingHearing)))
                .withId(prosecutionCaseId)
                .build();
    }

    private ConfirmedDefendant createConfirmedDefendant(final UUID defendantId, final UUID offenceId, final SeedingHearing seedingHearing) {
        return ConfirmedDefendant.confirmedDefendant()
                .withId(defendantId)
                .withOffences(singletonList(createConfirmedOffence(offenceId,seedingHearing)))
                .build();
    }

    private ConfirmedOffence createConfirmedOffence(final UUID offenceId, final SeedingHearing seedingHearing) {
        return ConfirmedOffence.confirmedOffence()
                .withId(offenceId)
                .withSeedingHearing(seedingHearing)
                .build();
    }

    private ProsecutionCase createProsecutionCase(final UUID offenceId, final UUID defendantId, final UUID caseId) {
        return ProsecutionCase.prosecutionCase().withId(caseId)
                .withDefendants(singletonList(createDefendant(defendantId, offenceId)))
                .build();
    }

    private Defendant createDefendant(final UUID defendantId, final UUID offenceId) {
        return Defendant.defendant()
                .withId(defendantId)
                .withOffences(singletonList(createOffence(offenceId)))
                .build();
    }

    private Offence createOffence(final UUID offenceId) {
        return Offence.offence()
                .withOffenceCode("one")
                .withId(offenceId)
                .build();
    }


    private JsonObject createProsecutionCaseJson(final UUID offenceId, final UUID defendantId, final UUID caseId) {
        return createObjectBuilder()
                .add("id", caseId.toString())
                .add("defendants", Json.createArrayBuilder().add(createObjectBuilder()
                                .add("id", defendantId.toString())
                                .add("offences", Json.createArrayBuilder().add(createObjectBuilder()
                                                .add("id", offenceId.toString())
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private JsonObject createHearingJson(final JsonObject hearing) {
        return createObjectBuilder()
                .add("hearing", hearing)
                .build();
    }

    private UpdateHearingForPartialAllocation buildUpdateHearingForPartialAllocation(UUID hearingId) {
        return UpdateHearingForPartialAllocation.updateHearingForPartialAllocation()
                .withHearingId(hearingId)
                .build();
    }

    private ListCourtHearing buildListCourtHearing(final UUID hearingId) {
        return ListCourtHearing.listCourtHearing()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build()))
                .build();
    }

    private ListNextHearings buildListNextHearings(final UUID hearingId, final UUID seedingHearingId){
        return ListNextHearings.listNextHearings()
                .withHearings(Arrays.asList(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build()))
                .withHearingId(seedingHearingId)
                .build();
    }

    @Test
    public void shouldCallInitiateHearingWhenProsecutionCasesNotPresent() throws Exception {
        final Initiate arbitraryInitiateObj = initiate().withHearing(
                Hearing.hearing()
                        .withId(UUID.randomUUID())
                        .withHearingDays(Arrays.asList(HearingDay.hearingDay().build()))
                        .withCourtApplications(Arrays.asList(CourtApplication.courtApplication()
                                .withId(randomUUID())
                                .withJudicialResults(Arrays.asList(JudicialResult.judicialResult()
                                        .withJudicialResultId(randomUUID())
                                        .build()))
                                .build()))
                        .build()
        ).build();
        //Given
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        //When
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), Initiate.class))
                .thenReturn(arbitraryInitiateObj);


        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(enveloper.withMetadataFrom(envelope, "hearing.initiate")).thenReturn(enveloperFunction);

        eventProcessor.processHearingInitiatedEnrichedEvent(envelope);

        //Then
        verify(sender, times(1)).send(finalEnvelope);
        verify(progressionService).populateHearingToProbationCaseworker(any(JsonEnvelope.class), any(UUID.class));
    }

    @Test
    public void shouldNotSendPostalNotificationForStandaloneApplicationsWithHearingTypeWarrants(){
        final UUID applicationId = randomUUID();
        final UUID warrantHearingTypeId = fromString("638ced9d-3f95-4e99-b27b-47fa5a2c6add");

        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withIsGroupProceedings(true)
                .withCourtApplicationIds(List.of(applicationId))
                .withType(HearingType.hearingType().withDescription("Warrant of Further Detention").withId(warrantHearingTypeId).build())
                .build();

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withCourtApplications(List.of(CourtApplication.courtApplication().withId(applicationId).build()))
                .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withType(HearingType.hearingType().withId(warrantHearingTypeId). withDescription("Warrant of Further Detention").build())
                        .build());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        eventProcessor.processEvent(envelope);

        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        verify(logger,times(0)).info("Sending notification as hearing type is not: Application or Trial");

    }

    @Test
    public void shouldNotSendPostalNotificationForStandaloneApplicationsWithHearingTypePCB(){
        final UUID applicationId = randomUUID();
        final UUID pcbHearingTypeId = fromString("3a2d160f-363b-4360-96e1-0007a400a64c");
        final ConfirmedHearing confirmedHearing = ConfirmedHearing.confirmedHearing()
                .withId(randomUUID())
                .withIsGroupProceedings(true)
                .withCourtApplicationIds(List.of(applicationId))
                .withType(HearingType.hearingType().withDescription("Pre-Charge Bail").withId(pcbHearingTypeId).build())
                .build();

        final Hearing hearingInProgression = Hearing.hearing()
                .withId(randomUUID())
                .withSeedingHearing(SeedingHearing.seedingHearing().build())
                .withCourtApplications(List.of(CourtApplication.courtApplication().withId(applicationId).build()))
                .build();

        when(hearingConfirmed.getConfirmedHearing()).thenReturn(confirmedHearing);
        when(envelope.payloadAsJsonObject()).thenReturn(payload);
        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), HearingConfirmed.class)).thenReturn(hearingConfirmed);
        when(progressionService.transformConfirmedHearing(any(), any(), any())).thenReturn(
                Hearing.hearing()
                        .withId(randomUUID())
                        .withHearingDays(singletonList(HearingDay.hearingDay().withSittingDay(new UtcClock().now()).build()))
                        .withType(HearingType.hearingType().withId(pcbHearingTypeId). withDescription("Pre-Charge Bail").build())
                        .build());
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);
        when(progressionService.retrieveHearing(any(), any())).thenReturn(hearingInProgression);
        when(enveloperFunction.apply(any())).thenReturn(finalEnvelope);
        eventProcessor.processEvent(envelope);

        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(1)).send(captor.capture());
        verify(logger,times(0)).info("Sending notification as hearing type is not: Application or Trial");

    }
}