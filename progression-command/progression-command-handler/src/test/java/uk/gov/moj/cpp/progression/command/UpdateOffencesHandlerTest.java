package uk.gov.moj.cpp.progression.command;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.UpdateOffencesForProsecutionCase;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.handler.UpdateOffencesHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateOffencesHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CasesReferredToCourt.class);

    @InjectMocks
    private UpdateOffencesHandler updateOffencesHandler;

    private CaseAggregate aggregate;

    @Before
    public void setup() {
        aggregate = new CaseAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateOffencesHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-offences-for-prosecution-case")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        List<Offence> offences = new ArrayList<>();
        Offence offence = Offence.offence().withId(UUID.randomUUID())
                .withOffenceCode("test")
                .build();
        offences.add(offence);
        final DefendantCaseOffences defendantCaseOffences =
                DefendantCaseOffences.defendantCaseOffences().withOffences(offences)
                        .withProsecutionCaseId(UUID.randomUUID())
                        .withDefendantId(UUID.randomUUID())
                        .build();
        UpdateOffencesForProsecutionCase updateDefendantCaseOffences = UpdateOffencesForProsecutionCase.updateOffencesForProsecutionCase().withDefendantCaseOffences(defendantCaseOffences).build();
        aggregate.updateOffences(offences, UUID.randomUUID(), UUID.randomUUID());


        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-offences-for-prosecution-case")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<UpdateOffencesForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendantCaseOffences);

        updateOffencesHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);
    }
}
