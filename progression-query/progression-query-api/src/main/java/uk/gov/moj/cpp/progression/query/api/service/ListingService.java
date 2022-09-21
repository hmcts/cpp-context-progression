package uk.gov.moj.cpp.progression.query.api.service;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import javax.inject.Inject;
import javax.json.JsonObject;

import java.util.Optional;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ListingService {

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    public Optional<JsonObject> searchTrialReadiness(final JsonEnvelope envelope) {
        final MetadataBuilder metadataBuilder = metadataFrom(envelope.metadata()).withName("listing.cotr.search.hearings");
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder, envelope.payloadAsJsonObject());
        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);
        return ofNullable(response.payload());
    }

}
