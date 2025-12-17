package uk.gov.moj.cpp.progression.processor;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentCreatedProcessorTest {

    private static final String PROGRESSION_COMMAND_UPDATE_FINANCIAL_MEANS_DATA = "progression.command.update-financial-means-data";
    private static final String PROGRESSION_PUBLIC_COURT_DOCUMENT_CREATED = "public.progression.events.court-document-created";

    @Mock
    private Sender sender;


    @InjectMocks
    private CourtDocumentCreatedProcessor processor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;


    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;


    @Test
    public void shouldRaiseUpdateFinancialMeansDataCommand() {

        JsonObject courtDocumentPayload = buildDocumentCategoryJsonObject();
        final String courtDocumentId = courtDocumentPayload.getJsonObject("courtDocument").getJsonString("courtDocumentId").getString();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.court-document-created"),
                courtDocumentPayload);

        processor.processCourtDocumentCreated(requestMessage);
        verify(sender, times(2)).send(envelopeCaptor.capture());

        final List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        assertThat(commands.get(0).metadata().name(), is(PROGRESSION_PUBLIC_COURT_DOCUMENT_CREATED));
        assertThat(commands.get(1).metadata().name(), is(PROGRESSION_COMMAND_UPDATE_FINANCIAL_MEANS_DATA));
        assertThat(commands.get(0).payload().getJsonObject("courtDocument").getJsonString("courtDocumentId").getString(), is(courtDocumentId));
        JsonObject commandCreateCourtDocumentPayload = commands.get(1).payload();
        assertThat(commandCreateCourtDocumentPayload.getJsonObject("courtDocument").getBoolean("containsFinancialMeans"), is(true));

    }

    private static JsonObject buildDocumentCategoryJsonObject() {

        final JsonObject documentCategory =
                createObjectBuilder().add("defendantDocument",
                        createObjectBuilder()
                                .add("prosecutionCaseId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("defendants", createArrayBuilder().add("e1d32d9d-29ec-4934-a932-22a50f223966"))).build();

        final JsonObject courtDocument =
                createObjectBuilder().add("courtDocument",
                        createObjectBuilder()
                                .add("courtDocumentId", "2279b2c3-b0d3-4889-ae8e-1ecc20c39e27")
                                .add("documentCategory", documentCategory)
                                .add("name", "SJP Notice")
                                .add("documentTypeId", "0bb7b276-9dc0-4af2-83b9-f4acef0c7898")
                                .add("documentTypeDescription", "SJP Notice")
                                .add("mimeType", "pdf")
                                .add("materials", createObjectBuilder().add("id", "5e1cc18c-76dc-47dd-99c1-d6f87385edf1"))
                                .add("containsFinancialMeans", true)
                ).build();

        return courtDocument;
    }


}
