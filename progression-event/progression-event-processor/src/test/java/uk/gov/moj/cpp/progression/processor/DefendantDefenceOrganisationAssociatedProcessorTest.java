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
import uk.gov.moj.cpp.progression.events.RepresentationType;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDefenceOrganisationAssociatedProcessorTest {
    protected static final String PUBLIC_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA = "public.progression.defence-organisation-for-laa-associated";

    private static final String PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED = "public.progression.defence-organisation-for-laa-disassociated";

    private static final String PUBLIC_DEFENDANT_LAA_CONTRACT_ASSOCIATED = "public.progression.defendant-laa-contract-associated";

    @Mock
    private Sender sender;

    @InjectMocks
    private DefendantDefenceOrganisationAssociatedProcessor processor;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static UUID defendantId = randomUUID();

    private static UUID organisationId = randomUUID();

    private static UUID caseId = randomUUID();

    private static String organisationName = "org1";

    private static String reprensentativeType = RepresentationType.REPRESENTATION_ORDER.toString();

    private static String laaContractNumber = "LAA3456";


    @Test
    public void shouldRaiseAssociateDefendantDefenceOrganisation() {

        JsonObject courtDocumentPayload = buildPayloadForDefendantDefenceOrganisationAssociation();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-defence-organisation-associated"),
                courtDocumentPayload);

        processor.processAssociatedEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> publicEvent = envelopeCaptor.getAllValues();
        assertThat(publicEvent.get(0).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PUBLIC_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA));
        JsonObject commandCreateCourtDocumentPayload = publicEvent.get(0).payload();
        assertThat(commandCreateCourtDocumentPayload.getString("defendantId"), equalTo(defendantId.toString()));
        assertThat(commandCreateCourtDocumentPayload.getString("representationType"), equalTo(reprensentativeType));
        assertThat(commandCreateCourtDocumentPayload.getString("laaContractNumber"), equalTo(laaContractNumber));
        assertThat(commandCreateCourtDocumentPayload.getString("organisationId"), equalTo(organisationId.toString()));
        assertThat(commandCreateCourtDocumentPayload.getString("organisationName"), equalTo(organisationName.toString()));

    }

    @Test
    public void shouldRaisePublicEventForDefendantDefenceOrganisationDisassociated() {

        JsonObject disassociatedPayload = buildPayloadForDefendantDefenceOrganisationDisAssociated();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-defence-organisation-disassociated"),
                disassociatedPayload);

        processor.processDisassociatedEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> capturedEvents = envelopeCaptor.getAllValues();
        assertThat(capturedEvents.get(0).metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName(PUBLIC_DEFENCE_ORGANISATION_FOR_LAA_DISASSOCIATED));
        JsonObject capturedEventPayload = capturedEvents.get(0).payload();
        assertThat(capturedEventPayload.getString("defendantId"), equalTo(defendantId.toString()));
        assertThat(capturedEventPayload.getString("caseId"), equalTo(caseId.toString()));
        assertThat(capturedEventPayload.getString("organisationId"), equalTo(organisationId.toString()));

    }

    @Test
    public void shouldRaisePublicEventForDefendantLAAContract() {

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

    private static JsonObject buildPayloadForDefendantDefenceOrganisationDisAssociated() {

        final JsonObject defendantDefenceOrganisationDisassociated =
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("organisationId", organisationId.toString())
                        .add("caseId", caseId.toString())
                        .build();

        return defendantDefenceOrganisationDisassociated;
    }

    private static JsonObject buildPayloadForDefendantLAAContractAssociated() {

        final JsonObject defendantDefenceOrganisationDisassociated =
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("laaContractNumber",laaContractNumber)
                        .add("isAssociatedByLAA", false)
                        .build();

        return defendantDefenceOrganisationDisassociated;
    }

}
