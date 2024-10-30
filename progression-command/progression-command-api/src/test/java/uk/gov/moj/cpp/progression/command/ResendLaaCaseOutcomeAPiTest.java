package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResendLaaCaseOutcomeAPiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private ResendLaaCaseOutcomeAPi resendLaaCaseOutcomeAPi;

    @Test
    public void testHandleResendLaaCaseOutcome() {

        final UUID caseId1 = randomUUID();
        final UUID caseId2 = randomUUID();
        final JsonObject commandPayload = Json.createObjectBuilder()
                .add("caseIds", createArrayBuilder()
                        .add(caseId1.toString())
                        .add(caseId2.toString())
                        .build())
                .build();
        final Metadata commandMetadata = metadataBuilder().withName("progression.command.resend-laa-outcome-concluded")
                .withClientCorrelationId(randomUUID().toString())
                .withId(randomUUID())
                .build();
        final JsonEnvelope commandEnvelope = envelopeFrom(commandMetadata, commandPayload);

        resendLaaCaseOutcomeAPi.handle(commandEnvelope);

        ArgumentCaptor<Envelope> captor = forClass(Envelope.class);
        verify(sender, times(2)).send(captor.capture());

        List<Envelope> currentEvents = captor.getAllValues();
        assertThat(currentEvents.get(0).metadata().name(), Matchers.is("progression.command.handler.resend-laa-outcome-concluded"));
        assertThat(currentEvents.get(1).metadata().name(), Matchers.is("progression.command.handler.resend-laa-outcome-concluded"));

        assertThat(currentEvents.get(0).payload().toString(), containsString(caseId1.toString()));
        assertThat(currentEvents.get(1).payload().toString(), containsString(caseId2.toString()));
    }

}
