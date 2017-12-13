package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.progression.command.handler.convertor.OffencesForDefendantConverter;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class UpdateOffencesForDefendantHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private UpdateOffencesForDefendantHandler handler;

    @Mock
    private OffencesForDefendantUpdated offencesForDefendantUpdated;

    @Mock
    private OffencesForDefendantConverter offencesForDefendantConverter;

    @Test
    public void shouldUpdateOffencesForDefendant() throws EventStreamException {
        when(offencesForDefendantConverter.convert(jsonEnvelope))
                        .thenReturn(offencesForDefendantUpdated);
        when(caseProgressionAggregate.updateOffencesForDefendant(offencesForDefendantUpdated))
                        .thenReturn(events);

        handler.updateOffences(jsonEnvelope);

        verify(offencesForDefendantConverter).convert(jsonEnvelope);
        verify(caseProgressionAggregate).updateOffencesForDefendant(offencesForDefendantUpdated);
    }

}
