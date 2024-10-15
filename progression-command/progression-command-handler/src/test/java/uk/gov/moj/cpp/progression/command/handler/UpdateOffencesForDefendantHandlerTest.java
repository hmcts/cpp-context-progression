package uk.gov.moj.cpp.progression.command.handler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.progression.command.handler.convertor.OffencesForDefendantConverter;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Deprecated
@ExtendWith(MockitoExtension.class)
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
        when(caseAggregate.updateOffencesForDefendant(offencesForDefendantUpdated))
                        .thenReturn(events);

        handler.updateOffences(jsonEnvelope);

        verify(offencesForDefendantConverter).convert(jsonEnvelope);
        verify(caseAggregate).updateOffencesForDefendant(offencesForDefendantUpdated);
    }

}
