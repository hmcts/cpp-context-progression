package uk.gov.moj.cpp.progression.processor.document;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CourtDocumentReviewRequiredProcessorTest {

    @Mock
    private Sender sender;

    @InjectMocks
    private CourtDocumentReviewRequiredProcessor courtDocumentReviewRequiredProcessor;

    @Mock
    private ProgressionService progressionService;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeArgumentCaptor;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldSendPublicEventDocumentReviewRequired() {

        final UUID materialId = randomUUID();
        final UUID documentId = randomUUID();
        final JsonEnvelope eventEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("progression.event.document-review-required"),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .add("receivedDateTime", "2020-01-20T13:50:00Z")
                        .add("documentId", documentId.toString())
                        .add("source", "OTHER")
                        .add("urn", "abcd123")
                        .add("prosecutingAuthority", "abc")
                        .add("documentType", "Applications")
                        .add("caseId", "caseId")
                        .add("code", createArrayBuilder().add("uploaded-review-required"))
        );

        final Optional<JsonObject> prosecutionCasePayload = Optional.of(createObjectBuilder()
                .add("prosecutionCase", createObjectBuilder()
                        .add("prosecutionCaseIdentifier", createObjectBuilder()
                                .add("prosecutionAuthorityReference", "URNCASE1124")
                                .add("prosecutionAuthorityCode", "PRAUTH").build())
                        .build())
                .build());

        when(progressionService.getProsecutionCaseDetailById(any(), anyString()))
                .thenReturn(prosecutionCasePayload);
        when(jsonObjectToObjectConverter.convert(prosecutionCasePayload.get().getJsonObject("prosecutionCase"), ProsecutionCase.class))
                .thenReturn(ProsecutionCase.prosecutionCase()
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withCaseURN("URNCASE1124")
                                .withProsecutionAuthorityCode("PRAUTH")
                                .build())
                        .build());
        courtDocumentReviewRequiredProcessor.processDocumentReviewRequired(eventEnvelope);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope<JsonValue> jsonValueEnvelope = envelopeArgumentCaptor.getValue();

        assertThat(jsonValueEnvelope.metadata(), withMetadataEnvelopedFrom(eventEnvelope).withName("public.progression.document-review-required"));
        assertThat(jsonValueEnvelope.payload(), payloadIsJson(allOf(
                withJsonPath("$.materialId", equalTo(materialId.toString())),
                withJsonPath("$.receivedDateTime", equalTo("2020-01-20T13:50:00Z")),
                withJsonPath("$.documentId", equalTo(documentId.toString())),
                withJsonPath("$.source", equalTo("OTHER")),
                withJsonPath("$.urn", equalTo("URNCASE1124")),
                withJsonPath("$.prosecutingAuthority", equalTo("PRAUTH")),
                withJsonPath("$.documentType", equalTo("Applications")),
                withJsonPath("$.code.[0]", equalTo("uploaded-review-required"))
        )));
    }
}