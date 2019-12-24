package uk.gov.moj.cpp.progression.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.AbstractIT;

import java.time.LocalDate;

import com.jayway.restassured.response.Response;
import org.junit.Before;
import org.junit.Test;

public class ProgressionIT extends AbstractIT {

    private String caseId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
    }

    @Test
    public void shouldAddCaseToCrownCourt() throws Exception {
        addDefendant(caseId);
        Response writeResponse = addCaseToCrownCourt(caseId);
        assertThatRequestIsAccepted(writeResponse);

        pollCaseProgressionFor(caseId);

        writeResponse = postCommand(getWriteUrl("/cases/" + caseId),
                "application/vnd.progression.command.sending-committal-hearing-information+json",
                getJsonBodyStr("progression.command.sending-committal-hearing-information.json"));
        assertThatRequestIsAccepted(writeResponse);


        pollCaseProgressionFor(caseId,
                withJsonPath("$.sendingCommittalDate", is(LocalDate.now().toString())));

        writeResponse = postCommand(getWriteUrl("/cases/" + caseId),
                "application/vnd.progression.command.sentence-hearing-date+json",
                getJsonBodyStr("progression.command.sentence-hearing-date.json"));
        assertThatRequestIsAccepted(writeResponse);


        pollCaseProgressionFor(caseId,
                withJsonPath("$.sentenceHearingDate", is(LocalDate.now().toString())),
                withJsonPath("$.status", is("INCOMPLETE"))
        );
    }


    private String getJsonBodyStr(final String fileName) {
        return getPayload(fileName)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("DEF_ID_1", randomUUID().toString())
                .replace("DEF_ID_2", randomUUID().toString())
                .replace("TODAY", LocalDate.now().toString());
    }
}
