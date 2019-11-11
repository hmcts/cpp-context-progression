package uk.gov.moj.cpp.progression;

import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertCaseMarkers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertCaseMarkersIsEmpty;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateCaseMarkersHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class UpdateCaseMarkersIT {

    private String caseId;
    private String courtDocumentId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    private ProsecutionCaseUpdateCaseMarkersHelper helper;

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        materialIdActive = UUID.randomUUID().toString();
        materialIdDeleted = UUID.randomUUID().toString();
        courtDocumentId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        referralReasonId = UUID.randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();

        helper = new ProsecutionCaseUpdateCaseMarkersHelper(caseId);
    }

    @Test
    public void shouldUpdateProsecutionCaseMarkers() throws Exception {
        // given

        createMockEndpoints();
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final String initialResponse = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutionCase = getProsecutionCaseJsonObject(initialResponse);
        assertCaseMarkers(prosecutionCase, "WP", "Prohibited Weapons");

        helper.updateCaseMarkers();

        helper.verifyInActiveMQ();

        final String updatedResponse = getProsecutioncasesProgressionFor(caseId);
        final JsonObject updatedProsecutionCase = getProsecutionCaseJsonObject(updatedResponse);
        assertCaseMarkers(updatedProsecutionCase, "DD", "Child Abuse");

        helper.verifyInMessagingQueueForCaseMarkersUpdated();
    }

    @Test
    public void shouldRemoveProsecutionCaseMarkers() throws Exception {
        // given

        createMockEndpoints();
        //given
        initiateCourtProceedings(caseId, defendantId, materialIdActive, materialIdDeleted, courtDocumentId, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        final String initialResponse = getProsecutioncasesProgressionFor(caseId);
        final JsonObject prosecutionCase = getProsecutionCaseJsonObject(initialResponse);
        assertCaseMarkers(prosecutionCase, "WP", "Prohibited Weapons");

        helper.removeCaseMarkers();

        helper.verifyInActiveMQ();

        final String updatedResponse = getProsecutioncasesProgressionFor(caseId);
        System.out.println(updatedResponse);
        final JsonObject updatedProsecutionCase = getProsecutionCaseJsonObject(updatedResponse);
        assertCaseMarkersIsEmpty(updatedProsecutionCase);

        helper.verifyInMessagingQueueForCaseMarkersUpdated();
    }

    private JsonObject getProsecutionCaseJsonObject(final String response) {
        final JsonObject prosecutioncasesJsonObject = getJsonObject(response);
        return prosecutioncasesJsonObject.getJsonObject("prosecutionCase");
    }
}
