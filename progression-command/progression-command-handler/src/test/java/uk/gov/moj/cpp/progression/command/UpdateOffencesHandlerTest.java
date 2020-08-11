package uk.gov.moj.cpp.progression.command;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.CasesReferredToCourt;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantCaseOffences;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
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

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(CasesReferredToCourt.class);
    @Mock
    private EventSource eventSource;
    @Mock
    private EventStream eventStream;
    @Mock
    private AggregateService aggregateService;
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

        UUID caseId = randomUUID();
        UUID defendantId = randomUUID();
        List<Offence> offences = new ArrayList<>();
        Offence offence = Offence.offence().withId(randomUUID())
                .withOffenceCode("test")
                .build();
        offences.add(offence);
        final DefendantCaseOffences defendantCaseOffences =
                DefendantCaseOffences.defendantCaseOffences().withOffences(offences)
                        .withProsecutionCaseId(caseId)
                        .withDefendantId(defendantId)
                        .build();
        UpdateOffencesForProsecutionCase updateDefendantCaseOffences = UpdateOffencesForProsecutionCase.updateOffencesForProsecutionCase().withDefendantCaseOffences(defendantCaseOffences).build();

        aggregate = new CaseAggregate() {{
            apply(ProsecutionCaseCreated.prosecutionCaseCreated()
                    .withProsecutionCase(ProsecutionCase.prosecutionCase()
                            .withId(caseId)
                            .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier().build())
                            .withDefendants(singletonList(Defendant.defendant()
                                    .withId(defendantId)
                                    .withOffences(singletonList(Offence.offence()
                                            .withId(randomUUID())
                                            .withOrderIndex(1)
                                            .build()))
                                    .build()))
                            .build())
                    .build());
        }};
        when(this.aggregateService.get(this.eventStream, CaseAggregate.class)).thenReturn(aggregate);

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-offences-for-prosecution-case")
                .withId(randomUUID())
                .build();

        final Envelope<UpdateOffencesForProsecutionCase> envelope = envelopeFrom(metadata, updateDefendantCaseOffences);

        updateOffencesHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);
    }
}
