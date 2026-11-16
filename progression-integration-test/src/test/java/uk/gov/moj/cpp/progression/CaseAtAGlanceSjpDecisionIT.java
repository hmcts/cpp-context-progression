package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryResultDefinitionsByIds;
import static uk.gov.moj.cpp.progression.stub.ResultsStub.stubSjpDecisionResults;
import static uk.gov.moj.cpp.progression.stub.ResultsStub.verifySjpDecisionResultsNotQueried;
import static uk.gov.moj.cpp.progression.stub.SjpStub.stubSjpCaseWithDecisions;
import static uk.gov.moj.cpp.progression.stub.SjpStub.verifySjpCaseNotQueried;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.stub.SjpStub.SjpDecision;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CHD-3311: Case at a Glance shows the SJP decision of a case created from SJP (initiation code J) once an in-scope
 * application (here a statutory declaration, MC80528) is linked to it. The decisions come from SJP, their results from
 * the payloads Results stored, and the short codes from reference data.
 */
public class CaseAtAGlanceSjpDecisionIT extends AbstractIT {

    private static final String SJP_ORIGIN_CASE = "progression.command.initiate-court-proceedings.json";
    private static final String LINKED_APPLICATION = "applications/progression.initiate-court-proceedings-for-generic-linked-application.json";
    private static final String OUT_OF_SCOPE_APPLICATION_TYPE_CODE = "AS14518";
    private static final String STATUTORY_DECLARATION_TYPE_CODE = "MC80528";
    private static final String RESULT_DEFINITIONS = "stub-data/referencedata.query-result-definitions-by-ids.sjp.json";

    private static final String OFFENCE_RESULTS = "$.defendants[0].caagDefendantOffences[0].caagResults";
    private static final String DEFENDANT_RESULTS = "$.defendants[0].defendantJudicialResults";

    private String caseId;
    private String defendantId;
    private String applicationId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        applicationId = randomUUID().toString();
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        stubQueryResultDefinitionsByIds(RESULT_DEFINITIONS);
    }

    @Test
    public void shouldShowSjpDecisionResultsWithShortCodesWhenStatutoryDeclarationIsLinked() throws Exception {
        final String decisionId = randomUUID().toString();
        stubSjpCaseWithDecisions(caseId, singletonList(new SjpDecision(decisionId, "2026-01-05T10:00:00.000Z", false)));
        stubSjpDecisionResults(decisionId, caseId, defendantId, "2026-01-05", "2026-01-05T12:30:00.000Z", "100.00");

        createSjpOriginCase();
        linkApplication(STATUTORY_DECLARATION_TYPE_CODE);

        pollProsecutionCasesProgressionForCAAG(caseId,
                withJsonPath("$.linkedApplications[0].applicationId", equalTo(applicationId)),

                withJsonPath(OFFENCE_RESULTS + ".length()", is(1)),
                withJsonPath(OFFENCE_RESULTS + "[0].label", equalTo("Fine")),
                withJsonPath(OFFENCE_RESULTS + "[0].resultText", equalTo("FO - Fine\nAmount of fine £100.00")),
                withJsonPath(OFFENCE_RESULTS + "[0].useResultText", is(true)),
                withJsonPath(OFFENCE_RESULTS + "[0].orderedDate", equalTo("2026-01-05")),
                withJsonPath(OFFENCE_RESULTS + "[0].lastSharedDateTime", equalTo("2026-01-05T12:30:00.000Z")),
                withJsonPath(OFFENCE_RESULTS + "[0].caagResultPrompts[0].label", equalTo("Amount of fine")),
                withJsonPath(OFFENCE_RESULTS + "[0].caagResultPrompts[0].value", equalTo("£100.00")),

                withJsonPath("$.defendants[0].caagDefendantOffences[0].verdict.verdictDate", equalTo("2026-01-05")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].verdict.verdictType.categoryType", equalTo("GUILTY_CONVICTED")),

                withJsonPath(DEFENDANT_RESULTS + ".length()", is(1)),
                withJsonPath(DEFENDANT_RESULTS + "[0].resultText", equalTo("FCOST - Costs\nAmount of costs £85.00")),
                withJsonPath(DEFENDANT_RESULTS + "[0].lastSharedDateTime", equalTo("2026-01-05T12:30:00.000Z"))
        );
    }

    @Test
    public void shouldShowOnlyTheDecisionsMadeAfterTheLatestSetAside() throws Exception {
        final String originalDecisionId = randomUUID().toString();
        final String setAsideDecisionId = randomUUID().toString();
        final String reDecisionId = randomUUID().toString();
        stubSjpCaseWithDecisions(caseId, asList(
                new SjpDecision(originalDecisionId, "2026-01-05T10:00:00.000Z", false),
                new SjpDecision(setAsideDecisionId, "2026-02-01T10:00:00.000Z", true),
                new SjpDecision(reDecisionId, "2026-02-10T10:00:00.000Z", false)));
        stubSjpDecisionResults(originalDecisionId, caseId, defendantId, "2026-01-05", "2026-01-05T12:30:00.000Z", "100.00");
        stubSjpDecisionResults(reDecisionId, caseId, defendantId, "2026-02-10", "2026-02-10T12:30:00.000Z", "50.00");

        createSjpOriginCase();
        linkApplication(STATUTORY_DECLARATION_TYPE_CODE);

        pollProsecutionCasesProgressionForCAAG(caseId,
                withJsonPath("$.linkedApplications[0].applicationId", equalTo(applicationId)),
                withJsonPath(OFFENCE_RESULTS + ".length()", is(1)),
                withJsonPath(OFFENCE_RESULTS + "[0].resultText", equalTo("FO - Fine\nAmount of fine £50.00")),
                withJsonPath(OFFENCE_RESULTS + "[0].lastSharedDateTime", equalTo("2026-02-10T12:30:00.000Z")),
                withJsonPath(DEFENDANT_RESULTS + ".length()", is(1))
        );
        verifySjpDecisionResultsNotQueried(originalDecisionId);
        verifySjpDecisionResultsNotQueried(setAsideDecisionId);
    }

    @Test
    public void shouldNotQuerySjpWhenNoInScopeApplicationIsLinked() throws Exception {
        final String decisionId = randomUUID().toString();
        stubSjpCaseWithDecisions(caseId, singletonList(new SjpDecision(decisionId, "2026-01-05T10:00:00.000Z", false)));
        stubSjpDecisionResults(decisionId, caseId, defendantId, "2026-01-05", "2026-01-05T12:30:00.000Z", "100.00");

        createSjpOriginCase();
        linkApplication(OUT_OF_SCOPE_APPLICATION_TYPE_CODE);

        pollProsecutionCasesProgressionForCAAG(caseId,
                withJsonPath("$.linkedApplications[0].applicationId", equalTo(applicationId)),
                withoutJsonPath(OFFENCE_RESULTS + "[0]"),
                withoutJsonPath(DEFENDANT_RESULTS + "[0]")
        );
        verifySjpCaseNotQueried(caseId);
        verifySjpDecisionResultsNotQueried(decisionId);
    }

    private void createSjpOriginCase() throws Exception {
        initiateCourtProceedings(SJP_ORIGIN_CASE, caseId, defendantId, randomUUID().toString(), randomUUID().toString(),
                randomUUID().toString(), ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString(),
                ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString(), LocalDate.now().minusYears(30).toString());
        pollProsecutionCasesProgressionForCAAG(caseId, withJsonPath("$.caseId", equalTo(caseId)));
    }

    private void linkApplication(final String applicationTypeCode) {
        final String payload = getPayload(LINKED_APPLICATION)
                .replace("\"code\": \"" + OUT_OF_SCOPE_APPLICATION_TYPE_CODE + "\"", "\"code\": \"" + applicationTypeCode + "\"")
                .replace("APPLICATION_ID", applicationId)
                .replace("HEARING_ID", randomUUID().toString())
                .replaceAll("CASE_ID", caseId);
        initiateCourtProceedingsForCourtApplication(payload);
    }
}
