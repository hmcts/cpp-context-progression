package uk.gov.moj.cpp.progression.it;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.NowsRequestHelper;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class StagingEnforcementIT extends AbstractIT {

    private final String PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";

    private NowsRequestHelper nowsRequestHelper;

    private MessageProducer producer;

    @Before
    public void onceBeforeEachTest(){
        producer = publicEvents.createProducer();
    }

    @Test
    public void shouldReceiveAcknowledgement() {
        final String requestId = UUID.randomUUID().toString();
        final String payload = this.prepareAddNowDocumentRequestPayload(requestId);
        nowsRequestHelper = new NowsRequestHelper();
        nowsRequestHelper.makeNowsRequest(requestId, payload);
        final String accountNumber = "AER123451";
        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("updated", "2019-12-01T10:00:00Z")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", accountNumber).build())
                .build();

        sendMessage(producer, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, stagingEnforcementAckPayload,
                metadataOf(randomUUID(), PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT)
                        .withUserId(USER_ID_VALUE_AS_ADMIN.toString()).build());

        nowsRequestHelper.verifyAccountNumberAddedToRequest(accountNumber, requestId);
    }

    @Test
    public void shouldReceiveErrorAcknowledgement() {
        final String requestId = UUID.randomUUID().toString();
        final String payload = this.prepareAddNowDocumentRequestPayload(requestId);
        nowsRequestHelper = new NowsRequestHelper();
        nowsRequestHelper.makeNowsRequest(requestId, payload);
        final String errorCode = "ERR12";
        final String errorMessage = "Post code is invalid";
        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_EXPORT_FAILED")
                .add("updated", "2019-12-01T10:00:00Z")
                .add("acknowledgement", createObjectBuilder().add("errorCode", errorCode).add("errorMessage", errorMessage).build())
                .build();

        sendMessage(producer, PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, stagingEnforcementAckPayload,
                metadataOf(randomUUID(), PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT)
                        .withUserId(USER_ID_VALUE_AS_ADMIN.toString()).build());

        nowsRequestHelper.verifyErrorEventRaised(errorCode, errorMessage);
    }

    private String prepareAddNowDocumentRequestPayload(final String requestId) {
        String body = getPayload("enforcement/progression.add-now-document-request.json");
        body = body.replaceAll("%HEARING_ID%", UUID.randomUUID().toString())
                .replaceAll("%MATERIAL_ID%", UUID.randomUUID().toString())
                .replaceAll("%REQUEST_ID%", requestId)
                .replaceAll("%DEFENDANT_ID%", UUID.randomUUID().toString());
        return body;
    }

    @After
    public void tearDown() throws JMSException {
        producer.close();
        closeSilently(nowsRequestHelper);
    }
}
