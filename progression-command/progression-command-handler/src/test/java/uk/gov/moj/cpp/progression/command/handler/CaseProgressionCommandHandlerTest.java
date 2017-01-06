package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;

@RunWith(MockitoJUnitRunner.class)
public class CaseProgressionCommandHandlerTest {

    protected static final UUID CASE_ID = UUID.randomUUID();
    private static final String ACTION_NAME = "actionName";
    private static final String CASE_URN = "caseUrn";
    private boolean updateAllowed = true;

    @Mock
    protected EventSource eventSource;

    @Mock
    protected Enveloper enveloper;

    @Mock
    protected AggregateService aggregateService;

    @Mock
    protected JsonObjectToObjectConverter converter;

    @Mock
    protected JsonEnvelope jsonEnvelope;

    @Mock
    protected JsonObject jsonObject;

    @Mock
    protected EventStream eventStream;

    @Mock
    protected CaseProgressionAggregate caseProgressionAggregate;

    @Mock
    protected Function function;

    @Mock
    protected Stream<Object> events;

    @Mock
    protected Stream<JsonEnvelope> jsonEvents;

    @Mock
    private Metadata metadata;


    @InjectMocks
    private CaseProgressionCommandHandler caseProgressionCommandHandler;

    private Function<CaseProgressionAggregate, Stream<Object>> aggregateFunction =
                    caseProgressionAggregate -> events;

    @Before
    @SuppressWarnings("unchecked")
    public void setupMocks() {
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.name()).thenReturn(ACTION_NAME);
        when(jsonObject.getString(CaseProgressionCommandHandler.FIELD_STREAM_ID))
                        .thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseProgressionAggregate.class))
                        .thenReturn(caseProgressionAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
    }

    @After
    @SuppressWarnings("unchecked")
    public void verifyMocks() throws EventStreamException {
        verify(jsonEnvelope, atLeast(1)).payloadAsJsonObject();
        verify(jsonObject, atLeast(1)).getString(CaseProgressionCommandHandler.FIELD_STREAM_ID);
        verify(eventSource).getStreamById(CASE_ID);
        verify(aggregateService).get(eventStream, CaseProgressionAggregate.class);
        verify(enveloper).withMetadataFrom(jsonEnvelope);
        verify(events).map(function);
        verify(eventStream).append(jsonEvents);

        verifyNoMoreInteractions(eventSource);
        verifyNoMoreInteractions(enveloper);
        verifyNoMoreInteractions(aggregateService);
        verifyNoMoreInteractions(converter);
        verifyNoMoreInteractions(jsonEnvelope);
        verifyNoMoreInteractions(eventStream);
        verifyNoMoreInteractions(caseProgressionAggregate);
        verifyNoMoreInteractions(function);
        verifyNoMoreInteractions(events);
        verifyNoMoreInteractions(jsonEvents);
    }

    @Test
    public void testApplyToCaseAggregate() throws EventStreamException {
        caseProgressionCommandHandler.applyToCaseProgressionAggregate(jsonEnvelope,
                        aggregateFunction);
    }

}
