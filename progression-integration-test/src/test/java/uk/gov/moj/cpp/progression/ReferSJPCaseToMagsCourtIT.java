package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToMagsCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.referSJPCaseToMagsCourt;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCourtRoomById;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import org.junit.jupiter.api.Test;

public class ReferSJPCaseToMagsCourtIT extends AbstractIT {

    private String caseId;
    private String defendantId;


    @Test
    public void shouldGetProsecutionCaseAndVerifyVerdictFromSjp() throws Exception {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        addProsecutionCaseToMagsCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.offenceId", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.id", is("83a10a5f-5d26-32d3-9eb5-4463d0a2c89c")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.verdictCode", is("PSJ")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.cjsVerdictCode", is("G")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.categoryType", is("GUILTY"))
        )));
    }

    @Test
    public void shouldReferSJPCaseToCourtWithNextHearing() throws Exception {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        final String courtCentreId = randomUUID().toString();

        stubQueryCourtRoomById(courtCentreId);
        referSJPCaseToMagsCourt(caseId, defendantId, courtCentreId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.offenceId", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.id", is("83a10a5f-5d26-32d3-9eb5-4463d0a2c89c")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.verdictCode", is("PSJ")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.cjsVerdictCode", is("G")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].verdict.verdictType.categoryType", is("GUILTY"))
        )));
    }

}

