package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
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

import uk.gov.justice.core.courts.CreateProsecutionCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
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

import java.util.ArrayList;
import java.util.List;
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
public class CreateProsecutionCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            ProsecutionCaseCreated.class);

    @InjectMocks
    private CreateProsecutionCaseHandler createProsecutionCaseHandler;


    private CaseAggregate aggregate;


    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new CreateProsecutionCaseHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.create-prosecution-case")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final Defendant defendant = Defendant.defendant().withId(UUID.randomUUID()).withPersonDefendant(PersonDefendant.personDefendant().build())
                .withOffences(singletonList(Offence.offence().build()))
                .build();
        final List<Defendant> defendants = new ArrayList<Defendant>() {{ add(defendant); }};
        final CreateProsecutionCase createProsecutionCase = CreateProsecutionCase.createProsecutionCase()
                .withProsecutionCase(ProsecutionCase.prosecutionCase()
                        .withDefendants(defendants)
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withProsecutionAuthorityId(UUID.randomUUID())
                                .withProsecutionAuthorityCode("code")
                                .withProsecutionAuthorityReference("reference")
                                .build())
                        .withClassOfCase("Class 1")
                        .build())
                .build();
        aggregate.apply(createProsecutionCase.getProsecutionCase());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.create-prosecution-case")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<CreateProsecutionCase> envelope = envelopeFrom(metadata, createProsecutionCase);

        createProsecutionCaseHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.prosecution-case-created"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.prosecutionCase", notNullValue ()),
                                withJsonPath("$.prosecutionCase.classOfCase", is("Class 1")))
                        ))

                )
        );
    }



}
