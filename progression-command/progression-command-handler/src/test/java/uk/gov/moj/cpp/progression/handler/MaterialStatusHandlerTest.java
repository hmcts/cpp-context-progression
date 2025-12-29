package uk.gov.moj.cpp.progression.handler;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowDocumentRequestToBeAcknowledged;
import uk.gov.justice.core.courts.NowDocumentRequested;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.core.courts.RecordNowsMaterialRequest;
import uk.gov.justice.core.courts.UpdateNowsMaterialStatus;
import uk.gov.justice.core.courts.nowdocument.NowDocumentContent;
import uk.gov.justice.core.courts.nowdocument.NowDocumentRequest;
import uk.gov.justice.core.courts.nowdocument.Nowdefendant;
import uk.gov.justice.core.courts.nowdocument.OrderCourt;
import uk.gov.justice.core.courts.nowdocument.ProsecutionCase;
import uk.gov.justice.domain.aggregate.Aggregate;
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
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.domain.event.MaterialStatusUpdateIgnored;
import uk.gov.moj.cpp.progression.service.ProsecutionCaseQueryService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MaterialStatusHandlerTest {

    private static final String ADD_NOW_DOCUMENT_REQUEST_COMMAND_NAME = "progression.command.add-now-document-request";

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            NowsMaterialRequestRecorded.class,
            NowsMaterialStatusUpdated.class,
            MaterialStatusUpdateIgnored.class,
            NowDocumentRequested.class,
            MaterialStatusUpdateIgnored.class,
            NowDocumentRequestToBeAcknowledged.class);

    private static final UUID MATERIAL_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private MaterialStatusHandler materialStatusHandler;

    @InjectMocks
    private NowDocumentRequestHandler nowDocumentRequestHandler;

    private MaterialAggregate aggregate;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        aggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void recordNowsMaterialWithWelshTranslationAndUpdateTest() throws Throwable {
        recordNowsMaterialAndUpdateTest(true, Boolean.TRUE);
    }

    @Test
    public void recordNowsMaterialWithOutWelshTranslationAndUpdateTest() throws Throwable {
        recordNowsMaterialAndUpdateTest(true, Boolean.FALSE);
    }

    @Test
    public void recordNowsMaterialAndUpdateTestUnkownUpdate() throws Throwable {
        recordNowsMaterialAndUpdateTest(false, null);
    }


    private void recordNowsMaterialAndUpdateTest(final boolean knownUpdate, final Boolean welshTranslationRequired) throws Throwable {


        final UUID materialId = UUID.randomUUID();
        final RecordNowsMaterialRequest commandObject = RecordNowsMaterialRequest.recordNowsMaterialRequest()
                .withContext(MaterialDetails.materialDetails()
                        .withMaterialId(materialId)
                        .withWelshTranslationRequired(welshTranslationRequired)
                        .withSecondClassLetter(true)
                        .build()
                ).build();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(
                MaterialStatusHandler.PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST), objectToJsonObjectConverter.convert(commandObject));

        this.materialStatusHandler.recordNowsMaterial(command);

        final ProsecutionCase prosecutionCase = mock(ProsecutionCase.class);

        final Nowdefendant nowdefendant = mock(Nowdefendant.class);
        final UUID hearingId = UUID.randomUUID();
        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                .withMaterialId(materialId)
                .withHearingId(hearingId)
                .withWelshTranslationRequired(welshTranslationRequired)
                .withNowContent(NowDocumentContent.nowDocumentContent()
                        .withCases(Arrays.asList(prosecutionCase))
                        .withDefendant(nowdefendant)
                        .build())
                .build();

        final NowDocumentRequested nowDocumentRequested = NowDocumentRequested.nowDocumentRequested()
                .withMaterialId(materialId)
                .withNowDocumentRequest(nowDocumentRequest)
                .build();

        aggregate.apply(nowDocumentRequested);


        final UUID updateMaterialId = knownUpdate ? materialId : UUID.randomUUID();

        final UpdateNowsMaterialStatus statusCommandObject = UpdateNowsMaterialStatus.updateNowsMaterialStatus()
                .withMaterialId(updateMaterialId)
                .withStatus("GENERATED")
                .build();

        final JsonEnvelope statusCommand = envelopeFrom(metadataWithRandomUUID(
                MaterialStatusHandler.PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS), objectToJsonObjectConverter.convert(statusCommandObject));

        if (!knownUpdate) {
            when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(new MaterialAggregate());
        }

        this.materialStatusHandler.updateStatus(statusCommand);

        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);

        (Mockito.verify(eventStream, times(2))).append(argumentCaptor.capture());
        final List<Stream> streams = argumentCaptor.getAllValues();

        final Envelope recordJsonEnvelope = (JsonEnvelope) streams.get(0).findFirst().orElse(null);

        assertThat(recordJsonEnvelope.metadata().name(), is("progression.event.nows-material-request-recorded"));

        final NowsMaterialRequestRecorded record = jsonObjectToObjectConverter.convert((JsonObject) recordJsonEnvelope.payload(), NowsMaterialRequestRecorded.class);

        assertThat(record.getContext().getMaterialId(), is(commandObject.getContext().getMaterialId()));

        final JsonEnvelope statusJsonEnvelope = (JsonEnvelope) streams.get(1).findFirst().orElse(null);

        if (knownUpdate) {
            assertThat(statusJsonEnvelope.metadata().name(), is("progression.event.nows-material-status-updated"));
            final NowsMaterialStatusUpdated updated = jsonObjectToObjectConverter.convert((JsonObject) statusJsonEnvelope.payload(), NowsMaterialStatusUpdated.class);
            assertThat(updated.getDetails().getMaterialId(), is(commandObject.getContext().getMaterialId()));
            assertThat(updated.getStatus(), is(statusCommandObject.getStatus()));
            assertThat(updated.getWelshTranslationRequired(), is(Boolean.TRUE.equals(welshTranslationRequired)));
        } else {
            assertThat(statusJsonEnvelope.metadata().name(), is("progression.events.material-status-update-ignored"));
            final MaterialStatusUpdateIgnored ignored = jsonObjectToObjectConverter.convert((JsonObject) statusJsonEnvelope.payload(), MaterialStatusUpdateIgnored.class);
            assertThat(ignored.getMaterialId(), is(updateMaterialId));
            assertThat(ignored.getStatus(), is(statusCommandObject.getStatus()));
        }

    }


    private <T extends Aggregate> void setupMockedEventStream(UUID id, EventStream eventStream, T aggregate) {
        when(this.eventSource.getStreamById(id)).thenReturn(eventStream);
        Class<T> clz = (Class<T>) aggregate.getClass();
        when(this.aggregateService.get(eventStream, clz)).thenReturn(aggregate);
    }

    @Test
    public void testApiUpdateStatus() throws EventStreamException {
        final UUID materialId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final RecordNowsMaterialRequest commandObject = RecordNowsMaterialRequest.recordNowsMaterialRequest()
                .withContext(MaterialDetails.materialDetails()
                        .withMaterialId(materialId)
                        .withCaseId(caseId)
                        .withIsNotificationApi(true)
                        .withIsCps(true)
                        .withSecondClassLetter(true)
                        .build()
                ).build();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(
                MaterialStatusHandler.PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST), objectToJsonObjectConverter.convert(commandObject));

        this.materialStatusHandler.recordNowsMaterial(command);

        final UpdateNowsMaterialStatus statusCommandObject = UpdateNowsMaterialStatus.updateNowsMaterialStatus()
                .withMaterialId(materialId)
                .withStatus("GENERATED")
                .build();

        final JsonEnvelope statusCommand = envelopeFrom(metadataWithRandomUUID(
                MaterialStatusHandler.PROGRESSION_COMMAND_UPDATE_NOWS_MATERIAL_STATUS), objectToJsonObjectConverter.convert(statusCommandObject));

        List<UUID> cases = new ArrayList<>();
        cases.add(caseId);
        cases.add(randomUUID());

        final NowDocumentRequest nowDocumentRequest = NowDocumentRequest.nowDocumentRequest()
                    .withMaterialId(materialId)
                    .withCases(cases)
                    .withNowContent(NowDocumentContent.nowDocumentContent()
                            .withCases(Collections.singletonList(ProsecutionCase.prosecutionCase().withIsCps(false).build()))
                            .withOrderingCourt(OrderCourt.orderCourt().withWelshCourtCentre(false).build())
                            .withDefendant(Nowdefendant.nowdefendant().withProsecutingAuthorityReference("ref").build())
                            .build())
                    .build();

        Envelope<NowDocumentRequest> buildEnvelope =  Envelope.envelopeFrom(metadataWithRandomUUID(ADD_NOW_DOCUMENT_REQUEST_COMMAND_NAME).withUserId(randomUUID().toString()),
                nowDocumentRequest);
        nowDocumentRequestHandler.handleAddNowDocumentRequest(buildEnvelope);

        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(aggregate);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());
        final JsonObject sampleJsonObject = createObjectBuilder().add("prosecutionCase", createObjectBuilder()
                                                                                .add("id", caseId.toString())
                                                                                .add("prosecutionCaseIdentifier", createObjectBuilder()
                                                                                                        .add("caseURN" , "case123")
                                                                                        .add("prosecutionAuthorityReference", "ref")
                                                                                        .add("prosecutionAuthorityOUCode", "ouCode")
                                                                                                        .build())
                                                                                .build())
                                                                 .build();
        when(prosecutionCaseQueryService.getProsecutionCase(any(),anyString())).thenReturn(java.util.Optional.ofNullable(sampleJsonObject));
        this.materialStatusHandler.updateStatus(statusCommand);

        final ArgumentCaptor<Stream> argumentCaptor = ArgumentCaptor.forClass(Stream.class);

        (Mockito.verify(eventStream, times(3))).append(argumentCaptor.capture());
        final List<Stream> streams = argumentCaptor.getAllValues();

        final Envelope recordJsonEnvelope = (JsonEnvelope) streams.get(0).findFirst().orElse(null);

        assertThat(recordJsonEnvelope.metadata().name(), is("progression.event.nows-material-request-recorded"));

        final NowsMaterialRequestRecorded record = jsonObjectToObjectConverter.convert((JsonObject) recordJsonEnvelope.payload(), NowsMaterialRequestRecorded.class);

        assertThat(record.getContext().getMaterialId(), is(commandObject.getContext().getMaterialId()));

        final JsonEnvelope statusJsonEnvelope = (JsonEnvelope) streams.get(2).findFirst().orElse(null);

            assertThat(statusJsonEnvelope.metadata().name(), is("progression.event.nows-material-status-updated"));
            final NowsMaterialStatusUpdated updated = jsonObjectToObjectConverter.convert((JsonObject) statusJsonEnvelope.payload(), NowsMaterialStatusUpdated.class);
            assertThat(updated.getDetails().getMaterialId(), is(commandObject.getContext().getMaterialId()));
            assertThat(updated.getStatus(), is(statusCommandObject.getStatus()));
            assertThat(updated.getCaseSubjects().size(), is(2));
    }
}
