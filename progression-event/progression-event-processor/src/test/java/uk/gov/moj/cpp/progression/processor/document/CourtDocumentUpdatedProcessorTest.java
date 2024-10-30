package uk.gov.moj.cpp.progression.processor.document;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentUpdatedProcessorTest {
    public static final String CASE_DOCUMENT_ID = "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27";
    protected static final String PUBLIC_COURT_DOCUMENT_UPDATED = "public.progression.events.court-document-updated";

    @InjectMocks
    private CourtDocumentUpdatedProcessor courtDocumentUpdatedProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Test
    public void shouldHandleCourtDocumentUpdatedEvent(){
        final JsonObject documentCategory =
                createObjectBuilder().add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", CASE_DOCUMENT_ID)
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        final JsonObject courtDocumentPayload =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", CASE_DOCUMENT_ID)
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", "0bb7b276-9dc0-4af2-83b9-f4acef0c7898")
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1"))
                                .add("containsFinancialMeans", true)
                               .add("seqNum",10)

                ).build();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-updated"),
                courtDocumentPayload);


        courtDocumentUpdatedProcessor.handleCourtDocumentUpdatedEvent(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata().name(), is(PUBLIC_COURT_DOCUMENT_UPDATED));
    }
}
