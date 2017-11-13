package uk.gov.moj.cpp.progression.command.handler.convertor;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OffencesForDefendantConverter {

    public OffencesForDefendantUpdated convert(JsonEnvelope command) {
        JsonObject payload = command.payloadAsJsonObject();
        List<OffenceForDefendant> offenceForDefendantList = convertOffences(payload.getJsonArray("offences"));
        UUID caseId = JsonObjects.getUUID(payload, "caseId").orElse(null);
        UUID defendantId = JsonObjects.getUUID(payload, "defendantId").orElse(null);
        return new OffencesForDefendantUpdated(caseId, defendantId, offenceForDefendantList);
    }

    private List<OffenceForDefendant> convertOffences(JsonArray jsonArray) {
        return jsonArray.stream()
                .map(jsonObject -> convertToOffence((JsonObject) jsonObject))
                .collect(Collectors.toList());
    }

    private OffenceForDefendant convertToOffence(JsonObject jsonObject) {

        UUID offenceId = JsonObjects.getUUID(jsonObject, "id").orElse(null);
        String offenceCode = JsonObjects.getString(jsonObject, "offenceCode").orElse(null);
        String modeOfTrial = JsonObjects.getString(jsonObject, "modeOfTrial").orElse(null);
        String indicatedPlea = JsonObjects.getString(jsonObject, "indicatedPlea").orElse(null);
        String section = JsonObjects.getString(jsonObject, "section").orElse(null);
        String wording = JsonObjects.getString(jsonObject, "wording").orElse(null);
        LocalDate startDate = JsonObjects.getString(jsonObject, "startDate").map(s -> LocalDate.parse(s)).orElse(null);
        LocalDate endDate = JsonObjects.getString(jsonObject, "endDate").map(s -> LocalDate.parse(s)).orElse(null);
        int orderIndex = JsonObjects.getJsonNumber(jsonObject, "orderIndex").map( s -> s.intValue()).orElse(0);
        Integer count = JsonObjects.getJsonNumber(jsonObject, "count").map( s -> s.intValue()).orElse(null);
        return new OffenceForDefendant(offenceId, offenceCode, indicatedPlea, section, wording, startDate, endDate, orderIndex,count,modeOfTrial);
    }


}
