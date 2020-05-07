package uk.gov.moj.cpp.progression.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
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

import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.InitiateCourtProceedings;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.progression.aggregate.CasesReferredToCourtAggregate;
import uk.gov.moj.cpp.progression.handler.InitiateCourtProceedingsHandler;

import java.util.Collections;
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
public class InitiateCourtProceedingsHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CourtProceedingsInitiated.class);

    @InjectMocks
    private InitiateCourtProceedingsHandler initiateCourtProceedingsHandler;


    private CasesReferredToCourtAggregate aggregate;

    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a66");


    @Before
    public void setup() {
        aggregate = new CasesReferredToCourtAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CasesReferredToCourtAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new InitiateCourtProceedingsHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.initiate-court-proceedings")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {
        final InitiateCourtProceedings initiateCourtProceedings = generateInitiateCourtProceedings();

        aggregate.initiateCourtProceedings(initiateCourtProceedings.getInitiateCourtProceedings());

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.initiate-court-proceedings")
                .withId(randomUUID())
                .build();

        final Envelope<InitiateCourtProceedings> envelope = envelopeFrom(metadata, initiateCourtProceedings);

        initiateCourtProceedingsHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.court-proceedings-initiated"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.courtReferral", notNullValue ()))
                        ))

                )
        );
    }

    private static InitiateCourtProceedings generateInitiateCourtProceedings() {
        return InitiateCourtProceedings.initiateCourtProceedings().withInitiateCourtProceedings(generateCourtReferral()).build();
    }

    private static CourtReferral generateCourtReferral() {
         return CourtReferral.courtReferral().withProsecutionCases(generateProsecutionCases()).build();
    }

    private static List<ProsecutionCase> generateProsecutionCases() {
        return Collections.singletonList(
                ProsecutionCase.prosecutionCase().withId(CASE_ID).build()
        );
    }


}
