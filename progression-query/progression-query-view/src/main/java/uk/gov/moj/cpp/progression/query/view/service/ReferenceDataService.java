package uk.gov.moj.cpp.progression.query.view.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.query.view.service.exception.ReferenceDataServiceException;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_VIEW;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

public class ReferenceDataService {

    static final String REFERENCEDATA_GET_PROSECUTOR = "referencedata.query.prosecutor";

    public static final String REFERENCEDATA_QUERY_LANGUAGES = "referencedata.query.languages";
    private static final String REFERENCEDATA_GET_HEARINGTYPES = "referencedata.query.hearing-types";
    static final String REFERENCEDATA_QUERY_RESULT_DEFINITIONS_BY_IDS = "referencedata.query-result-definitions-by-ids";


    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);

    @Inject
    @ServiceComponent(QUERY_VIEW)
    private Requester requester;


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


    public JsonObject getLanguages() {
        final JsonObject request = createObjectBuilder().build();

        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder().
                        withId(randomUUID()).
                        withName(REFERENCEDATA_QUERY_LANGUAGES),
                request);

        final Envelope<JsonObject> response = requester.requestAsAdmin(requestEnvelope, JsonObject.class);

        if (isNull(response.payload())) {
            throw new ReferenceDataServiceException("Failed to get languages from reference Data");
        }
        LOGGER.info("Got languages from reference data context");
        return response.payload();
    }

    public JsonArray getHearingTypes(final JsonEnvelope event) {
        final Metadata metadata = metadataFrom(event.metadata())
                .withName(REFERENCEDATA_GET_HEARINGTYPES)
                .build();
        final JsonEnvelope jsonEnvelop = requester.request(envelopeFrom(metadata, createObjectBuilder().build()));

        return jsonEnvelop.payloadAsJsonObject().getJsonArray("hearingTypes");
    }

    /**
     * @return the short code of each of the given result definitions that reference data holds, by result definition ID.
     */
    public Map<UUID, String> getResultShortCodes(final JsonEnvelope envelope, final Collection<UUID> resultDefinitionIds) {
        if (resultDefinitionIds.isEmpty()) {
            return emptyMap();
        }
        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(REFERENCEDATA_QUERY_RESULT_DEFINITIONS_BY_IDS)
                .build();
        final JsonObject payload = createObjectBuilder()
                .add("ids", resultDefinitionIds.stream().map(UUID::toString).collect(joining(",")))
                .build();
        final JsonEnvelope response = requester.requestAsAdmin(envelopeFrom(metadata, payload));
        final JsonArray resultDefinitions = isNull(response) || !(response.payload() instanceof JsonObject)
                ? null : response.payloadAsJsonObject().getJsonArray("resultDefinitions");
        final Map<UUID, String> shortCodes = new HashMap<>();
        if (nonNull(resultDefinitions)) {
            resultDefinitions.getValuesAs(JsonObject.class).stream()
                    .filter(definition -> definition.containsKey("shortCode") && !definition.isNull("shortCode"))
                    .forEach(definition -> shortCodes.put(UUID.fromString(definition.getString("id")), definition.getString("shortCode")));
        }
        return shortCodes;
    }
}
