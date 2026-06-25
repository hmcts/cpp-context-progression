package uk.gov.moj.cpp.progression.command.cotr;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;
import uk.gov.moj.cpp.progression.command.service.OrganisationService;

import java.time.LocalDate;
import java.util.Arrays;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CotrCommandApiTest {
    private static final String DEFENDANT_ID1 = "bd8d80d0-e995-40fb-9f59-340a53a1a688";
    private static final String DEFENDANT_ID2 = "c46ca4a8-39ae-440d-9016-e12e936313e3";
    private static final String COTR_ID = "cotrId";
    private static final String DEFENDANT_IDS = "defendantIds";
    private static final String PROGRESSION_SERVE_DEFENDANT_COTR = "progression.serve-defendant-cotr";
    private static final String PROGRESSION_CREATE_COTR = "progression.create-cotr";

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Spy
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private Sender sender;

    @Mock
    private UserDetailsLoader userDetailsLoader;

    @Mock
    private OrganisationService organisationService;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private CotrCommandApi cotrCommandApi;

    @Mock
    private Requester requester;

    @Test
    public void shouldPassThroughCreateCotrForDefendantsOfHearing() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID(PROGRESSION_CREATE_COTR), requestPayload);

        cotrCommandApi.createCotrForDefendantsOfHearing(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.create-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldPassThroughServeProsecutionCotr() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.serve-prosecution-cotr"), requestPayload);

        cotrCommandApi.serveProsecutionCotr(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.serve-prosecution-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldPassArchiveCotr() {
        final String cotrId = randomUUID().toString();
        final JsonObject requestPayload = createObjectBuilder().add("cotrId", cotrId).build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.archive-cotr"), requestPayload);

        cotrCommandApi.archiveCotr(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.archive-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldPassThroughDefendantCotr() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID(PROGRESSION_SERVE_DEFENDANT_COTR), requestPayload);

        cotrCommandApi.serveDefendantCotr(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.serve-defendant-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldPassThroughChangeDefendantsCotr() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.change-defendants-cotr"), requestPayload);

        when(userDetailsLoader.isDefenceClient(any(), any())).thenReturn(false);
        cotrCommandApi.changeDefendantsCotr(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.change-defendants-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldPassThroughAddFurtherInfoForProsecutionCotr() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.add-further-info-prosecution-cotr"), requestPayload);

        cotrCommandApi.addFurtherInfoForProsecutionCotr(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.add-further-info-prosecution-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldPassThroughAddFurtherInfoForDefenceCotr() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.add-further-info-defence-cotr"), requestPayload);

        cotrCommandApi.addFurtherInfoForDefenceCotr(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.add-further-info-defence-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldPassThroughUpdateReviewNotes() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.update-review-notes"), requestPayload);

        cotrCommandApi.updateReviewNotes(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.update-review-notes"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }


    @Test
    public void shouldRaiseExceptionIfUserNotAssociatedToDefendant() {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PROGRESSION_CREATE_COTR)
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadata, getCotrRequestPayload());
        when(userDetailsLoader.isDefenceClient(any(), any())).thenReturn(true);

        assertThrows(ForbiddenRequestException.class, () -> cotrCommandApi.createCotrForDefendantsOfHearing(commandJsonEnvelope));
    }

    @Test
    public void shouldCreateCotrIfDefenceClient() {
        final JsonObject requestPayload =  getCotrRequestPayload();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PROGRESSION_CREATE_COTR)
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadata, requestPayload);
        when(userDetailsLoader.isDefenceClient(any(), any())).thenReturn(true);
        when(organisationService.getAssociatedDefendants(any(), any())).thenReturn(Arrays.asList(fromString(DEFENDANT_ID1), fromString(DEFENDANT_ID2)));

        cotrCommandApi.createCotrForDefendantsOfHearing(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.create-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    @Test
    public void shouldRaiseExceptionWhileServeDefendantIfUserNotAssociatedToDefendant() {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PROGRESSION_SERVE_DEFENDANT_COTR)
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadata, getServeCotrRequestPayload());
        when(userDetailsLoader.isDefenceClient(any(), any())).thenReturn(true);


        assertThrows(ForbiddenRequestException.class, () -> cotrCommandApi.serveDefendantCotr(commandJsonEnvelope));
    }

    @Test
    public void shouldServeDefendantIfDefenceClient() {
        final JsonObject requestPayload = getServeCotrRequestPayload();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PROGRESSION_SERVE_DEFENDANT_COTR)
                .withUserId(randomUUID().toString())
                .build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadata, requestPayload);
        when(userDetailsLoader.isDefenceClient(any(), any())).thenReturn(true);
        when(organisationService.getAssociatedDefendants(any(), any())).thenReturn(Arrays.asList(fromString(DEFENDANT_ID1), fromString(DEFENDANT_ID2)));

        cotrCommandApi.serveDefendantCotr(commandJsonEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final DefaultEnvelope actualSentEnvelope = envelopeCaptor.getValue();
        assertThat(actualSentEnvelope.metadata().name(), is("progression.command.serve-defendant-cotr"));
        assertThat(actualSentEnvelope.payload(), is(requestPayload));
    }

    private JsonObject getCotrRequestPayload() {
        return createObjectBuilder()
                .add(COTR_ID, randomUUID().toString())
                .add("hearingId",randomUUID().toString())
                .add("caseId", randomUUID().toString())
                .add("caseUrn", "caseUrn")
                .add("hearingDate", LocalDate.now().toString())
                .add("jurisdictionType", JurisdictionType.CROWN.toString())
                .add("courtCenter", "Lavender hill mags")
                .add(DEFENDANT_IDS, createArrayBuilder()
                        .add(DEFENDANT_ID1)
                        .add(DEFENDANT_ID2)
                        .build())
                .build();
    }

    private JsonObject getServeCotrRequestPayload() {
        return createObjectBuilder()
                .add(COTR_ID, randomUUID().toString())
                .add("defendantId", DEFENDANT_ID1)
                .add("servedByName","name")
                .add("defendantFormData", "formData")
                .build();
    }

    private Envelope getAssociatedDefendantsEnvelope(final Metadata metadata, final String defendant1, final String defendant2){
        final JsonObject jsonObjectPayload = JsonObjects.createObjectBuilder()
                .add(DEFENDANT_IDS, JsonObjects.createArrayBuilder()
                        .add(defendant1)
                        .add(defendant2)
                ).build();
        return Envelope.envelopeFrom(metadata, jsonObjectPayload);
    }
}
