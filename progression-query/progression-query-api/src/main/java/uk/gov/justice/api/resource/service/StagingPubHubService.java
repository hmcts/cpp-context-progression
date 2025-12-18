package uk.gov.justice.api.resource.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StagingPubHubService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StagingPubHubService.class);

    private static final String PUBHUB_PUBLISH_STANDARD_LIST = "stagingpubhub.command.publish-standard-list";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public void publishStandardList(final JsonObject standardList, final UUID userId) {


        final JsonEnvelope stagingPubhub = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBHUB_PUBLISH_STANDARD_LIST)
                        .withUserId(userId.toString())
                        .build(),
                JsonObjects.createObjectBuilder()
                        .add("standardList", standardList)
                        .build());

        try {
            requester.request(stagingPubhub);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed call stagingpubhub.command.publish-standard-list", ex);
        }

    }
}
