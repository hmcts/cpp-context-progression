package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.CASE_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.DEFENDANT_DATA;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.FINALISED_FORM_DATA;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.FIRST_NAME;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.FORM_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.IS_YOUTH;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.LAST_NAME;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PET_DEFENDANTS;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PET_FORM_DATA;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PET_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PROSECUTION_CASE;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_DETAIL_UPDATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_CREATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_FINALISED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_UPDATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_OPERATION_FAILED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.SUBMISSION_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.UPDATED_BY;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.USER_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.USER_NAME;
import static uk.gov.moj.cpp.progression.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.utils.FileUtil.jsonFromString;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.moj.cpp.progression.command.UpdateCpsDefendantId;
import uk.gov.moj.cpp.progression.helper.DocmosisTextHelper;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.RefDataService;
import uk.gov.moj.cpp.progression.service.UsersGroupService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class PetFormEventProcessorTest {

    public static final String userId = randomUUID().toString();
    public static final String COURT_DOCUMENT_ID = "courtDocumentId";
    public static final String DOCUMENT_CATEGORY = "documentCategory";
    public static final String DOCUMENT_TYPE_ID = "documentTypeId";
    public static final String CASE_DOCUMENT = "caseDocument";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";

    public static final String PROGRESSION_COMMAND_CREATE_PET_FORM = "progression.command.create-pet-form";
    public static final String PROGRESSION_COMMAND_UPDATE_CPS_DEFENDANT_ID = "progression.command.update-cps-defendant-id";
    public static final String PROGRESSION_EVENT_PET_OPERATION_FAILED = "progression.event.pet-operation-failed";
    public static final String PROGRESSION_EVENT_PET_DETAIL_UPDATED = "progression.event.pet-detail-updated";
    public static final String PROGRESSION_EVENT_PET_FORM_FINALISED = "progression.event.pet-form-finalised";
    public static final String PROGRESSION_EVENT_PET_FORM_DEFENDANT_UPDATED = "progression.event.pet-form-defendant-updated";
    public static final String PROGRESSION_EVENT_PET_FORM_UPDATED = "progression.event.pet-form-updated";
    public static final String PROGRESSION_EVENT_PET_FORM_CREATED = "progression.event.pet-form-created";
    public static final String PROGRESSION_COMMAND_ADD_COURT_DOCUMENT = "progression.command.add-court-document";
    public static final String MATERIAL_ID = "materialId";
    public static final String COURT_DOCUMENT = "courtDocument";
    public static final UUID CASE_DOCUMENT_TYPE_ID = fromString("6b9df1fb-7bce-4e33-88dd-db91f75adeb8");
    public static final String NAME = "name";
    public static final String CPS_USER_NAME = "cps user name";
    private static final String CPS_DEFENDANT_ID = "cpsDefendantId";

    @Mock
    private Sender sender;

    @InjectMocks
    private PetFormEventProcessor petFormEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> defaultEnvelopeArgumentCaptor;

    @Mock
    private MaterialService materialService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private Requester requester;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private DocmosisTextHelper docmosisTextHelper;

    @Mock
    private UsersGroupService usersGroupService;


    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldRaisePetFormCreatedPublicEvent() {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String formId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_CREATED).withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
                        .add(FORM_ID, formId)
                        .add(IS_YOUTH, true)
                        .add(PET_DEFENDANTS, createArrayBuilder().build())
                        .add(USER_NAME, "cps user name")
        );

        petFormEventProcessor.petFormCreated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_CREATED));
        verifyPetFormPublicEvent(petId, caseId, userId, envelope);
    }

    @Test
    public void shouldRaisePetFormUpdatedPublicEvent() {

        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();

        when(usersGroupService.getUserById(any(), any())).thenReturn(createObjectBuilder().add(ID, userId).add(FIRST_NAME, FIRST_NAME).add(LAST_NAME, LAST_NAME).build());

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
                        .add(PET_FORM_DATA, "")
                        .add(USER_ID, userId)
        );

        petFormEventProcessor.petFormUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_UPDATED));
        verifyPetFormPublicEvent(petId, caseId, userId, envelope);
    }

    @Test
    public void shouldRaisePetFormDefendantUpdatedEvent() {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String userId = randomUUID().toString();

        when(usersGroupService.getUserById(any(), any())).thenReturn(createObjectBuilder().add(ID, userId).add(FIRST_NAME, FIRST_NAME).add(LAST_NAME, LAST_NAME).build());

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_DEFENDANT_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
                        .add(DEFENDANT_ID, defendantId)
                        .add(DEFENDANT_DATA, "defendantData")
                        .add(USER_ID, userId)

        );

        petFormEventProcessor.petFormDefendantUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_DEFENDANT_UPDATED));
        verifyPetFormPublicEvent(petId, caseId, userId, envelope);
    }

    @Test
    public void shouldRaisePetFormFinalisedPublicEvent() throws IOException {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();
        final String submissionId = randomUUID().toString();

        final String fileName = "Jane JOHNSON, 22 May 1976, created on 22 December 10:45 2022.pdf";

        final String inputEvent = Resources.toString(getResource("pet-finalised-form-data.json"), defaultCharset())
                .replaceAll("%caseId%", caseId)
                .replaceAll("%petId%", petId)
                .replaceAll("%userId%", userId);
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        JsonArrayBuilder arrayBuilder = createArrayBuilder();
        readData.getJsonArray("finalisedFormData").forEach(x -> arrayBuilder.add(x.toString()));

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_FINALISED).withUserId(PetFormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
                        .add(PROSECUTION_CASE, createObjectBuilder().build())
                        .add(USER_ID, userId)
                        .add(MATERIAL_ID, UUID.randomUUID().toString())
                        .add(FINALISED_FORM_DATA, arrayBuilder.build())
                        .add(SUBMISSION_ID, submissionId)
        );

        when(documentGeneratorService.generateFormDocument(any(), any(), any(), any())).thenReturn(fileName);
        when(usersGroupService.getUserById(any(), any())).thenReturn(createObjectBuilder().add(ID, userId).add(FIRST_NAME, FIRST_NAME).add(LAST_NAME, LAST_NAME).build());

        petFormEventProcessor.petFormFinalised(requestEnvelope);
        verify(sender, times(4)).send(envelopeArgumentCaptor.capture());

        final List<JsonEnvelope> envelopes = envelopeArgumentCaptor.getAllValues();

        for (int i = 0; i < 3; i++) {
            assertAddCourtOrderCommandFromEnvelope(caseId, envelopes.get(i));
        }

        final JsonEnvelope envelope1 = envelopes.get(3);
        assertThat(envelope1.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_FINALISED));
        final JsonObject payload1 = envelope1.payloadAsJsonObject();
        assertThat(payload1.getString(MATERIAL_ID), is(notNullValue()));
        verifyPetFormPublicEvent(petId, caseId, userId, envelope1);
        assertThat(payload1.getString(SUBMISSION_ID), is(notNullValue()));
    }

    @Test
    public void shouldRaisePetDetailUpdatedEvent() {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_DETAIL_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
                        .add(PET_DEFENDANTS, createArrayBuilder().build())
                        .add(USER_ID, userId)
        );

        when(usersGroupService.getUserById(any(), any())).thenReturn(createObjectBuilder().add(ID, userId).add(FIRST_NAME, FIRST_NAME).add(LAST_NAME, LAST_NAME).build());

        petFormEventProcessor.petDetailUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_DETAIL_UPDATED));
        verifyPetFormPublicEvent(petId, caseId, userId, envelope);
    }

    @Test
    public void shouldRaisePetOperationFailedEvent() {
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_OPERATION_FAILED).withUserId(userId),
                createObjectBuilder()
        );

        petFormEventProcessor.petOperationFailed(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_OPERATION_FAILED));
    }


    private void assertAddCourtOrderCommandFromEnvelope(final String caseId, final JsonEnvelope envelope) {
        assertThat(envelope.metadata().name(), Matchers.is(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT));
        final JsonObject payload2 = envelope.payloadAsJsonObject();
        assertThat(payload2.getString(MATERIAL_ID), Matchers.is(Matchers.notNullValue()));
        final JsonObject courtDocument1 = payload2.getJsonObject(COURT_DOCUMENT);
        assertThat(courtDocument1.getString(COURT_DOCUMENT_ID), Matchers.is(Matchers.notNullValue()));
        assertThat(courtDocument1.getJsonObject(DOCUMENT_CATEGORY).getJsonObject(CASE_DOCUMENT).getString(PROSECUTION_CASE_ID), Matchers.is(caseId));
        assertThat(courtDocument1.getString(DOCUMENT_TYPE_ID), Matchers.is(CASE_DOCUMENT_TYPE_ID.toString()));
        assertThat(courtDocument1.getBoolean("sendToCps"), Matchers.is(true));
    }

    private void verifyPetFormPublicEvent(final String petId, final String caseId, final String userId, final JsonEnvelope envelope1) {
        final JsonObject payload1 = envelope1.payloadAsJsonObject();
        assertThat(payload1.getString(PET_ID), is(petId));
        assertThat(payload1.getString(CASE_ID), is(caseId));
        assertThat(payload1.getJsonObject(UPDATED_BY), notNullValue());
        if (payload1.getJsonObject(UPDATED_BY).containsKey(NAME)) {
            assertThat(payload1.getJsonObject(UPDATED_BY).getString(NAME), is(CPS_USER_NAME));
        } else {
            assertThat(payload1.getJsonObject(UPDATED_BY).getString(ID), is(userId));
            assertThat(payload1.getJsonObject(UPDATED_BY).getString(FIRST_NAME), is(FIRST_NAME));
            assertThat(payload1.getJsonObject(UPDATED_BY).getString(LAST_NAME), is(LAST_NAME));
        }
    }

    @Test
    public void shouldHandleServePetSubmittedPublicEvent() {

        final String inputEvent = generatePetFormDataFromReferenceData();
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);

        when(referenceDataService.getPetForm(any(), any())).thenReturn(Optional.ofNullable(readData));

        String payload = getPayload("cps-serve-pet-submitted.json");
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-pet-submitted"),
                jsonPayload);

        petFormEventProcessor.handleServePetSubmittedPublicEvent(envelope);

        verify(sender, atLeastOnce()).send(defaultEnvelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldHandleServePetSubmittedPublicEvent_RaiseUpdateCpsDefendantIdCommand() {

        final String inputEvent = generatePetFormDataFromReferenceData();
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);

        when(referenceDataService.getPetForm(any(), any())).thenReturn(Optional.ofNullable(readData));

        String payload = getPayload("cps-serve-pet-submitted.json");
        final JsonObject jsonPayload = jsonFromString(payload);

        final JsonEnvelope envelope = envelopeFrom(
                metadataWithRandomUUID("public.prosecutioncasefile.cps-serve-pet-submitted"),
                jsonPayload);

        petFormEventProcessor.handleServePetSubmittedPublicEvent(envelope);

        verify(sender, times(2)).send(defaultEnvelopeArgumentCaptor.capture());
        final List<DefaultEnvelope> envelopeList = defaultEnvelopeArgumentCaptor.getAllValues();
        assertThat(((Envelope) envelopeList.get(0)).metadata().name(), is(PROGRESSION_COMMAND_CREATE_PET_FORM));
        assertThat(((Envelope) envelopeList.get(0)).payload().toString(), isJson(allOf(
                withJsonPath("$.isYouth", equalTo(true))
        )));
        verifyUpdateCpsDefendantIdCommand("1a8fe782-a287-11eb-bcbc-0242ac130077", "8f8fe782-a287-11eb-bcbc-0242ac130002", "915d2c62-af1f-4674-863a-0891e67e323a", ((Envelope) envelopeList.get(1)));
    }

    private String generatePetFormDataFromReferenceData() {
        return "{\n" +
                "  \"form_id\": \"f8254db1-1683-483e-afb3-b87fde5a0a26\",\n" +
                "  \"description\": \"Pet Form data\",\n" +
                "  \"petDefendants\": \"[]\",\n" +
                "  \"data\":\"{\\n  \\\"field1\\\": \\\"value1\\\",\\n  \\\"field2\\\": \\\"value2\\\"\\n}\",\n" +
                "  \"version\": 1\n" +
                "}";

    }

    private void verifyUpdateCpsDefendantIdCommand(final String cpsDefendantId1, final String caseId, final String defendantId1, final Envelope envelope) {
        assertThat(envelope.metadata().name(), is(PROGRESSION_COMMAND_UPDATE_CPS_DEFENDANT_ID));
        UpdateCpsDefendantId payload = (UpdateCpsDefendantId) envelope.payload();
        assertThat(payload.getCaseId().toString(), is(caseId));
        assertThat(payload.getDefendantId().toString(), is(defendantId1));
        assertThat(payload.getCpsDefendantId().toString(), is(cpsDefendantId1));
    }
}

