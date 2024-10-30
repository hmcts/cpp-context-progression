package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.AssignDefendantRequestToExtendHearing;
import uk.gov.justice.core.courts.DefendantRequestToExtendHearingCreated;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.HearingAggregate;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AssignDefendantRequestToExtendHearingHandlerTest {

    private static final String ASSIGN_DEFENDANT_REQUEST_TO_EXTEND_HEARING = "progression.command.assign-defendant-request-to-extend-hearing";
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID PROSECUTION_CASE_ID = randomUUID();
    private static final UUID HEARING_ID = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private AssignDefendantRequestToExtendHearingHandler assignDefendantRequestToExtendHearingHandler;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private final JsonObjectToObjectConverter jsonToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DefendantRequestToExtendHearingCreated.class);

    @BeforeEach
    public void setup() {
        setField(this.jsonToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new AssignDefendantRequestToExtendHearingHandler(), isHandler(COMMAND_HANDLER)
                .with(method("assignDefendantRequestToExtendHearing")
                        .thatHandles(ASSIGN_DEFENDANT_REQUEST_TO_EXTEND_HEARING)
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final HearingAggregate aggregate = new HearingAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, HearingAggregate.class)).thenReturn(aggregate);

        assignDefendantRequestToExtendHearingHandler.assignDefendantRequestToExtendHearing(buildEnvelope());
        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.defendant-request-to-extend-hearing-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                                withJsonPath("$.defendantRequests", notNullValue()),
                                withJsonPath("$.defendantRequests[0].prosecutionCaseId", equalTo(PROSECUTION_CASE_ID.toString())),
                                withJsonPath("$.defendantRequests[0].defendantId", equalTo(DEFENDANT_ID.toString()))
                                )
                        ))
                )
        );
    }

    private Envelope<AssignDefendantRequestToExtendHearing> buildEnvelope() {
        final AssignDefendantRequestToExtendHearing assignDefendantRequestToExtendHearing = AssignDefendantRequestToExtendHearing.assignDefendantRequestToExtendHearing()
                .withHearingId(HEARING_ID)
                .withDefendantRequests(Arrays.asList(ListDefendantRequest.listDefendantRequest()
                        .withProsecutionCaseId(PROSECUTION_CASE_ID)
                        .withDefendantId(DEFENDANT_ID)
                        .build()))
                .build();
        return envelope(ASSIGN_DEFENDANT_REQUEST_TO_EXTEND_HEARING, assignDefendantRequestToExtendHearing);
    }

    private <T> Envelope<T> envelope(final String name, final T t) {
        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID(name).withUserId(randomUUID().toString()).build());
        return envelopeFrom(metadataBuilder, t);
    }
}
