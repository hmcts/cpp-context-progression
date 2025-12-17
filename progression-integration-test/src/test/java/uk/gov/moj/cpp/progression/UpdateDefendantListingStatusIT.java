package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateDefendantListingStatusChanged;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdateDefendantListingStatusIT extends AbstractIT {

    private String hearingId;
    private String caseId;
    private String defendantId;


    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldSetJudiciaryResultsAtHearingsLevelForHearingAtAGlance() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        updateDefendantListingStatusChanged(hearingId, "progression.update-defendant-listing-status.json");
        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearingListingStatus", is("SENT_FOR_LISTING")),
                withJsonPath("$.hearing.jurisdictionType", is("CROWN"))
        );
    }
}

