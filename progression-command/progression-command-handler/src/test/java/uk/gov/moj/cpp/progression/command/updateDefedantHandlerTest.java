package uk.gov.moj.cpp.progression.command;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseUpdateDefendantsWithMatchedRequested;
import uk.gov.justice.core.courts.UpdateDefendantForProsecutionCase;
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
import uk.gov.moj.cpp.progression.handler.UpdateDefendantHandler;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class updateDefedantHandlerTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ProsecutionCaseDefendantUpdated.class,
            ProsecutionCaseUpdateDefendantsWithMatchedRequested.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
    @InjectMocks
    private UpdateDefendantHandler updateDefendantHandler;

    private CaseAggregate aggregate;

    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateDefendantHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-defendant-for-prosecution-case")
                ));
    }

    @Test
    @SuppressWarnings("squid:S00112")
    public void shouldProcessCommand() throws Exception {

        UUID defendantId = UUID.randomUUID();
        final DefendantUpdate defendant =
                DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(UUID.randomUUID())
                        .withId(defendantId)
                        .build();
        UpdateDefendantForProsecutionCase updateDefendant = UpdateDefendantForProsecutionCase.updateDefendantForProsecutionCase().withDefendant(defendant).build();


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-prosecution-case")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<UpdateDefendantForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendant);

        updateDefendantHandler.handle(envelope);

        Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-defendant-updated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    @SuppressWarnings("squid:S00112")
    public void shouldProcessCommandWithMatchedDefendants() throws Exception {

        UUID defendantId = UUID.randomUUID();
        final DefendantUpdate defendant =
                DefendantUpdate.defendantUpdate().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(UUID.randomUUID())
                        .withId(defendantId)
                        .build();
        UUID matchedDefendantHearingId = UUID.randomUUID();
        UpdateDefendantForProsecutionCase updateDefendant = UpdateDefendantForProsecutionCase.updateDefendantForProsecutionCase()
                .withDefendant(defendant)
                .withMatchedDefendantHearingId(matchedDefendantHearingId)
                .build();


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-prosecution-case")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<UpdateDefendantForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendant);

        updateDefendantHandler.handle(envelope);

        Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-update-defendants-with-matched-requested"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.defendant.id", is(defendantId.toString())),
                                withJsonPath("$.matchedDefendantHearingId", is(matchedDefendantHearingId.toString()))
                                )
                        ))

                )
        );
    }
}
