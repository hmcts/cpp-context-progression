package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateCaseMarkersHelper;

import java.time.LocalDate;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class UpdateCaseMarkersIT extends AbstractIT {
    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS = "progression.command.initiate-court-proceedings.json";

    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    private ProsecutionCaseUpdateCaseMarkersHelper helper;

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();

        helper = new ProsecutionCaseUpdateCaseMarkersHelper(caseId);
    }

    @Test
    public void shouldUpdateProsecutionCaseMarkers() throws Exception {
        // given

        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("WP", "Prohibited Weapons"));

        helper.updateCaseMarkers();

        helper.verifyInActiveMQ();

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("DD", "Child Abuse"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated();
    }

    @Test
    public void shouldRemoveProsecutionCaseMarkers() throws Exception {
        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        pollProsecutionCasesProgressionFor(caseId, getCaseMarkersMatchers("WP", "Prohibited Weapons"));

        helper.removeCaseMarkers();

        helper.verifyInActiveMQ();

        pollProsecutionCasesProgressionFor(caseId, withoutJsonPath("$.prosecutionCase.caseMarkers"));

        helper.verifyInMessagingQueueForCaseMarkersUpdated();
    }

    private Matcher[] getCaseMarkersMatchers(final String caseMarkerCode, final String caseMarkerDesc) {
        return new Matcher[]{
                withJsonPath("$.prosecutionCase.caseMarkers[0].markerTypeCode", is(caseMarkerCode)),
                withJsonPath("$.prosecutionCase.caseMarkers[0].markerTypeDescription", is(caseMarkerDesc))
        };

    }
}
