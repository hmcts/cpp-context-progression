package uk.gov.moj.cpp.progression.query.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.api.resource.utils.ReportsTransformer;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static javax.json.Json.createObjectBuilder;

public class RecordSheetService {
    public static final String DEFENDANT_ID = "defendantId";
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordSheetService.class);
    public static final String RECORD_SHEET = "RecordSheet";
    public static final String CASE_ID = "caseId";
    private static final String OFFENCE_IDS = "offenceIds";
    private static final String PAYLOAD = "payload";
    private static final String PAYLOADS = "payloads";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String DEFENDANTS = "defendants";
    private static final String OFFENCES = "offences";
    private static final String ID = "id";
    private static final String DEFENDANT_NAME = "defendantName";
    private static final String DEFENDANT = "defendant";
    private static final String NAME = "name";

    @Inject
    private ReportsTransformer reportsTransformer;

    public JsonEnvelope getTrialRecordSheetPayload(final JsonEnvelope envelope, final JsonEnvelope document, final UUID userId) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonObject payloadAsJsonObject = envelope.payloadAsJsonObject();
        String caseId = payloadAsJsonObject.containsKey(CASE_ID) ? payloadAsJsonObject.getString(CASE_ID) : null;
        String defendantId = payloadAsJsonObject.containsKey(DEFENDANT_ID) ? payloadAsJsonObject.getString(DEFENDANT_ID) : null;

        try {
            LOGGER.info("Prosecution case for Record sheet for a defendant {} with caseId {} ", defendantId, caseId);
            JsonObject result = reportsTransformer.getTransformedPayload(document, defendantId, RECORD_SHEET, Arrays.asList(), userId);
            LOGGER.info("Successfully fetched transformed payload for defendantId: {} and caseId: {}", defendantId, caseId);
            jsonObjectBuilder.add(PAYLOAD, result);
        } catch (final Exception e) {
            LOGGER.error("Error generating record sheet for case id : {}, defendant id : {}", caseId, defendantId, e);
        }
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    public JsonEnvelope getTrialRecordSheetPayloadForApplication(final JsonEnvelope envelope, final JsonEnvelope document, final UUID userId) {
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final JsonObject payloadAsJsonObject = envelope.payloadAsJsonObject();
        final String caseId = payloadAsJsonObject.getString(CASE_ID);
        final List<String> offenceIds = Arrays.stream(payloadAsJsonObject.getString(OFFENCE_IDS).split(",")).toList();

        List<String> defendantIds = findRelatedDefendantIdsForCaseIdAndOffenceId(document.payloadAsJsonObject(), offenceIds);
        defendantIds.forEach(defendantId ->
        {
            try {
                LOGGER.info("Prosecution case for Record sheet for a defendant {} with caseId {} ", defendantId, caseId);
                JsonObject result = reportsTransformer.getTransformedPayload(document, defendantId, RECORD_SHEET, emptyList(), userId);
                LOGGER.info("Successfully fetched transformed payload for defendantId: {} and caseId: {}", defendantId, caseId);

                jsonArrayBuilder.add(Json.createObjectBuilder()
                        .add(PAYLOAD, result)
                        .add(DEFENDANT_NAME, getDefendantName(result))
                        .build());
            } catch (final Exception e) {
                LOGGER.error("Error generating record sheet for case id : {}, defendant id : {}", caseId, defendantId, e);
            }
        });

        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                Json.createObjectBuilder().add(PAYLOADS, jsonArrayBuilder.build()).build());
    }

    private static String getDefendantName(final JsonObject result) {
        return Optional.ofNullable(result)
                .filter(r -> r.containsKey(DEFENDANT))
                .map(r -> r.getJsonObject(DEFENDANT).getString(NAME))
                .orElse("");
    }

    private List<String> findRelatedDefendantIdsForCaseIdAndOffenceId(final JsonObject payload, final List<String> offenceIds) {
        return payload.getJsonObject(PROSECUTION_CASE).getJsonArray(DEFENDANTS).stream()
                .filter(defendant -> ((JsonObject) defendant).getJsonArray(OFFENCES).stream()
                        .anyMatch(offence -> offenceIds.contains(((JsonObject) offence).getString(ID))))
                .map(defendant -> ((JsonObject) defendant).getString(ID))
                .toList();

    }
}
