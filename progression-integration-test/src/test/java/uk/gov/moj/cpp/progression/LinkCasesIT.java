package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.deleteRelatedReference;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCaseLsmInfoFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getRelatedReference;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.linkCases;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.mergeCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.splitCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.unlinkCases;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.LSMCasesHelper.getLsmQueryMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class LinkCasesIT extends AbstractIT {

    public static final String LINKED_CASES = "linkedCases";
    public static final String MERGED_CASES = "mergedCases";
    public static final String SPLIT_CASES = "splitCases";
    private final String linkCasesPayloadJson = "progression.command.link-cases.json";
    private final String mergeCasePayloadJson = "progression.command.merge-cases.json";
    private final String splitCasesPayloadJson = "progression.command.split-cases.json";
    private final String unlinkCasesPayloadJson = "progression.command.unlink-cases.json";

    private static final String INITIAL_COURT_PROCEEDINGS_WITH_MULTIPLE_DEFENDANTS = "ingestion/progression.command.initiate-court-proceedings-multiple-defendants.json";
    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_WITH_RELATED_URN_JSON = "ingestion/progression.command.initiate-court-proceedings-with-related-urn.json";

    private String prosecutionCaseId_1;
    private String caseUrn_1;
    private String defendantId_1;
    private String prosecutionCaseId_2;
    private String caseUrn_2;
    private String defendantId_2;
    private String prosecutionCaseId_3;
    private String caseUrn_3;
    private String defendantId_3;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;


    @Before
    public void setUp() {
        stubInitiateHearing();
        prosecutionCaseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        caseUrn_1 = generateUrn();
        prosecutionCaseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        caseUrn_2 = generateUrn();
        prosecutionCaseId_3 = randomUUID().toString();
        defendantId_3 = randomUUID().toString();
        caseUrn_3 = generateUrn();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();

    }

    @Test
    public void shouldLinkUnlinkMergeAndSplitCases() throws IOException {
        // initiation of cases
        initiateCourtProceedings(INITIAL_COURT_PROCEEDINGS_WITH_MULTIPLE_DEFENDANTS, prosecutionCaseId_1, defendantId_1, randomUUID().toString(), materialIdActive, materialIdDeleted, referralReasonId, caseUrn_1, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, emptyList()));

        initiateCourtProceedings(INITIAL_COURT_PROCEEDINGS_WITH_MULTIPLE_DEFENDANTS, prosecutionCaseId_2, defendantId_2, randomUUID().toString(), materialIdActive, materialIdDeleted, referralReasonId, caseUrn_2, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList()));

        initiateCourtProceedings(INITIAL_COURT_PROCEEDINGS_WITH_MULTIPLE_DEFENDANTS, prosecutionCaseId_3, defendantId_3, randomUUID().toString(), materialIdActive, materialIdDeleted, referralReasonId, caseUrn_3, listedStartDateTime, earliestStartDateTime, defendantDOB);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_3, getProsecutionCaseMatchers(prosecutionCaseId_3, defendantId_3, emptyList()));


        //link cases
        linkCases(prosecutionCaseId_1, caseUrn_2, caseUrn_3, linkCasesPayloadJson);


        final Matcher[] lsmResponseMatchers = getLsmQueryMatchers(LINKED_CASES,
                2,
                new String[]{prosecutionCaseId_2, prosecutionCaseId_3},
                new String[]{defendantId_2, defendantId_3});

        final JsonObject responseAsJson = new StringToJsonObjectConverter().convert(getCaseLsmInfoFor(prosecutionCaseId_1, lsmResponseMatchers));
        final String caseId = responseAsJson.getJsonArray(LINKED_CASES).getJsonObject(0).getString("caseId");

        final String linkGroupId;
        if (caseId.equals(prosecutionCaseId_2)) {
            linkGroupId = responseAsJson.getJsonArray(LINKED_CASES).getJsonObject(0).getString("linkGroupId");
        } else {
            linkGroupId = responseAsJson.getJsonArray(LINKED_CASES).getJsonObject(1).getString("linkGroupId");
        }

        //shouldUnlink...
        unlinkCases(prosecutionCaseId_1, caseUrn_1, prosecutionCaseId_2, caseUrn_2, linkGroupId, unlinkCasesPayloadJson);

        final Matcher[] lsmResponseMatchersAfterUnlink = getLsmQueryMatchers(LINKED_CASES,
                1,
                new String[]{prosecutionCaseId_3},
                new String[]{defendantId_3});

        getCaseLsmInfoFor(prosecutionCaseId_1, lsmResponseMatchersAfterUnlink);

        //should merge
        mergeCase(prosecutionCaseId_1, caseUrn_3, mergeCasePayloadJson);

        final Matcher[] lsmResponseMatchers_2 = getLsmQueryMatchers(MERGED_CASES,
                1,
                new String[]{prosecutionCaseId_3},
                new String[]{defendantId_3});

        getCaseLsmInfoFor(prosecutionCaseId_1, lsmResponseMatchers_2);

        //shouldMergeIntoSameCaseMultipleTimes..
        mergeCase(prosecutionCaseId_2, caseUrn_3, mergeCasePayloadJson);

        splitCase(prosecutionCaseId_1, caseUrn_1 + "/1", caseUrn_1 + "/2", splitCasesPayloadJson);

        final Matcher[] lsmSplitResponseMatchers = getLsmQueryMatchers(SPLIT_CASES,
                1,
                new String[]{prosecutionCaseId_1},
                new String[]{defendantId_1});
    }

    @Test
    public void shouldRelatedReferenceUrnOnCaseCreation() throws IOException {

        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS_WITH_RELATED_URN_JSON, prosecutionCaseId_2, defendantId_2, randomUUID().toString(), materialIdActive, materialIdDeleted, referralReasonId, caseUrn_2, listedStartDateTime, earliestStartDateTime, defendantDOB, caseUrn_1);
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList()));

        final List<Matcher> matchers = new ArrayList<>();
        matchers.add(withJsonPath("$.relatedReferenceList[0].prosecutionCaseId", is(prosecutionCaseId_2)));
        final Matcher[] relatedReferenceMatchers = matchers.toArray(new Matcher[matchers.size()]);

        JsonObject responseAsJson = new StringToJsonObjectConverter().convert(getRelatedReference(prosecutionCaseId_2, relatedReferenceMatchers));
        final String relatedReference = responseAsJson.getJsonArray("relatedReferenceList").getJsonObject(0).getString("relatedReference");
        final String relatedReferenceId = responseAsJson.getJsonArray("relatedReferenceList").getJsonObject(0).getString("relatedReferenceId");

        assertThat(relatedReference, is(caseUrn_1));

        deleteRelatedReference(prosecutionCaseId_2, relatedReferenceId);

    }
}
