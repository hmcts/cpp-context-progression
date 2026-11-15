package uk.gov.moj.cpp.progression.query.view.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;

class SjpDecisionPayloadsTest {

    private static final String SHARED_TIME = "2026-01-01T10:15:30.000Z";
    private static final UUID FVS = UUID.randomUUID();
    private static final UUID FCOST = UUID.randomUUID();
    private static final UUID FO = UUID.randomUUID();
    private static final UUID FCOMP = UUID.randomUUID();
    private static final UUID LEP = UUID.randomUUID();

    @Test
    void shouldSetLastSharedDateTimeOnEveryJudicialResultThatHasNone() {
        final String existing = "2025-12-31T09:00:00.000Z";
        final JsonObject hearing = hearing(
                result(FVS, "Surcharge", null),
                result(FCOST, "Costs", null),
                result(FO, "Fine", null),
                result(FCOMP, "Compensation", null).add("lastSharedDateTime", existing),
                result(LEP, "Endorsed", null).addNull("lastSharedDateTime"));

        final JsonObject filled = (JsonObject) SjpDecisionPayloads.withLastSharedDateTime(hearing, Json.createValue(SHARED_TIME));

        assertThat(defendantResult(filled).getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(caseResult(filled).getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(offenceResults(filled).getJsonObject(0).getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(offenceResults(filled).getJsonObject(1).getString("lastSharedDateTime"), is(existing));
        assertThat(offenceResults(filled).getJsonObject(2).getString("lastSharedDateTime"), is(SHARED_TIME));
        assertThat(filled.containsKey("lastSharedDateTime"), is(false));
    }

    @Test
    void shouldPrefixResultTextWithShortCodeAtEveryLevel() {
        final JsonObject hearing = hearing(
                result(FVS, "Surcharge", "Surcharge\nAmount of surcharge 45.00"),
                result(FCOST, "Costs", "Costs\nAmount of costs 90.00"),
                result(FO, "Fine", "Fine\nAmount of fine 100.00"),
                result(FCOMP, "Compensation", null),
                result(LEP, "Driving record endorsed with penalty points", "LEP - Driving record endorsed with penalty points"));

        final JsonObject coded = (JsonObject) SjpDecisionPayloads.withShortCodes(hearing,
                Map.of(FVS, "FVS", FCOST, "FCOST", FO, "FO", FCOMP, "FCOMP", LEP, "LEP"));

        assertThat(defendantResult(coded).getString("resultText"), is("FVS - Surcharge\nAmount of surcharge 45.00"));
        assertThat(caseResult(coded).getString("resultText"), is("FCOST - Costs\nAmount of costs 90.00"));
        assertThat(offenceResults(coded).getJsonObject(0).getString("resultText"), is("FO - Fine\nAmount of fine 100.00"));
        assertThat(offenceResults(coded).getJsonObject(1).getString("resultText"), is("FCOMP - Compensation"));
        assertThat(offenceResults(coded).getJsonObject(2).getString("resultText"), is("LEP - Driving record endorsed with penalty points"));
    }

    @Test
    void shouldLeaveResultTextUnchangedWhenShortCodeIsUnknown() {
        final JsonObject hearing = hearing(
                result(FVS, "Surcharge", "Surcharge"),
                result(FCOST, "Costs", "Costs"),
                result(FO, "Fine", "Fine"),
                result(FCOMP, "Compensation", null),
                result(LEP, "Endorsed", "Endorsed"));

        final JsonObject coded = (JsonObject) SjpDecisionPayloads.withShortCodes(hearing, Map.of(FO, "FO"));

        assertThat(defendantResult(coded).getString("resultText"), is("Surcharge"));
        assertThat(offenceResults(coded).getJsonObject(0).getString("resultText"), is("FO - Fine"));
        assertThat(offenceResults(coded).getJsonObject(1).containsKey("resultText"), is(false));
    }

    @Test
    void shouldCollectResultDefinitionIdsAtEveryLevel() {
        final JsonObject hearing = hearing(
                result(FVS, "Surcharge", null),
                result(FCOST, "Costs", null),
                result(FO, "Fine", null),
                result(FCOMP, "Compensation", null),
                result(FO, "Fine", null));

        assertThat(SjpDecisionPayloads.resultDefinitionIds(hearing), containsInAnyOrder(FVS, FCOST, FO, FCOMP));
    }

    private static JsonObject hearing(final JsonObjectBuilder defendantResult, final JsonObjectBuilder caseResult,
                                      final JsonObjectBuilder... offenceResults) {
        final javax.json.JsonArrayBuilder offenceResultsArray = createArrayBuilder();
        for (final JsonObjectBuilder offenceResult : offenceResults) {
            offenceResultsArray.add(offenceResult);
        }
        return createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("defendantJudicialResults", createArrayBuilder().add(createObjectBuilder().add("judicialResult", defendantResult)))
                .add("prosecutionCases", createArrayBuilder().add(createObjectBuilder()
                        .add("defendants", createArrayBuilder().add(createObjectBuilder()
                                .add("defendantCaseJudicialResults", createArrayBuilder().add(caseResult))
                                .add("offences", createArrayBuilder().add(createObjectBuilder()
                                        .add("judicialResults", offenceResultsArray)))))))
                .build();
    }

    private static JsonObjectBuilder result(final UUID resultDefinitionId, final String label, final String resultText) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("judicialResultId", resultDefinitionId.toString())
                .add("judicialResultTypeId", resultDefinitionId.toString())
                .add("label", label);
        if (resultText != null) {
            builder.add("resultText", resultText);
        }
        return builder;
    }

    private static JsonObject defendant(final JsonObject hearing) {
        return hearing.getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0);
    }

    private static JsonObject defendantResult(final JsonObject hearing) {
        return hearing.getJsonArray("defendantJudicialResults").getJsonObject(0).getJsonObject("judicialResult");
    }

    private static JsonObject caseResult(final JsonObject hearing) {
        return defendant(hearing).getJsonArray("defendantCaseJudicialResults").getJsonObject(0);
    }

    private static JsonArray offenceResults(final JsonObject hearing) {
        return defendant(hearing).getJsonArray("offences").getJsonObject(0).getJsonArray("judicialResults");
    }
}
