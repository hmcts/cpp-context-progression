package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApplicationLaaAssociatedProcessorTest {

    private static final String PUBLIC_DEFENDANT_LAA_CONTRACT_ASSOCIATED = "public.progression.defendant-laa-contract-associated";

    @Mock
    private Sender sender;

    @InjectMocks
    private ApplicationLaaAssociatedProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static UUID defendantId = randomUUID();

    private static String laaContractNumber = "LAA3456";

    @Test
    void shouldRaisePublicEventForDefendantLAAContract() {

        JsonObject laaContractAssociated = buildPayloadForDefendantLAAContractAssociated();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-laa-associated"),
                laaContractAssociated);

        processor.processDefendantLAAAssociated(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> capturedEvents = envelopeCaptor.getAllValues();
        assertThat(capturedEvents.get(0).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PUBLIC_DEFENDANT_LAA_CONTRACT_ASSOCIATED));
        JsonObject capturedEventPayload = capturedEvents.get(0).payload();
        assertThat(capturedEventPayload.getString("defendantId"), equalTo(defendantId.toString()));
        assertThat(capturedEventPayload.getString("laaContractNumber"), equalTo(laaContractNumber));
        assertThat(capturedEventPayload.getBoolean("isAssociatedByLAA"),  is(false));

    }

    private static JsonObject buildPayloadForDefendantLAAContractAssociated() {

        return createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("laaContractNumber",laaContractNumber)
                        .add("isAssociatedByLAA", false)
                        .build();
    }

}
