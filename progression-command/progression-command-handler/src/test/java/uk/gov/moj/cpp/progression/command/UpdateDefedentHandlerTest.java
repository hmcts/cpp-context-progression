package uk.gov.moj.cpp.progression.command;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.UpdateDefendantForProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.handler.UpdateDefedantHandler;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefedentHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CasesReferredToCourt.class);

    @InjectMocks
    private UpdateDefedantHandler updateDefedantHandler;

    private CaseAggregate aggregate;

    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateDefedantHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-defendant-for-prosecution-case")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        final Defendant defendant =
                Defendant.defendant().withPersonDefendant(PersonDefendant.personDefendant().build())
                        .withProsecutionCaseId(UUID.randomUUID())
                        .withId(UUID.randomUUID())
                        .build();
        UpdateDefendantForProsecutionCase updateDefendant = UpdateDefendantForProsecutionCase.updateDefendantForProsecutionCase().withDefendant(defendant).build();
        aggregate.updateDefedantDetails(updateDefendant.getDefendant());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-defendant-for-prosecution-case")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<UpdateDefendantForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendant);

        updateDefedantHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);
    }
}
