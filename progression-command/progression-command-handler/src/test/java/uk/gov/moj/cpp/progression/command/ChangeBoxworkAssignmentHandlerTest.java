package uk.gov.moj.cpp.progression.command;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.BoxworkAssignmentChanged;
import uk.gov.justice.progression.courts.ChangeBoxworkAssignment;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.handler.ChangeBoxworkAssigmentHandler;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChangeBoxworkAssignmentHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            BoxworkAssignmentChanged.class);

    @InjectMocks
    private ChangeBoxworkAssigmentHandler handler;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private ApplicationAggregate aggregate;

    @Before
    public void setup() {
        aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new ChangeBoxworkAssigmentHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles(ChangeBoxworkAssigmentHandler.COMMAND_NAME)
                ));
    }

    @Test
    public void shouldProcessEvent() throws Exception {
        final BoxworkAssignmentChanged privateEvent =BoxworkAssignmentChanged.boxworkAssignmentChanged()
                .withApplicationId(UUID.randomUUID())
                .withUserId(UUID.randomUUID()).build();
        //does nothing - no test
        aggregate.apply(privateEvent);
    }


    @Test
    public void shouldProcessCommand() throws Exception {
      shouldProcessCommand(UUID.randomUUID());
    }

    @Test
    public void shouldProcessCommandNullUser() throws Exception {
        shouldProcessCommand(null);
    }


    public void shouldProcessCommand(UUID userId) throws Exception {

        final ChangeBoxworkAssignment command =
                ChangeBoxworkAssignment.changeBoxworkAssignment()
                        .withApplicationId(UUID.randomUUID())
                        .withUserId(userId)
                        .build();
        aggregate.apply(command);


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(ChangeBoxworkAssigmentHandler.COMMAND_NAME)
                .withId(UUID.randomUUID())
                .build();

        final Envelope<ChangeBoxworkAssignment> envelope = envelopeFrom(metadata, command);

        handler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        final JsonObject payload = envelopeStream.findAny().map(e->e.payloadAsJsonObject()).orElseThrow(
                () -> new RuntimeException("no event in stream")
        );

        final BoxworkAssignmentChanged event = jsonObjectToObjectConverter.convert(payload, BoxworkAssignmentChanged.class);

        assertEquals(event.getApplicationId(), command.getApplicationId());
        assertEquals(event.getUserId(), command.getUserId());

    }


}
