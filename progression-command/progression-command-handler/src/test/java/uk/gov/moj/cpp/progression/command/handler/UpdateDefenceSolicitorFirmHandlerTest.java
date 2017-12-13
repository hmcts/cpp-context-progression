package uk.gov.moj.cpp.progression.command.handler;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantDefenceSolicitorFirm;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefenceSolicitorFirmHandlerTest extends CaseProgressionCommandHandlerTest {

    @InjectMocks
    private UpdateDefenceSolicitorFirmHandler handler;

    @Mock
    private UpdateDefendantDefenceSolicitorFirm updateDefendantDefenceSolicitorFirm;

    @Test
    public void shouldUpdateDefendantSolicitorFirm() throws EventStreamException {
        when(converter.convert(jsonObject, UpdateDefendantDefenceSolicitorFirm.class))
                .thenReturn(updateDefendantDefenceSolicitorFirm);
        when(caseProgressionAggregate.updateDefenceSolicitorFirm(updateDefendantDefenceSolicitorFirm))
                .thenReturn(events);

        handler.updateDefendantSolicitorFirmForDefendant(jsonEnvelope);

        verify(converter).convert(jsonObject, UpdateDefendantDefenceSolicitorFirm.class);
        verify(caseProgressionAggregate).updateDefenceSolicitorFirm(updateDefendantDefenceSolicitorFirm);
    }

}
