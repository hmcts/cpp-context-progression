package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ReferApplicationToCourt;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferApplicationToCourtHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ApplicationReferredToCourt.class);

    @InjectMocks
    private ReferApplicationToCourtHandler referApplicationToCourtHandler;

    @Test
    public void shouldHandleCommand() {
        assertThat(new ReferApplicationToCourtHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.refer-application-to-court")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final ReferApplicationToCourt referApplicationToCourt = createReferApplicationToCourt();

        final ApplicationAggregate applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);

        applicationAggregate.referApplicationToCourt(referApplicationToCourt.getHearingRequest());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.refer-application-to-court")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<ReferApplicationToCourt> envelope = envelopeFrom(metadata, referApplicationToCourt);
        referApplicationToCourtHandler.handle(envelope);
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.application-referred-to-court"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingRequest", notNullValue()))
                        ))

                )
        );
    }

    private static ReferApplicationToCourt createReferApplicationToCourt() {
        return ReferApplicationToCourt.referApplicationToCourt()
                .withHearingRequest(HearingListingNeeds.hearingListingNeeds()
                        .withId(UUID.randomUUID())
                        .build())
                .build();
    }
}
