package uk.gov.moj.cpp.progression;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentCaseLevel;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentDefendantLevel;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetOrganisationById;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForIds;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;
import uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.jms.JMSException;

import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DocumentAddedEmailNotificationIT extends AbstractIT {
    private String caseId;
    private String docId;
    private String defendantId1;
    private String defendantId2;
    private String userId;

    private static final String USER_GROUP_NOT_PRESENT_DROOL = randomUUID().toString();
    private static final String USER_GROUP_NOT_PRESENT_RBAC = randomUUID().toString();

    @BeforeAll
    public static void init() {

        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_DROOL), "stub-data/usersgroups.get-invalid-groups-by-user.json");
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
    }

    @AfterAll
    public static void tearDown() throws JMSException {
        setupUsersGroupQueryStub();
    }


    @BeforeEach
    public void setup() {
        UsersAndGroupsStub.stubGetUsersAndGroupsQuery();
        HearingStub.stubInitiateHearing();
        NotificationServiceStub.setUp();
        IdMapperStub.setUp();
        stubGetOrganisationById(REST_RESOURCE_REF_DATA_GET_ORGANISATION_JSON);
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        userId = randomUUID().toString();
    }

    @Test
    public void shouldGenerateNotificationForCourtDocumentAddedByNonDefenceUserForCaseLevelDocumentForMultipleDefendants() throws IOException, JSONException {
        final String defendant1FirstName = randomAlphanumeric(10);
        final String defendant1LastName = randomAlphanumeric(10);

        final String defendant2FirstName = randomAlphanumeric(10);
        final String defendant2LastName = randomAlphanumeric(10);
        final List<String> organizationIds = Arrays.asList("fcb1edc9-786a-462d-9400-318c95c7c700", "fcb1edc9-786a-462d-9400-318c95c7b700");
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence-lawyers.json");
        stubForCaseDefendantsOrganisation("stub-data/defence.query.case-defendants-organisation.json", caseId, defendantId1, defendantId2, defendant1FirstName, defendant1LastName, defendant2FirstName, defendant2LastName);
        stubGetOrganisationDetailForIds("stub-data/usersgroups.get-organisations-details.json", organizationIds, userId);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentCaseLevel("expected/expected.progression.add-court-document-for-email-caselevel.json", caseId, docId);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList("joe2@example.com", getDefendantFullName(defendant1FirstName, defendant1LastName), getDefendantFullName(defendant2FirstName, defendant2LastName), "Magistrate's Sending sheet", "SJP Notice"), 1);
        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList("joe@example.com", getDefendantFullName(defendant1FirstName, defendant1LastName), getDefendantFullName(defendant2FirstName, defendant2LastName), "Magistrate's Sending sheet", "SJP Notice"), 1);
    }

    @Test
    public void shouldGenerateNotificationForCourtDocumentForDefendantLevelDocument() throws IOException, JSONException {
        final String defendant1FirstName = randomAlphanumeric(10);
        final String defendant1LastName = randomAlphanumeric(10);

        final String defendant2FirstName = randomAlphanumeric(10);
        final String defendant2LastName = randomAlphanumeric(10);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        final List<String> organizationIds = Arrays.asList("fcb1edc9-786a-462d-9400-318c95c7c700", "fcb1edc9-786a-462d-9400-318c95c7b700");
        ReferenceDataStub.stubQueryDocumentTypeData("/restResource/ref-data-document-type-for-defence-lawyers.json");
        stubForCaseDefendantsOrganisation("stub-data/defence.query.case-defendants-organisation.json", caseId, defendantId1, defendantId2, defendant1FirstName, defendant1LastName, defendant2FirstName, defendant2LastName);
        stubGetOrganisationDetailForIds("stub-data/usersgroups.get-organisations-details.json", organizationIds, userId);
        addCourtDocumentDefendantLevel("expected/expected.progression.add-court-document-for-email.json", docId, defendantId1, null, caseId);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList("joe@example.com", getDefendantFullName(defendant1FirstName, defendant1LastName), "Magistrate's Sending sheet", "SJP Notice"), 1);

    }

    private static String getDefendantFullName(final String defendantFirstName, final String defendantLastName) {
        if (isNullOrEmpty(defendantFirstName) && isNullOrEmpty(defendantLastName)) {
            return "";
        }
        return on(" ").join(defendantFirstName, defendantLastName);
    }
}