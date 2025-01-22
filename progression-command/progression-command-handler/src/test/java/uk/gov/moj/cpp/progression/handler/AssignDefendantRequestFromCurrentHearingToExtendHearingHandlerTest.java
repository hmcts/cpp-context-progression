package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.AssignDefendantRequestFromCurrentHearingToExtendHearing;
import uk.gov.justice.core.courts.DefendantRequestFromCurrentHearingToExtendHearingCreated;
import uk.gov.justice.core.courts.HearingDefendantRequestCreated;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssignDefendantRequestFromCurrentHearingToExtendHearingHandlerTest {

    private static final String ASSIGN_DEFENDANT_REQUEST_FROM_CURRENT_HEARING_TO_EXTEND_HEARING = "progression.command.assign-defendant-request-from-current-hearing-to-extend-hearing";
    private static final UUID CURRENT_HEARING_ID = randomUUID();
    private static final UUID EXTEND_HEARING_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AssignDefendantRequestFromCurrentHearingToExtendHearingHandler assignDefendantRequestFromCurrentHearingToExtendHearingHandler;

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DefendantRequestFromCurrentHearingToExtendHearingCreated.class);

    @Test
    public void shouldHandleCommand() {
        assertThat(new AssignDefendantRequestFromCurrentHearingToExtendHearingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("assignDefendantRequestFromCurrentHearingToExtendHearing")
                        .thatHandles(ASSIGN_DEFENDANT_REQUEST_FROM_CURRENT_HEARING_TO_EXTEND_HEARING)
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final UUID defendantId = randomUUID();
        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        hearingAggregate.apply(HearingDefendantRequestCreated.hearingDefendantRequestCreated()
                .withDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .build()))
                .build());
        assignDefendantRequestFromCurrentHearingToExtendHearingHandler.assignDefendantRequestFromCurrentHearingToExtendHearing(buildEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendant-request-from-current-hearing-to-extend-hearing-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.currentHearingId", is(CURRENT_HEARING_ID.toString())),
                                withJsonPath("$.extendHearingId", is(EXTEND_HEARING_ID.toString())),
                                withJsonPath("$.defendantRequests[0].defendantId", is(defendantId.toString()))
                                )
                        ))
                )
        );
    }

    @Test
    public void shouldProcessCommandWhenDefendantRequestsIsNullInAggregate() throws Exception {

        final HearingAggregate hearingAggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(hearingAggregate);

        assignDefendantRequestFromCurrentHearingToExtendHearingHandler.assignDefendantRequestFromCurrentHearingToExtendHearing(buildEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream.findFirst(), is(Optional.empty()));
    }

    private Envelope<AssignDefendantRequestFromCurrentHearingToExtendHearing> buildEnvelope() {
        final AssignDefendantRequestFromCurrentHearingToExtendHearing assignDefendantRequestFromCurrentHearingToExtendHearing = AssignDefendantRequestFromCurrentHearingToExtendHearing.assignDefendantRequestFromCurrentHearingToExtendHearing()
                .withCurrentHearingId(CURRENT_HEARING_ID)
                .withExtendHearingId(EXTEND_HEARING_ID)
                .build();
        return envelope(ASSIGN_DEFENDANT_REQUEST_FROM_CURRENT_HEARING_TO_EXTEND_HEARING, assignDefendantRequestFromCurrentHearingToExtendHearing);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(randomUUID().toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }
}
