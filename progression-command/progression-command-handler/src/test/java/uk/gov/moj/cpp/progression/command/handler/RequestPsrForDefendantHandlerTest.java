package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@Deprecated
@ExtendWith(MockitoExtension.class)
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
