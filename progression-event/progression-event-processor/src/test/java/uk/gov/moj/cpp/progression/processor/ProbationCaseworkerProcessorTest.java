package uk.gov.moj.cpp.progression.processor;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;
import uk.gov.moj.cpp.progression.service.RestEasyClientService;

import java.io.IOException;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProbationCaseworkerProcessorTest {

    private static final String HEARING_DETAILS_URL = "https://dummyUrl/probation/api/v1/hearing/details";
    private static final String HEARING_DELETED_URL = "https://dummyUrl/probation/api/v1/hearing/deleted";
    @Mock
    private RestEasyClientService restEasyClientService;

    @Mock
    private Response response;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @InjectMocks
    private ProbationCaseworkerProcessor probationCaseworkerProcessor;

    @BeforeEach
    public void setUp(){
        setField(probationCaseworkerProcessor, "probationHearingDetailsUrl", HEARING_DETAILS_URL);
        setField(probationCaseworkerProcessor, "probationHearingDeleteUrl", HEARING_DELETED_URL);
    }

    @Test
    public void shouldProcessHearingPopulatedToProbationCaseworker() throws IOException {

        when(restEasyClientService.post(eq(HEARING_DETAILS_URL), any(), any())).thenReturn(response);

        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.json"), defaultCharset()));
        final JsonObject payload = JsonObjects.createObjectBuilder().add("hearing", hearing).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.events.hearing-populated-to-probation-caseworker"),
                payload);

        probationCaseworkerProcessor.processHearingPopulatedToProbationCaseworker(jsonEnvelope);

        final String expectedHearing = Resources.toString(getResource("expected.hearing.json"), defaultCharset());

        verify(restEasyClientService).post(eq(HEARING_DETAILS_URL),
                eq(expectedHearing),
                any());
    }

    @Test
    public void shouldProcessDeletedHearingPopulatedToProbationCaseworker() throws IOException {

        when(restEasyClientService.post(eq(HEARING_DELETED_URL), any(), any())).thenReturn(response);

        final JsonObject hearing = stringToJsonObjectConverter.convert(Resources.toString(getResource("hearing.json"), defaultCharset()));
        final JsonObject payload = JsonObjects.createObjectBuilder().add("hearing", hearing).build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("progression.events.hearing-populated-to-probation-caseworker"),
                payload);

        probationCaseworkerProcessor.processDeletedHearingPopulatedToProbationCaseworker(jsonEnvelope);

        final String expectedHearing = Resources.toString(getResource("expected.hearing.json"), defaultCharset());

        verify(restEasyClientService).post(eq(HEARING_DELETED_URL),
                eq(expectedHearing),
                any());
    }
}
