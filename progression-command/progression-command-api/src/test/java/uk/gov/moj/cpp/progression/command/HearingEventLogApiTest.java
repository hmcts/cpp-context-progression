package uk.gov.moj.cpp.progression.command;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.UserGroupQueryService;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingEventLogApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private HearingEventLogApi hearingEventLogApi;

    @Mock
    private JsonEnvelope command;

    @Mock
    private UserGroupQueryService userGroupQueryService;

    @Test
    public void shouldHandleHearingEventLogDocument() {
        when(userGroupQueryService.doesUserBelongsToHmctsOrganisation(any(JsonEnvelope.class), any())).thenReturn(true);
        when(command.metadata()).thenReturn(CommandClientTestBase.metadataFor("progression.command.create-hearing-event-log-document", UUID.randomUUID().toString()));
        hearingEventLogApi.handleHearingEventLog(command);
        verify(sender, times(1)).send(any());

    }

    @Test
    public void shouldHandleAaagHearingEventLogDocument() {
        when(userGroupQueryService.doesUserBelongsToHmctsOrganisation(any(JsonEnvelope.class), any())).thenReturn(true);
        when(command.metadata()).thenReturn(CommandClientTestBase.metadataFor("progression.command.create-aaag-hearing-event-log-document", UUID.randomUUID().toString()));
        hearingEventLogApi.handleAaagHearingEventLog(command);
        verify(sender, times(1)).send(any());

    }

    @Test
    public void shouldNotHandleHearingEventLogDocument() {
        when(userGroupQueryService.doesUserBelongsToHmctsOrganisation(any(JsonEnvelope.class), any())).thenReturn(false);
        when(command.metadata()).thenReturn(CommandClientTestBase.metadataFor("progression.command.create-hearing-event-log-document", UUID.randomUUID().toString()));
        hearingEventLogApi.handleHearingEventLog(command);

        verify(sender, times(0)).send(any());

    }

    @Test
    public void shouldNotHandleAaagHearingEventLogDocument() {
        when(userGroupQueryService.doesUserBelongsToHmctsOrganisation(any(JsonEnvelope.class), any())).thenReturn(false);
        when(command.metadata()).thenReturn(CommandClientTestBase.metadataFor("progression.command.create-aaag-hearing-event-log-document", UUID.randomUUID().toString()));
        hearingEventLogApi.handleAaagHearingEventLog(command);

        verify(sender, times(0)).send(any());

    }

}
