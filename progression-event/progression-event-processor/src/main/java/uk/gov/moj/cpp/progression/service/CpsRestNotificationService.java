package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpsRestNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CpsRestNotificationService.class);

    @Inject
    @Value(key = "cpsPayloadTransformAndSendUrl", defaultValue = "http://localhost:8080/notification-cms/v1/transformAndSendCms")
    private String cpsPayloadTransformAndSendUrl;

    @Inject
    @Value(key = "progressionCourtDocument.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    @Inject
    private RestEasyClientService restEasyClientService;

    @Inject
    private MaterialService materialService;

    public void sendMaterial(final String payload) {
        final Response response = restEasyClientService.post(cpsPayloadTransformAndSendUrl, payload, subscriptionKey);
        LOGGER.info("API-M {} called with Request: {} and received status response: {}", cpsPayloadTransformAndSendUrl, payload, response.getStatus());
    }
}