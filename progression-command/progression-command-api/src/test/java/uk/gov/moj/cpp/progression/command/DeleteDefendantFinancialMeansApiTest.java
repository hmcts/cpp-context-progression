package uk.gov.moj.cpp.progression.command;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.justice.services.messaging.JsonObjects;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteDefendantFinancialMeansApiTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private DeleteDefendantFinancialMeansApi commandApi;

    @Test
    public void shouldRaiseCommandToDeleteDefendantFinancialMeans() throws Exception {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("progression.delete-financial-means"),
                JsonObjects.createObjectBuilder()
                        .build()
        );

        commandApi.handle(jsonEnvelope);

        verify(sender).send(isA(Envelope.class));
    }
}
