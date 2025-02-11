package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.stub.ListingStub.getPostListCourtHearing;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.progression.AbstractIT;

import java.nio.charset.Charset;

import com.google.common.io.Resources;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class CourtAppealApplicationIT extends AbstractIT {

    @Test
    public void shouldCreateLinkedApplicationForCourtAppeal() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-court-appeal-application.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.type.appealFlag", is(true)),
                withJsonPath("$.courtApplication.type.courtOfAppealFlag", is(true)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of Time reason for Court appeal"))
        };

        final String applicationPayload = pollForCourtApplication(applicationId, applicationMatchers);
        final String applicationReference = new StringToJsonObjectConverter().convert(applicationPayload).getJsonObject("courtApplication").getString("applicationReference");

        String expectedListingRequest = Resources.toString(Resources.getResource("expected/expected.progression.application-referred-to-court-hearing.json"), Charset.defaultCharset());
        String listingRequest = getPostListCourtHearing(applicationId);
        assertEquals(expectedListingRequest, listingRequest, getCustomComparator(applicationId, applicationReference));
    }

    @Test
    public void shouldCreateStandAloneApplicationForCourtAppeal() throws Exception {
        final String applicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-stand-alone-court-appeal-application.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.type.appealFlag", is(true)),
                withJsonPath("$.courtApplication.type.courtOfAppealFlag", is(true)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of Time reason for Court appeal"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    private CustomComparator getCustomComparator(String applicationId, String applicationReference) {
        return new CustomComparator(STRICT,
                new Customization("hearings[0].id", (o1, o2) -> o1 != null && o2 != null),
                new Customization("hearings[0].courtApplications[0].id", (o1, o2) -> applicationId.equals(o1)),
                new Customization("hearings[0].courtApplications[0].applicationReference", (o1, o2) -> applicationReference.equals(o1))
        );
    }
}
