package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentCaseLevel;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentDefendantLevel;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupUsersGroupQueryStub;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForIds;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;
import uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.base.Strings;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DocumentAddedEmailNotificationIT extends AbstractIT {
    private String caseId;
    private String docId;
    private String defendantId1;
    private String defendantId2;
    private String userId;

    private static final String USER_GROUP_NOT_PRESENT_DROOL = UUID.randomUUID().toString();
    private static final String USER_GROUP_NOT_PRESENT_RBAC = UUID.randomUUID().toString();
    private MessageConsumer consumerForCourDocumentSendToCps;
    private MessageConsumer consumerForProgressionCommandEmail;
    private MessageProducer messageProducerClientPublic;
    private MessageConsumer publicEventConsumer;

    @BeforeClass
    public static void init() {

        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_DROOL), "stub-data/usersgroups.get-invalid-groups-by-user.json");
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        setupUsersGroupQueryStub();
    }

    @After
    public void tearDownQueues() throws JMSException {
        consumerForCourDocumentSendToCps.close();
        consumerForProgressionCommandEmail.close();
        messageProducerClientPublic.close();
        publicEventConsumer.close();
    }


    @Before
    public void setup() {
        UsersAndGroupsStub.stubGetUsersAndGroupsQuery();
        HearingStub.stubInitiateHearing();
        NotificationServiceStub.setUp();
        IdMapperStub.setUp();
        ReferenceDataStub.stubGetOrganisationById("/restResource/ref-data-get-organisation.json");
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        userId = randomUUID().toString();

        consumerForCourDocumentSendToCps = privateEvents.createPrivateConsumer("progression.event.court-document-send-to-cps");
        consumerForProgressionCommandEmail = privateEvents.createPrivateConsumer("progression.event.email-requested");
        messageProducerClientPublic = publicEvents.createPublicProducer();
        publicEventConsumer = publicEvents.createPublicConsumer("public.progression.court-document-added");
    }

    @Test
    public void shouldGenerateNotificationForCourtDocumentAddedByNonDefenceUserForCaseLevelDocumentForMultipleDefendants() throws IOException {
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


        verifyInMessagingQueueForPublicCourtDocumentAdded();
        verifyInMessagingQueueForEmailSendForDocumentAdded(caseId, defendant1FirstName, defendant1LastName, defendant2FirstName, defendant2LastName, false);
    }

    @Test
    public void shouldGenerateNotificationForCourtDocumentForDefendantLevelDocument() throws IOException {
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


        verifyInMessagingQueueForPublicCourtDocumentAdded();
        verifyInMessagingQueueForEmailSendForDocumentAdded(caseId, defendant1FirstName, defendant1LastName, null, null, true);

    }

    private void verifyInMessagingQueueForPublicCourtDocumentAdded() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(publicEventConsumer);
        assertThat(message, notNullValue());
    }

    private void verifyInMessagingQueueForEmailSendForDocumentAdded(final String caseId, final String defendant1FirstName, final String defendant1LastName, final String defendant2FirstName, final String defendant2LastName, final boolean expectSeperrateEmails) {
        final Optional<JsonObject> messageOptional = QueueUtil.retrieveMessageAsJsonObject(consumerForProgressionCommandEmail);
        messageOptional.ifPresent(message -> {
            if ((expectSeperrateEmails)) {
                checkMultipleNotificationsSeperately(caseId, defendant1FirstName, defendant1LastName, defendant2FirstName, defendant2LastName, message);
            } else {
                checkMultipleNotificationsInSameCall(caseId, defendant1FirstName, defendant1LastName, defendant2FirstName, defendant2LastName, message);
            }
        });
    }

    private static void checkMultipleNotificationsInSameCall(final String caseId, final String defendant1FirstName, final String defendant1LastName, final String defendant2FirstName, final String defendant2LastName, final JsonObject message) {
        checkCommonFields(caseId, message);
        checkMultipleDefendantsInSingleNotification(message, getDefendantFullName(defendant1FirstName, defendant1LastName), getDefendantFullName(defendant2FirstName, defendant2LastName));
    }

    private void checkMultipleNotificationsSeperately(final String caseId, final String defendant1FirstName, final String defendant1LastName, final String defendant2FirstName, final String defendant2LastName, final JsonObject message) {
        checkCommonFields(caseId, message);
        checkSingleDefendantInSingleNotification(message, getDefendantFullName(defendant1FirstName, defendant1LastName), getDefendantFullName(defendant2FirstName, defendant2LastName));
        if(!Strings.isNullOrEmpty(defendant2FirstName)) {
            final Optional<JsonObject> messageOptional2 = QueueUtil.retrieveMessageAsJsonObject(consumerForProgressionCommandEmail);
            messageOptional2.ifPresent(message2 -> {
                checkCommonFields(caseId, message2);
                checkSingleDefendantInSingleNotification(message2, getDefendantFullName(defendant1FirstName, defendant1LastName), getDefendantFullName(defendant2FirstName, defendant2LastName));
            });
        }
    }


    private static void checkMultipleDefendantsInSingleNotification(final JsonObject message, final String defendantFullName, final String defendantFullName2) {
        assertThat(message, allOf(
                isJson(withJsonPath("$.notifications[0].personalisation.defendant_list", Matchers.hasToString(Matchers.containsString(defendantFullName)))),
                isJson(withJsonPath("$.notifications[0].personalisation.defendant_list", Matchers.hasToString(Matchers.containsString(defendantFullName2))))));
    }

    private static void checkCommonFields(final String caseId, final JsonObject message) {
        assertThat(message, notNullValue());
        assertThat(message, isJson(withJsonPath("$.caseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
        assertThat(message, isJson(withJsonPath("$.notifications[0].personalisation.DOCUMENT_SECTION",
                Matchers.hasToString(Matchers.containsString("Magistrate's Sending sheet")))));
        assertThat(message, isJson(withJsonPath("$.notifications[0].personalisation.DOCUMENT_TITLE",
                Matchers.hasToString(Matchers.containsString("SJP Notice")))));
    }

    private static void checkSingleDefendantInSingleNotification(final JsonObject message, final String defendantFullName, final String defendantFullName2) {
        assertThat(message, anyOf(
                isJson(withJsonPath("$.notifications[0].personalisation.defendant_list", Matchers.hasToString(Matchers.containsString(defendantFullName)))),
                isJson(withJsonPath("$.notifications[0].personalisation.defendant_list", Matchers.hasToString(Matchers.containsString(defendantFullName2))))));
    }

    private static String getDefendantFullName(final String defendant1FirstName, final String defendant1LastName) {
        return defendant1FirstName + " " + defendant1LastName;
    }


    private void verifyForProgressionCommandEmail() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForProgressionCommandEmail);
        assertThat(message, notNullValue());
        assertThat(message.get(), isJson(withJsonPath("$.caseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
        assertThat(message.get(), isJson(withJsonPath("$.notifications[0].templateId",
                Matchers.hasToString(Matchers.containsString("85fe515a-ff7a-4d16-acbd-cf93d0c75f57")))));
        assertThat(message.get(), isJson(withJsonPath("$.notifications[0].sendToAddress",
                Matchers.hasToString(Matchers.containsString("abc@xyz.co.uk")))));
    }
}





