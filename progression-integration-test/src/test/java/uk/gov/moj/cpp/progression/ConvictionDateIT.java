package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;

import uk.gov.moj.cpp.progression.util.ConvictionDateHelper;

import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class ConvictionDateIT extends AbstractIT {

    private ConvictionDateHelper helper;
    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        final String offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        helper = new ConvictionDateHelper(caseId, offenceId);
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendant() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")));

        // when
        helper.addConvictionDate();

        // then
        helper.verifyInActiveMQForConvictionDateChanged();

        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].convictionDate", is("2017-02-02"))
        };
        pollProsecutionCasesProgressionFor(caseId, convictionAddedMatchers);

        helper.removeConvictionDate();

        helper.verifyInActiveMQForConvictionDateRemoved();

        pollProsecutionCasesProgressionFor(caseId, withoutJsonPath("$.prosecutionCase.defendants[0].offences[0].convictionDate"));
    }

}

