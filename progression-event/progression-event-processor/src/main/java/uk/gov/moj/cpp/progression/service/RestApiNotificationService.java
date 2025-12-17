package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestApiNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApiNotificationService.class);

    @Inject
    @Value(key = "apiNotificationUrl", defaultValue = "https://dummyUrl/CPS/v1/nowsapi")
    private String apiNotificationUrl;

    @Inject
    @Value(key = "progressionApiNotification.subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    @Inject
    private RestEasyClientService restEasyClientService;

    public void sendApiNotification(final String payload) {
        final Response response = restEasyClientService.post(apiNotificationUrl, payload, subscriptionKey);
        LOGGER.info("API-NOTIFICATION {} called with Request: {} and received status response: {}", apiNotificationUrl, payload, response.getStatus());
    }
}