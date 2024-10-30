package uk.gov.moj.cpp.progression.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static javax.json.Json.createObjectBuilder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
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

        final CourtDocument courtDocument = CourtDocument.courtDocument().build();
        when(jsonObjectConverter.convert(any(), any())).thenReturn(courtDocument);

        final String payloadAsString = Resources.toString(getResource("CpsRestNotification.json"), defaultCharset());
        cpsRestNotificationService.sendMaterial(payloadAsString, UUID.randomUUID(), envelope);

        verify(restEasyClientService).post(eq(COURT_DOCUMENT_REST_API_URL), any(), any());
        verify(jsonObjectConverter, times(1)).convert(any(), any());
    }
}
