package uk.gov.moj.cpp.progression.command.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Deprecated
@ExtendWith(MockitoExtension.class)
public class ProgressionCommandApiTest {

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope command;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> function;


    @InjectMocks
    private ProgressionCommandApi progressionCommandApi;

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        final String userId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final JsonObject value = mock(JsonObject.class);
        when(enveloper.withMetadataFrom(command, "progression.command.handler.add-case-to-crown-court"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);
        progressionCommandApi.addCaseToCrownCourt(command);
        verify(sender).send(command);
    }

    @Test
    public void shouldSendCommittalHearingInformation() throws Exception {
        when(enveloper.withMetadataFrom(command, "progression.command.handler.sending-committal-hearing-information"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);

        progressionCommandApi.sendCommittalHearingInformation(command);
        verify(sender).send(command);
    }

    @Test
    public void shouldSentenceHearingDate() throws Exception {
        when(enveloper.withMetadataFrom(command, "progression.command.record-sentence-hearing-date"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);

        progressionCommandApi.addSentenceHearingDate(command);
        verify(sender).send(command);
    }


    @Test
    public void shouldRequestPSRForDefendants() throws Exception {
        when(enveloper.withMetadataFrom(command, "progression.command.handler.request-psr-for-defendants"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);
        progressionCommandApi.requestPSRForDefendants(command);
        verify(sender).send(command);
    }

    @Test
    public void shouldCompleteSendingSheet() throws Exception {
        progressionCommandApi.completeSendingSheet(command);
        verify(sender).send(command);
    }
}
