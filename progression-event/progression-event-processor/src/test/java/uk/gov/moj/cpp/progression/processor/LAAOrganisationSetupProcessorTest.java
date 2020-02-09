package uk.gov.moj.cpp.progression.processor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.HearingUpdated;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.DefendantsLAAService;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;

@RunWith(MockitoJUnitRunner.class)
public class LAAOrganisationSetupProcessorTest {

    private static final String LAA_CONTRACT_NUMBER = "laaContractNumber";
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String DEFENDANT_ID = "defendantId";

    private final String laaContractNumber = "LAA3456";
    private final String organisationId = UUID.randomUUID().toString();
    private final String organisationName = "Greg Associates Ltd.";
    private final String defendantId = UUID.randomUUID().toString();
    private static final String PROGRESSION_COMMAND_HANDLER_ASSOCIATE_ORPHANED_CASE = "progression.command.handler.associate-orphaned-case";


    @InjectMocks
    private LAAOrganisationSetupProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private DefendantsLAAService defendantsLAAService;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleSetUpOrganisation() {
        //Given
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("public.usersgroups.organisation-created").build(),
                createPayloadForOrganisationSetup());
        when(defendantsLAAService.getDefendantsByLAAContractNumber(jsonEnvelope, laaContractNumber)).thenReturn(Json.createArrayBuilder().add(defendantId).build());

        //When
        eventProcessor.setUpLAAOrganisation(jsonEnvelope);

        //Then
        verify(sender, VerificationModeFactory.times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(),
                withMetadataEnvelopedFrom(jsonEnvelope).withName(PROGRESSION_COMMAND_HANDLER_ASSOCIATE_ORPHANED_CASE));
        JsonObject commandAssociateOrphanedCasePayload = commands.get(0).payload();
        assertEquals(commandAssociateOrphanedCasePayload.getString(LAA_CONTRACT_NUMBER), laaContractNumber);
        assertEquals(commandAssociateOrphanedCasePayload.getString(ORGANISATION_ID), organisationId);
        assertEquals(commandAssociateOrphanedCasePayload.getString(ORGANISATION_NAME), organisationName);
        assertEquals(commandAssociateOrphanedCasePayload.getString(DEFENDANT_ID), defendantId);

    }

    @Test
    public void shouldHandleUpdateOrganisation() {
        //Given
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("public.usersgroups.organisation-updated").build(),
                createPayloadForOrganisationSetup());
        when(defendantsLAAService.getDefendantsByLAAContractNumber(jsonEnvelope, laaContractNumber)).thenReturn(Json.createArrayBuilder().add(defendantId).build());

        //When
        eventProcessor.updateOrganisationLAAContractNumber(jsonEnvelope);

        //Then
        verify(sender, VerificationModeFactory.times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata(),
                withMetadataEnvelopedFrom(jsonEnvelope).withName(PROGRESSION_COMMAND_HANDLER_ASSOCIATE_ORPHANED_CASE));
        JsonObject commandAssociateOrphanedCasePayload = commands.get(0).payload();
        assertEquals(commandAssociateOrphanedCasePayload.getString(LAA_CONTRACT_NUMBER), laaContractNumber);
        assertEquals(commandAssociateOrphanedCasePayload.getString(ORGANISATION_ID), organisationId);
        assertEquals(commandAssociateOrphanedCasePayload.getString(ORGANISATION_NAME), organisationName);
        assertEquals(commandAssociateOrphanedCasePayload.getString(DEFENDANT_ID), defendantId);

    }

    private JsonObject createPayloadForOrganisationSetup() {
        return Json.createObjectBuilder()
                .add("organisationDetails", Json.createObjectBuilder()
                        .add(ORGANISATION_ID, organisationId)
                        .add(ORGANISATION_NAME, organisationName)
                        .add(LAA_CONTRACT_NUMBER, laaContractNumber)
                        .add("timeTriggered", "2011-12-03T10:15:30+01:00")
                        .add("organisationType", "LEGAL_ORGANISATION")
                        .add("addressLine1", "Address Line1")
                        .add("addressLine4", "Address Line4")
                        .add("addressPostcode", "SE14 2AB")
                        .add("phoneNumber", "080012345678")
                        .add("email", "joe@example.com")
                        .build())
                .build();

    }
}
