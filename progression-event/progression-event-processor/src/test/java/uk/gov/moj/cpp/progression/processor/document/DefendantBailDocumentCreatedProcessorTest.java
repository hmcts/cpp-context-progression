package uk.gov.moj.cpp.progression.processor.document;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.DefendantBailDocumentCreated;
import uk.gov.moj.cpp.progression.service.MaterialService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantBailDocumentCreatedProcessorTest {

    @InjectMocks
    private DefendantBailDocumentCreatedProcessor defendantBailDocumentCreatedProcessor;

    @Mock
    private RefDataService referenceDataService;

    @Spy
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private MaterialService materialService;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Mock
    private Sender sender;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @Test
    public void shouldHandleDefendantBailDocumentCreatedEvent(){
        final UUID materialId = UUID.randomUUID();
        final DefendantBailDocumentCreated defendantBailDocumentCreated = new DefendantBailDocumentCreated(randomUUID(), randomUUID(), materialId,randomUUID());
        final ObjectToJsonObjectConverter objectToJsonConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());
        final JsonObject docCreatedPayload = objectToJsonConverter.convert(defendantBailDocumentCreated);

        final JsonEnvelope requestMessage = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.event.defendant-bail-document-created"),
                docCreatedPayload);

        final String inputEvent = "{\n  \"documentsMetadata\": [\n    {\n      \"id\": \"460f6f7a-c002-11e8-a355-529269fb1459\",\n      \"documentCategory\": \"Defendant level\",\n      \"documentType\": \"Bail and Custody\",\n      \"documentAccess\": [\n        \"Legal advisors\",\n        \"Court Admin\",\n        \"Crown court clerk\",\n        \"Listing officer\"\n      ],\n      \"canReadUserGroups\": [\n        \"Listing Officers\",\n        \"Legal advisors\",\n        \"Magistrates\"\n      ],\n      \"canCreateUserGroups\": [\n        \"Listing Officers\",\n        \"Legal advisors\"\n      ],\n      \"canDownloadUserGroups\": [\n        \"Listing Officers\",\n        \"Legal advisors\",\n        \"Magistrates\"\n      ]\n    },\n    {\n      \"id\": \"460f6f7a-c002-11e8-a355-529269fb1459\",\n      \"documentCategory\": \"Defendant level\",\n      \"documentType\": \"Magistrate's Sending sheet\",\n      \"documentAccess\": [\n        \"Legal advisors\",\n        \"Court Admin\",\n        \"Crown court clerk\",\n        \"Listing officer\"\n      ],\n      \"canReadUserGroups\": [\n        \"Listing Officers\",\n        \"Legal advisors\",\n        \"Magistrates\"\n      ],\n      \"canCreateUserGroups\": [\n        \"Listing Officers\",\n        \"Legal advisors\"\n      ],\n      \"canDownloadUserGroups\": [\n        \"Listing Officers\",\n        \"Legal advisors\",\n        \"Magistrates\"\n      ]\n    }\n  ]\n}";
        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);

        when(jsonObjectConverter.convert(requestMessage.payloadAsJsonObject(), DefendantBailDocumentCreated.class)).thenReturn(defendantBailDocumentCreated);

        final JsonObject payload = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("fileName", "abc.txt")
                .add("mimeType", "text")
                .add("materialAddedDate", String.valueOf(ZonedDateTime.now()))
                .build();
        when(materialService.getMaterialMetadata(requestMessage,defendantBailDocumentCreated.getMaterialId())).thenReturn(Optional.ofNullable(payload));
        when(objectToJsonObjectConverter.convert(any())).thenReturn(createObjectBuilder().build());
        when(referenceDataService.getAllDocumentsTypes(Mockito.eq(requestMessage), any(), any())).thenReturn(Optional.ofNullable(readData));
        when(enveloper.withMetadataFrom(any(), any())).thenReturn(enveloperFunction);

        defendantBailDocumentCreatedProcessor.handleDefendantBailDocumentCreatedEvent(requestMessage);
        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
    }
}
