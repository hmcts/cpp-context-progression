package uk.gov.moj.cpp.progression;

import static java.lang.String.join;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;

import uk.gov.justice.core.courts.nces.NcesNotificationRequested;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.test.TestTemplates;

import java.util.Arrays;
import java.util.List;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607"})
public class NCESNotificationIT extends AbstractIT {

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final String DOCUMENT_TEXT = STRING.next();
    public static final String PROGRESSION_QUERY_API_QUERY_API_REST_PROGRESSION = "/progression-query-api/query/api/rest/progression";
    public static final String APPLICATION_VND_PROGRESSION_QUERY_PROSECUTION_NOTIFICATION_STATUS_JSON = "application/vnd.progression.query.prosecution.notification-status+json";

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    private static final String PUBLIC_HEARING_EVENT_NCES_NOTIFICATION_REQUESTED = "public.hearing.event.nces-notification-requested";

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldSendNCESNotification() {

        NotificationServiceStub.setUp();

        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);

        final NcesNotificationRequested ncesNotificationRequested = TestTemplates.generateNcesNotificationRequested();
        IdMapperStub.stubForIdMapperSuccess(Response.Status.OK, ncesNotificationRequested.getCaseId());

        final JsonObject requestAsJson = objectToJsonObjectConverter.convert(ncesNotificationRequested);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_EVENT_NCES_NOTIFICATION_REQUESTED, randomUUID()), requestAsJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_EVENT_NCES_NOTIFICATION_REQUESTED, publicEventEnvelope);

        pollForResponse(join("", "/prosecutioncases/",
                        ncesNotificationRequested.getCaseId().toString(), "/notification-status"),
                APPLICATION_VND_PROGRESSION_QUERY_PROSECUTION_NOTIFICATION_STATUS_JSON);

        //DocumentGeneratorStub.verifyCreate(singletonList(ncesNotificationRequested.getDocumentContent().getUrn()));
        List<String> details = Arrays.asList("subject", ncesNotificationRequested.getDocumentContent().getAmendmentType(), ncesNotificationRequested.getMaterialId().toString());
        verifyEmailNotificationIsRaisedWithoutAttachment(details);
    }

}

