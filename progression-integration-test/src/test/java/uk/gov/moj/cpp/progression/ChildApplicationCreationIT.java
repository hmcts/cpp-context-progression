package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubGetHearingWithCourtApplications;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubUserWithPermission;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.jayway.jsonpath.ReadContext;
import io.restassured.response.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CHD-3025: backend enforcement of the child-application creation rules for applications created
 * from Applications At A Glance (AAAG) and Manage Hearings (MH).
 */
public class ChildApplicationCreationIT extends AbstractIT {

    private static final String INACTIVE_PARENT_FIXTURE = "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json";
    private static final String HEARING_APPLICATION_FIXTURE = "applications/progression.initiate-court-proceedings-for-application.json";
    private static final String PERMISSIONS_WITH_CREATE_CHILD_APPLICATION = "stub-data/usersgroups.get-logged-in-user-permissions-with-create-child-application.json";
    private static final String AAAG = "AAAG";
    private static final String MH = "MH";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubUserWithPermission(randomUUID().toString(), getPayload(PERMISSIONS_WITH_CREATE_CHILD_APPLICATION));
    }

    // ---- AAAG ----

    @Test
    public void shouldCreateChildApplicationFromAaagWhenParentIsInactiveAndRejectGrandchild() throws Exception {
        createCaseAndGetHearing();
        final String parentId = createInactiveParentApplication();

        final String childId = randomUUID().toString();
        final Response childResponse = initiate(payload(INACTIVE_PARENT_FIXTURE, childId, null, AAAG, parentId));

        assertThat(childResponse.getStatusCode(), is(SC_ACCEPTED));
        pollForApplication(childId,
                withJsonPath("$.courtApplication.id", is(childId)),
                withJsonPath("$.courtApplication.parentApplicationId", is(parentId)));
        pollForCourtProceedings(childId, withJsonPath("$.courtApplication.parentApplicationId", is(parentId)));

        final Response grandchildResponse = initiate(payload(INACTIVE_PARENT_FIXTURE, randomUUID().toString(), null, AAAG, childId));

        assertThat(grandchildResponse.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldRejectChildApplicationFromAaagWhenParentIsActive() throws Exception {
        final String hearingId = createCaseAndGetHearing();
        final String parentId = randomUUID().toString();
        final Response parentResponse = initiate(payload(HEARING_APPLICATION_FIXTURE, parentId, hearingId, null, null));
        assertThat(parentResponse.getStatusCode(), is(SC_ACCEPTED));
        pollForCourtProceedings(parentId, withJsonPath("$.courtApplication.id", is(parentId)));

        final Response childResponse = initiate(payload(INACTIVE_PARENT_FIXTURE, randomUUID().toString(), null, AAAG, parentId));

        assertThat(childResponse.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldRejectApplicationFromAaagWithoutParentApplicationId() {
        final Response response = initiate(payload(INACTIVE_PARENT_FIXTURE, randomUUID().toString(), null, AAAG, null));

        assertThat(response.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldRejectChildApplicationFromAaagWhenParentDoesNotExist() {
        final Response response = initiate(payload(INACTIVE_PARENT_FIXTURE, randomUUID().toString(), null, AAAG, randomUUID().toString()));

        assertThat(response.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldForbidChildApplicationFromAaagWithoutCreateChildApplicationPermission() throws Exception {
        createCaseAndGetHearing();
        final String parentId = createInactiveParentApplication();
        stubUserWithPermission(randomUUID().toString(), permissionsWithout());

        final Response response = initiate(payload(INACTIVE_PARENT_FIXTURE, randomUUID().toString(), null, AAAG, parentId));

        assertThat(response.getStatusCode(), is(SC_FORBIDDEN));
    }

    // ---- MH ----

    @Test
    public void shouldRejectApplicationFromMhWithoutCourtHearingId() {
        final Response response = initiate(payload(INACTIVE_PARENT_FIXTURE, randomUUID().toString(), null, MH, null));

        assertThat(response.getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldAllowOnlyParentApplicationFromMhWhenHearingHasSingleActiveApplication() throws Exception {
        final String hearingId = createCaseAndGetHearing();
        final String activeApplicationId = randomUUID().toString();
        stubGetHearingWithCourtApplications(hearingId, activeApplication(activeApplicationId));

        final Response childResponse = initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, activeApplicationId));
        assertThat(childResponse.getStatusCode(), is(SC_BAD_REQUEST));

        final String parentId = randomUUID().toString();
        final Response parentResponse = initiate(payload(HEARING_APPLICATION_FIXTURE, parentId, hearingId, MH, null));
        assertThat(parentResponse.getStatusCode(), is(SC_ACCEPTED));
        pollForApplication(parentId, withJsonPath("$.courtApplication.id", is(parentId)));
    }

    @Test
    public void shouldAllowOnlyChildApplicationFromMhWhenHearingHasSingleInactiveApplication() throws Exception {
        final String hearingId = createCaseAndGetHearing();
        final String parentId = createInactiveParentApplication();
        stubGetHearingWithCourtApplications(hearingId, inactiveApplication(parentId));

        final Response parentOnlyResponse = initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, null));
        assertThat(parentOnlyResponse.getStatusCode(), is(SC_BAD_REQUEST));

        final Response wrongParentResponse = initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, randomUUID().toString()));
        assertThat(wrongParentResponse.getStatusCode(), is(SC_BAD_REQUEST));

        final String childId = randomUUID().toString();
        final Response childResponse = initiate(payload(HEARING_APPLICATION_FIXTURE, childId, hearingId, MH, parentId));
        assertThat(childResponse.getStatusCode(), is(SC_ACCEPTED));
        pollForApplication(childId,
                withJsonPath("$.courtApplication.id", is(childId)),
                withJsonPath("$.courtApplication.parentApplicationId", is(parentId)));
    }

    @Test
    public void shouldRejectApplicationFromMhWhenHearingHasActiveAndInactiveApplications() {
        final String hearingId = randomUUID().toString();
        final String activeApplicationId = randomUUID().toString();
        final String inactiveApplicationId = randomUUID().toString();
        stubGetHearingWithCourtApplications(hearingId, activeApplication(activeApplicationId), inactiveApplication(inactiveApplicationId));

        assertThat(initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, null)).getStatusCode(), is(SC_BAD_REQUEST));
        assertThat(initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, activeApplicationId)).getStatusCode(), is(SC_BAD_REQUEST));
        assertThat(initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, inactiveApplicationId)).getStatusCode(), is(SC_BAD_REQUEST));
    }

    @Test
    public void shouldRejectChildOfChildApplicationFromMhWhenHearingHasOnlyChildApplication() {
        final String hearingId = randomUUID().toString();
        final String childApplicationId = randomUUID().toString();
        stubGetHearingWithCourtApplications(hearingId, childApplication(childApplicationId, randomUUID().toString()));

        assertThat(initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, null)).getStatusCode(), is(SC_BAD_REQUEST));
        assertThat(initiate(payload(HEARING_APPLICATION_FIXTURE, randomUUID().toString(), hearingId, MH, childApplicationId)).getStatusCode(), is(SC_BAD_REQUEST));
    }

    // ---- helpers ----

    private String createCaseAndGetHearing() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        return pollCaseAndGetHearingForDefendant(caseId, defendantId);
    }

    /**
     * Creates a court-order linked application (INACTIVE parent) on the current case with no applicationSource,
     * i.e. existing behaviour. The case must already exist.
     */
    private String createInactiveParentApplication() {
        final String parentId = randomUUID().toString();
        final Response response = initiate(payload(INACTIVE_PARENT_FIXTURE, parentId, null, null, null));
        assertThat(response.getStatusCode(), is(SC_ACCEPTED));
        pollForCourtProceedings(parentId,
                withJsonPath("$.courtApplication.id", is(parentId)),
                withJsonPath("$.courtApplication.courtOrder", notNullValue()));
        return parentId;
    }

    private static Response initiate(final String payload) {
        return initiateCourtProceedingsForCourtApplication(payload);
    }

    @SafeVarargs
    private static void pollForCourtProceedings(final String applicationId, final Matcher<? super ReadContext>... matchers) {
        pollForResponse("/court-proceedings/application/" + applicationId,
                "application/vnd.progression.query.court-proceedings-for-application+json",
                randomUUID().toString(),
                matchers);
    }

    private String payload(final String fixture, final String applicationId, final String hearingId,
                           final String applicationSource, final String parentApplicationId) {
        final String template = getPayload(fixture)
                .replace("APPLICATION_ID", applicationId)
                .replace("CASE_ID", caseId)
                .replace("DEFENDANT_ID", defendantId)
                .replace("MASTERDEFENDANTID", defendantId)
                .replace("HEARING_ID", nonNull(hearingId) ? hearingId : randomUUID().toString());
        final JsonObject initiate = stringToJsonObjectConverter.convert(template);

        final JsonObjectBuilder courtApplication = createObjectBuilder(initiate.getJsonObject("courtApplication"));
        if (nonNull(parentApplicationId)) {
            courtApplication.add("parentApplicationId", parentApplicationId);
        }
        final JsonObjectBuilder payload = createObjectBuilder(initiate).add("courtApplication", courtApplication);
        if (nonNull(applicationSource)) {
            payload.add("applicationSource", applicationSource);
        }
        return payload.build().toString();
    }

    private static JsonObject activeApplication(final String applicationId) {
        return linkedApplication(applicationId)
                .add("courtApplicationCases", createArrayBuilder().add(createObjectBuilder()
                        .add("prosecutionCaseId", randomUUID().toString())
                        .add("offences", createArrayBuilder().add(createObjectBuilder()
                                .add("id", randomUUID().toString())
                                .add("proceedingsConcluded", false)))))
                .build();
    }

    private static JsonObject inactiveApplication(final String applicationId) {
        return linkedApplication(applicationId)
                .add("courtOrder", createObjectBuilder().add("id", randomUUID().toString()))
                .build();
    }

    private static JsonObject childApplication(final String applicationId, final String parentApplicationId) {
        return linkedApplication(applicationId)
                .add("parentApplicationId", parentApplicationId)
                .build();
    }

    private static JsonObjectBuilder linkedApplication(final String applicationId) {
        return createObjectBuilder()
                .add("id", applicationId)
                .add("type", createObjectBuilder().add("linkType", "LINKED"));
    }

    private static String permissionsWithout() {
        return createObjectBuilder()
                .add("permissions", createArrayBuilder().add(createObjectBuilder()
                        .add("permissionId", randomUUID().toString())
                        .add("object", "Edit Standalone application")
                        .add("action", "Link")))
                .build()
                .toString();
    }
}
