package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@Deprecated
@ExtendWith(MockitoExtension.class)
public class AddDefendantHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private AddDefendantHandler addDefendantHandler;

    @Mock
    private AddDefendant defendant;

    @Test
    public void shouldAddDefendant() throws EventStreamException {
        when(converter.convert(jsonObject, AddDefendant.class)).thenReturn(defendant);
        when(caseAggregate.addDefendant(defendant)).thenReturn(events);

        addDefendantHandler.addDefendant(jsonEnvelope);

        verify(converter).convert(jsonObject, AddDefendant.class);
        verify(caseAggregate).addDefendant(defendant);
    }
}
