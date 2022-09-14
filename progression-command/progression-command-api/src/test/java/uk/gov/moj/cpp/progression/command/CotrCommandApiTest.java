package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.command.api.UserDetailsLoader;
import uk.gov.moj.cpp.progression.command.cotr.CotrCommandApi;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CotrCommandApiTest {

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Mock
    private UserDetailsLoader userDetailsLoader;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private CotrCommandApi cotrCommandApi;

    @Test
    public void shouldPassThroughCreateCotrForDefendantsOfHearing() {
        final JsonObject requestPayload = createObjectBuilder().build();

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.create-cotr"), requestPayload);

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

        final JsonEnvelope commandJsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.serve-defendant-cotr"), requestPayload);

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

}
