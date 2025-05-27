package uk.gov.moj.cpp.progression.query.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.api.resource.utils.CourtExtractTransformer;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Arrays;
import java.util.UUID;

import static javax.json.Json.createObjectBuilder;

public class RecordSheetService {
    public static final String DEFENDANT_ID = "defendantId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordSheetService.class);
    public static final String RECORD_SHEET = "RecordSheet";
    public static final String CASE_ID = "caseId";
    @Inject
    private CourtExtractTransformer courtExtractTransformer;

    public JsonEnvelope getTrialRecordSheetPayload(final JsonEnvelope envelope, final JsonEnvelope document, final UUID userId) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonObject payloadAsJsonObject = envelope.payloadAsJsonObject();
        String caseId = payloadAsJsonObject.containsKey(CASE_ID) ? payloadAsJsonObject.getString(CASE_ID) : null;
        String defendantId = payloadAsJsonObject.containsKey(DEFENDANT_ID) ? payloadAsJsonObject.getString(DEFENDANT_ID) : null;

        try {
            LOGGER.info("Prosecution case for Record sheet for a defendant {} with caseId {} ", defendantId, caseId);
            JsonObject result = courtExtractTransformer.getTransformedPayload(document, defendantId, RECORD_SHEET, Arrays.asList(), userId);
            LOGGER.info("Successfully fetched transformed payload for defendantId: {} and caseId: {}", defendantId, caseId);
            jsonObjectBuilder.add("payload", result);
        } catch (final Exception e) {
            LOGGER.error("Error generating record sheet for case id : {}, defendant id : {}", caseId, defendantId, e);
        }
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }
}
