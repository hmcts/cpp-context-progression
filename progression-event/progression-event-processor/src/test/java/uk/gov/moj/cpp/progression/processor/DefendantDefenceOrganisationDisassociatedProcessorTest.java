package uk.gov.moj.cpp.progression.processor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import javax.json.JsonObject;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

@RunWith(MockitoJUnitRunner.class)
public class DefendantDefenceOrganisationDisassociatedProcessorTest {
    private static final String PROGRESSION_COMMAND_DISASSOCIATE_DEFENCE_ORGANISATION = "progression.command.handler.disassociate-defence-organisation";

    @Mock
    private Sender sender;


    @InjectMocks
    private DefendantDefenceOrganisationDisassociatedProcessor processor;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static UUID defendantId = randomUUID();

    private static UUID organisationId = randomUUID();


    @Test
    public void shouldRaiseDisassociateDefendantDefenceOrganisation() {

        JsonObject disAssociatedOrganisationPayload = buildPayloadForDefendantDefenceOrganisationDisassociation();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-defence-organisation-disassociated"),
                disAssociatedOrganisationPayload);

        processor.processEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_DISASSOCIATE_DEFENCE_ORGANISATION));
        JsonObject commandCreateCourtDocumentPayload = commands.get(0).payload();
        assertThat(commandCreateCourtDocumentPayload.getString("defendantId"), equalTo(defendantId.toString()));
        assertThat(commandCreateCourtDocumentPayload.getString("organisationId"), equalTo(organisationId.toString()));

    }

    private static JsonObject buildPayloadForDefendantDefenceOrganisationDisassociation() {


        final JsonObject disAssocaiateDefenceOrganisation =
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("organisationId",organisationId.toString())
                .build();

        return disAssocaiateDefenceOrganisation;
    }

}
