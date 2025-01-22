package uk.gov.moj.cpp.progression.it;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.NowsRequestHelper;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class StagingEnforcementIT extends AbstractIT {

    private final String PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT = "public.stagingenforcement.enforce-financial-imposition-acknowledgement";

    private NowsRequestHelper nowsRequestHelper;

    private static final JmsMessageProducerClient producer = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @Test
    public void shouldNotReceiveAcknowledgementWhenRequestDoubled() {
        final String requestId = randomUUID().toString();
        final String accountNumber = "AER123451";
        makeNowsRequest(requestId);
        sendSuccessAcknowledgement(requestId, accountNumber);
        nowsRequestHelper.verifyAccountNumberAddedToRequest(accountNumber, requestId);
        sendSuccessAcknowledgement(requestId, accountNumber);
        nowsRequestHelper.verifyAccountNumberIgnoredToRequest(accountNumber, requestId);
    }

    @Test
    public void shouldReceiveErrorAcknowledgement() {
        final String requestId = randomUUID().toString();
        makeNowsRequest(requestId);
        sendErrorAcknowledgementAndVerify(requestId);
    }

    private void makeNowsRequest(final String requestId) {
        final String payload = this.prepareAddNowDocumentRequestPayload(requestId);
        nowsRequestHelper = new NowsRequestHelper();
        nowsRequestHelper.makeNowsRequest(requestId, payload);
    }

    private String prepareAddNowDocumentRequestPayload(final String requestId) {
        String body = getPayload("enforcement/progression.add-now-document-request.json");
        body = body.replaceAll("%HEARING_ID%", randomUUID().toString())
                .replaceAll("%MATERIAL_ID%", randomUUID().toString())
                .replaceAll("%REQUEST_ID%", requestId)
                .replaceAll("%DEFENDANT_ID%", randomUUID().toString());
        return body;
    }

    private void sendSuccessAcknowledgement(final String requestId, final String accountNumber) {
        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_ACKNOWLEDGED")
                .add("updated", "2019-12-01T10:00:00Z")
                .add("acknowledgement", createObjectBuilder().add("accountNumber", accountNumber).build())
                .build();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, USER_ID_VALUE_AS_ADMIN), stagingEnforcementAckPayload);
        producer.sendMessage(PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, publicEventEnvelope);

    }

    private void sendErrorAcknowledgementAndVerify(final String requestId) {
        final String errorCode = "ERR12";
        final String errorMessage = "Post code is invalid";
        final JsonObject stagingEnforcementAckPayload = createObjectBuilder().add("originator", "courts")
                .add("requestId", requestId)
                .add("exportStatus", "ENFORCEMENT_EXPORT_FAILED")
                .add("updated", "2019-12-01T10:00:00Z")
                .add("acknowledgement", createObjectBuilder().add("errorCode", errorCode).add("errorMessage", errorMessage).build())
                .build();

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, USER_ID_VALUE_AS_ADMIN), stagingEnforcementAckPayload);
        producer.sendMessage(PUBLIC_EVENT_STAGINGENFORCEMENT_ENFORCE_FINANCIAL_IMPOSITION_ACKNOWLEDGEMENT, publicEventEnvelope);

        nowsRequestHelper.verifyErrorEventRaised(errorCode, errorMessage);
    }
}
