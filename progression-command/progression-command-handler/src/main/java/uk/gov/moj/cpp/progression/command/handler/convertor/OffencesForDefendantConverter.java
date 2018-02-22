package uk.gov.moj.cpp.progression.command.handler.convertor;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceIndicatedPlea;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencePlea;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import javax.json.JsonArray;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class OffencesForDefendantConverter {

    public OffencesForDefendantUpdated convert(final JsonEnvelope command) {
        final JsonObject payload = command.payloadAsJsonObject();
        final List<OffenceForDefendant> offenceForDefendantList = convertOffences(payload.getJsonArray("offences"));
        final UUID caseId = JsonObjects.getUUID(payload, "caseId").orElse(null);
        final UUID defendantId = JsonObjects.getUUID(payload, "defendantId").orElse(null);
        return new OffencesForDefendantUpdated(caseId, defendantId, offenceForDefendantList);
    }

    private List<OffenceForDefendant> convertOffences(final JsonArray jsonArray) {
        return jsonArray.stream()
                .map(jsonObject -> convertToOffence((JsonObject) jsonObject))
                .collect(Collectors.toList());
    }

    private OffenceForDefendant convertToOffence(final JsonObject jsonObject) {

        final UUID offenceId = JsonObjects.getUUID(jsonObject, "id").orElse(null);
        final String offenceCode = JsonObjects.getString(jsonObject, "offenceCode").orElse(null);
        final OffencePlea offencePlea = JsonObjects.getJsonObject(jsonObject, "plea").map(pleaJsonObject -> convertToOffencePlea(pleaJsonObject)).orElse(null);
        final OffenceIndicatedPlea offenceIndicatedPlea = JsonObjects.getJsonObject(jsonObject, "indicatedPlea").map(indicatedPleaJsonObject -> convertToOffenceIndicatedPlea(indicatedPleaJsonObject)).orElse(null);
        final String section = JsonObjects.getString(jsonObject, "section").orElse(null);
        final String wording = JsonObjects.getString(jsonObject, "wording").orElse(null);
        final LocalDate startDate = JsonObjects.getString(jsonObject, "startDate").map(s -> LocalDate.parse(s)).orElse(null);
        final LocalDate endDate = JsonObjects.getString(jsonObject, "endDate").map(s -> LocalDate.parse(s)).orElse(null);
        final LocalDate convictionDate = JsonObjects.getString(jsonObject, "convictionDate").map(s -> LocalDate.parse(s)).orElse(null);
        final int orderIndex = JsonObjects.getJsonNumber(jsonObject, "orderIndex").map( s -> s.intValue()).orElse(0);
        final Integer count = JsonObjects.getJsonNumber(jsonObject, "count").map( s -> s.intValue()).orElse(null);
        return new OffenceForDefendant(offenceId, offenceCode, section, wording, startDate, endDate, orderIndex, count, offencePlea, offenceIndicatedPlea, convictionDate);
    }

    private OffencePlea convertToOffencePlea(JsonObject pleaJsonObject) {
        final UUID pleaId = JsonObjects.getUUID(pleaJsonObject, "id").orElse(null);
        final String value = JsonObjects.getString(pleaJsonObject, "value").orElse(null);
        final LocalDate pleaDate = JsonObjects.getString(pleaJsonObject, "pleaDate").map(s -> LocalDate.parse(s)).orElse(null);
        return new OffencePlea(pleaId, value, pleaDate);
    }

    private OffenceIndicatedPlea convertToOffenceIndicatedPlea(JsonObject indicatedPleaJsonObject) {
        final UUID indicatedPleasId = JsonObjects.getUUID(indicatedPleaJsonObject, "id").orElse(null);
        final String value = JsonObjects.getString(indicatedPleaJsonObject, "value").orElse(null);
        final String allocationDecision = JsonObjects.getString(indicatedPleaJsonObject, "allocationDecision").orElse(null);
        return new OffenceIndicatedPlea(indicatedPleasId, value, allocationDecision);
    }

}
