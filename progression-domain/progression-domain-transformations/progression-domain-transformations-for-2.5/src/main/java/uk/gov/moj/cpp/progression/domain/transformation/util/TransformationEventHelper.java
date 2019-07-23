package uk.gov.moj.cpp.progression.domain.transformation.util;

import org.slf4j.Logger;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.UUID;

import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.HearingHelper.transformHearing;
import static uk.gov.moj.cpp.progression.domain.transformation.util.OffenceHelper.transformOffences;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ProsecutionCaseHelper.transformProsecutionCase;


public class TransformationEventHelper {

    private static final Logger LOGGER = getLogger(TransformationEventHelper.class);
    public static final String HEARING = "hearing";
    public static final String PROSECUTION_CASE = "prosecutionCase";

    public JsonEnvelope buildDefendantCaseOffencesTransformedPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject().getJsonObject("defendantCaseOffences");
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Before -> {}", payload);
        }

        //Get the mandatory fields
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add("defendantCaseOffences", transformDefendantCaseOffences(payload));

        final JsonObject transformedPayload = transformedPayloadObjectBuilder.build();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("After -> {}", transformedPayload);
        }
        return envelopeFrom(metadataBuilder()
                .withName(newEvent)
                .withId(UUID.fromString(event.metadata().asJsonObject().getString("id"))), transformedPayload);
    }

    public JsonEnvelope buildListingStatusChangedTransformedPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Before -> {}", payload);
        }

        //Get the mandatory fields
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add("hearingListingStatus", payload.getString("hearingListingStatus"))
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)));

        final JsonObject transformedPayload = transformedPayloadObjectBuilder.build();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("After -> {}", transformedPayload);
        }
        return envelopeFrom(metadataBuilder()
                .withName(newEvent)
                .withId(UUID.fromString(event.metadata().asJsonObject().getString("id"))), transformedPayload);
    }

    public JsonEnvelope buildProsecutionCaseCreatedTransformedPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Before -> {}", payload);
        }

        //Get the mandatory fields
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(PROSECUTION_CASE, transformProsecutionCase(payload.getJsonObject(PROSECUTION_CASE)));

        final JsonObject transformedPayload = transformedPayloadObjectBuilder.build();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("After -> {}", transformedPayload);
        }
        return envelopeFrom(metadataBuilder()
                .withName(newEvent)
                .withId(UUID.fromString(event.metadata().asJsonObject().getString("id"))), transformedPayload);
    }

    public JsonEnvelope buildHearingResultedTransformedPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Before -> {}", payload);
        }

        //Get the mandatory fields
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add("sharedTime", payload.getJsonObject(HEARING).getJsonArray("hearingDays").getJsonObject(0).getString("sittingDay"))
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)));

        final JsonObject transformedPayload = transformedPayloadObjectBuilder.build();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("After -> {}", transformedPayload);
        }
        return envelopeFrom(metadataBuilder()
                .withName(newEvent)
                .withId(UUID.fromString(event.metadata().asJsonObject().getString("id"))), transformedPayload);
    }

    private JsonObject transformDefendantCaseOffences(final JsonObject payload) {
        JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add("defendantId", payload.getString("defendantId"))
                .add("prosecutionCaseId", payload.getString("prosecutionCaseId"))
                .add("offences", transformOffences(payload.getJsonArray("offences")));

        return transformedPayloadObjectBuilder.build();
    }

}
