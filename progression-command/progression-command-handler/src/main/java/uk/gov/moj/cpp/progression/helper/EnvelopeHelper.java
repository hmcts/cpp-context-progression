package uk.gov.moj.cpp.progression.helper;

import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.METADATA;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;

import uk.gov.justice.services.core.accesscontrol.AccessControlViolationException;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class EnvelopeHelper {

    @Inject
    private SystemUserProvider systemUserProvider;

    public JsonEnvelope withMetadataInPayload(final JsonEnvelope envelope) {
        final AtomicReference<Function<String, Boolean>> excludeCausation = new AtomicReference<>(key -> !"causation".equals(key));


        final Metadata metadataWithSystemUser = metadataFrom(envelope.metadata())
                .withUserId(systemUserProvider.getContextSystemUserId().orElseThrow(() -> new AccessControlViolationException("System user not found")).toString())
                .build();

        final JsonObjectBuilder metadataWithoutCausation = createObjectBuilderWithFilter(metadataWithSystemUser.asJsonObject(), excludeCausation.get());

        final JsonObject payloadWithMetadata = createObjectBuilder(envelope.payloadAsJsonObject()).add(METADATA, metadataWithoutCausation).build();
        return JsonEnvelope.envelopeFrom(envelope.metadata(), payloadWithMetadata);
    }
}
