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
import uk.gov.moj.cpp.progression.events.RepresentationType;

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
public class DefendantDefenceOrganisationAssociatedProcessorTest {
    private static final String PROGRESSION_COMMAND_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA = "progression.command.handler.associate-defence-organisation-for-laa";

    @Mock
    private Sender sender;


    @InjectMocks
    private DefendantDefenceOrganisationAssociatedProcessor processor;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static UUID defendantId = randomUUID();

    private static UUID organisationId = randomUUID();

    private static String organisationName = "org1";


    private static String reprensentativeType = RepresentationType.REPRESENTATION_ORDER.toString();

    private static String laaContractNumber = "LAA3456";


    @Test
    public void shouldRaiseAssociateDefendantDefenceOrganisation() {

        JsonObject courtDocumentPayload = buildPayloadForDefendantDefenceOrganisationAssociation();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-defence-organisation-associated"),
                courtDocumentPayload);

        processor.processEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PROGRESSION_COMMAND_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA));
        JsonObject commandCreateCourtDocumentPayload = commands.get(0).payload();
        assertThat(commandCreateCourtDocumentPayload.getString("defendantId"), equalTo(defendantId.toString()));
        assertThat(commandCreateCourtDocumentPayload.getString("representationType"), equalTo(reprensentativeType));
        assertThat(commandCreateCourtDocumentPayload.getString("laaContractNumber"), equalTo(laaContractNumber));
        assertThat(commandCreateCourtDocumentPayload.getString("organisationId"), equalTo(organisationId.toString()));
        assertThat(commandCreateCourtDocumentPayload.getString("organisationName"), equalTo(organisationName.toString()));

    }

    private static JsonObject buildPayloadForDefendantDefenceOrganisationAssociation() {


        final JsonObject assocaiateDefenceOrganisation =
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("organisationId", organisationId.toString())
                        .add("organisationName", organisationName)
                        .add("representationType",reprensentativeType)
                        .add("laaContractNumber",laaContractNumber)
                .build();

        return assocaiateDefenceOrganisation;
    }

}
