package uk.gov.justice.api.resource.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.listing.domain.Hearing;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

public class ListingQueryService {
    public static final String LISTING_SEARCH_HEARING = "listing.search.hearing";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    public Hearing searchHearing(final UUID userId, final UUID hearingId) {

        final Metadata metadata = metadataWithNewActionName(getListingQueryJsonEnvelop(userId).metadata(), LISTING_SEARCH_HEARING);
        final JsonObject jsonPayLoad = Json.createObjectBuilder()
                .add("id", hearingId.toString())
                .build();
        return requester.requestAsAdmin(envelopeFrom(metadata, jsonPayLoad), Hearing.class).payload();
    }

    private static Metadata metadataWithNewActionName(final Metadata metadata, final String actionName) {
        final MetadataBuilder metadataBuilder = Envelope.metadataBuilder().withId(UUID.randomUUID())
                .withName(actionName)
                .createdAt(ZonedDateTime.now())
                .withCausation(metadata.causation().toArray(new UUID[metadata.causation().size()]));

        metadata.clientCorrelationId().ifPresent(metadataBuilder::withClientCorrelationId);
        metadata.sessionId().ifPresent(metadataBuilder::withSessionId);
        metadata.streamId().ifPresent(metadataBuilder::withStreamId);
        metadata.userId().ifPresent(metadataBuilder::withUserId);
        metadata.version().ifPresent(metadataBuilder::withVersion);

        return metadataBuilder.build();
    }

    private JsonEnvelope getListingQueryJsonEnvelop(final UUID userId) {
        return JsonEnvelope.envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(LISTING_SEARCH_HEARING)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );
    }
}
