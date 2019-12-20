package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class ProsecutionCaseUpdateDefendantIT extends AbstractIT {

    ProsecutionCaseUpdateDefendantHelper helper;
    private String caseId;
    private String defendantId;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendant() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        // when
        helper.updateDefendant();

        // then
        helper.verifyInActiveMQ();

        Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
        };
        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        helper.verifyInMessagingQueueForDefendentChanged();
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantWithYouthFlagSetToTrue() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        helper.updateYouthFlagForDefendant();

        helper.verifyInActiveMQ();
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].isYouth", is(true)));
        helper.verifyInMessagingQueueForDefendentChanged();
    }

}

