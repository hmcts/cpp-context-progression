package uk.gov.moj.cpp.progression.domain.transformation.util;

import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ADDED_OFFENCES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.APPLICATION_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ARN;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.CASE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COUNT;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_APPLICATION;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_DOCUMENTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.COURT_REFERRAL;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.CROWN_COURT_HEARING;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DEFENDANT_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.DELETED_OFFENCES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HEARING;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HEARING_LISTING_STATUS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.HEARING_REQUEST;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.LIST_HEARING_REQUESTS;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.MODIFIED_DATE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.OFFENCES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASE;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.PROSECUTION_CASE_ID;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SHARED_TIME;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.SJP_REFERRAL;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CommonHelper.UPDATED_OFFENCES;
import static uk.gov.moj.cpp.progression.domain.transformation.util.CourtApplicationHelper.transformCourtApplication;
import static uk.gov.moj.cpp.progression.domain.transformation.util.HearingHelper.transformHearing;
import static uk.gov.moj.cpp.progression.domain.transformation.util.HearingListingNeeds.transformHearingListingNeeds;
import static uk.gov.moj.cpp.progression.domain.transformation.util.OffenceHelper.transformOffences;
import static uk.gov.moj.cpp.progression.domain.transformation.util.OffenceHelper.transformOffencesForDefendantCaseOffences;
import static uk.gov.moj.cpp.progression.domain.transformation.util.OffenceHelper.transformOffencesforDefendents;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ProsecutionCaseHelper.transformProsecutionCase;
import static uk.gov.moj.cpp.progression.domain.transformation.util.ProsecutionCaseHelper.transformReferedProsecutionCases;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;


public class TransformationEventHelper {

    private static final Logger LOGGER = getLogger(TransformationEventHelper.class);
    public static final String TRANSFORMED_PAYLOAD_AS_PER_MOT = "Transformed Payload as per mot -> {} ";
    public static final String ACTUAL_PAYLOAD_AS_PER_MASTER = "Actual Payload as per master -> {} ";


    public JsonEnvelope buildTransformedPayloadForHearing(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)))
                .add(SHARED_TIME, payload.getJsonString(SHARED_TIME));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);

        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }

    public JsonEnvelope buildOffencesForDefendentPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(OFFENCES, transformOffencesforDefendents(payload.getJsonArray(OFFENCES)))
                .add(CASE_ID, payload.getJsonString(CASE_ID))
                .add(DEFENDANT_ID, payload.getJsonString(DEFENDANT_ID));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);

        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }



    public JsonEnvelope buildCourtApplicationPayLoad(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(COURT_APPLICATION, transformCourtApplication(payload.getJsonObject(COURT_APPLICATION)));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);

        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);

    }

    public JsonEnvelope buildSendingSheetPayLoad(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)))
                .add(CROWN_COURT_HEARING, payload.getJsonObject(CROWN_COURT_HEARING));


        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);

        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }

    public JsonEnvelope buildHearingRequestPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);

        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(HEARING_REQUEST, transformHearingListingNeeds(payload.getJsonObject(HEARING_REQUEST)));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);

        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);

    }

    @SuppressWarnings({"squid:S1845"})
    public JsonEnvelope buildCourtApplicationPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder();
        if(payload.containsKey(COURT_APPLICATION)){
            transformedPayloadObjectBuilder.add(COURT_APPLICATION, transformCourtApplication(payload.getJsonObject(COURT_APPLICATION)));
        }
        if(payload.containsKey(COUNT)){
            transformedPayloadObjectBuilder.add(COUNT, payload.getJsonObject(COUNT));
        }
        if(payload.containsKey(ARN)){
            transformedPayloadObjectBuilder.add(ARN, payload.getJsonObject(ARN));
        }

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);

        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }

    public JsonEnvelope buildPublicHearingPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)))
                .add(SHARED_TIME, payload.getString(SHARED_TIME));
        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }

    public JsonEnvelope buildHearingPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)));
        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }

    public JsonEnvelope buildHearingListingStatusPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(HEARING, transformHearing(payload.getJsonObject(HEARING)))
                .add(HEARING_LISTING_STATUS, payload.getString(HEARING_LISTING_STATUS));
        if(payload.containsKey(APPLICATION_ID)){
            //Please refer event "hearing-application-link-created" for this property
            transformedPayloadObjectBuilder.add(APPLICATION_ID, payload.getJsonObject(APPLICATION_ID));
        }
        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }

    public JsonEnvelope buildOffencesForDefendebtPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder()
                .add(MODIFIED_DATE, payload.getString(MODIFIED_DATE));
        if(payload.containsKey(DELETED_OFFENCES)){
            transformedPayloadObjectBuilder.add(DELETED_OFFENCES, payload.getJsonArray(DELETED_OFFENCES));
        }
        if(payload.containsKey(ADDED_OFFENCES)){
            transformedPayloadObjectBuilder.add(ADDED_OFFENCES, transformOffencesForDefendantCaseOffences(payload.getJsonArray(ADDED_OFFENCES)));
        }
        if(payload.containsKey(UPDATED_OFFENCES)){
            transformedPayloadObjectBuilder.add(UPDATED_OFFENCES, transformOffencesForDefendantCaseOffences(payload.getJsonArray(UPDATED_OFFENCES)));
        }
        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }


    public JsonEnvelope buildProsecutionCasePayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder();
        if(payload.containsKey(PROSECUTION_CASE)){
            transformedPayloadObjectBuilder.add(PROSECUTION_CASE, transformProsecutionCase(payload.getJsonObject(PROSECUTION_CASE)));
        }
        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);
    }

    public JsonEnvelope buildProsecutionCaseOffencesPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder();

            transformedPayloadObjectBuilder.add("defendantCaseOffences", transformDefendantCaseOffences(payload.getJsonObject("defendantCaseOffences")));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);

    }

    private JsonObject transformDefendantCaseOffences(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        jsonObjectBuilder.add(DEFENDANT_ID, jsonObject.getString(DEFENDANT_ID));
        jsonObjectBuilder.add(PROSECUTION_CASE_ID, jsonObject.getString(PROSECUTION_CASE_ID));
        jsonObjectBuilder.add(OFFENCES, transformOffences(jsonObject.getJsonArray(OFFENCES)));
        return jsonObjectBuilder.build();
    }

    @SuppressWarnings({"squid:S4144"})
    public JsonEnvelope buildCaseReferToCourtPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder();

        transformedPayloadObjectBuilder.add(COURT_REFERRAL, transformCourtReferral(payload.getJsonObject(COURT_REFERRAL)));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);

    }

    private JsonObject transformCourtReferral(final JsonObject jsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        //refer Events "progression.event.cases-referred-to-court" and "progression.event.court-proceedings-initiated" for below condition
        if(jsonObject.containsKey(SJP_REFERRAL)){
            jsonObjectBuilder.add(SJP_REFERRAL, jsonObject.getJsonObject(SJP_REFERRAL));
        }

        jsonObjectBuilder.add(PROSECUTION_CASES, transformReferedProsecutionCases(jsonObject.getJsonArray(PROSECUTION_CASES)));
        jsonObjectBuilder.add(COURT_DOCUMENTS, jsonObject.getJsonArray(COURT_DOCUMENTS));
        jsonObjectBuilder.add(LIST_HEARING_REQUESTS, jsonObject.getJsonArray(LIST_HEARING_REQUESTS));
        return jsonObjectBuilder.build();
    }

    @SuppressWarnings({"squid:S4144"})
    public JsonEnvelope buildCourtProceedingPayload(final JsonEnvelope event, final String newEvent) {
        final JsonObject payload = event.payloadAsJsonObject();
        LOGGER.debug(ACTUAL_PAYLOAD_AS_PER_MASTER, payload);
        final JsonObjectBuilder transformedPayloadObjectBuilder = createObjectBuilder();

        transformedPayloadObjectBuilder.add(COURT_REFERRAL, transformCourtReferral(payload.getJsonObject(COURT_REFERRAL)));

        final JsonObject transformedObject = transformedPayloadObjectBuilder.build();
        LOGGER.debug(TRANSFORMED_PAYLOAD_AS_PER_MOT, transformedObject);
        return envelopeFrom(metadataBuilder()
                        .withName(newEvent)
                        .withId(UUID.fromString(event.metadata().asJsonObject().getString(ID))),
                transformedObject);

    }

}
