package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.transformer;

import uk.gov.moj.cpp.external.domain.hearing.Case;
import uk.gov.moj.cpp.external.domain.hearing.Defendant;
import uk.gov.moj.cpp.external.domain.hearing.Hearing;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.external.domain.hearing.Judge;
import uk.gov.moj.cpp.external.domain.hearing.Offence;
import uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

@SuppressWarnings("squid:S1118")
public class InitiateHearingTransformer {


    public static InitiateHearing transformToInitiateHearing(JsonObject jsonObject) {
        UUID caseId = java.util.UUID.fromString(jsonObject.getString(ProcessMapConstant.CASE_ID));
        String urn = jsonObject.getString("urn");
        List<Case> cases = new ArrayList<>();
        cases.add(new Case(caseId, urn));
        Hearing hearing = getHearingObject(jsonObject.getJsonObject("hearing"));
        return new InitiateHearing(cases, hearing);
    }

    private static Hearing getHearingObject(JsonObject jsonObject) {
        UUID hearingId = java.util.UUID.fromString(jsonObject.getString("id"));
        String type = jsonObject.getString("type");
        UUID courtCentreId = java.util.UUID.fromString(jsonObject.getString("courtCentreId"));
        UUID courtRoomId = java.util.UUID.fromString(jsonObject.getString("courtRoomId"));
        UUID judgeId = java.util.UUID.fromString(jsonObject.getString("judgeId"));
        Judge judge = new Judge(judgeId);
        JsonArray hearingDaysJson = jsonObject.getJsonArray("hearingDays");
        List<String> hearingDays = hearingDaysJson.stream()
                .map(json -> ((JsonString) json).getString())
                .collect(Collectors.toList());
        List<Defendant> defendants = getDefendants(jsonObject.getJsonArray("defendants"));

        return new Hearing(hearingId, courtCentreId, courtRoomId, judge, type,
                hearingDays, defendants);
    }

    private static List<Defendant> getDefendants(JsonArray jsonArray) {

        List<Defendant> defendants = new ArrayList<>();
        if (jsonArray != null) {
            IntStream.rangeClosed(0, jsonArray.size() - 1).forEach(i -> {
                JsonObject defendantJson = jsonArray.getJsonObject(i);
                Defendant defendant = new Defendant(UUID.fromString(defendantJson.getString("id")));
                defendant.getOffences().addAll(getOffences(defendantJson.getJsonArray("offences")));
                defendants.add(defendant);
            });
        }

        return defendants;
    }

    private static List<Offence> getOffences(JsonArray jsonArray) {
        List<Offence> offences = new ArrayList<>();
        if (jsonArray != null) {
            IntStream.rangeClosed(0, jsonArray.size() - 1).forEach(i -> {
                JsonObject offenceJson = jsonArray.getJsonObject(i);
                offences.add(new Offence(UUID.fromString(offenceJson.getString("id"))));
            });
        }
        return offences;
    }

}
