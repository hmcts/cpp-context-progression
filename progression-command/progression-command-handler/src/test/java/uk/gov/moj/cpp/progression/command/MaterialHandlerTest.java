package uk.gov.moj.cpp.progression.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.common.reflection.ReflectionUtils.setField;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.MaterialDetails;
import uk.gov.justice.core.courts.NowsMaterialRequestRecorded;
import uk.gov.justice.core.courts.NowsMaterialStatusUpdated;
import uk.gov.justice.core.courts.RecordNowsMaterialRequest;
import uk.gov.justice.core.courts.UpdateNowsMaterialStatus;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.domain.event.MaterialStatusUpdateIgnored;
import uk.gov.moj.cpp.progression.handler.MaterialStatusHandler;

import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)

public class MaterialHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(
            NowsMaterialRequestRecorded.class,
            NowsMaterialStatusUpdated.class,
            MaterialStatusUpdateIgnored.class);

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private MaterialStatusHandler materialStatusHandler;

    private MaterialAggregate aggregate;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        aggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void recordNowsMaterialAndUpdateTest() throws Throwable {
        recordNowsMaterialAndUpdateTest(true);
    }

    @Test
    public void recordNowsMaterialAndUpdateTestUnkownUpdate() throws Throwable {
        recordNowsMaterialAndUpdateTest(false);
    }


    private void recordNowsMaterialAndUpdateTest(final boolean knownUpdate) throws Throwable {


        final UUID materialId = UUID.randomUUID();
        final RecordNowsMaterialRequest commandObject = RecordNowsMaterialRequest.recordNowsMaterialRequest()
                .withContext(MaterialDetails.materialDetails()
                        .withMaterialId(materialId)
                        .build()
                ).build();

        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID(
                MaterialStatusHandler.PROGRESSION_COMMAND_RECORD_NOWS_MATERIAL_REQUEST), objectToJsonObjectConverter.convert(commandObject));

        this.materialStatusHandler.recordNowsMaterial(command);


        final UUID updateMaterialId = knownUpdate ? materialId : UUID.randomUUID();

        final UpdateNowsMaterialStatus statusCommandObject = UpdateNowsMaterialStatus.updateNowsMaterialStatus()
                .withMaterialId(updateMaterialId.toString())
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


}
