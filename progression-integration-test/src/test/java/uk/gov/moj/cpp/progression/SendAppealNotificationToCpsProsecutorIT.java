package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.POST_CODE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForApplicationAtAGlance;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesWithCourtOrdersFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.sendNotification;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorDataForGivenProsecutionAuthorityId;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import java.time.LocalDate;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that when a notification is sent for an appeal court application, the CPS prosecutor currently on the
 * linked prosecution case is emailed on its CPS CC email address.
 * <p>
 * The flow exercised is:
 * <ol>
 *     <li>A prosecution case is created, a hearing confirmed, and its prosecutor is updated to a CPS prosecutor
 *         ({@code prosecutor.isCps == true}).</li>
 *     <li>An appeal court application is initiated ({@code type.appealFlag == true}) linked to that case, so the
 *         application aggregate holds the {@code CourtApplicationProceedingsInitiated} state that
 *         {@code ApplicationAggregate.sendNotificationForApplication} requires.</li>
 *     <li>An appeal application notification is raised. {@code NotificationService#sendNotificationToProsecutor}
 *         resolves the current prosecutor via {@code getCurrentProsecutor}, which issues the internal
 *         {@code progression.query.prosecutioncase} query and reads {@code prosecutor.prosecutorId} /
 *         {@code prosecutor.isCps}.</li>
 *     <li>Because the prosecutor is a CPS prosecutor, {@code CourtApplicationService#getProsecutingAuthority}
 *         is called with {@code isCps == true} and the CPS CC email address is used as the recipient.</li>
 * </ol>
 */
public class SendAppealNotificationToCpsProsecutorIT extends AbstractIT {

    // Must match the "prosecutionAuthorityId" in progression.update-cps-prosecutor-details.json - this is the
    // prosecutorId that CaseProsecutorUpdateHelper stamps onto the case as the CPS prosecutor.
    private static final String CPS_PROSECUTOR_ID = "df73207f-3ced-488a-82a0-3fba79c2ce95";
    private static final String CPS_PROSECUTOR_OU_CODE = "B01EF01";
    private static final String CPS_CC_EMAIL_ADDRESS = "cps-cc@cps.gov.uk";
    private static final String CPS_PROSECUTOR_REF_DATA = "restResource/referencedata.query.cps.prosecutor.with.cc.email.json";
    private static final String CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application-send-notification.json";
    private static final String SEND_NOTIFICATION_JSON = "progression.command.send-notification-for-application.json";
    private static final String HEARING_CONFIRMED_EVENT = "public.listing.hearing-confirmed";
    private static final String HEARING_CONFIRMED_JSON = "public.listing.hearing-confirmed-for-group-cases.json";

    private final JmsMessageProducerClient messageProducerClientPublic =
            newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String hearingId;
    private String parentApplicationId;
    private String applicationId;
    private String respondentDefendantId;
    // The informant authority is a different id to the CPS prosecutor so the prosecutor notification is not
    // de-duplicated against the informant notification.
    private String informantAuthorityId;

    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        parentApplicationId = randomUUID().toString();
        applicationId = randomUUID().toString();
        respondentDefendantId = randomUUID().toString();
        informantAuthorityId = randomUUID().toString();

        setupLoggedInUsersPermissionQueryStub();
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", respondentDefendantId);

        // Reference-data lookup for the CPS prosecutor returns a response carrying a CPS CC email address. This is
        // registered against the specific CPS prosecutor id so the informant (a different authority) still resolves
        // via the default prosecutor stub.
        stubQueryProsecutorDataForGivenProsecutionAuthorityId(CPS_PROSECUTOR_REF_DATA, CPS_PROSECUTOR_ID, CPS_PROSECUTOR_OU_CODE);
    }

    @Test
    public void shouldEmailCpsProsecutorOnCpsCcEmailAddressWhenAppealApplicationNotificationSent() throws Exception {
        // Given a prosecution case whose current prosecutor is a CPS prosecutor
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesWithCourtOrdersFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)));
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        confirmHearing();

        new CaseProsecutorUpdateHelper(caseId).updateCaseProsecutor();
        pollProsecutionCasesWithCourtOrdersFor(caseId,
                withJsonPath("$.prosecutionCase.prosecutor.prosecutorId", equalTo(CPS_PROSECUTOR_ID)),
                withJsonPath("$.prosecutionCase.prosecutor.isCps", is(true)));

        // And an appeal court application linked to that case has been initiated (so the application aggregate holds
        // the proceedings-initiated state required for send-notification to be actioned rather than ignored).
        initiateAppealCourtApplication(parentApplicationId, null);
        initiateAppealCourtApplication(applicationId, parentApplicationId);

        // When an appeal application notification is raised for that case
        sendAppealApplicationNotification();

        // Then the CPS prosecutor is emailed on its CPS CC email address
        verifyEmailNotificationIsRaisedWithAttachment(singletonList(CPS_CC_EMAIL_ADDRESS));
    }

    private void confirmHearing() {
        final String payload = getHearingPayload();
        final JsonEnvelope publicEventEnvelope = envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName(HEARING_CONFIRMED_EVENT)
                        .withUserId(userId)
                        .build(),
                stringToJsonObjectConverter.convert(payload));
        messageProducerClientPublic.sendMessage(HEARING_CONFIRMED_EVENT, publicEventEnvelope);

        pollForHearing(hearingId, withJsonPath("$.hearing.id", is(hearingId)));
    }

    private String getHearingPayload() {
        return uk.gov.moj.cpp.progression.util.FileUtil.getPayload(HEARING_CONFIRMED_JSON)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
    }

    private void initiateAppealCourtApplication(final String courtApplicationId, final String parentApplicationId) {
        addCourtApplicationForApplicationAtAGlance(caseId,
                courtApplicationId,
                STRING.next(),                                   // particulars
                now().toLocalDate().toString(),                  // applicantReceivedDate
                STRING.next(),                                   // applicationType
                Boolean.TRUE,                                    // appeal
                Boolean.FALSE,                                   // applicantAppellantFlag
                STRING.next(),                                   // paymentReference
                STRING.next(),                                   // applicantSynonym
                STRING.next(),                                   // applicantFirstName
                STRING.next(),                                   // applicantLastName
                STRING.next(),                                   // applicantNationality
                STRING.next(),                                   // applicantRemandStatus
                STRING.next(),                                   // applicantRepresentation
                STRING.next(),                                   // interpreterLanguageNeeds
                PAST_LOCAL_DATE.next(),                          // applicantDoB
                STRING.next(),                                   // applicantAddress1
                STRING.next(),                                   // applicantAddress2
                STRING.next(),                                   // applicantAddress3
                STRING.next(),                                   // applicantAddress4
                STRING.next(),                                   // applicantAddress5
                POST_CODE.next(),                                // applicantPostCode
                randomAlphanumeric(4).toUpperCase() + randomNumeric(7), // applicationReference
                respondentDefendantId,
                STRING.next(),                                   // respondentOrganisationName
                STRING.next(),                                   // respondentOrganisationAddress1
                STRING.next(),                                   // respondentOrganisationAddress2
                STRING.next(),                                   // respondentOrganisationAddress3
                STRING.next(),                                   // respondentOrganisationAddress4
                STRING.next(),                                   // respondentOrganisationAddress5
                POST_CODE.next(),                                // respondentOrganisationPostcode
                STRING.next(),                                   // respondentRepresentativeFirstName
                STRING.next(),                                   // respondentRepresentativeLastName
                STRING.next(),                                   // respondentRepresentativePosition
                caseId,                                          // prosecutionCaseId (linked case == seeded case)
                informantAuthorityId,                            // prosecutionAuthorityId (informant)
                STRING.next(),                                   // prosecutionAuthorityCode
                STRING.next(),                                   // prosecutionAuthorityReference
                parentApplicationId,
                CREATE_COURT_APPLICATION_JSON);

        assertThat(getApplicationFor(courtApplicationId), is(notNullValue()));
    }

    private void sendAppealApplicationNotification() {
        // The notification is raised for the initiated application; the aggregate resolves the current (CPS)
        // prosecutor of the linked case and emails it on the CPS CC email address.
        sendNotification(caseId,
                applicationId,
                STRING.next(),                                   // particulars
                now().toLocalDate().toString(),                  // applicantReceivedDate
                STRING.next(),                                   // applicationType
                Boolean.TRUE,                                    // appeal
                Boolean.FALSE,                                   // applicantAppellantFlag
                STRING.next(),                                   // paymentReference
                STRING.next(),                                   // applicantSynonym
                STRING.next(),                                   // applicantFirstName
                STRING.next(),                                   // applicantLastName
                STRING.next(),                                   // applicantNationality
                STRING.next(),                                   // applicantRemandStatus
                STRING.next(),                                   // applicantRepresentation
                STRING.next(),                                   // interpreterLanguageNeeds
                PAST_LOCAL_DATE.next(),                          // applicantDoB
                STRING.next(),                                   // applicantAddress1
                STRING.next(),                                   // applicantAddress2
                STRING.next(),                                   // applicantAddress3
                STRING.next(),                                   // applicantAddress4
                STRING.next(),                                   // applicantAddress5
                POST_CODE.next(),                                // applicantPostCode
                randomAlphanumeric(4).toUpperCase() + randomNumeric(7), // applicationReference
                STRING.next(),                                   // respondentOrganisationName
                STRING.next(),                                   // respondentOrganisationAddress1
                STRING.next(),                                   // respondentOrganisationAddress2
                STRING.next(),                                   // respondentOrganisationAddress3
                STRING.next(),                                   // respondentOrganisationAddress4
                STRING.next(),                                   // respondentOrganisationAddress5
                POST_CODE.next(),                                // respondentOrganisationPostcode
                STRING.next(),                                   // respondentRepresentativeFirstName
                STRING.next(),                                   // respondentRepresentativeLastName
                STRING.next(),                                   // respondentRepresentativePosition
                caseId,                                          // prosecutionCaseId (linked case == seeded case)
                informantAuthorityId,                            // prosecutionAuthorityId (informant)
                STRING.next(),                                   // prosecutionAuthorityCode
                STRING.next(),                                   // prosecutionAuthorityReference
                parentApplicationId,                             // parentApplicationId
                SEND_NOTIFICATION_JSON,
                Boolean.FALSE,                                   // isBoxWorkRequest
                Boolean.FALSE);                                  // isWelshTranslationRequired
    }
}
