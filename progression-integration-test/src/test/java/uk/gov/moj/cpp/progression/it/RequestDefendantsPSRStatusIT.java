package uk.gov.moj.cpp.progression.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.join;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;

import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.StubUtil;

import java.io.IOException;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class RequestDefendantsPSRStatusIT extends AbstractIT {

    private static final String PROGRESSION_QUERY_DEFENDANT_MEDIA_TYPE = "application/vnd.progression.query.defendant+json";
    private String caseId;
    private String firstDefendantId;
    private String secondDefendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        firstDefendantId = randomUUID().toString();
        secondDefendantId = randomUUID().toString();
    }

    @Test
    public void shouldRequestPSRForDefendant() throws Exception {
        addDefendant(caseId, firstDefendantId);
        addDefendant(caseId, secondDefendantId);

        pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                PROGRESSION_QUERY_DEFENDANT_MEDIA_TYPE);

        final Response writeResponse = postCommand(getWriteUrl("/cases/" + caseId + "/defendants/requestpsr"),
                "application/vnd.progression.command.request-psr-for-defendants+json",
                StubUtil.getJsonBodyStr(
                        "progression.command.request-psr-for-defendants.json", caseId, firstDefendantId, secondDefendantId));

        assertThatRequestIsAccepted(writeResponse);

        pollForResponse(join("", "/cases/", caseId, "/defendants/", firstDefendantId),
                PROGRESSION_QUERY_DEFENDANT_MEDIA_TYPE,
                withJsonPath("$.additionalInformation.probation.preSentenceReport.psrIsRequested", is(false))
        );


        pollForResponse(join("", "/cases/", caseId, "/defendants/", secondDefendantId),
                PROGRESSION_QUERY_DEFENDANT_MEDIA_TYPE,
                withJsonPath("$.additionalInformation.probation.preSentenceReport.psrIsRequested", is(true))
        );
    }

}
