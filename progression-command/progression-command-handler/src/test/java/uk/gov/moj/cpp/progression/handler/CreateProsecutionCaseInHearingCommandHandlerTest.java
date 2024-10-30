package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
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

import uk.gov.justice.core.courts.ProsecutionCaseCreatedInHearing;
import uk.gov.justice.progression.courts.CreateProsecutionCaseInHearing;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.handler.CreateProsecutionCaseInHearingCommandHandler;

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
public class CreateProsecutionCaseInHearingCommandHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ProsecutionCaseCreatedInHearing.class);

    @InjectMocks
    private CreateProsecutionCaseInHearingCommandHandler createProsecutionCaseInHearingCommandHandler;


    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateProsecutionCaseInHearingCommandHandler(), isHandler(COMMAND_HANDLER)
                .with(method("createProsecutionCaseInHearing")
                        .thatHandles("progression.command.create-prosecution-case-in-hearing")
                ));
    }

    @Test
    public void shouldCreateProsecutionCaseInHearingEvent() throws Exception {

        final UUID prosecutionCaseId = randomUUID();

        final CreateProsecutionCaseInHearing createProsecutionCaseInHearing = CreateProsecutionCaseInHearing.createProsecutionCaseInHearing()
                .withProsecutionCaseId(prosecutionCaseId)
                .build();

        final Metadata metadata = Envelope.metadataBuilder()
                .withName("progression.command.create-prosecution-case-in-hearing")
                .withId(randomUUID())
                .build();

        final Envelope<CreateProsecutionCaseInHearing> envelope = envelopeFrom(metadata, createProsecutionCaseInHearing);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        createProsecutionCaseInHearingCommandHandler.createProsecutionCaseInHearing(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-created-in-hearing"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.prosecutionCaseId", equalTo(prosecutionCaseId.toString())))
                        )
                )
        ));

    }
}
