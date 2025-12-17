package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("squid:CallToDeprecatedMethod")
public class ListingService {

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Inject
    private Enveloper enveloper;


    public Optional<JsonObject> searchCourtlist(final JsonEnvelope envelope) {

        final MetadataBuilder metadataBuilder = metadataFrom(envelope.metadata())
                .withName("listing.search.court.list.payload");

        final JsonEnvelope requestEnvelope = envelopeFrom(metadataBuilder, envelope.payloadAsJsonObject());
        final JsonEnvelope jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);
        return nonNull(jsonResultEnvelope) ? ofNullable(jsonResultEnvelope.payloadAsJsonObject()) : Optional.empty();

    }
}
