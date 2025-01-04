package uk.gov.moj.cpp.progression.processor;

import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.Country;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.NotificationNotifyService;
import uk.gov.moj.cpp.progression.service.NotificationService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("WeakerAccess")
@ExtendWith(MockitoExtension.class)
public class NotificationRequestProcessorTest {

    private static final String ONLINE_GUILTY_TEMPLATE_TYPE = "onlineGuiltyPleaCourtHearing";
    private static final String ONLINE_GUILTY_TEMPLATE_ID = "9a9d3078-9b52-4081-a183-22b3540e0edb";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private NotificationService notificationService;

    @Mock
    private Sender sender;

    @Mock
    private NotificationNotifyService notificationNotifyService;

    @Mock
    private ApplicationParameters applicationParameters;

    @Mock
    private RefDataService referenceDataService;

    @Mock
    private Requester requester;

    @InjectMocks
    private NotificationRequestProcessor notificationRequestProcessor;

    @Spy
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    @Captor
    private ArgumentCaptor<JsonObject> notificationJson;

    private UUID caseId;
    private UUID materialId;

    @BeforeEach
    public void setup() {
        caseId = randomUUID();
        materialId = randomUUID();
    }


    @Test
    public void shouldPrintDocument() throws Exception {

        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();
        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.print-requested")
                        .withClientCorrelationId(clientId),
                createObjectBuilder()
                        .add("materialId", materialId.toString())
                        .add("notificationId", notificationId.toString())
                        .add("caseId", caseId.toString())
                        .add("postage", false)
                        .build());

        notificationRequestProcessor.printDocument(event);

        verify(notificationNotifyService).sendLetterNotification(event, notificationId, materialId, false);
        verify(notificationService).recordPrintRequestAccepted(event);
    }

    @Test
    public void shouldEmailDocument() {

        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();
        final JsonObject notification = createObjectBuilder()
                .add("materialId", materialId.toString())
                .add("notificationId", notificationId.toString())
                .add("caseId", caseId.toString())
                .add("postage", false)
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("notifications", createArrayBuilder()
                        .add(notification)
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.email-requested")
                        .withClientCorrelationId(clientId), payload
        );

        notificationRequestProcessor.emailDocument(event);

        verify(notificationNotifyService).sendEmailNotification(event, notification);
        verify(notificationService).recordEmailRequestAccepted(event);
    }

    @Test
    public void shouldEmailDocumentForOnlinePlea() {

        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();
        final String templateId = randomUUID().toString();
        final String urn = "AB1243";
        final String email = "email@hmcts.net";

        final JsonObject payload = Json.createObjectBuilder()
                .add("systemDocGeneratorId", materialId.toString())
                .add("notificationId", notificationId.toString())
                .add("caseId", caseId.toString())
                .add("email", email)
                .add("urn", urn)
                .add("pleaNotificationType", "CompanyOnlinePlea")
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.notification-sent-for-plea-document")
                        .withClientCorrelationId(clientId), payload
        );

        when(applicationParameters.getOnlinePleaProsecutorTemplateId()).thenReturn(templateId);
        notificationRequestProcessor.handleNotificationSentForPleaDocument(event);
        verify(notificationNotifyService).sendEmailNotification(eq(event), notificationJson.capture());
        assertThat(notificationJson.getValue().getString("notificationId"), is(notNullValue()));
        assertThat(notificationJson.getValue().getString("templateId"), is(templateId));
        assertThat(notificationJson.getValue().getString("sendToAddress"), is(email));
        assertThat(notificationJson.getValue().getString("fileId"), is(materialId.toString()));
    }

    @Test
    public void shouldNotifyDefendantAboutPleaSubmission() {

        final String clientId = randomUUID().toString();
        final UUID notificationId = randomUUID();
        final String urn = "AB1243";
        final String email = "email@hmcts.net";
        final String postcode = "CR0 5QT";

        final JsonObject payload = Json.createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("email", email)
                .add("urn", urn)
                .add("postcode", postcode)
                .add("templateType", ONLINE_GUILTY_TEMPLATE_TYPE)
                .add("templateId", ONLINE_GUILTY_TEMPLATE_ID)
                .add("notificationId", notificationId.toString())
                .build();

        final JsonEnvelope event = envelopeFrom(
                metadataWithRandomUUID("progression.event.notification-sent-for-defendant-document")
                        .withClientCorrelationId(clientId), payload
        );
        when(applicationParameters.getOnlineGuiltyPleaCourtHearingEnglishTemplateId()).thenReturn(ONLINE_GUILTY_TEMPLATE_ID);
        when(referenceDataService.getCountryByPostcode(event, postcode, requester)).thenReturn(Country.ENGLAND.toString());
        notificationRequestProcessor.notifyDefendantAboutPleaSubmission(event);
        verify(notificationNotifyService).sendEmailNotification(event.metadata(), urn, email, notificationId, ONLINE_GUILTY_TEMPLATE_ID, empty());
    }

}
