package uk.gov.moj.cpp.progression.query.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.util.UUID;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

public class HearingDetailsLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDetailsLoader.class);

    private static final String QUERY_HEARING = "hearing.get.hearing";
    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_FIELD = "hearing";
    private static final String TYPE_FIELD = "type";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String ID_FIELD = "id";
    private static final String JUDICIARY_FIELD = "judiciary";
    private static final String USER_ID_FIELD = "userId";

    public HearingDetails getHearingDetails(final Requester requester, final UUID hearingId) {
        LOGGER.info(" Calling {} to get hearing with hearing id {} ", QUERY_HEARING, hearingId);
        final MetadataBuilder metadataBuilder = metadataBuilder()
                .withId(randomUUID())
                .withName(QUERY_HEARING);
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder, createObjectBuilder().add(HEARING_ID, hearingId.toString()));
        final Envelope<JsonObject> response = requester.requestAsAdmin(envelope, JsonObject.class);
        final HearingDetails hearingDetails = new HearingDetails();
        hearingDetails.setType(response.payload().getJsonObject(HEARING_FIELD).getJsonObject(TYPE_FIELD).getString(DESCRIPTION_FIELD));
        hearingDetails.setHearingTypeId(fromString(response.payload().getJsonObject(HEARING_FIELD).getJsonObject(TYPE_FIELD).getString(ID_FIELD)));
        final JsonArray judiciaries = response.payload().getJsonObject(HEARING_FIELD).getJsonArray(JUDICIARY_FIELD);
        for(final JsonObject judiciary : judiciaries.getValuesAs(JsonObject.class)) {
            if(judiciary.containsKey(USER_ID_FIELD)) {
                hearingDetails.addUserId(fromString(judiciary.getString(USER_ID_FIELD)));
            }
        }
        return hearingDetails;
    }
}
