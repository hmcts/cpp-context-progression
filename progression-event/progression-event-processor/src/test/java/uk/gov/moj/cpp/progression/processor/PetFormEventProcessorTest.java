package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.MaterialService;

import javax.json.JsonObject;

import java.util.List;
import java.util.Optional;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.CASE_DOCUMENT_TYPE_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.CASE_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.DATA;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.MATERIAL_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PET_ID;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PROGRESSION_COMMAND_ADD_COURT_DOCUMENT;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PROSECUTION_CASE;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_DETAIL_UPDATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_CREATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_DEFENDANT_UPDATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_FINALISED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_FORM_UPDATED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.PUBLIC_PROGRESSION_PET_OPERATION_FAILED;
import static uk.gov.moj.cpp.progression.processor.PetFormEventProcessor.USER_ID;


@RunWith(MockitoJUnitRunner.class)
public class PetFormEventProcessorTest {

    public static final String userId = randomUUID().toString();
    public static final String COURT_DOCUMENT_ID = "courtDocumentId";
    public static final String DOCUMENT_CATEGORY = "documentCategory";
    public static final String DOCUMENT_TYPE_ID = "documentTypeId";
    public static final String CASE_DOCUMENT = "caseDocument";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";

    public static final String PROGRESSION_EVENT_PET_OPERATION_FAILED = "progression.event.pet-operation-failed";
    public static final String PROGRESSION_EVENT_PET_DETAIL_UPDATED = "progression.event.pet-detail-updated";
    public static final String PROGRESSION_EVENT_PET_FORM_FINALISED = "progression.event.pet-form-finalised";
    public static final String PROGRESSION_EVENT_PET_FORM_DEFENDANT_UPDATED = "progression.event.pet-form-defendant-updated";
    public static final String PROGRESSION_EVENT_PET_FORM_UPDATED = "progression.event.pet-form-updated";
    public static final String PROGRESSION_EVENT_PET_FORM_CREATED = "progression.event.pet-form-created";

    @Mock
    private Sender sender;

    @InjectMocks
    private PetFormEventProcessor petFormEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Mock
    private MaterialService materialService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Before
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }


    @Test
    public void shouldRaisePetFormCreatedPublicEvent() {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_CREATED).withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
        );

        petFormEventProcessor.petFormCreated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_CREATED));
    }

    @Test
    public void shouldRaisePetFormUpdatedPublicEvent() {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
        );

        petFormEventProcessor.petFormUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_UPDATED));
    }

    @Test
    public void shouldRaisePetFormDefendantUpdatedEvent() {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_DEFENDANT_UPDATED).withUserId(userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
                        .add(DEFENDANT_ID, defendantId)
        );

        petFormEventProcessor.petFormDefendantUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_DEFENDANT_UPDATED));
    }

    @Test
    public void shouldRaisePetFormFinalisedPublicEvent() {
        final String petId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String userId = randomUUID().toString();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataWithRandomUUID(PROGRESSION_EVENT_PET_FORM_FINALISED).withUserId(PetFormEventProcessorTest.userId),
                createObjectBuilder()
                        .add(PET_ID, petId)
                        .add(CASE_ID, caseId)
                        .add(PROSECUTION_CASE, createObjectBuilder().build())
                        .add(USER_ID, userId)
        );

        when(materialService.getStructuredForm(any(), any())).thenReturn(Optional.of(createObjectBuilder().add(DATA, "{\"name\": \"value\"}").build()));
        petFormEventProcessor.petFormFinalised(requestEnvelope);
        verify(sender, times(2)).send(envelopeArgumentCaptor.capture());

        final List<JsonEnvelope> envelopes = envelopeArgumentCaptor.getAllValues();

        final JsonEnvelope envelope1 = envelopes.get(0);
        assertThat(envelope1.metadata().name(), is(PUBLIC_PROGRESSION_PET_FORM_FINALISED));
        final JsonObject payload1 = envelope1.payloadAsJsonObject();
        assertThat(payload1.getString(PET_ID), is(petId));
        assertThat(payload1.getString(CASE_ID), is(caseId));
        assertThat(payload1.getString(USER_ID), is(userId));
        assertThat(payload1.getString(MATERIAL_ID), is(notNullValue()));

        final JsonEnvelope envelope2 = envelopes.get(1);
        assertThat(envelope2.metadata().name(), is(PROGRESSION_COMMAND_ADD_COURT_DOCUMENT));
        final JsonObject payload2 = envelope2.payloadAsJsonObject();
        assertThat(payload2.getString(MATERIAL_ID), is(notNullValue()));
        final JsonObject courtDocument = payload2.getJsonObject(COURT_DOCUMENT);
        assertThat(courtDocument.getString(COURT_DOCUMENT_ID), is(notNullValue()));
        assertThat(courtDocument.getJsonObject(DOCUMENT_CATEGORY).getJsonObject(CASE_DOCUMENT).getString(PROSECUTION_CASE_ID), is(caseId));
        assertThat(courtDocument.getString(DOCUMENT_TYPE_ID), is(CASE_DOCUMENT_TYPE_ID.toString()));
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
        );

        petFormEventProcessor.petDetailUpdated(requestEnvelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope envelope = envelopeArgumentCaptor.getValue();
        assertThat(envelope.metadata().name(), is(PUBLIC_PROGRESSION_PET_DETAIL_UPDATED));
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

}
