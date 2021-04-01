package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.time.ZonedDateTime.now;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.IdMapperStub.stubForDocumentId;
import static uk.gov.moj.cpp.progression.stub.IdMapperStub.stubForMaterialId;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.messaging.Metadata;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObjectBuilder;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class UpdateCourtDocumentIT extends AbstractIT {
    private static final String PUBLIC_NOTIFICATION_SENT = "public.notificationnotify.events.notification-sent";
    private static final String PROGRESSION_EVENT_PRINT_REQUESTED = "progression.event.print-requested";
    private static final String PROGRESSION_EVENT_PRINT_TIME_UPDATED = "progression.event.court-document-print-time-updated";

    private String caseId;
    private String defendantId;
    private ZonedDateTime completedAt;
    private ZonedDateTime sentTime;
    private UUID documentId;
    private UUID materialId;
    private UUID notificationId;

    private static final MessageProducer PUBLIC_MESSAGE_PRODUCER = publicEvents.createProducer();
    private static final MessageConsumer PRINT_REQUESTED_CONSUMER = privateEvents.createConsumer(PROGRESSION_EVENT_PRINT_REQUESTED);
    private static final MessageConsumer PRINT_TIME_UPDATED_CONSUMER = privateEvents.createConsumer(PROGRESSION_EVENT_PRINT_TIME_UPDATED);

    @AfterClass
    public static void tearDown() throws JMSException {
        PUBLIC_MESSAGE_PRODUCER.close();
        PRINT_REQUESTED_CONSUMER.close();
        PRINT_TIME_UPDATED_CONSUMER.close();
    }

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        documentId = randomUUID();
        materialId = randomUUID();
        notificationId = randomUUID();
        completedAt = now();
        sentTime = now().minusMinutes(5);
        stubForMaterialId(notificationId, materialId);
        stubForDocumentId(materialId, documentId);
    }

    @Test
    public void shouldUpdateCourtDocumentPrintDateTimeWhenDocumentHasBeenConfirmedAsPrinted() throws IOException {
        addCourtDocument();
        produceNotificationSentPublicEvent(notificationId, sentTime, completedAt);
        verifyPrintDateTimeUpdated(completedAt);
    }

    @Test
    public void shouldNotUpdateCourtDocumentPrintDateTimeWhenDocumentHasNotBeenConfirmedAsPrinted() throws IOException {
        addCourtDocument();
        produceNotificationSentPublicEvent(notificationId, sentTime, null);
        verifyPrintDateTimeUpdated(null);
    }

    private void verifyPrintDateTimeUpdated(final ZonedDateTime completedAt) {
        getCourtDocumentFor(documentId.toString(), allOf(
                withJsonPath("$.courtDocument.materials", hasSize(greaterThanOrEqualTo(1))),
                withJsonPath("$.courtDocument.materials[0].id", equalTo(materialId.toString())),
                nonNull(completedAt) ?
                        withJsonPath("$.courtDocument.materials[0].printedDateTime", equalTo(completedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")))) :
                        withoutJsonPath("$.courtDocument.materials[0].printedDateTime")
        ));
    }

    private void addCourtDocument() throws IOException {
        final String body = getPayload("progression.court-document-to-be-printed.json").replaceAll("%DOCUMENT_ID%", documentId.toString())
                .replaceAll("%CASE_ID%", caseId)
                .replaceAll("%MATERIAL_ID%", materialId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId);
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + documentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private void produceNotificationSentPublicEvent(final UUID notificationId, final ZonedDateTime sentTime, final ZonedDateTime completedAt) {
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withUserId(USER_ID)
                .withName(PUBLIC_NOTIFICATION_SENT)
                .build();

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("sentTime", sentTime.withFixedOffsetZone().toString());

        if (nonNull(completedAt)) {
            objectBuilder.add("completedAt", completedAt.withFixedOffsetZone().toString());
        }

        sendMessage(PUBLIC_MESSAGE_PRODUCER, PUBLIC_NOTIFICATION_SENT, objectBuilder.build(), metadata);
    }
}
