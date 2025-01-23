package uk.gov.moj.cpp.progression.processor;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.RestEasyClientService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.progression.transformer.HearingHelper;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class ProbationCaseworkerProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProbationCaseworkerProcessor.class.getName());

    @Inject
    @Value(key = "probationHearingDetailsUrl", defaultValue = "http://localhost:8080/probation/api/v1/hearing/details")
    private String probationHearingDetailsUrl;

    @Inject
    @Value(key = "probationHearingDeleteUrl", defaultValue = "http://localhost:8080/probation/api/v1/hearing/deleted")
    private String probationHearingDeleteUrl;

    @Inject
    @Value(key = "probationCaseWorker.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    @Inject
    private RestEasyClientService restEasyClientService;

    @Handles("progression.events.hearing-populated-to-probation-caseworker")
    public void processHearingPopulatedToProbationCaseworker(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        LOGGER.info("progression.events.hearing-populated-to-probation-caseworker event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), payload);

        final JsonObject externalPayload = HearingHelper.transformedHearing(payload);

        final Response response = restEasyClientService.post(probationHearingDetailsUrl, externalPayload.toString(), subscriptionKey);
        LOGGER.info("Azure Function {} invoked with Request: {} Received response: {}",
                probationHearingDetailsUrl, externalPayload, response.getStatus());

    }

    @Handles("progression.events.deleted-hearing-populated-to-probation-caseworker")
    public void processDeletedHearingPopulatedToProbationCaseworker(final JsonEnvelope jsonEnvelope) {
        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        LOGGER.info("progression.events.deleted-hearing-populated-to-probation-caseworker event received with metadata {} and payload {}",
                jsonEnvelope.metadata(), payload);

        final JsonObject externalPayload = HearingHelper.transformedHearing(payload);

        final Response response = restEasyClientService.post(probationHearingDeleteUrl, externalPayload.toString(), subscriptionKey);
        LOGGER.info("Azure Function {} invoked with Request: {} Received response: {}",
                probationHearingDeleteUrl, externalPayload, response.getStatus());

    }
}
