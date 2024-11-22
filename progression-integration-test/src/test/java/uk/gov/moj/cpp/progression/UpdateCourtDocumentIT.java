package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.USER_ID;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.IdMapperStub.stubForDocumentId;
import static uk.gov.moj.cpp.progression.stub.IdMapperStub.stubForMaterialId;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObjectBuilder;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateCourtDocumentIT extends AbstractIT {
    private static final String PUBLIC_NOTIFICATION_SENT = "public.notificationnotify.events.notification-sent";

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String caseId;
    private String defendantId;
    private ZonedDateTime completedAt;
    private ZonedDateTime sentTime;
    private UUID documentId;
    private UUID materialId;
    private UUID notificationId;

    private UtcClock utcClock = new UtcClock();

    @BeforeEach
    public void setUp() throws IOException, JSONException {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        documentId = randomUUID();
        materialId = randomUUID();
        notificationId = randomUUID();
        completedAt = utcClock.now();
        sentTime = utcClock.now().minusMinutes(5);
        stubForMaterialId(notificationId, materialId);
        stubForDocumentId(materialId, documentId);
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));

    }

    @Test
    public void shouldUpdateCourtDocumentPrintDateTimeWhenDocumentHasBeenConfirmedAsPrinted() throws IOException {
        addCourtDocumentWithPrintedDate(completedAt);
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

    private void addCourtDocumentWithPrintedDate(final ZonedDateTime completedAt) throws IOException {
        final String body = getPayload("progression.court-document-to-be-printed-with-printed-date.json").replaceAll("%DOCUMENT_ID%", documentId.toString())
                .replaceAll("%CASE_ID%", caseId)
                .replaceAll("%MATERIAL_ID%", materialId.toString())
                .replaceAll("%DEFENDANT_ID%", defendantId)
                .replaceAll("%PRINT_DATE%", completedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")));
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + documentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
    }

    private void produceNotificationSentPublicEvent(final UUID notificationId, final ZonedDateTime sentTime, final ZonedDateTime completedAt) {

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("notificationId", notificationId.toString())
                .add("sentTime", sentTime.withFixedOffsetZone().toString());

        if (nonNull(completedAt)) {
            objectBuilder.add("completedAt", completedAt.withFixedOffsetZone().toString());
        }

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_NOTIFICATION_SENT, USER_ID), objectBuilder.build());
        messageProducerClientPublic.sendMessage(PUBLIC_NOTIFICATION_SENT, publicEventEnvelope);
    }
}
