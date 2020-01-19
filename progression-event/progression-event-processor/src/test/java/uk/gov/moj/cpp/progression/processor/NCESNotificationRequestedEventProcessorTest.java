package uk.gov.moj.cpp.progression.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.material.url.MaterialUrlGenerator;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.DocumentGeneratorService;
import uk.gov.moj.cpp.progression.service.NotificationService;

import javax.json.JsonObject;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNcesNotificationRequested;
import static uk.gov.moj.cpp.progression.test.TestTemplates.generateNowDocumentRequestTemplate;

public class NCESNotificationRequestedEventProcessorTest {

    public static final String USER_ID = UUID.randomUUID().toString();

    private NCESNotificationRequestedEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Mock
    private NotificationService notificationService;

    @Mock
    private DocumentGeneratorService documentGeneratorService;

    @Mock
    private MaterialUrlGenerator materialUrlGenerator;

    @Mock
    private ApplicationParameters applicationParameters;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();

    @Spy
    @InjectMocks
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        eventProcessor = new NCESNotificationRequestedEventProcessor(
                this.sender,
                this.documentGeneratorService,
                this.jsonObjectToObjectConverter,this.materialUrlGenerator, this.notificationService , this.applicationParameters
        );
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldGenerateNowAndStoreInFileStore()  {

        final NcesNotificationRequested nowDocumentRequest = generateNcesNotificationRequested();

        final JsonEnvelope eventEnvelope = envelope(nowDocumentRequest);

        when(applicationParameters.getNcesEmailTemplateId()).thenReturn(UUID.randomUUID().toString());
        this.eventProcessor.processPublicNCESNotificationRequested(eventEnvelope);
        verify(documentGeneratorService).generateNcesDocument(any(),any(),any(),any());
        verify(notificationService).sendEmail(any(),any(),any(),any(),any(),any(), any());
    }


    public JsonEnvelope envelope(final NcesNotificationRequested nowDocumentRequest) {
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(nowDocumentRequest);
        return envelopeFrom(
                metadataWithRandomUUID("progression.event.nows-requested").withUserId(USER_ID),
                objectToJsonObjectConverter.convert(jsonObject)
        );
    }


}