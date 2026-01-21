package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.InjectMocks;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.progression.command.helper.LAAHelper;
import uk.gov.moj.cpp.progression.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

@SuppressWarnings({"squid:S5976"})
@ExtendWith(MockitoExtension.class)
public class RecordLAAReferenceApiTest {

    @Mock
    private Sender sender;

    @Captor
    ArgumentCaptor<Envelope<JsonObject>> envelopeArgumentCaptor;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @InjectMocks
    private RecordLAAReferenceApi recordLAAReferenceApi;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Spy
    private LAAHelper laaHelper;

    @Mock
    private ProsecutionCaseQueryService progressionQueryService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Mock
    private CourtApplicationRepository courtApplicationRepository;

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
    public void shouldRecordLAAReferenceForOffence() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.handler.record-laareference-for-offence", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        recordLAAReferenceApi.handle(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.record-laareference-for-offence"));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNotValidUUIDForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offenceId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsnullForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfDefendantIdIsNotValidUUIDForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", "invalid-uuid")
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfDefendantIdIsNullForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfCasedIsNullForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("offenceId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfProsecutionCaseIdIsNotValidUUIDForCase() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-defendant", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("defendantId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .add("prosecutionCaseId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handle(envelope));

    }



    @Test
    public void shouldRecordLAAReferenceForApplication() {
        final UUID parentApplicationId = randomUUID();
        final UUID childApplicationId1 = randomUUID();
        final UUID childApplicationId2 = randomUUID();
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.handler.record-laareference-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
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

        recordLAAReferenceApi.handleForApplication(envelope);
        verify(sender, times(3)).send(envelopeArgumentCaptor.capture());
        final List<Envelope<JsonObject>> allValues = envelopeArgumentCaptor.getAllValues();
        assertThat(allValues.get(0).metadata().name(), is("progression.command.handler.record-laareference-for-application-on-application"));
        assertThat(allValues.get(0).payload().getString("applicationId"), is(childApplicationId1.toString()));
        assertThat(allValues.get(1).metadata().name(), is("progression.command.handler.record-laareference-for-application-on-application"));
        assertThat(allValues.get(1).payload().getString("applicationId"), is(childApplicationId2.toString()));

        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.record-laareference-for-application"));
    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNullForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfApplicationIdIsNotValidUUIDForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", "invalid-uuid")
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNullForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfSubjectIdIsNotValidUUIDForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", "invalid-uuid")
                .add("offenceId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNullForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldThrowBadRequestIfOffenceIdIsNotValidUUIDForApplication() {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.receive-representationorder-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", "invalid-uuid")
                .build());

        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

    @Test
    public void shouldUpdateLAAReferenceAsApplicationHasNoOffencesAndIsNotChildApplication() throws Exception {

        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.record-laareference-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-without-offences.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        recordLAAReferenceApi.handleForApplicationOnApplication(envelope);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.record-laareference-for-application-on-application"));
    }

    @Test
    public void shouldUpdateLAAReferenceAsApplicationHasOOffencesAndHasChildApplications() throws Exception {

        final UUID parentApplicationId = randomUUID();
        final UUID childApplicationId1 = randomUUID();
        final UUID childApplicationId2 = randomUUID();

        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.record-laareference-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", parentApplicationId.toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-without-offences.json", JsonObject.class);

        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
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

        recordLAAReferenceApi.handleForApplicationOnApplication(envelope);
        verify(sender, times(3)).send(envelopeArgumentCaptor.capture());
        final List<Envelope<JsonObject>> allValues = envelopeArgumentCaptor.getAllValues();
        assertThat(allValues.get(0).metadata().name(), is("progression.command.handler.record-laareference-for-application-on-application"));
        assertThat(allValues.get(0).payload().getString("applicationId"), is(parentApplicationId.toString()));
        assertThat(allValues.get(1).metadata().name(), is("progression.command.handler.record-laareference-for-application-on-application"));
        assertThat(allValues.get(1).payload().getString("applicationId"), is(childApplicationId1.toString()));
        assertThat(allValues.get(2).metadata().name(), is("progression.command.handler.record-laareference-for-application-on-application"));
        assertThat(allValues.get(2).payload().getString("applicationId"), is(childApplicationId2.toString()));

        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.handler.record-laareference-for-application-on-application"));
    }

    @Test
    public void shouldRejectUpdatingLAAReferenceWhenApplicationHasOffences() throws Exception {

        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.record-laareference-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-with-offences.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplicationOnApplication(envelope));
    }

    @Test
    public void shouldRejectUpdatingLAAReferenceWhenApplicationIsChildApplication() throws Exception {
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.record-laareference-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-child-application.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplicationOnApplication(envelope));
    }

    @Test
    public void shouldRejectUpdatingLAAReferenceWhenApplicationNotFound() throws Exception {

        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.record-laareference-for-application-on-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", randomUUID().toString())
                .build());
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplicationOnApplication(envelope));
    }

    @Test
    public void shouldThrowBadRequestExceptionRecordLAAReferenceForApplication() {
        final UUID applicationId = randomUUID();
        final Metadata metadata = CommandClientTestBase.metadataFor("progression.command.handler.record-laareference-for-application", randomUUID().toString());
        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadata, Json.createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("subjectId", randomUUID().toString())
                .add("offenceId", randomUUID().toString())
                .build());
        final JsonObject payload = CommandClientTestBase.readJson("json/progression-query-application-on-case-without-offences.json", JsonObject.class);
        when(progressionQueryService.getCourtApplicationById(any(),any())).thenReturn(Optional.of(payload));
        assertThrows(BadRequestException.class, () -> recordLAAReferenceApi.handleForApplication(envelope));

    }

}
