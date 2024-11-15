package uk.gov.moj.cpp.progression.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import org.mockito.ArgumentCaptor;
import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.messaging.Metadata;

@ExtendWith(MockitoExtension.class)
public class CpsRestNotificationServiceTest {

    private static final String COURT_DOCUMENT_REST_API_URL = "https://spnl-apim-int-gw.cpp.nonlive/probation/api/v1/hearing/details";

    @Mock
    private RestEasyClientService restEasyClientService;

    @Mock
    private Sender sender;

    @Mock
    private Response response;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @InjectMocks
    private CpsRestNotificationService cpsRestNotificationService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setUp() {
        setField(cpsRestNotificationService, "cpsPayloadTransformAndSendUrl", COURT_DOCUMENT_REST_API_URL);
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());

    }

    @Test
    public void shouldProcessHearingPopulatedToProbationCaseworker() throws IOException {

        when(restEasyClientService.post(eq(COURT_DOCUMENT_REST_API_URL), any(), any())).thenReturn(response);
        when(response.getStatus()).thenReturn(200);

        final String payloadAsString = Resources.toString(getResource("CpsRestNotification.json"), defaultCharset());
        cpsRestNotificationService.sendMaterial(payloadAsString, UUID.randomUUID(), envelope);

        verify(restEasyClientService).post(eq(COURT_DOCUMENT_REST_API_URL), any(), any());
    }

    @Test
    public void shouldProcessMaterialWithCourtDocument() throws IOException {

        final UUID courtDocumentId = UUID.randomUUID();

        when(restEasyClientService.post(eq(COURT_DOCUMENT_REST_API_URL), any(), any())).thenReturn(response);
        when(response.getStatus()).thenReturn(200);

        final CourtDocument courtDocument = CourtDocument.courtDocument().withCourtDocumentId(courtDocumentId).build();
        when(jsonObjectConverter.convert(any(), any())).thenReturn(courtDocument);

        final JsonEnvelope materialEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().add("courtDocument", objectToJsonObjectConverter.convert(courtDocument)).build());

        final String payloadAsString = Resources.toString(getResource("CpsRestNotification.json"), defaultCharset());
        cpsRestNotificationService.sendMaterialWithCourtDocument(payloadAsString, courtDocumentId, materialEnvelope);

        verify(restEasyClientService).post(eq(COURT_DOCUMENT_REST_API_URL), any(), any());
        verify(jsonObjectConverter, times(1)).convert(any(), any());
        final ArgumentCaptor<Envelope> envelopeCaptor = forClass(Envelope.class);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope envelope = envelopeCaptor.getValue();
        final Metadata metadata = envelope.metadata();
        final JsonObject payload = (JsonObject)envelope.payload();
        assertThat(metadata.name(), is("progression.command.update-send-to-cps-flag"));
        assertThat(payload.getString("courtDocumentId"), is(courtDocumentId.toString()));
        assertThat(payload.getBoolean("sendToCps"), is(true));
        assertThat(payload.getJsonObject("courtDocument").getString("courtDocumentId"), is(courtDocumentId.toString()));
    }
}
