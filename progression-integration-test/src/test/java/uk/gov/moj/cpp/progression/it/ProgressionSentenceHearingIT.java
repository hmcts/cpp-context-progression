package uk.gov.moj.cpp.progression.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.AbstractIT;

import java.io.IOException;
import java.time.LocalDate;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

public class ProgressionSentenceHearingIT extends AbstractIT {


    private String caseId;
    private final LocalDate futureDate = LocalDate.now().plusDays(10);

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
    }

    @Test
    public void shouldAddSentenceHearing() throws Exception {
        addDefendant(caseId);

        Response writeResponse = postCommand(getWriteUrl("/cases/" + caseId),
                "application/vnd.progression.command.sentence-hearing-date+json",
                getJsonBodyStr("progression.command.sentence-hearing-date.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        pollCaseProgressionFor(caseId,
                withJsonPath("$.sentenceHearingDate", is(LocalDate.now().toString()))
        );


        writeResponse = postCommand(getWriteUrl("/cases/" + caseId),
                "application/vnd.progression.command.sentence-hearing-date+json",
                getJsonBodyForHearingDate("progression.command.sentence-hearing-date.json"));
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        pollCaseProgressionFor(caseId,
                withJsonPath("$.sentenceHearingDate", is(futureDate.toString()))
        );

    }


    private String getJsonBodyStr(final String fileName) throws IOException {
        return getPayload(fileName)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("TODAY", LocalDate.now().toString());
    }

    private String getJsonBodyForHearingDate(final String fileName) throws IOException {
        return getPayload(fileName)
                .replace("TODAY", futureDate.toString());
    }

}
