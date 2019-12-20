package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseProgressionFor;

import org.junit.Before;
import org.junit.Test;

public class AddCaseToCrownCourtIT extends AbstractIT {

    private String caseId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        // given
        addDefendant(caseId);
        // when
        addCaseToCrownCourt(caseId);
        // then
        pollCaseProgressionFor(caseId, withJsonPath("$.courtCentreId", is("e8821a38-546d-4b56-9992-ebdd772a561f")));
    }
}

