package uk.gov.moj.cpp.progression.service;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.entity.ContentType.TEXT_PLAIN;
import static org.apache.http.entity.mime.MultipartEntityBuilder.create;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.justice.services.common.configuration.Value;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

@SuppressWarnings({"squid:S2629", "squid:S2221", "squid:S00112"})
public class CpsApiService {
    private static final Logger LOGGER = getLogger(CpsApiService.class);

    private static final String NOTIFICATION_TXT = "Notification.txt";

    @Inject
    @Value(key = "bcmNotificationUrl", defaultValue = "http://localhost:8080/CPS/v1/notification/bcm-notification")
    private String bcmNotificationUrl;

    @Inject
    @Value(key = "subscription.key", defaultValue = "3674a16507104b749a76b29b6c837352")
    private String subscriptionKey;

    private final HttpClientWrapper httpClientWrapper = new HttpClientWrapper();

    public void sendNotification(final JsonObject payload) {
        LOGGER.info(String.format("Sending bcm notification to CPS with payload %s", bcmNotificationUrl));
        final HttpEntity data = create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addBinaryBody("Notification", new ByteArrayInputStream(payload.toString().getBytes()), TEXT_PLAIN, NOTIFICATION_TXT)
                .build();
        final HttpUriRequest request = RequestBuilder
                .post(bcmNotificationUrl)
                .addHeader("Ocp-Apim-Subscription-Key", subscriptionKey)
                .setEntity(data)
                .build();

        LOGGER.info(String.format("CPS JSON Payload : %s", payload));
        LOGGER.info(String.format("Executing request : %s", request.getRequestLine()));

        try (final CloseableHttpClient httpClient = httpClientWrapper.createHttpClient();
             final CloseableHttpResponse response = httpClient.execute(request)) {

            final int statusCode = response.getStatusLine().getStatusCode();
            final String messageBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            if (statusCode != SC_OK) {
                LOGGER.info("Call to CPS notification endpoint failed with http status code : " + statusCode +
                        ", : response body :  " + messageBody);
            } else {
                LOGGER.info(String.format("Call to CPS notification endpoint successful with http status code : %s and payload : %s", statusCode, messageBody));
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

    }
}