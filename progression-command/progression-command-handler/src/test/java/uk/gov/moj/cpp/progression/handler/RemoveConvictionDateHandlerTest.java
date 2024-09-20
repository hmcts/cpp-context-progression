package uk.gov.moj.cpp.progression.handler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.RemoveConvictionDate;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RemoveConvictionDateHandlerTest {
    protected static final UUID CASE_ID = UUID.randomUUID();
    protected static final UUID APPLICATION_ID = UUID.randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    protected EventStream eventStream;

    @Mock
    protected CaseAggregate caseAggregate;

    @Mock
    protected ApplicationAggregate applicationAggregate;

    @Mock
    protected Stream<Object> events;

    @Mock
    protected Stream<JsonEnvelope> jsonEvents;

    @Mock
    protected Function function;


    @InjectMocks
    private RemoveConvictionDateHandler removeConvictionDateHandler;


    @Test
    public void removeConvictionDateToOffenceUnderProsecutionCase() throws EventStreamException {

        final UUID offenceId = UUID.randomUUID();

        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class))
                .thenReturn(caseAggregate);
        when(caseAggregate.removeConvictionDate(eq(CASE_ID), eq(offenceId)))
                .thenReturn(events);

        RemoveConvictionDate removeConvictionDate = RemoveConvictionDate.removeConvictionDate()
                .withCaseId(CASE_ID)
                .withOffenceId(offenceId).build();

        removeConvictionDateHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("hearing.conviction-date-removed"),
                removeConvictionDate));

        verify(caseAggregate).removeConvictionDate(CASE_ID, offenceId);

    }

    @Test
    public void addConvictionDateToOffenceUnderCourtApplicationCaseCase() throws EventStreamException {

        final UUID offenceId = UUID.randomUUID();

        when(eventSource.getStreamById(APPLICATION_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class))
                .thenReturn(applicationAggregate);

        when(applicationAggregate.removeConvictionDate(eq(APPLICATION_ID), eq(offenceId)))
                .thenReturn(events);

        RemoveConvictionDate removeConvictionDate = RemoveConvictionDate.removeConvictionDate()
                .withCourtApplicationId(APPLICATION_ID)
                .withOffenceId(offenceId).build();

        removeConvictionDateHandler.handle(Envelope.envelopeFrom(metadataWithRandomUUID("hearing.conviction-date-removed"),
                removeConvictionDate));

        verify(applicationAggregate).removeConvictionDate(APPLICATION_ID, offenceId);

    }
}
