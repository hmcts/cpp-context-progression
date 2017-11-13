package uk.gov.moj.cpp.progression.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AuthorisationServiceStub.stubSetStatusForCapability;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.assignCaseForReview;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatResponseIndicatesFeatureDisabled;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class AssignCaseForReviewIT {

    private String caseId;
    private String caseProgressionId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        caseProgressionId = caseId;
        createMockEndpoints();

    }

    @Test
    public void shouldAssignCaseToReviewWithCapabilityEnabled() throws Exception {
        // given
        addDefendant(caseId);
        Response caseAddedToCrownCourtResponse = addCaseToCrownCourt(caseId, caseProgressionId);
        assertThatRequestIsAccepted(caseAddedToCrownCourtResponse);

        // when
        final Response writeResponse = assignCaseForReview(caseId, caseProgressionId);

        // then
        assertThatRequestIsAccepted(writeResponse);
        // and
        final String response = getCaseProgressionFor(caseId);
        final JsonObject responseJsonObject = getJsonObject(response);
        assertThat(responseJsonObject.getString("status"), equalTo("ASSIGNED_FOR_REVIEW"));
    }

    @Test
    public void shouldNotAssignCaseToReviewWithFeatureDisabled() throws Exception {
        // given
        givenAssignCaseToReviewFeatureDisabled();
        // and
        addCaseToCrownCourt(caseId, caseProgressionId);

        // when
        final Response writeResponse = assignCaseForReview(caseId, caseProgressionId);

        // then
        assertThatResponseIndicatesFeatureDisabled(writeResponse);
    }

    private void givenAssignCaseToReviewFeatureDisabled() {
        stubSetStatusForCapability("progression.command.case-assigned-for-review", false);
    }
}
