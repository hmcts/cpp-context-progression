package uk.gov.moj.cpp.progression.command;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.core.courts.ApplicationReferredToCourt;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.UpdateCourtApplicationStatus;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.aggregate.ApplicationAggregate;
import uk.gov.moj.cpp.progression.handler.UpdateCourtApplicationStatusHandler;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.helper.EventStreamMockHelper.verifyAppendAndGetArgumentFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;

import java.util.UUID;
import java.util.stream.Stream;

@RunWith(MockitoJUnitRunner.class)
public class UpdateCourtApplicationStatusHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @InjectMocks
    private UpdateCourtApplicationStatusHandler updateCourtApplicationStatusHandler;

    private ApplicationAggregate aggregate;
   
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(ApplicationReferredToCourt.class);

    @Before
    public void setup() {
        aggregate = new ApplicationAggregate();
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, ApplicationAggregate.class)).thenReturn(aggregate);
    }

    @Test
    public void shouldHandleCommand() {
        assertThat(new UpdateCourtApplicationStatusHandler(), isHandler(COMMAND_HANDLER)
                .with(method("handle")
                        .thatHandles("progression.command.update-court-application-status")
                ));
    }
    
    @Test
    public void shouldProcessCommand() throws Exception {

        final UpdateCourtApplicationStatus updateApplicationStatus = createUpdateCourtApplicationStatus();
        aggregate.updateApplicationStatus(updateApplicationStatus.getId(), updateApplicationStatus.getApplicationStatus());
        

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.command.update-court-application-status")
                .withId(UUID.randomUUID())
                .build();

        final Envelope<UpdateCourtApplicationStatus> envelope = envelopeFrom(metadata, updateApplicationStatus);
        updateCourtApplicationStatusHandler.handle(envelope);
        verifyAppendAndGetArgumentFrom(eventStream);

    }

    private static UpdateCourtApplicationStatus createUpdateCourtApplicationStatus() {
        return UpdateCourtApplicationStatus.updateCourtApplicationStatus()
                .withId(UUID.randomUUID())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .build();
    }

}
