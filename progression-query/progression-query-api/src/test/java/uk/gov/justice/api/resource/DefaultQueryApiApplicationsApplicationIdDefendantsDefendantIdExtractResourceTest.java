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
import static uk.gov.justice.api.resource.utils.FileUtil.jsonFromPath;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.api.resource.service.DefenceQueryService;
import uk.gov.justice.api.resource.service.HearingQueryService;
import uk.gov.justice.api.resource.service.ListingQueryService;
import uk.gov.justice.api.resource.service.ReferenceDataService;
import uk.gov.justice.api.resource.utils.CourtExtractHelper;
import uk.gov.justice.api.resource.utils.CourtExtractTransformer;
import uk.gov.justice.api.resource.utils.TransformationHelper;
import uk.gov.justice.api.resource.utils.payload.PleaValueDescriptionBuilder;
import uk.gov.justice.api.resource.utils.payload.ResultTextFlagBuilder;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.services.adapter.rest.mapping.ActionMapper;
import uk.gov.justice.services.adapter.rest.multipart.FileInputDetailsFactory;
import uk.gov.justice.services.adapter.rest.parameter.ParameterCollectionBuilderFactory;
import uk.gov.justice.services.adapter.rest.processor.RestProcessor;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.logging.HttpTraceLoggerHelper;
import uk.gov.justice.services.messaging.logging.TraceLogger;
import uk.gov.moj.cpp.progression.query.view.service.HearingService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClient;
import uk.gov.moj.cpp.system.documentgenerator.client.DocumentGeneratorClientProducer;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Named;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiApplicationsApplicationIdDefendantsDefendantIdExtractResourceTest {

    @Mock
    private RestProcessor restProcessor;

    @Mock
    @Named("DefaultQueryApiLinkedApplicationsExtractResourceActionMapper")
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

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private CourtExtractTransformer courtExtractTransformer;

    @Mock
    private TransformationHelper transformationHelper;

    @Mock
    private CourtExtractHelper courtExtractHelper;

    @Mock
    private ListingQueryService listingQueryService;

    @Spy
    private PleaValueDescriptionBuilder pleaValueDescriptionBuilder;

    @Spy
    private ResultTextFlagBuilder resultTextFlagBuilder;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private HearingQueryService hearingQueryService;

    @InjectMocks
    private DefaultQueryApiApplicationsApplicationIdDefendantsDefendantIdExtractResource defaultQueryApiLinkedApplicationsExtractResource;

    @Captor
    private ArgumentCaptor<JsonObject> jsonObjectArgumentCaptor;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private HearingService hearingService;

    @Mock
    private DefenceQueryService defenceQueryService;

    @Test
    public void shouldGetLinkedApplicationExtractByApplicationIdAndDefendantId() throws Exception {
        final String applicationId = "0b73afac-51f4-4080-a916-c4d9d18c340c";
        final String defendantId = "a60cb92d-782d-43f1-ab65-13e1b311d4b3";
        final String hearingIds = "38461819-8588-4012-ac95-e716f62a0725";
        final UUID userId = fromString("3feb1195-4027-4a35-b686-279f32a3c361");

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.courtExtractTransformer, "transformationHelper", transformationHelper);
        setField(this.courtExtractTransformer, "courtExtractHelper", courtExtractHelper);
        setField(this.courtExtractTransformer, "listingQueryService", listingQueryService);
        setField(this.courtExtractTransformer, "referenceDataService", referenceDataService);
        setField(this.courtExtractTransformer, "hearingQueryService", hearingQueryService);
        setField(this.courtExtractTransformer, "hearingRepository", hearingRepository);
        setField(this.courtExtractTransformer, "hearingService", hearingService);
        setField(this.courtExtractTransformer, "defenceQueryService", defenceQueryService);
        setField(this.courtExtractTransformer, "stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField(this.courtExtractTransformer, "jsonObjectToObjectConverter", new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper()));
        setField(this.pleaValueDescriptionBuilder, "referenceDataService", referenceDataService);
        setField(this.defaultQueryApiLinkedApplicationsExtractResource, "pleaValueDescriptionBuilder", pleaValueDescriptionBuilder);

        final JsonEnvelope jsonEnvelope = mock(JsonEnvelope.class);
        final JsonEnvelope prosecutionCaseQueryJsonEnvelope = mock(JsonEnvelope.class);
        final DocumentGeneratorClient documentGeneratorClient = mock(DocumentGeneratorClient.class);
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();

        final JsonObject applicationPayload = jsonFromPath("linked-application-extract/progression.query.application.json");
        final JsonObject prosecutionCasePayload = jsonFromPath("linked-application-extract/progression.query.prosecution-case.json");

        final String newPayload = Resources.toString(getResource("linked-application-extract/linked-application-extract-payload.json"), Charset.defaultCharset());

        when(interceptorChainProcessor.process(any())).thenReturn(of(jsonEnvelope), of(prosecutionCaseQueryJsonEnvelope));
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(applicationPayload);
        when(prosecutionCaseQueryJsonEnvelope.payloadAsJsonObject()).thenReturn(prosecutionCasePayload);

        when(documentGeneratorClientProducer.documentGeneratorClient()).thenReturn(documentGeneratorClient);
        when(documentGeneratorClient.generatePdfDocument(any(), anyString(), any())).thenReturn(newPayload.getBytes());
        when(serviceContextSystemUserProvider.getContextSystemUserId()).thenReturn(of(UUID.randomUUID()));

        when(referenceDataService.retrievePleaTypeDescriptions()).thenReturn(buildPleaStatusTypeDescriptions());

        defaultQueryApiLinkedApplicationsExtractResource.getApplicationsByApplicationIdDefendantsByDefendantIdExtract(applicationId, defendantId, hearingIds, userId);

        verify(documentGeneratorClient).generatePdfDocument(jsonObjectArgumentCaptor.capture(), anyString(), any());
        assertThat(jsonObjectArgumentCaptor.getValue().toString(), is(newPayload));
    }

    private Map<String, String> buildPleaStatusTypeDescriptions() {
        final Map<String, String> pleaStatusTypeDescriptions = new HashMap<>();
        pleaStatusTypeDescriptions.put("CHANGE_TO_GUILTY_AFTER_SWORN_IN", "Change of Plea: Not Guilty to Guilty (After Jury sworn in)");
        pleaStatusTypeDescriptions.put("CHANGE_TO_GUILTY_NO_SWORN_IN", "Change of Plea: Not Guilty to Guilty (No Jury sworn in)");
        return pleaStatusTypeDescriptions;
    }

}