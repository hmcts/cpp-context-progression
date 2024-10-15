package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.CreateHearingApplicationRequest.createHearingApplicationRequest;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CreateHearingApplicationRequest;
import uk.gov.justice.core.courts.HearingApplicationRequestCreated;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateHearingApplicationRequestHandlerTest {

    private static final UUID APPLICATION_PARTY_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(HearingApplicationRequestCreated.class);

    @InjectMocks
    private CreateHearingApplicationRequestHandler handler;

    @Test
    public void shouldCreateHearingApplicationRequest() throws EventStreamException {

        final CreateHearingApplicationRequest payload = createHearingApplicationRequest()
                .withApplicationRequests(singletonList(CourtApplicationPartyListingNeeds
                        .courtApplicationPartyListingNeeds()
                        .withCourtApplicationPartyId(APPLICATION_PARTY_ID)
                        .withCourtApplicationPartyType("not needed")
                        .withCourtApplicationId(APPLICATION_ID)
                        .build()))
                .build();

        final Metadata metadata = metadataBuilder()
                .withName("progression.command.create-hearing-application-request")
                .withId(randomUUID())
                .build();

        final Envelope<CreateHearingApplicationRequest> envelope = envelopeFrom(metadata, payload);

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        handler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.hearing-application-request-created"),
                        payload().isJson(allOf(
                                withJsonPath("$.applicationRequests[0].courtApplicationId", is(APPLICATION_ID.toString())),
                                withJsonPath("$.applicationRequests[0].courtApplicationPartyId", is(APPLICATION_PARTY_ID.toString()))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateHearingApplicationRequestHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-hearing-application-request")
                ));
    }

}