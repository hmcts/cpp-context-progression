package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    static final String REFERENCEDATA_GET_PROSECUTOR = "referencedata.query.prosecutor";

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public Optional<JsonObject> getProsecutor(final String prosecutorId) {
        final JsonObject payload = createObjectBuilder().add("id", prosecutorId).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCEDATA_GET_PROSECUTOR),
                payload);

        final JsonEnvelope jsonResultEnvelope = requester.requestAsAdmin(requestEnvelope);

        return ofNullable(jsonResultEnvelope.payloadAsJsonObject());
    }
}
