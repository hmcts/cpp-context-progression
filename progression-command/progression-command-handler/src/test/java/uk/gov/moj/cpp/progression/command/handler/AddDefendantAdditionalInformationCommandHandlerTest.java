package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;


@RunWith(MockitoJUnitRunner.class)
public class AddDefendantAdditionalInformationCommandHandlerTest
                extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private AddDefendantAdditionalInformationHandler addDefendantHandler;

    @Mock
    private DefendantCommand defendant;

    @Mock
    private ProgressionEventFactory factory;

    @Mock
    private DefendantAdditionalInformationAdded defendantEvent;

    @Test
    public void shouldAddDefendant() throws EventStreamException {
        when(converter.convert(jsonObject, DefendantCommand.class)).thenReturn(defendant);
        when(factory.addDefendantEvent(defendant)).thenReturn(defendantEvent);
        when(caseProgressionAggregate.addAdditionalInformationForDefendant(defendant))
                        .thenReturn(events);

        addDefendantHandler.addAdditionalInformationForDefendant(jsonEnvelope);

        verify(converter).convert(jsonObject, DefendantCommand.class);
        verify(caseProgressionAggregate).addAdditionalInformationForDefendant(defendant);
    }
}
