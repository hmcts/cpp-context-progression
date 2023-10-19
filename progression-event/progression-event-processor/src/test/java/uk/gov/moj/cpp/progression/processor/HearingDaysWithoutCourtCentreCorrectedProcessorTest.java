package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingDaysWithoutCourtCentreCorrectedProcessorTest {

    @InjectMocks
    private HearingDaysWithoutCourtCentreCorrectedProcessor hearingDaysWithoutCourtCentreCorrectedProcessor;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @Mock
    private Sender sender;

    @Mock
    private JsonEnvelope finalEnvelope;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Test
    public void shouldCorrectHearingDaysWithoutCourtCentre(){
        final UUID id = randomUUID();
        final JsonObject payload = createObjectBuilder().add("id", id.toString()).build();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(payload);

        when(enveloperFunction.apply(any(JsonObject.class))).thenReturn(finalEnvelope);
        when(enveloper.withMetadataFrom(jsonEnvelope, "progression.command.populate-hearing-to-probation-caseworker")).thenReturn(enveloperFunction);

        hearingDaysWithoutCourtCentreCorrectedProcessor.correctHearingDaysWithoutCourtCentre(jsonEnvelope);

        verify(progressionService, times(1)).populateHearingToProbationCaseworker(jsonEnvelope, id);
    }
}
