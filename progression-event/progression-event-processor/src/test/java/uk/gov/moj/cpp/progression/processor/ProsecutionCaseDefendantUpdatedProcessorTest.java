package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated;
import static uk.gov.justice.core.courts.UpdatedOrganisation.updatedOrganisation;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.progression.events.DefendantCustodialInformationUpdateRequested.defendantCustodialInformationUpdateRequested;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_ID;
import static uk.gov.moj.cpp.progression.helper.LinkSplitMergeHelper.CASE_URN;
import static uk.gov.moj.cpp.progression.processor.ProsecutionCaseDefendantUpdatedProcessor.DEFENDANTS;
import static uk.gov.moj.cpp.progression.processor.ProsecutionCaseDefendantUpdatedProcessor.MASTER_DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.processor.ProsecutionCaseDefendantUpdatedProcessor.MATCHED_MASTER_DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.processor.ProsecutionCaseDefendantUpdatedProcessor.PROGRESSION_COMMAND_UPDATE_DEFENDANT_CUSTODIAL_INFORMATION;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.DefendantCustodialInformationUpdateRequested;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.value.object.CPSNotificationVO;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseDefendantUpdatedProcessorTest {

    @InjectMocks
    private ProsecutionCaseDefendantUpdatedProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private ProgressionService progressionService;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private JsonObject payload;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> notificationServiceEnvelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<CPSNotificationVO> notificationServiceCPSNotificationVOArgumentCaptor;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverterSpy;

    public static final String MATCHED_DEFENDANT_CASES = "matchedDefendantCases";


    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        jsonObjectConverterSpy = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleDefendantCustodialInformationUpdatedEvent_WhenDifferentCase_DifferentDefendantId() {

        final String caseIdProsecutionCaseService = "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b";
        final String defendantIdProsecutionCaseService = "924cbf53-0b51-4633-9e99-2682be854af4";
        final String masterDefendantIdProsecutionCaseService = "976017a3-abfc-40fc-8ea2-ea0804716d61";

        final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested = defendantCustodialInformationUpdateRequested()
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.events.CustodialEstablishment.custodialEstablishment()
                        .withName("name")
                        .withCustody("custody")
                        .withId(randomUUID())
                        .build())
                .withDefendantId(UUID.fromString(defendantIdProsecutionCaseService))
                .withMasterDefendantId(UUID.fromString(masterDefendantIdProsecutionCaseService))
                .withProsecutionCaseId(UUID.fromString(caseIdProsecutionCaseService))
                .build();
        when(jsonObjectConverter.convert(any(), eq(DefendantCustodialInformationUpdateRequested.class)))
                .thenReturn(defendantCustodialInformationUpdateRequested);
        when(objectToJsonObjectConverter.convert(Mockito.any(uk.gov.moj.cpp.progression.events.CustodialEstablishment.class))).thenReturn(payload);
        when(progressionService.searchLinkedCases(any(), anyString())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(MATCHED_DEFENDANT_CASES, Json.createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add(CASE_ID, randomUUID().toString())
                                        .add(CASE_URN, "caseIdProsecutionCaseService")
                                        .add(MATCHED_MASTER_DEFENDANT_ID, masterDefendantIdProsecutionCaseService)
                                        .add(DEFENDANTS, createArrayBuilder().add(createObjectBuilder()
                                                        .add("id", randomUUID().toString())
                                                        .add("firstName", "firstName")
                                                        .add(MASTER_DEFENDANT_ID, masterDefendantIdProsecutionCaseService)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()
        ));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.defendant-custodial-information-update-requested")
                        .build(),
                objectToJsonObjectConverter.convert(defendantCustodialInformationUpdateRequested));

        this.eventProcessor.handleDefendantCustodialInformationUpdatedEvent(jsonEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is(PROGRESSION_COMMAND_UPDATE_DEFENDANT_CUSTODIAL_INFORMATION));

    }

    @Test
    public void shouldHandleDefendantCustodialInformationUpdatedEvent_WhenSameCase_DifferentDefendantId() {

        final String caseIdProsecutionCaseService = "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b";
        final String defendantIdProsecutionCaseService = "924cbf53-0b51-4633-9e99-2682be854af4";
        final String masterDefendantIdProsecutionCaseService = "976017a3-abfc-40fc-8ea2-ea0804716d61";

        final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested = defendantCustodialInformationUpdateRequested()
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.events.CustodialEstablishment.custodialEstablishment()
                        .withName("name")
                        .withCustody("custody")
                        .withId(randomUUID())
                        .build())
                .withDefendantId(UUID.fromString(defendantIdProsecutionCaseService))
                .withMasterDefendantId(UUID.fromString(masterDefendantIdProsecutionCaseService))
                .withProsecutionCaseId(UUID.fromString(caseIdProsecutionCaseService))
                .build();
        when(jsonObjectConverter.convert(any(), eq(DefendantCustodialInformationUpdateRequested.class)))
                .thenReturn(defendantCustodialInformationUpdateRequested);
        when(objectToJsonObjectConverter.convert(Mockito.any(DefendantCustodialInformationUpdateRequested.class))).thenReturn(payload);
        when(objectToJsonObjectConverter.convert(Mockito.any(uk.gov.moj.cpp.progression.events.CustodialEstablishment.class))).thenReturn(payload);
        when(progressionService.searchLinkedCases(any(), anyString())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(MATCHED_DEFENDANT_CASES, Json.createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add(CASE_ID, caseIdProsecutionCaseService)
                                        .add(CASE_URN, "caseIdProsecutionCaseService")
                                        .add(MATCHED_MASTER_DEFENDANT_ID, masterDefendantIdProsecutionCaseService)
                                        .add(DEFENDANTS, createArrayBuilder().add(createObjectBuilder()
                                                        .add("id", randomUUID().toString())
                                                        .add("firstName", "firstName")
                                                        .add(MASTER_DEFENDANT_ID, masterDefendantIdProsecutionCaseService)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()
        ));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.defendant-custodial-information-update-requested")
                        .build(),
                objectToJsonObjectConverter.convert(defendantCustodialInformationUpdateRequested));

        this.eventProcessor.handleDefendantCustodialInformationUpdatedEvent(jsonEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is(PROGRESSION_COMMAND_UPDATE_DEFENDANT_CUSTODIAL_INFORMATION));

    }

    @Test
    public void shouldHandleDefendantCustodialInformationUpdatedEvent_WhenSameCase_SameDefendantId() {

        final String caseIdProsecutionCaseService = "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b";
        final String defendantIdProsecutionCaseService = "924cbf53-0b51-4633-9e99-2682be854af4";
        final String masterDefendantIdProsecutionCaseService = "976017a3-abfc-40fc-8ea2-ea0804716d61";

        final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested = defendantCustodialInformationUpdateRequested()
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.events.CustodialEstablishment.custodialEstablishment()
                        .withName("name")
                        .withCustody("custody")
                        .withId(randomUUID())
                        .build())
                .withDefendantId(UUID.fromString(defendantIdProsecutionCaseService))
                .withMasterDefendantId(UUID.fromString(masterDefendantIdProsecutionCaseService))
                .withProsecutionCaseId(UUID.fromString(caseIdProsecutionCaseService))
                .build();
        when(jsonObjectConverter.convert(any(), eq(DefendantCustodialInformationUpdateRequested.class)))
                .thenReturn(defendantCustodialInformationUpdateRequested);
        when(objectToJsonObjectConverter.convert(Mockito.any(uk.gov.moj.cpp.progression.events.CustodialEstablishment.class))).thenReturn(payload);
        when(progressionService.searchLinkedCases(any(), anyString())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(MATCHED_DEFENDANT_CASES, Json.createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add(CASE_ID, caseIdProsecutionCaseService)
                                        .add(CASE_URN, "caseIdProsecutionCaseService")
                                        .add(MATCHED_MASTER_DEFENDANT_ID, masterDefendantIdProsecutionCaseService)
                                        .add(DEFENDANTS, createArrayBuilder().add(createObjectBuilder()
                                                        .add("id", defendantIdProsecutionCaseService)
                                                        .add("firstName", "firstName")
                                                        .add(MASTER_DEFENDANT_ID, masterDefendantIdProsecutionCaseService)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()
        ));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.defendant-custodial-information-update-requested")
                        .build(),
                objectToJsonObjectConverter.convert(defendantCustodialInformationUpdateRequested));

        this.eventProcessor.handleDefendantCustodialInformationUpdatedEvent(jsonEnvelope);

        verifyNoMoreInteractions(sender);

    }

    @Test
    public void shouldHandleDefendantCustodialInformationUpdatedEvent_WhenNoMatchingMasterDefendantIdFound() {

        final String caseIdProsecutionCaseService = "01702930-c1c8-4cfb-8f1c-1df9a58f4e5b";
        final String defendantIdProsecutionCaseService = "924cbf53-0b51-4633-9e99-2682be854af4";
        final String masterDefendantIdProsecutionCaseService = "976017a3-abfc-40fc-8ea2-ea0804716d61";

        final DefendantCustodialInformationUpdateRequested defendantCustodialInformationUpdateRequested = defendantCustodialInformationUpdateRequested()
                .withCustodialEstablishment(uk.gov.moj.cpp.progression.events.CustodialEstablishment.custodialEstablishment()
                        .withName("name")
                        .withCustody("custody")
                        .withId(randomUUID())
                        .build())
                .withDefendantId(UUID.fromString(defendantIdProsecutionCaseService))
                .withMasterDefendantId(randomUUID())
                .withProsecutionCaseId(UUID.fromString(caseIdProsecutionCaseService))
                .build();
        when(jsonObjectConverter.convert(any(), eq(DefendantCustodialInformationUpdateRequested.class)))
                .thenReturn(defendantCustodialInformationUpdateRequested);
        when(progressionService.searchLinkedCases(any(), anyString())).thenReturn(Optional.of(
                Json.createObjectBuilder().add(MATCHED_DEFENDANT_CASES, Json.createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add(CASE_ID, caseIdProsecutionCaseService)
                                        .add(CASE_URN, "caseIdProsecutionCaseService")
                                        .add(MATCHED_MASTER_DEFENDANT_ID, masterDefendantIdProsecutionCaseService)
                                        .add(DEFENDANTS, createArrayBuilder().add(createObjectBuilder()
                                                        .add("id", defendantIdProsecutionCaseService)
                                                        .add("firstName", "firstName")
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build()
        ));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.defendant-custodial-information-update-requested")
                        .build(),
                objectToJsonObjectConverter.convert(defendantCustodialInformationUpdateRequested));

        this.eventProcessor.handleDefendantCustodialInformationUpdatedEvent(jsonEnvelope);

        verifyNoMoreInteractions(sender);

    }

    @Test
    public void shouldHandleCasesReferredToCourtEventMessage() throws Exception {
        //Given
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        final DefendantUpdate pc = DefendantUpdate.defendantUpdate().withId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withOffences(Collections.emptyList())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withId(randomUUID())
                                .withName("HMP Belmarsh")
                                .withCustody("Prison")
                                .build())
                        .build())
                .build();
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(pc);
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(prosecutionCaseDefendantUpdated));

        //When
        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        //Then
        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

    }

    @Test
    public void shouldCallUpdateDefendantHearingWhenHearingIdExists() {
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        final UUID hearingId = randomUUID();
        final DefendantUpdate pc = DefendantUpdate.defendantUpdate().withId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withOffences(Collections.emptyList())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withCustodialEstablishment(CustodialEstablishment.custodialEstablishment()
                                .withId(randomUUID())
                                .withName("HMP Belmarsh")
                                .withCustody("Prison")
                                .build())
                        .build())
                .build();
        when(prosecutionCaseDefendantUpdated.getDefendant()).thenReturn(pc);
        when(prosecutionCaseDefendantUpdated.getHearingIds()).thenReturn(Arrays.asList(hearingId));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));

        //When
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(prosecutionCaseDefendantUpdated));

        //When
        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        //Then
        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
    }


    @Test
    public void shouldSendUpdateDefendantAssociationNotification_whenFutureHearingExists() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));

        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);

        final GetHearingsAtAGlance buildGetHearingsAtAGlanceObject = buildGetHearingsAtAGlanceObject();
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(inputEvent);
        when(jsonObjectToObjectConverter.convert(any(), eq(GetHearingsAtAGlance.class))).thenReturn(buildGetHearingsAtAGlanceObject);
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getReferenceDataProsecutorResponse()));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final String testCPSEmail = "abc@xyz.com";
        final JsonObject sampleJsonObject = createObjectBuilder().add("cpsEmailAddress", testCPSEmail).build();

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(sampleJsonObject));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
        verify(notificationService, times(1)).sendCPSNotification(any(), any());
    }



    @Test
    public void shouldSendUpdateDefendantToApplication_whenApplicationSummariesExists() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));

        when(jsonObjectConverter.convert(any(), eq(ProsecutionCaseDefendantUpdated.class))).thenReturn(inputEvent);
        when(objectToJsonObjectConverter.convert(Mockito.any(DefendantUpdate.class))).thenReturn(payload);
        final GetHearingsAtAGlance buildGetHearingsAtAGlanceObject = buildGetHearingsAtAGlanceObject();
        when(jsonObjectToObjectConverter.convert(any(), eq(GetHearingsAtAGlance.class))).thenReturn(buildGetHearingsAtAGlanceObject);
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getReferenceDataProsecutorResponse()));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse_WithApplicationSummaries()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final JsonObject sampleJsonObject = createObjectBuilder().build();

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(sampleJsonObject));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(notificationService, never()).sendCPSNotification(any(), any());
    }


    @Test
    public void shouldNotSendUpdateDefendantAssociationNotification_whenFutureHearingExists_AndCpsEmailDoesNotExists() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));
        final GetHearingsAtAGlance buildGetHearingsAtAGlanceObject = buildGetHearingsAtAGlanceObject();

        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(inputEvent);
        when(jsonObjectToObjectConverter.convert(any(), eq(GetHearingsAtAGlance.class))).thenReturn(buildGetHearingsAtAGlanceObject);
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getReferenceDataProsecutorResponse()));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final JsonObject sampleJsonObject = createObjectBuilder().build();

        when(referenceDataService.getOrganisationUnitById(any(), any(), any())).thenReturn(Optional.of(sampleJsonObject));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
        verify(this.notificationService, never()).sendCPSNotification(any(), any());
    }

    @Test
    public void shouldNotSendUpdateDefendantAssociationNotification_whenHearingsDoesNotExists() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));

        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        final GetHearingsAtAGlance buildGetHearingsAtAGlanceObject = buildGetHearingsAtAGlanceObject();
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(inputEvent);
        when(jsonObjectToObjectConverter.convert(any(), eq(GetHearingsAtAGlance.class))).thenReturn(buildGetHearingsAtAGlanceObject);
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getReferenceDataProsecutorResponse()));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse_WithoutHearings()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
        verify(this.notificationService, never()).sendCPSNotification(any(), any());
    }


    @Test
    public void shouldNotSendUpdateDefendantAssociationNotification_whenProsecutionIdDoesNotExists() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject_WithOutProsecutionId(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));

        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(inputEvent);
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
        verify(this.notificationService, never()).sendCPSNotification(any(), any());
    }


    @Test
    public void shouldNotSendUpdateDefendantAssociationNotification_whenUpdatedOrganisationDoesNotExists() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject_WithOutUpdatedOrganisation(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));

        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(inputEvent);
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
        verify(this.notificationService, never()).sendCPSNotification(any(), any());
    }


    @Test
    public void shouldNotSendUpdateDefendantAssociationNotification_whenProsecutorIsNotCPS() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));

        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class))
                .thenReturn(inputEvent);
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getReferenceDataNonCPSProsecutorResponse()));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
        verify(this.notificationService, never()).sendCPSNotification(any(), any());
    }


    @Test
    public void shouldNotSendUpdateDefendantAssociationNotification_whenFutureHearingDoesNotExists() {
        final UUID PROSECUTOR_ID = randomUUID();
        final String PROSECUTOR_CODE = "D24AW";
        final String CASE_URN = "90GD8989122";
        final UUID hearingId = randomUUID();
        final ProsecutionCaseDefendantUpdated inputEvent = buildProsecutionCaseDefendantUpdatedObject(CASE_URN, PROSECUTOR_CODE, PROSECUTOR_ID, Arrays.asList(hearingId));

        when(objectToJsonObjectConverter.convert(Mockito.any())).thenReturn(payload);
        final GetHearingsAtAGlance buildGetHearingsAtAGlanceObject = buildGetHearingsAtAGlanceObject();
        when(jsonObjectConverter.convert(payload, ProsecutionCaseDefendantUpdated.class)).thenReturn(inputEvent);
        when(jsonObjectToObjectConverter.convert(any(), eq(GetHearingsAtAGlance.class))).thenReturn(buildGetHearingsAtAGlanceObject);
        when(referenceDataService.getProsecutor(any(), any(), any())).thenReturn(Optional.of(getReferenceDataProsecutorResponse()));
        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse_WithoutFutureHearings()));
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(Optional.empty());

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(inputEvent));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(2)).send(this.envelopeArgumentCaptor.capture());
        verify(this.notificationService, never()).sendCPSNotification(any(), any());

    }

    @Test
    public void shouldSendCommandToUpdateActiveApplicationsOnCase() {

        final DefendantUpdate defendantUpdate = DefendantUpdate.defendantUpdate().withId(randomUUID())
                .withProsecutionCaseId(randomUUID())
                .withMasterDefendantId(randomUUID())
                .withProsecutionAuthorityReference("DERPF")
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .build())
                .withIsYouth(false)
                .withOffences(Collections.emptyList())
                .build();
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = ProsecutionCaseDefendantUpdated.prosecutionCaseDefendantUpdated()
                .withDefendant(defendantUpdate).build();
        //Given
        lenient().when(jsonObjectConverter.convert(any(), eq(ProsecutionCaseDefendantUpdated.class)))
                .thenReturn(prosecutionCaseDefendantUpdated);
        when(objectToJsonObjectConverter.convert(Mockito.any(DefendantUpdate.class))).thenReturn(payload);
        when(progressionService.getActiveApplicationsOnCase(any(), any())).thenReturn(
                Optional.ofNullable(createObjectBuilder().add("linkedApplications",
                        createArrayBuilder()
                                .add(createObjectBuilder()
                                        .add("applicationId", randomUUID().toString())
                                        .add("hearingIds",createArrayBuilder().add(randomUUID().toString()).build()).build())
                                .add(createObjectBuilder()
                                        .add("applicationId", randomUUID().toString())
                                        .add("hearingIds",createArrayBuilder().add(randomUUID().toString()).build()).build())
                                .build()).build()));

        when(progressionService.getProsecutionCaseDetailById(any(), any())).thenReturn(Optional.of(getProsecutionCaseResponse()));

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(JsonEnvelope.metadataBuilder()
                        .withUserId(randomUUID().toString())
                        .withId(randomUUID())
                        .withName("progression.event.prosecution-case-defendant-updated")
                        .build(),
                objectToJsonObjectConverter.convert(prosecutionCaseDefendantUpdated));

        this.eventProcessor.handleProsecutionCaseDefendantUpdatedEvent(jsonEnvelope);

        verify(this.sender, times(3)).send(this.envelopeArgumentCaptor.capture());
    }


    private JsonObject getProsecutionCaseResponse() {
        String response = null;
        try {
            response = Resources.toString(getResource("progression.event.prosecutioncase.defendantAssociation.cpsnotification-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }

    private JsonObject getProsecutionCaseResponse_WithApplicationSummaries() {
        String response = null;
        try {
            response = Resources.toString(getResource("progression.event.prosecutioncase.application-summaries-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }


    private JsonObject getProsecutionCaseResponse_WithoutHearings() {
        String response = null;
        try {
            response = Resources.toString(getResource("progression.event.prosecutioncase.defendantAssociation.nohearing-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }


    private JsonObject getProsecutionCaseResponse_WithoutFutureHearings() {
        String response = null;
        try {
            response = Resources.toString(getResource("progression.event.prosecutioncase.defendantAssociation.nofuturehearings-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }


    private JsonObject getReferenceDataProsecutorResponse() {
        String response = null;
        try {
            response = Resources.toString(getResource("referencedata.query.get-prosecutor-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }


    private JsonObject getReferenceDataNonCPSProsecutorResponse() {
        String response = null;
        try {
            response = Resources.toString(getResource("referencedata.query.get-noncps-prosecutor-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }
        return new StringToJsonObjectConverter().convert(response);
    }


    private ProsecutionCaseDefendantUpdated buildProsecutionCaseDefendantUpdatedObject(final String caseUrn, final String prosecutorCode, final UUID prosecutorId, List<UUID> hearingIdList) {
        String defendantString = null;
        try {
            defendantString = Resources.toString(getResource("defendantAssociation.cpsnotification-defendant-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }

        final JsonObject defendantJsonObject = new StringToJsonObjectConverter().convert(defendantString);
        final DefendantUpdate defendantUpdate = jsonObjectConverterSpy.convert(defendantJsonObject, DefendantUpdate.class);
        return prosecutionCaseDefendantUpdated()
                .withCaseUrn(caseUrn)
                .withProsecutionAuthorityId(prosecutorId.toString())
                .withHearingIds(hearingIdList)
                .withDefendant(defendantUpdate)
                .withUpdatedOrganisation(updatedOrganisation()
                        .withName("organisationName")
                        .withPhoneNumber("9875643645")
                        .withLaaContractNumber("AFSC553FAASA")
                        .withEmail("email@email.com")
                        .withId(randomUUID())
                        .withAddressPostcode("CF045J")
                        .withAddressLine4("Address4")
                        .withAddressLine3("Address3")
                        .withAddressLine2("Address2")
                        .withAddressLine1("Address1")
                        .build())
                .build();
    }

    private GetHearingsAtAGlance buildGetHearingsAtAGlanceObject() {
        String defendantString = null;
        try {
            defendantString = Resources.toString(getResource("progression.event.prosecutioncase.defendantAssociation.hearingsataglance-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }

        final JsonObject defendantJsonObject = new StringToJsonObjectConverter().convert(defendantString);
        final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectConverterSpy.convert(defendantJsonObject, GetHearingsAtAGlance.class);
        return hearingsAtAGlance;
    }


    private ProsecutionCaseDefendantUpdated buildProsecutionCaseDefendantUpdatedObject_WithOutProsecutionId(final String caseUrn, final String prosecutorCode, final UUID prosecutorId, List<UUID> hearingIdList) {
        String defendantString = null;
        try {
            defendantString = Resources.toString(getResource("defendantAssociation.cpsnotification-defendant-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }

        final JsonObject defendantJsonObject = new StringToJsonObjectConverter().convert(defendantString);
        final DefendantUpdate defendantUpdate = jsonObjectConverterSpy.convert(defendantJsonObject, DefendantUpdate.class);
        return prosecutionCaseDefendantUpdated()
                .withCaseUrn(caseUrn)
                .withHearingIds(hearingIdList)
                .withDefendant(defendantUpdate)
                .withUpdatedOrganisation(updatedOrganisation()
                        .withName("organisationName")
                        .withPhoneNumber("9875643645")
                        .withLaaContractNumber("AFSC553FAASA")
                        .withEmail("email@email.com")
                        .withId(randomUUID())
                        .withAddressPostcode("CF045J")
                        .withAddressLine4("Address4")
                        .withAddressLine3("Address3")
                        .withAddressLine2("Address2")
                        .withAddressLine1("Address1")
                        .build())
                .build();
    }


    private ProsecutionCaseDefendantUpdated buildProsecutionCaseDefendantUpdatedObject_WithOutUpdatedOrganisation(final String caseUrn, final String prosecutorCode, final UUID prosecutorId, List<UUID> hearingIdList) {
        String defendantString = null;
        try {
            defendantString = Resources.toString(getResource("defendantAssociation.cpsnotification-defendant-data.json"), defaultCharset());
        } catch (final Exception ignored) {
        }

        final JsonObject defendantJsonObject = new StringToJsonObjectConverter().convert(defendantString);
        final DefendantUpdate defendantUpdate = jsonObjectConverterSpy.convert(defendantJsonObject, DefendantUpdate.class);
        return prosecutionCaseDefendantUpdated()
                .withCaseUrn(caseUrn)
                .withHearingIds(hearingIdList)
                .withDefendant(defendantUpdate)
                .withProsecutionAuthorityId(randomUUID().toString())
                .build();
    }

}
