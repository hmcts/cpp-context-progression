package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

@Deprecated
@RunWith(MockitoJUnitRunner.class)
public class RequestPsrForDefendantHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private RequestPsrForDefendantHandler requestPsrForDefendantHandler;

    @Test
    public void shouldRequestPsrForDefendant() throws EventStreamException {

        when(caseAggregate.requestPsrForDefendant(jsonEnvelope))
                        .thenReturn(events);

        requestPsrForDefendantHandler.requestPsrForDefendants(jsonEnvelope);
        verify(caseAggregate).requestPsrForDefendant(jsonEnvelope);
    }
}
