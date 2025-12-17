package uk.gov.moj.cpp.progression.handler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import uk.gov.justice.core.courts.DeleteFinancialMeans;
import uk.gov.justice.core.courts.FinancialMeansDeleted;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteDefendantFinancialMeansHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private DeleteDefendantFinancialMeansHandler deleteDefendantFinancialMeansHandler;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            FinancialMeansDeleted.class);

    private static final UUID CASE_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a77");

    private static final UUID DEFENDANT_ID = UUID.fromString("5002d600-af66-11e8-b568-0800200c9a88");

    @Test
    public void shouldHandleCommand() {
        assertThat(new DeleteDefendantFinancialMeansHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.delete-financial-means")
                ));
    }

    @Test
    public void shouldProcessCommand() throws Exception {

        DeleteFinancialMeans deleteFinancialMeans = DeleteFinancialMeans.deleteFinancialMeans()
                .withCaseId(CASE_ID)
                .withDefendantId(DEFENDANT_ID)
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.delete-financial-means")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<DeleteFinancialMeans> envelope = envelopeFrom(metadata, deleteFinancialMeans);

        final CaseAggregate caseAggregate = new CaseAggregate();
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        deleteDefendantFinancialMeansHandler.handle(envelope);

        verifyAppendAndGetArgumentFrom(eventStream);

    }
}
