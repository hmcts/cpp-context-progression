package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EditCourtFeeForCivilApplicationApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private EditCourtFeeForCivilApplicationApi editCourtFeeForCivilApplicationApi;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @Test
    public void shouldEditCivilFee() {
        //Given
        final JsonObject payload = Json.createObjectBuilder()
                .add("applicationId", "cca197ad-2a4c-4cb2-acf9-d4c5e114e3d1")
                .add("courtApplicationPayment", Json.createArrayBuilder().add(Json.createObjectBuilder()
                        .add("feeType", "Initial")
                        .add("feeStatus", "Outstanding")
                        .add("paymentReference", "REF001")
                        .build()).build())
                .build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName("progression.edit-court-fee-for-civil-application")
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();
        final JsonEnvelope commandEnvelope = new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);

        //When
        editCourtFeeForCivilApplicationApi.handle(commandEnvelope);

        //Then
        verifyAddCivilFeeResults(payload);
    }

    private void verifyAddCivilFeeResults(final JsonObject payload) {
        verify(sender, atLeastOnce()).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.edit-court-fee-for-civil-application"));
        assertThat(capturedEnvelope.payload(), is(payload));
    }
}
