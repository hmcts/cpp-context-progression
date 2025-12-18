package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class PublicNowNotificationGeneratedIT extends AbstractIT {

    private static final String MATERIAL_ID = "materialId";
    private static final String HEARING_ID = "hearingId";
    private static final String STATUS = "status";
    private static final String STATUS_VALUE = "generated";
    final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @Test
    public void shouldUpdateCourtDocumentEntityWithMaterialGenerationStatus() throws IOException {
        final String materialId = randomUUID().toString();
        final String hearingId = randomUUID().toString();
        final String documentId = randomUUID().toString();

        addCourtDocument("progression.add-court-document-now-notification-generated.json", documentId, materialId);
        sendPublicEventForNowNotificationGenerated(materialId, hearingId);
        verifyCourtDocumentEntityUpdated(documentId, materialId);
    }

    private void sendPublicEventForNowNotificationGenerated(String materialId, String hearingId) {
        final String eventName = "public.hearingnows.now-notification-generated";
        final JsonObject payload = createObjectBuilder()
                .add(MATERIAL_ID, materialId)
                .add(HEARING_ID, hearingId)
                .add(STATUS, STATUS_VALUE)
                .build();

        messageProducerClientPublic.sendMessage(eventName, envelopeFrom(metadataOf(randomUUID(), eventName)
                .withUserId(randomUUID().toString())
                .build(), payload));
    }

    private void verifyCourtDocumentEntityUpdated(String documentId, String materialId) {
        getCourtDocumentFor(documentId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(documentId)),
                withJsonPath("$.courtDocument.materials[0].id", equalTo(materialId)),
                withJsonPath("$.courtDocument.materials[0].generationStatus", equalTo(STATUS_VALUE))
        ));
    }

    private void addCourtDocument(final String resourceAddCourtDocument, final String documentId, final String materialId) throws IOException {
        String body = prepareAddCourtDocumentPayload(resourceAddCourtDocument, documentId, materialId);

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + documentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        getCourtDocumentFor(documentId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(documentId)),
                withJsonPath("$.courtDocument.materials[0].id", equalTo(materialId))
        ));
    }

    private String prepareAddCourtDocumentPayload(final String resourceAddCourtDocument, final String documentId, final String materialId) throws IOException {
        String body = Resources.toString(Resources.getResource(resourceAddCourtDocument),
                Charset.defaultCharset());
        body = body
                .replaceAll("%DOCUMENT_ID%", documentId)
                .replaceAll("%MATERIAL_ID%", materialId);
        return body;
    }
}
