package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.NowNotificationGenerated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.MaterialAggregate;
import uk.gov.moj.cpp.progression.command.RecordNowNotificationGenerated;
import uk.gov.moj.cpp.progression.helper.EventStreamHelper;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class NowNotificationGeneratedHandlerTest {

    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();
    private static final UUID USER_ID = randomUUID();
    private static final String STATUS = "generated";

    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    EventStreamHelper eventStreamHelper;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private NowNotificationGeneratedHandler nowNotificationGeneratedHandler;
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            NowNotificationGenerated.class);

    @Test
    public void shouldHandleCommand() {
        assertThat(nowNotificationGeneratedHandler, isHandler(Component.COMMAND_HANDLER)
                .with(method("recordNowNotificationGenerated")
                        .thatHandles("progression.command.record-now-notification-generated")));
    }

    @Test
    public void shouldRecordNowNotificationGenerated() throws Exception {
        final MaterialAggregate materialAggregate = new MaterialAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, MaterialAggregate.class)).thenReturn(materialAggregate);

        final Envelope<RecordNowNotificationGenerated> envelope = createRecordNowNotificationGeneratedEnvelope();

        nowNotificationGeneratedHandler.recordNowNotificationGenerated(envelope);

        verifyRecordNowNotificationGeneratedResults();
    }

    private void verifyRecordNowNotificationGeneratedResults() throws Exception {
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                        jsonEnvelope(
                                metadata()
                                        .withName("progression.event.now-notification-generated"),
                                JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                                withJsonPath("$.materialId", is(MATERIAL_ID.toString())),
                                                withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                                                withJsonPath("$.status", is(STATUS)),
                                                withJsonPath("$.userId", is(USER_ID.toString()))
                                        )
                                ))

                )
        );
    }

    private Envelope<RecordNowNotificationGenerated> createRecordNowNotificationGeneratedEnvelope() {
        RecordNowNotificationGenerated recordNowNotificationGenerated = new RecordNowNotificationGenerated(HEARING_ID, MATERIAL_ID, STATUS);

        final JsonEnvelope requestEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithRandomUUID("progression.command.record-now-notification-generated").withUserId(USER_ID.toString()),
                createObjectBuilder().build());

        return Enveloper.envelop(recordNowNotificationGenerated)
                .withName("progression.command.record-now-notification-generated")
                .withMetadataFrom(requestEnvelope);
    }
}
