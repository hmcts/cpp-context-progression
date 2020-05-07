package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
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

import uk.gov.justice.core.courts.ApplicationEjected;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.EjectCaseOrApplication;
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
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

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
public class EjectCaseApplicationHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            CaseEjected.class, ApplicationEjected.class);

    @InjectMocks
    private EjectCaseApplicationHandler ejectCaseApplicationHandler;


    private CaseAggregate caseAggregate;

    private ApplicationAggregate applicationAggregate;

    private final UUID APPLICATION_ID = randomUUID();
    private final UUID CASE_ID = randomUUID();
    private final String REMOVAL_REASON = "Legal";

    @Before
    public void setup() {
        caseAggregate = new CaseAggregate();
        applicationAggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(applicationAggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new EjectCaseApplicationHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.eject-case-or-application")
                ));
    }


    @Test
    public void shouldProcessCommandWithProsecutionCaseId() throws Exception {

        EjectCaseOrApplication eject = EjectCaseOrApplication.ejectCaseOrApplication().withProsecutionCaseId(CASE_ID).withRemovalReason("Legal").build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.eject-case-or-application")
                .withId(randomUUID())
                .build();

        final Envelope<EjectCaseOrApplication> envelope = envelopeFrom(metadata, eject);

        ejectCaseApplicationHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.case-ejected"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.removalReason", is(REMOVAL_REASON)),
                                withJsonPath("$.prosecutionCaseId", is(CASE_ID.toString()))
                                )
                        ))

                )
        );
    }

    @Test
    public void shouldProcessCommandWithApplicationId() throws Exception {

        EjectCaseOrApplication eject = EjectCaseOrApplication.ejectCaseOrApplication().withApplicationId(APPLICATION_ID).withRemovalReason("Legal").build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.eject-case-or-application")
                .withId(randomUUID())
                .build();

        final Envelope<EjectCaseOrApplication> envelope = envelopeFrom(metadata, eject);

        ejectCaseApplicationHandler.handle(envelope);

        final Stream<JsonEnvelope> envelopeStream = verifyAppendAndGetArgumentFrom(eventStream);

        assertThat(envelopeStream, streamContaining(
                jsonEnvelope(
                        metadata()
                                .withName("progression.event.application-ejected"),
                        JsonEnvelopePayloadMatcher.payload().isJson(allOf(
                                withJsonPath("$.removalReason", is(REMOVAL_REASON)),
                                withJsonPath("$.applicationId", is(APPLICATION_ID.toString()))
                                )
                        ))

                )
        );
    }


}
