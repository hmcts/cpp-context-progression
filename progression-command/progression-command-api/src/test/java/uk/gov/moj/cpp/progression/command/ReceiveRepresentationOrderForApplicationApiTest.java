package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.command.helper.LAAHelper;
import uk.gov.moj.cpp.progression.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiveRepresentationOrderForApplicationApiTest {

    @Mock
    private Sender sender;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @InjectMocks
    private ReceiveRepresentationOrderForApplicationApi receiveRepresentationOrderForApplicationApi;

    @Spy
    private LAAHelper laaHelper;

    @Mock
    private ProsecutionCaseQueryService progressionQueryService;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @BeforeEach
    void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.laaHelper, "progressionQueryService", progressionQueryService);
        setField(this.laaHelper, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
        setField(this.laaHelper, "courtApplicationRepository", courtApplicationRepository);
        setField(this.laaHelper, "stringToJsonObjectConverter", stringToJsonObjectConverter);
    }

    @Test
    void shouldReceiveRepresentationOrderForApplicationAPI() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-with-offences.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        receiveRepresentationOrderForApplicationApi.handle(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.receive-representationOrder-for-application"));

    }

    @Test
    void shouldReceiveRepresentationOrderForApplicationAPIWithChildApplications() {
        final UUID parentApplicationId = randomUUID();
        final UUID childApplicationId1 = randomUUID();
        final UUID childApplicationId2 = randomUUID();

        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", parentApplicationId.toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        final CourtApplicationEntity courtApplicationEntity1 = new CourtApplicationEntity();
        courtApplicationEntity1.setApplicationId(childApplicationId1);
        courtApplicationEntity1.setPayload(createObjectBuilder()
                .add("id", childApplicationId1.toString())
                .build().toString());

        final CourtApplicationEntity courtApplicationEntity2 = new CourtApplicationEntity();
        courtApplicationEntity2.setApplicationId(childApplicationId2);
        courtApplicationEntity2.setPayload(createObjectBuilder()
                .add("id", childApplicationId2.toString())
                .build().toString());

        final List<CourtApplicationEntity> courtApplicationEntityList = Arrays.asList(courtApplicationEntity1, courtApplicationEntity2);


        when(courtApplicationRepository.findByParentApplicationId(parentApplicationId)).thenReturn(courtApplicationEntityList);
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-with-offences.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));

        receiveRepresentationOrderForApplicationApi.handle(envelope);
        verify(sender, times(3)).send(envelopeArgumentCaptor.capture());

        final List<Envelope<JsonObject>> envelopeList = envelopeArgumentCaptor.getAllValues();
        assertThat(envelopeList.get(0).metadata().name(), is("progression.command.handler.receive-representationOrder-for-application"));
        assertThat(envelopeList.get(0).payload().getString("applicationId"), is(parentApplicationId.toString()));
        assertThat(envelopeList.get(1).metadata().name(), is("progression.command.handler.receive-representationOrder-for-application-on-application"));
        assertThat(envelopeList.get(1).payload().getString("applicationId"), is(childApplicationId1.toString()));
        assertThat(envelopeList.get(2).metadata().name(), is("progression.command.handler.receive-representationOrder-for-application-on-application"));
        assertThat(envelopeList.get(2).payload().getString("applicationId"), is(childApplicationId2.toString()));
    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNull() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNotValidUUID() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", "invalid-uuid")
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNull() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNotValidUUID() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", "invalid-uuid")
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNull() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNotValidUUID() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }

    @Test
    void shouldReceiveRepresentationOrderForApplicationOnApplicationAPI() {
        final UUID parentApplicationId = randomUUID();
        final UUID childApplicationId1 = randomUUID();
        final UUID childApplicationId2 = randomUUID();

        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", parentApplicationId.toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-without-offences.json", JsonObject.class);

        final CourtApplicationEntity courtApplicationEntity1 = new CourtApplicationEntity();
        courtApplicationEntity1.setApplicationId(childApplicationId1);
        courtApplicationEntity1.setPayload(createObjectBuilder()
                .add("id", childApplicationId1.toString())
                .build().toString());

        final CourtApplicationEntity courtApplicationEntity2 = new CourtApplicationEntity();
        courtApplicationEntity2.setApplicationId(childApplicationId2);
        courtApplicationEntity2.setPayload(createObjectBuilder()
                .add("id", childApplicationId2.toString())
                .build().toString());

        final List<CourtApplicationEntity> courtApplicationEntityList = Arrays.asList(courtApplicationEntity1, courtApplicationEntity2);


        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        when(courtApplicationRepository.findByParentApplicationId(parentApplicationId)).thenReturn(courtApplicationEntityList);

        receiveRepresentationOrderForApplicationApi.handleForApplicationOnApplication(envelope);
        verify(sender, times(3)).send(envelopeArgumentCaptor.capture());
        final List<Envelope<JsonObject>> envelopeList = envelopeArgumentCaptor.getAllValues();
        assertThat(envelopeList.get(0).metadata().name(), is("progression.command.handler.receive-representationOrder-for-application-on-application"));
        assertThat(envelopeList.get(0).payload().getString("applicationId"), is(parentApplicationId.toString()));
        assertThat(envelopeList.get(1).metadata().name(), is("progression.command.handler.receive-representationOrder-for-application-on-application"));
        assertThat(envelopeList.get(1).payload().getString("applicationId"), is(childApplicationId1.toString()));
        assertThat(envelopeList.get(2).metadata().name(), is("progression.command.handler.receive-representationOrder-for-application-on-application"));
        assertThat(envelopeList.get(2).payload().getString("applicationId"), is(childApplicationId2.toString()));

    }

    @Test
    void shouldThrowBadRequestIfApplicationIdIsNull_ReceiveRepresentationOrderForApplicationOnApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handleForApplicationOnApplication(envelope));

    }

    @Test
    void shouldThrowBadRequestIfApplicationIdIsNotValidUUID_ReceiveRepresentationOrderForApplicationOnApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handleForApplicationOnApplication(envelope));

    }

    @Test
    public void shouldRejectReceiveRepresentationOrderWhenApplicationHasOffences() throws Exception {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-with-offences.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handleForApplicationOnApplication(envelope));
    }

    @Test
    public void shouldRejectReceiveRepresentationOrderWhenApplicationIsChildApplication() throws Exception {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-child-application.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handleForApplicationOnApplication(envelope));
    }

    @Test
    public void shouldRejectReceiveRepresentationOrderWhenApplicationNotFound() throws Exception {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .build());
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handleForApplicationOnApplication(envelope));
    }

    @Test
    public void shouldThrowBadRequestExceptionRecordRepresentationOrderForApplicationWithoutOffences() {
        final UUID applicationId = randomUUID();
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, JsonObjects.createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-on-case-without-offences.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        assertThrows(BadRequestException.class, () -> receiveRepresentationOrderForApplicationApi.handle(envelope));

    }
}
