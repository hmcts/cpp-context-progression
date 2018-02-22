package uk.gov.moj.cpp.progression.it;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class AddCaseToCrownCourtIT {

    private String caseId;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        createMockEndpoints();
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        // given
        addDefendant(caseId);
        // when
        addCaseToCrownCourt(caseId);
        // then
        final String response = getCaseProgressionFor(caseId);
        final JsonObject defendantsJsonObject = getJsonObject(response);
        assertThat(defendantsJsonObject.getString("courtCentreId"), equalTo("e8821a38-546d-4b56-9992-ebdd772a561f"));
    }
}

