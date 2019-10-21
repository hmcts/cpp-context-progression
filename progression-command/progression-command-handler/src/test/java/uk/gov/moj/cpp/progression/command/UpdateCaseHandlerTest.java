package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingResultedUpdateCase;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;
import uk.gov.moj.cpp.progression.handler.UpdateCaseHandler;

import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.matchEvent;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.metadataFor;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.toList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCaseHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(HearingResultedUpdateCase.class);

    @InjectMocks
    private UpdateCaseHandler updateCaseHandler;

    private CaseAggregate aggregate;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();


    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateCaseHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.hearing-resulted-update-case")
                ));
    }

    @Test
    public void shouldProcessHearingResultedUpdateCaseCommand() throws Exception {

        Defendant defendant = Defendant.defendant().withId(randomUUID()).build();
        List<Defendant> defendantList = new ArrayList<Defendant>();
        defendantList.add(defendant);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(randomUUID())
                .withId(randomUUID())
                .withDefendants(defendantList).build();

        aggregate.updateCase(prosecutionCase);

        final Envelope<HearingResultedUpdateCase> envelope =
                envelopeFrom(metadataFor("progression.command.hearing-resulted-update-case",
                        randomUUID()),
                        handlerTestHelper.convertFromFile("json/hearing-resulted-update-case.json", HearingResultedUpdateCase.class));

        updateCaseHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);

    }
}
