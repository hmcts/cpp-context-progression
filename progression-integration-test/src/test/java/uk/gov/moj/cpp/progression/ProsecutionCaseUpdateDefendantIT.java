package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;


public class ProsecutionCaseUpdateDefendantIT extends AbstractIT {

    ProsecutionCaseUpdateDefendantHelper helper;
    private String caseId;
    private String defendantId;
    private static final String YOUTH_RESTRICTION = "Section 49 of the Children and Young Persons Act 1933 applies";


    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        helper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendant() throws Exception {
        // given
        final String policeBailStatusId = randomUUID().toString();
        final String policeBailStatusDesc = "police bail status description";
        final String policeBailConditions = "police bail conditions";

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        // when
        helper.updateDefendantWithPoliceBailInfo(policeBailStatusId, policeBailStatusDesc, policeBailConditions);

        // then
        helper.verifyInActiveMQ();

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailConditions", is(policeBailConditions)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.policeBailStatus.description", is("Remanded into Custody"))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        helper.verifyInMessagingQueueForDefendentChanged();
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantToYouth() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        // when
        helper.updateDefendant();

        // then
        helper.verifyInActiveMQ();

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
        };
        pollProsecutionCasesProgressionFor(caseId, defendantUpdatedMatchers);
        helper.verifyInMessagingQueueForDefendentChanged();
    }

    @Test
    public void shouldUpdateDefendantDetailsWithCustodyEstablishment() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")))));

        // when
        helper.updateDefendantWithCustody();

        // then
        helper.verifyInActiveMQ();

        final Matcher[] defendantUpdatedMatchers = new Matcher[]{
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("updatedName")),
                withJsonPath("$.prosecutionCase.defendants[0].pncId", is("1234567")),
                withJsonPath("$.prosecutionCase.defendants[0].aliases", hasSize(1)),
                withoutJsonPath("$.prosecutionCase.defendants[0].isYouth"),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.name", is("HMP Croydon Category A")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodialEstablishment.custody", is("Prison")),
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


    @Test
    public void shouldUpdateOffencesWithYouthReportingRestrictionsWhenDefendantAgeUpdatedToYouth() throws Exception {

        final List<Matcher<? super ReadContext>> additionalMatchersForNoRR = new ArrayList<>();
        additionalMatchersForNoRR.add(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth", is("1980-01-01")));
        additionalMatchersForNoRR.add((withoutJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions")));

        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, additionalMatchersForNoRR));

        final List<Matcher> additionalMatchersForDOBUpdated = new ArrayList<>();
        additionalMatchersForDOBUpdated.add(withJsonPath("$.prosecutionCase.id", is(caseId)));
        additionalMatchersForDOBUpdated.add(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")));
        additionalMatchersForDOBUpdated.add(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth", is("2010-01-01")));

        final List<Matcher> additionalMatchersForReportingRestrictionsAfterDOBUpdated = new ArrayList<>();
        additionalMatchersForReportingRestrictionsAfterDOBUpdated.add(withJsonPath("$.prosecutionCase.id", is(caseId)));
        additionalMatchersForReportingRestrictionsAfterDOBUpdated.add(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")));
        additionalMatchersForReportingRestrictionsAfterDOBUpdated.add(withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.dateOfBirth", is("2010-01-01")));
        additionalMatchersForReportingRestrictionsAfterDOBUpdated.add(withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is(YOUTH_RESTRICTION)));

        helper.updateDateOfBirthForDefendant(caseId, defendantId, LocalDate.of(2010, 01, 01));
        pollProsecutionCasesProgressionFor(caseId, additionalMatchersForDOBUpdated.toArray(new Matcher[additionalMatchersForDOBUpdated.size()]));
        helper.verifyInMessagingQueueForDefendentChanged();


    }

    @Test
    public void shouldUpdateDefendantDetailsWithHearingLanguageNeeds() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].id", is(defendantId)))));

        helper.updateDefendantWithHearingLanguageNeeds("ENGLISH");
        helper.verifyInActiveMQ();

        final Matcher[] matchers = {withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.hearingLanguageNeeds", is("ENGLISH"))};
        pollProsecutionCasesProgressionFor(caseId, matchers);
        helper.verifyInMessagingQueueForDefendentChanged();
    }
}