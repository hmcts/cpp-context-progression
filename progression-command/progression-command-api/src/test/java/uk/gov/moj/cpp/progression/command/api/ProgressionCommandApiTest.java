package uk.gov.moj.cpp.progression.command.api;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectMetadata;
import uk.gov.moj.cpp.progression.command.api.service.StructureReadService;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

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

    @Mock
    private StructureReadService structureCaseService;

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
        when(structureCaseService.getStructureCaseDefendantsId(caseId, command.metadata().userId().toString()))
                .thenReturn(Arrays.asList(defendantId));
        when(enveloper.withMetadataFrom(command, "progression.command.add-case-to-progression"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(command);
        progressionCommandApi.addCaseToCrownCourt(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldSendCommittalHearingInformation() throws Exception {
        progressionCommandApi.sendCommittalHearingInformation(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldSentenceHearingDate() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.record-sentence-hearing-date"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        progressionCommandApi.addSentenceHearingDate(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldCaseToBeAssigned() throws Exception {
        progressionCommandApi.updateCaseToBeAssigned(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldCaseAssignedForReview() throws Exception {
        progressionCommandApi.updateCaseAssignedForReview(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldPrepareForSentenceHearing() throws Exception {
        progressionCommandApi.prepareForSentenceHearing(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddDefendantProgression() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.handler.add-defendant-additional-information"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        progressionCommandApi.addAdditionalInformationForDefendant(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldPassNoMoreInformationRequired() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.record-no-more-information-required"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        progressionCommandApi.noMoreInformationRequired(command);

        verify(sender, times(1)).send(commandEnvelope);
    }

    @Test
    public void shouldRequestPSRForDefendants() throws Exception {
        progressionCommandApi.requestPSRForDefendants(command);
        verify(sender, times(1)).send(command);
    }

    @Test
    public void shouldAddSentenceHearing() throws Exception {
        final JsonEnvelope commandEnvelope = mock(JsonEnvelope.class);
        when(enveloper.withMetadataFrom(command, "progression.command.record-sentence-hearing"))
                .thenReturn(function);
        when(function.apply(any())).thenReturn(commandEnvelope);

        progressionCommandApi.addSentenceHearing(command);

        verify(sender, times(1)).send(commandEnvelope);
    }
}
