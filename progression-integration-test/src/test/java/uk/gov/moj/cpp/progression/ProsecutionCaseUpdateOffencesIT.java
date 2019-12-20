package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.io.IOException;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseUpdateOffencesIT extends AbstractIT {

    private ProsecutionCaseUpdateOffencesHelper helper;
    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId);
    }

    @Test
    public void shouldUpdateProsecutionCaseOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );

        pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);

        // when
        helper.updateOffences();

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TEST")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1)));
    }

    @Test
    public void shouldUpdateProsecutionCaseAddDeleteOffences() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TTH105HY")))
        );
        pollProsecutionCasesProgressionFor(caseId, caseWithOffenceMatchers);

        final String newOffenceId = randomUUID().toString();

        // when
        helper.updateOffences(newOffenceId);

        // then
        helper.verifyInActiveMQ();
        helper.verifyInMessagingQueueForOffencesUpdated();

        final Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceCode", is("TEST")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(newOffenceId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].count", is(1))
        };
        pollProsecutionCasesProgressionFor(caseId, matchers);
    }
}

