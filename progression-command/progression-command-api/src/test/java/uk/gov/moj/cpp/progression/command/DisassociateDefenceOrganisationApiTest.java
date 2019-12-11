package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class DisassociateDefenceOrganisationApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private DisassociateDefenceOrganisationApi disAssociateDefenceOrganisationApiTest;

    @Test
    public void shouldAssociateDefenceOrganisation() {

        //Given
        JsonEnvelope commandEnvelope = createDisassociationCommandEnvelope();

        //When
        disAssociateDefenceOrganisationApiTest.handle(commandEnvelope);

        //Then
        verifyDefenceAssociationResults(commandEnvelope);

    }

    private JsonEnvelope createDisassociationCommandEnvelope() {

        final UUID uuid = randomUUID();
        final UUID userId = randomUUID();

        final JsonObject payload = Json.createObjectBuilder()
                .add("organisationId", randomUUID().toString())
                .build();

        return JsonEnvelope.envelopeFrom(
                metadataWithDefaults().withName("progression.disassociate-defence-organisation"),
                payload);

    }

    private void verifyDefenceAssociationResults(final JsonEnvelope commandEnvelope) {

        verify(sender).send(envelopeCaptor.capture());
        final DefaultEnvelope capturedEnvelope = envelopeCaptor.getValue();
        assertThat(capturedEnvelope.metadata().name(), is("progression.command.handler.disassociate-defence-organisation"));
        assertThat(capturedEnvelope.payload(), is(commandEnvelope.payloadAsJsonObject()));
    }
}
