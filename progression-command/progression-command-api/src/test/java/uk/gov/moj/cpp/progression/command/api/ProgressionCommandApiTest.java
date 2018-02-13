package uk.gov.moj.cpp.progression.command.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;

import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
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
        String userId = UUID.randomUUID().toString();
        String caseId = UUID.randomUUID().toString();
        String defendantId = UUID.randomUUID().toString();
        JsonObject value = mock(JsonObject.class);
        JsonObjectMetadata metadata = mock(JsonObjectMetadata.class);
        when(command.payloadAsJsonObject()).thenReturn(value);
        when(value.getString("caseId")).thenReturn(caseId);
        when(metadata.userId()).thenReturn(Optional.of(userId));
        when(command.metadata()).thenReturn(metadata);
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
    public void shouldCaseToBeAssigned() throws Exception {
        when(enveloper.withMetadataFrom(command, "progression.command.handler.case-to-be-assigned"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);
        progressionCommandApi.updateCaseToBeAssigned(command);
        verify(sender).send(command);
    }


    @Test
    public void shouldAddDefendantProgression() throws Exception {
        when(enveloper.withMetadataFrom(command, "progression.command.handler.add-defendant-additional-information"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);

        progressionCommandApi.addAdditionalInformationForDefendant(command);
        verify(sender).send(command);
    }


    @Test
    public void shouldPassNoMoreInformationRequired() throws Exception {
        when(enveloper.withMetadataFrom(command, "progression.command.record-no-more-information-required"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);

        progressionCommandApi.noMoreInformationRequired(command);
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
