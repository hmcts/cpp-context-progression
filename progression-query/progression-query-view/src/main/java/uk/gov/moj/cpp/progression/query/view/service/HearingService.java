package uk.gov.moj.cpp.progression.query.view.service;


import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingService.class);

    private static final String HEARING_APPLICATION_TIMELINE = "hearing.application.timeline";

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    public List<UUID> getApplicationHearings(final UUID applicationId) {
        LOGGER.info(" Calling {} to get hearing with hearing id {} ", HEARING_APPLICATION_TIMELINE, applicationId);
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(HEARING_APPLICATION_TIMELINE);

        final JsonEnvelope envelope = envelopeFrom(metadataBuilder, createObjectBuilder().add("id", applicationId.toString()));
        final Envelope<JsonObject> response = requester.requestAsAdmin(envelope, JsonObject.class);

        return Optional.ofNullable(response.payload().getJsonArray("hearingSummaries")).stream().flatMap(Collection::stream)
                .map(h -> h.asJsonObject().getString("hearingId"))
                .map(UUID::fromString)
                .toList();
    }
}
