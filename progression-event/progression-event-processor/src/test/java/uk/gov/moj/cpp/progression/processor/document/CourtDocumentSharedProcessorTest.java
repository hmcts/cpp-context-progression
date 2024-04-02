package uk.gov.moj.cpp.progression.processor.document;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentSharedProcessor.PUBLIC_COURT_DOCUMENT_SHARED;
import static uk.gov.moj.cpp.progression.processor.document.CourtDocumentSharedProcessor.PUBLIC_COURT_DOCUMENT_SHARE_FAILED;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.List;

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
public class CourtDocumentSharedProcessorTest {
    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();
    @InjectMocks
    private CourtDocumentSharedProcessor eventProcessor;
    @Mock
    private Sender sender;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    private static JsonObject buildDocumentJsonObject() {

        final JsonObject courtDocument =
                createObjectBuilder().add("shareCourtDocumentDetails",
                        createObjectBuilder()
                                .add("courtDocumentId", "3aa6d35d-70c3-45fb-a05e-bfedbce16413")
                                .add("hearingId", "3aa6d35d-70c3-45fb-a05e-afedbce16413")
                                .add("userGroupId", "3aa6d35d-70c3-45fb-a05e-afedbce16414")
                )
                        .build();

        return courtDocument;
    }


    @Test
    public void shouldPublishPublicEventWhenCourtDocumentSharedEventReceived() {

        final JsonObject courtDocumentPayload = buildDocumentJsonObject();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-shared"),
                courtDocumentPayload);


        eventProcessor.handleCourtDocumentSharedEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata().name(), is(PUBLIC_COURT_DOCUMENT_SHARED));

    }

    @Test
    public void shouldPublishPublicEventWhenCourtDocumentSharedEventReceivedV2() {

        final JsonObject courtDocumentPayload = buildDocumentJsonObject();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-shared-v2"),
                courtDocumentPayload);


        eventProcessor.handleCourtDocumentSharedEventV2(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata().name(), is(PUBLIC_COURT_DOCUMENT_SHARED));

    }


    @Test
    public void shouldPublishPublicEventWhenCourtDocumentShareFailedEventReceived() {

        final JsonObject courtDocumentPayload = buildDocumentJsonObject();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-share-failed"),
                courtDocumentPayload);


        eventProcessor.handleCourtDocumentShareFailedEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata().name(), is(PUBLIC_COURT_DOCUMENT_SHARE_FAILED));

    }

    @Test
    public void shouldPublishPublicEventWhenDuplicateShareCourtDocumentRequestReceivedEventReceived() {

        final JsonObject courtDocumentPayload = buildDocumentJsonObject();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.duplicate-share-court-document-request-received"),
                courtDocumentPayload);


        eventProcessor.handleDuplicateShareCourtDocumentRequestReceivedEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata().name(), is(PUBLIC_COURT_DOCUMENT_SHARED));

    }

}
