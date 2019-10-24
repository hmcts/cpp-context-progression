package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertcourtDocuments;

import uk.gov.justice.services.common.converter.ZonedDateTimes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;


public class InitiateCourtProceedingsIT {

    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        materialIdActive = UUID.randomUUID().toString();
        materialIdDeleted = UUID.randomUUID().toString();
        courtDocumentId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        referralReasonId = UUID.randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
    }

    @Test
    public void shouldInitiateCourtProceedingsWithCourtDocuments() throws IOException {
        createMockEndpoints();
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when

        //introduce delay by checking court document present first
        getCourtDocumentFor(courtDocumentId,
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(courtDocumentId))
        );

        final String response = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        //then
        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
        assertcourtDocuments(prosecutionCasesJsonObject.getJsonArray("courtDocuments").getJsonObject(0), caseId, courtDocumentId, materialIdActive);
    }

    @Test
    public void shouldInitiateCourtProceedingsWithDefendantIsYouth() throws IOException {
        createMockEndpoints();
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyPostListCourtHearing(caseId, defendantId, true);
    }

    @Test
    public void shouldInitiateCourtProceedingsWithDefendantIsNotYouth() throws IOException {
        createMockEndpoints();
        defendantDOB = LocalDate.now().minusYears(25).toString();
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyPostListCourtHearing(caseId, defendantId, false);
    }

    @Test
    public void shouldInitiateCourtProceedingsNoCourtDocuments() throws IOException {
        createMockEndpoints();
        //given
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when
        final String response = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        //then
        assertProsecutionCase(prosecutionCasesJsonObject.getJsonObject("prosecutionCase"), caseId, defendantId);
    }

}

