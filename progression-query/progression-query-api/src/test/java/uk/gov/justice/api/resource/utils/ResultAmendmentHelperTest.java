package uk.gov.justice.api.resource.utils;

import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.api.resource.dto.AmendmentLog.amendmentLog;
import static uk.gov.justice.api.resource.dto.AmendmentReason.amendmentReason;
import static uk.gov.justice.api.resource.service.ReferenceDataService.FIELD_RESULT_DEFINITIONS;
import static uk.gov.justice.api.resource.utils.FileUtil.jsonFromPath;
import static uk.gov.justice.api.resource.utils.ResultAmendmentHelper.extractAmendmentsDueToSlipRule;

import uk.gov.justice.api.resource.dto.AmendmentReason;
import uk.gov.justice.api.resource.dto.AmendmentRecord;
import uk.gov.justice.api.resource.dto.AmendmentType;
import uk.gov.justice.api.resource.dto.DraftResultsWrapper;
import uk.gov.justice.api.resource.dto.ResultDefinition;
import uk.gov.justice.api.resource.dto.ResultDefinitionPrompt;
import uk.gov.justice.api.resource.dto.ResultLine;
import uk.gov.justice.api.resource.dto.ResultPrompt;
import uk.gov.justice.progression.courts.exract.Amendments;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

public class ResultAmendmentHelperTest {

    private UUID caseId = randomUUID();
    private UUID defendantId = randomUUID();
    private UUID offenceId = randomUUID();

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());
    private final AmendmentReason slipRuleAmendmentReason = amendmentReason().withId(fromString("a02018a1-915c-3343-95ad-abc5f99b339a")).
            withReasonDescription("Error or Omission in result announced in court (Amendment under the Slip rule)").build();

    private final AmendmentReason adminErrorAmendmentReason = amendmentReason().withId(fromString("ca8b8285-5fc7-3b36-aa78-ecdf5ac6dad0")).
            withReasonDescription("Admin error on shared result (a result recorded incorrectly)").build();

    private static final UUID SLIP_RULE_AMENDMENT_REASON_ID = UUID.fromString("a02018a1-915c-3343-95ad-abc5f99b339a");

    @Test
    public void shouldReturnSharedAmendmentsDueToSlipRule() {

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final UUID resultLineId = randomUUID();
        final UUID resultDefId = randomUUID();
        final UUID promptId = randomUUID();
        final ZonedDateTime amendedDate = ZonedDateTime.now();
        final ZonedDateTime sharedDate = ZonedDateTime.now().plusHours(1);


        final List<ResultLine> resultsWithAmendments = List.of(getResultLineBuilder()
                .withResultLineId(resultLineId)
                .withValid(Boolean.TRUE)
                .withResultDefinitionId(resultDefId)
                .withAmendmentDate(amendedDate)
                .withSharedDate(sharedDate)
                .withAmendmentLog(amendmentLog()
                        .withIsAmended(Boolean.TRUE)
                        .withAmendmentsRecord(List.of(AmendmentRecord.amendmentRecord()
                                        .withAmendmentDate(ZonedDateTime.now())
                                        .withAmendmentReason(slipRuleAmendmentReason)
                                        .withResultPromptsRecord(List.of(ResultPrompt.prompt().withId(promptId)
                                                .withPromptRef("promptRef").withLabel("End Date").withValue(JsonObjects.getProvider().createValue("2024-03-21")).build()))
                                        .build(),
                                AmendmentRecord.amendmentRecord()
                                        .withAmendmentDate(ZonedDateTime.now())
                                        .withAmendmentReason(adminErrorAmendmentReason)
                                        .build()))
                        .build())
                .build());

        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(hearingId, hearingDay, resultsWithAmendments, null);

        final List<ResultDefinition> resultDefinitionList = List.of(ResultDefinition.builder()
                .withId(resultDefId)
                .withLabel("RD Label")
                .withPrompts(List.of(ResultDefinitionPrompt.prompt().setId(promptId).setLabel("End Date").setSequence(1),
                        ResultDefinitionPrompt.prompt().setId(randomUUID()).setLabel("Number of hours").setSequence(2)))
                .build());

        final Map<UUID, List<Amendments>> resultAmendmentsMap = extractAmendmentsDueToSlipRule(List.of(draftResultsWrapper), resultDefinitionList, SLIP_RULE_AMENDMENT_REASON_ID);
        assertThat(resultAmendmentsMap.size(), is(1));
        final List<Amendments> sharedAmendments = resultAmendmentsMap.get(resultLineId);
        assertThat(sharedAmendments.size(), is(1));
        assertThat(sharedAmendments.get(0).getDefendantId(), is(defendantId));
        assertThat(sharedAmendments.get(0).getJudicialResultId(), is(resultLineId));
        assertThat(sharedAmendments.get(0).getAmendmentType(), is(AmendmentType.AMENDED.name()));
        assertThat(sharedAmendments.get(0).getResultText(), is("RD Label\nEnd Date 2024-03-21"));
    }

    @Test
    public void shouldReturnSharedAmendmentsWhenResultLinesOneOfNameAddress() {

        final List<ResultLine> resultLines = getResultLines("hearing-results/payload-hearing-get-draft-result-oneof-nameaddress.json");
        assertThat(resultLines.size(), is(9));

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(hearingId, hearingDay, resultLines, null);


        final List<ResultDefinition> resultDefinitions = getResultDefinitions("hearing-results/referencedata.query.referencedata.all-definitions.json");
        assertThat(resultDefinitions.size(), is(11));

        final Map<UUID, List<Amendments>> resultAmendmentsMap = extractAmendmentsDueToSlipRule(List.of(draftResultsWrapper), resultDefinitions, SLIP_RULE_AMENDMENT_REASON_ID);
        assertThat(resultAmendmentsMap.size(), is(6));

        final List<Amendments> oneOfAmendments = resultAmendmentsMap.get(fromString("80ecc8ff-99ad-43b3-b53b-5de7b3901d91"));
        assertThat(oneOfAmendments.size(), is(1));
        assertThat(oneOfAmendments.get(0).getAmendmentType(), is("DELETED"));
        assertThat(oneOfAmendments.get(0).getResultText(), is("Compensation\nAmount of compensation 3000.00\nMajor creditor organisation name \"Asda Stores,Treasury Dept. Asda House,Great Wilson St,Leeds,LS11 5AD\""));

        final List<Amendments> deletedAmendments = resultAmendmentsMap.get(fromString("175d27a7-bc42-48af-89f2-39d66140be78"));
        assertThat(deletedAmendments, is(nullValue()));
    }

    @Test
    public void shouldReturnSharedAmendmentsWhenResultLinesHaveDeletedResults() {

        final List<ResultLine> resultLineWithAmendments = getResultLines("hearing-results/payload-hearing-get-draft-result-AD-deleted.json").stream()
                .filter(resultLine -> nonNull(resultLine.getAmendmentsLog()) && isNotEmpty(resultLine.getAmendmentsLog().getAmendmentsRecord())).collect(toList());

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(hearingId, hearingDay, resultLineWithAmendments, null);

        assertThat(resultLineWithAmendments.size(), is(1));

        final List<ResultDefinition> resultDefinitions = getResultDefinitions("hearing-results/referencedata.query.referencedata.all-definitions.json");
        assertThat(resultDefinitions.size(), is(11));

        final Map<UUID, List<Amendments>> resultAmendmentsMap = extractAmendmentsDueToSlipRule(List.of(draftResultsWrapper), resultDefinitions, SLIP_RULE_AMENDMENT_REASON_ID);
        assertThat(resultAmendmentsMap.size(), is(1));

        final List<Amendments> deletedAmendment = resultAmendmentsMap.get(fromString("c6aa5a13-8e0e-4f78-9911-15a9282675e5"));
        assertThat(deletedAmendment.get(0).getAmendmentType(), is("DELETED"));
        assertThat(deletedAmendment.get(0).getAmendmentDate().toLocalDateTime().toString(), is("2025-02-28T14:40:10.262"));
        assertThat(deletedAmendment.get(0).getResultText(), is("Absolute discharge"));
    }

    @Test
    public void shouldReturnSharedAmendmentsWhenResultLinesOneOfDuration() {

        final List<ResultLine> resultLineWithAmendments = getResultLines("hearing-results/payload-hearing-get-draft-result-oneoff-duration.json").stream()
                .filter(resultLine -> nonNull(resultLine.getAmendmentsLog()) && isNotEmpty(resultLine.getAmendmentsLog().getAmendmentsRecord())).collect(toList());

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final ZonedDateTime lastSharedDate = ZonedDateTime.parse("2025-03-20T14:43:09.456Z");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(hearingId, hearingDay, resultLineWithAmendments, lastSharedDate);

        assertThat(resultLineWithAmendments.size(), is(17));

        final List<ResultDefinition> resultDefinitions = getResultDefinitions("hearing-results/referencedata.query.referencedata.result-definitions-with-oneof.json");

        final Map<UUID, List<Amendments>> resultAmendmentsMap = extractAmendmentsDueToSlipRule(List.of(draftResultsWrapper), resultDefinitions, SLIP_RULE_AMENDMENT_REASON_ID);
        assertThat(resultAmendmentsMap.size(), is(15));

    }

    @Test
    public void shouldReturnSharedAmendmentWhenAmendedMultipleTimes() {

        final List<ResultLine> resultLineWithAmendments = getResultLines("hearing-results/payload-hearing-get-draft-result-multi-amends.json").stream()
                .filter(resultLine -> nonNull(resultLine.getAmendmentsLog()) && isNotEmpty(resultLine.getAmendmentsLog().getAmendmentsRecord())).collect(toList());

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final ZonedDateTime lastSharedDate = ZonedDateTime.parse("2025-03-17T12:58:14.056Z");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(hearingId, hearingDay, resultLineWithAmendments, lastSharedDate);

        assertThat(resultLineWithAmendments.size(), is(3));

        final List<ResultDefinition> resultDefinitions = getResultDefinitions("hearing-results/referencedata.query.referencedata.result-definitions-multi-amends.json");

        final Map<UUID, List<Amendments>> resultAmendmentsMap = extractAmendmentsDueToSlipRule(List.of(draftResultsWrapper), resultDefinitions, SLIP_RULE_AMENDMENT_REASON_ID);
        assertThat(resultAmendmentsMap.size(), is(3));
        assertThat(resultAmendmentsMap.get(fromString("43a9d819-d806-4d80-96e2-b5639ce8b045")).get(0).getResultText(), is("Entered in error\nReason Test"));
        assertThat(resultAmendmentsMap.get(fromString("43a9d819-d806-4d80-96e2-b5639ce8b045")).get(1).getResultText(), is("Entered in error"));

    }

    @Test
    public void shouldReturnLatestSharedAmendmentWhenResultDeletedWithMultipleAmendments() {

        final List<ResultLine> resultLineWithAmendments = getResultLines("hearing-results/payload-hearing-get-draft-result-multi-amends-delete.json").stream()
                .filter(resultLine -> nonNull(resultLine.getAmendmentsLog()) && isNotEmpty(resultLine.getAmendmentsLog().getAmendmentsRecord())).collect(toList());

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final ZonedDateTime lastSharedDate = ZonedDateTime.parse("2025-03-24T14:01:21.878Z");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(hearingId, hearingDay, resultLineWithAmendments, lastSharedDate);

        assertThat(resultLineWithAmendments.size(), is(16));

        final List<ResultDefinition> resultDefinitions = getResultDefinitions("hearing-results/referencedata.query.referencedata.result-definitions-multi-amends-delete.json");

        final Map<UUID, List<Amendments>> resultAmendmentsMap = extractAmendmentsDueToSlipRule(List.of(draftResultsWrapper), resultDefinitions, SLIP_RULE_AMENDMENT_REASON_ID);
        assertThat(resultAmendmentsMap.size(), is(16));

        final List<Amendments> deletedAmendments = new ArrayList<>();
        resultAmendmentsMap.forEach((k, v) -> deletedAmendments.addAll(v.stream().filter(a -> a.getAmendmentType().equals(AmendmentType.DELETED.name())).toList()));
        assertThat(deletedAmendments.size(), is(16));
        assertThat(deletedAmendments.stream().filter(amendment -> "Payment terms".equals(amendment.getResultText())).count(), is(1L));
        assertThat(deletedAmendments.stream().filter(amendment -> "Payment Method".equals(amendment.getResultText())).count(), is(1L));
    }

    @Test
    public void shouldReturnSharedAmendmentsWhenResultLinesHaveYesBoxPromptType() {

        final List<ResultLine> resultLineWithAmendments = getResultLines("hearing-results/payload-hearing-get-draft-result-yesbox.json").stream()
                .filter(resultLine -> nonNull(resultLine.getAmendmentsLog()) && isNotEmpty(resultLine.getAmendmentsLog().getAmendmentsRecord())).collect(toList());

        final UUID hearingId = randomUUID();
        final LocalDate hearingDay = LocalDate.now();
        final ZonedDateTime lastSharedDate = ZonedDateTime.parse("2025-04-08T13:05:53.925Z");
        final DraftResultsWrapper draftResultsWrapper = new DraftResultsWrapper(hearingId, hearingDay, resultLineWithAmendments, lastSharedDate);

        assertThat(resultLineWithAmendments.size(), is(16));

        final List<ResultDefinition> resultDefinitions = getResultDefinitions("hearing-results/referencedata.query.referencedata.result-definitions-with-yesbox.json");

        final Map<UUID, List<Amendments>> resultAmendmentsMap = extractAmendmentsDueToSlipRule(List.of(draftResultsWrapper), resultDefinitions, SLIP_RULE_AMENDMENT_REASON_ID);
        assertThat(resultAmendmentsMap.size(), is(14));
        assertThat(resultAmendmentsMap.get(fromString("2e858c86-0743-42f7-a92f-dbcd7a9a9469")).get(0).getResultText(),
                is("Imprisonment\nImprisonment Period 6 Weeks\nConcurrent true\nConsecutive to offence test\nwhich is on case number 123445\nImprisonment reasons failure to comply with a pre-sentence drug testing order\nReason for custody because of an unprovoked attack of a serious nature\nNumber of days in custody in foreign jurisdiction to count 22\nBail remand days to count (tagged days) 12\nThis offence is aggravated by the foreign power condition being met in relation to it as defined by section 31 of the National Security Act 2023 No"));
    }

    private List<ResultLine> getResultLines(final String pathToDraftResultsJson) {
        final JsonObject jsonObject = FileUtil.jsonFromPath(pathToDraftResultsJson);
        return jsonObject.getJsonObject("resultLines").values().stream()
                .map(jsonValue -> jsonObjectToObjectConverter.convert(jsonValue.asJsonObject(), ResultLine.class)).toList();
    }

    private List<ResultDefinition> getResultDefinitions(final String jsonPath) {
        final JsonObject resultDefinitions = jsonFromPath(jsonPath);

        return resultDefinitions.getJsonArray(FIELD_RESULT_DEFINITIONS).stream()
                .map(respPayload -> jsonObjectToObjectConverter.convert(respPayload.asJsonObject(), ResultDefinition.class)).collect(toList());
    }

    private ResultLine.Builder getResultLineBuilder() {
        return ResultLine.resultLine().withCaseId(caseId).withDefendantId(defendantId).withOffenceId(offenceId);
    }


}