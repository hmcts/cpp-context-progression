package uk.gov.justice.api.resource;

import static com.google.common.io.Resources.getResource;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.api.resource.service.HearingQueryService;
import uk.gov.justice.api.resource.service.ListingQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.api.resource.utils.CourtExtractHelper;
import uk.gov.justice.api.resource.utils.CourtExtractTransformer;
import uk.gov.justice.api.resource.utils.RequestedNameMapper;
import uk.gov.justice.api.resource.utils.TransformationHelper;
import uk.gov.justice.api.resource.utils.payload.PleaValueDescriptionBuilder;
import uk.gov.justice.api.resource.utils.payload.ResultTextFlagBuilder;
import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetailsFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResourceTest {

    @Mock
    private RestProcessor restProcessor;

    @Mock
    @Named("DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResourceActionMapper")
    private ActionMapper actionMapper;

    @Mock
    private InterceptorChainProcessor interceptorChainProcessor;

    @Mock
    private FileInputDetailsFactory fileInputDetailsFactory;

    @Mock
    private ParameterCollectionBuilderFactory validParameterCollectionBuilderFactory;

    @Mock
    private TraceLogger traceLogger;

    @Mock
    private HttpTraceLoggerHelper httpTraceLoggerHelper;

    @Mock
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Mock
    private DocumentGeneratorClientProducer documentGeneratorClientProducer;

    @Mock
    private CourtExtractTransformer courtExtractTransformer;

    @Mock
    private ListingQueryService listingQueryService;

    @Mock
    private CourtExtractHelper courtExtractHelper;

    @Spy
    private PleaValueDescriptionBuilder pleaValueDescriptionBuilder;

    @Spy
    private ResultTextFlagBuilder resultTextFlagBuilder;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private HearingQueryService hearingQueryService;

    @InjectMocks
    private DefaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource defaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Test
    public void shouldGetCourtExtractByCaseIdContent() throws Exception {
        final String caseId = "8d68d068-4d29-4f2b-9cd7-b162529ee4f3";
        final String defendantId = "5f080fe7-7020-4c38-ac9a-88681ad05a5e";
        final String template = "CrownCourtExtract";
        final String hearingIds = "632b12d5-a404-4472-8de0-3a60b2e6f7ca";
        final UUID userId = fromString("3feb1195-4027-4a35-b686-279f32a3c361");

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final DocumentGeneratorClient documentGeneratorClient = mock(DocumentGeneratorClient.class);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream("resulted.json");
             final JsonReader jsonReader = Json.createReader(stream)) {
            final JsonObject payload = jsonReader.readObject();
            final String newPayload = Resources.toString(getResource("payload-with-plea-description.json"), Charset.defaultCharset());

            final JsonReader reader = Json.createReader(new StringReader(newPayload));
            JsonObject res = reader.readObject();

            when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
            when(documentGeneratorClient.generatePdfDocument(any(), anyString(), any())).thenReturn(newPayload.getBytes());
            when(courtExtractTransformer.getTransformedPayload(any(), anyString(), anyString(), any(), any())).thenReturn(res);
            when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));
            when(interceptorChainProcessor.process(any())).thenReturn(of(jsonEnvelope));

            defaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.getCourtExtractByCaseIdContent(caseId, defendantId, template, hearingIds, userId);

            verify(documentGeneratorClient).generatePdfDocument(jsonObjectArgumentCaptor.capture(), anyString(), any());
            assertThat(jsonObjectArgumentCaptor.getValue().toString(), is(newPayload));
        }
    }

    @Test
    public void shouldGetRecordSheetByCaseIdContent() throws Exception {
        final String caseId = "8d68d068-4d29-4f2b-9cd7-b162529ee4f3";
        final String defendantId = "5f080fe7-7020-4c38-ac9a-88681ad05a5e";
        final String template = "RecordSheet";
        final String hearingIds = "632b12d5-a404-4472-8de0-3a60b2e6f7ca";
        final UUID userId = fromString("3feb1195-4027-4a35-b686-279f32a3c361");

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final DocumentGeneratorClient documentGeneratorClient = mock(DocumentGeneratorClient.class);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try (final InputStream stream = loader.getResourceAsStream("resulted.json");
             final JsonReader jsonReader = Json.createReader(stream)) {
            final JsonObject payload = jsonReader.readObject();
            final String newPayload = Resources.toString(getResource("record-sheet-payload-with-plea-description.json"), Charset.defaultCharset());
            final JsonReader reader = Json.createReader(new StringReader(newPayload));
            JsonObject res = reader.readObject();
            when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
            when(documentGeneratorClient.generatePdfDocument(any(), anyString(), any())).thenReturn(newPayload.getBytes());
            when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));
            when(interceptorChainProcessor.process(any())).thenReturn(of(jsonEnvelope));
            when(courtExtractTransformer.getTransformedPayload(any(), anyString(), anyString(), any(), any())).thenReturn(res);

            defaultQueryApiProsecutioncasesCaseIdDefendantsDefendantIdExtractTemplateResource.getCourtExtractByCaseIdContent(caseId, defendantId, template, hearingIds, userId);

            verify(documentGeneratorClient).generatePdfDocument(jsonObjectArgumentCaptor.capture(), anyString(), any());
            assertThat(jsonObjectArgumentCaptor.getValue().toString(), is(newPayload));
        }
    }

    private Map<String, String> buildPleaStatusTypeDescriptions(){
        final Map<String, String> pleaStatusTypeDescriptions = new HashMap<>();
        pleaStatusTypeDescriptions.put("CHANGE_TO_GUILTY_AFTER_SWORN_IN", "Change of Plea: Not Guilty to Guilty (After Jury sworn in)");
        pleaStatusTypeDescriptions.put("CHANGE_TO_GUILTY_NO_SWORN_IN", "Change of Plea: Not Guilty to Guilty (No Jury sworn in)");
        return pleaStatusTypeDescriptions;
    }
}