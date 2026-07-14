package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDateTime.now;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.PAST_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.POST_CODE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.sendNotification;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCpsProsecutorData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorDataForGivenProsecutionAuthorityId;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;

import javax.json.JsonObject;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for appeal-application notifications where the case's original Prosecuting Authority
 * (Surrey Police) is the Applicant AND the case's original prosecutor; CPS London South is Respondent 1
 * (an updated CPS prosecutor); the Defendant is Respondent 2. All three parties are notified, with Surrey
 * Police receiving exactly ONE email (the informant path is suppressed by the {@code InformantNotificationTracker}
 * because Surrey is already processed as the Applicant).
 *
 * <p>Party layout (from {@code progression.command.create-court-application-appeal-notification.json}):
 * <ul>
 *   <li>Applicant  = Surrey Police (prosecuting authority, email {@code mail_data_bureau_res@surrey.police.uk})</li>
 *   <li>Respondent 1 = CPS London South (prosecuting authority; CPS email {@code london.crowncourt@cps.gov.uk}
 *       resolved from reference data {@code ccCpsEmailAddress})</li>
 *   <li>Respondent 2 = Defendant (email {@code defendant-appeal@email.com}; no associated defence org)</li>
 *   <li>Original case prosecutor (embedded {@code prosecutionCaseIdentifier}) = Surrey Police (== Applicant)</li>
 * </ul>
 */
public class SendNotificationForAppealApplicationIT extends AbstractIT {

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String CREATE_APPLICATION_JSON = "progression.command.create-court-application-appeal-notification.json";
    private static final String SEND_NOTIFICATION_JSON = "progression.command.send-notification-for-application.json";
    private static final String INITIATE_APPLICATION_MEDIA_TYPE = "application/vnd.progression.initiate-court-proceedings-for-application+json";
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    // Fixed authority ids/emails so payload and reference-data stubs line up
    private static final String SURREY_AUTHORITY_ID = "764bff92-a135-34cb-b858-8bb6b4b66301";
    private static final String SURREY_OU_CODE = "0450000";
    private static final String SURREY_EMAIL = "mail_data_bureau_res@surrey.police.uk";
    private static final String CPS_LS_AUTHORITY_ID = "c8e6c4c8-f31f-3e48-b40a-6e7d33fe5652";
    private static final String CPS_LS_OU_CODE = "GAFCP00";
    private static final String CPS_LS_EMAIL = "london.crowncourt@cps.gov.uk";
    private static final String DEFENDANT_EMAIL = "defendant-appeal@email.com";

    private String userId;
    private String caseId;
    private String defendantId;
    private String hearingId;
    private String courtCentreId;
    private String courtApplicationId;
    private String particulars;
    private String applicantReceivedDate;
    private String applicationType;
    private Boolean appeal;
    private Boolean applicantAppellantFlag;
    private String paymentReference;
    private String applicantSynonym;
    private String applicantFirstName;
    private String applicantLastName;
    private String applicantNationality;
    private String applicantRemandStatus;
    private String applicantRepresentation;
    private String interpreterLanguageNeeds;
    private LocalDate applicantDoB;
    private String applicantAddress1;
    private String applicantAddress2;
    private String applicantAddress3;
    private String applicantAddress4;
    private String applicantAddress5;
    private String applicantPostCode;
    private String applicationReference;
    private String respondentDefendantId;
    private String respondentOrganisationName;
    private String respondentOrganisationAddress1;
    private String respondentOrganisationAddress2;
    private String respondentOrganisationAddress3;
    private String respondentOrganisationAddress4;
    private String respondentOrganisationAddress5;
    private String respondentOrganisationPostcode;
    private String respondentRepresentativeFirstName;
    private String respondentRepresentativeLastName;
    private String respondentRepresentativePosition;
    private String prosecutionCaseId;
    private String prosecutionAuthorityId;
    private String prosecutionAuthorityCode;
    private String prosecutionAuthorityReference;

    @BeforeEach
    public void setUp() {
        setupData();
        stubQueryCpsProsecutorData("/restResource/referencedata.query.cps.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_OK);

        // Reference-data prosecutor lookups (by prosecutionAuthorityId) used by both the CPS-party path and the
        // ref-data email fallback / informant path.
        stubQueryProsecutorDataForGivenProsecutionAuthorityId(
                "restResource/referencedata.query.prosecutor.surrey.appeal.json", SURREY_AUTHORITY_ID, SURREY_OU_CODE);
        stubQueryProsecutorDataForGivenProsecutionAuthorityId(
                "restResource/referencedata.query.prosecutor.cpsls.appeal.json", CPS_LS_AUTHORITY_ID, CPS_LS_OU_CODE);

        // Defendant (Respondent 2) has no associated defence organisation -> notified directly.
        stubForAssociatedOrganisation("stub-data/defence.get-no-associated-organisation.json", respondentDefendantId);
    }

    @Test
    public void shouldNotifyApplicantAuthorityCpsRespondentAndDefendant() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        doHearingConfirmedAndVerify();

        initiateAppealApplication(courtApplicationId);
        assertThat(getApplicationFor(courtApplicationId), is(notNullValue()));

        doSendNotification(null, false, false);

        // Applicant (Surrey Police, also the original case PA) -> ONE email (informant path suppressed)
        verifyEmailNotificationIsRaisedWithAttachment(singletonList(SURREY_EMAIL));
        // Respondent 1 (CPS London South) -> email at ccCpsEmailAddress from reference data
        verifyEmailNotificationIsRaisedWithAttachment(singletonList(CPS_LS_EMAIL));
        // Respondent 2 (Defendant) -> email
        verifyEmailNotificationIsRaisedWithAttachment(singletonList(DEFENDANT_EMAIL));
    }

    private void initiateAppealApplication(final String applicationId) {
        final String body = getPayload(CREATE_APPLICATION_JSON)
                .replace("RANDOM_APPLICATION_ID", applicationId)
                .replace("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_PROSECUTION_CASE_ID", prosecutionCaseId)
                .replace("RANDOM_RESPONDENT_DEFENDANT_ID", respondentDefendantId)
                .replace("RANDOM_REFERENCE", applicationReference);

        postCommand(getWriteUrl("/initiate-application"), INITIATE_APPLICATION_MEDIA_TYPE, body);
    }

    private void doHearingConfirmedAndVerify() {
        final JsonEnvelope publicEventEnvelope = envelopeFrom(metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build(),
                getHearingJsonObject("public.listing.hearing-confirmed-for-group-cases.json",
                        caseId, hearingId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForHearing(hearingId, withJsonPath("$.hearing.id", is(hearingId)));
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    /**
     * Fires the {@code /send-notification-for-application} command that triggers notifications for the initiated
     * application. The command's court-application content is ignored by the aggregate, which re-uses the stored
     * initiate-proceedings state, so the existing (person-shaped) template is fine purely as a trigger.
     */
    private void doSendNotification(final String parentApplicationId, final Boolean isBoxWorkRequest,
                                    final Boolean isWelshTranslationRequired) throws Exception {
        sendNotification(caseId,
                courtApplicationId,
                particulars,
                applicantReceivedDate,
                applicationType,
                appeal,
                applicantAppellantFlag,
                paymentReference,
                applicantSynonym,
                applicantFirstName,
                applicantLastName,
                applicantNationality,
                applicantRemandStatus,
                applicantRepresentation,
                interpreterLanguageNeeds,
                applicantDoB,
                applicantAddress1,
                applicantAddress2,
                applicantAddress3,
                applicantAddress4,
                applicantAddress5,
                applicantPostCode,
                applicationReference,
                respondentOrganisationName,
                respondentOrganisationAddress1,
                respondentOrganisationAddress2,
                respondentOrganisationAddress3,
                respondentOrganisationAddress4,
                respondentOrganisationAddress5,
                respondentOrganisationPostcode,
                respondentRepresentativeFirstName,
                respondentRepresentativeLastName,
                respondentRepresentativePosition,
                prosecutionCaseId,
                prosecutionAuthorityId,
                prosecutionAuthorityCode,
                prosecutionAuthorityReference,
                parentApplicationId,
                SEND_NOTIFICATION_JSON,
                isBoxWorkRequest,
                isWelshTranslationRequired);

        assertThat(getApplicationFor(courtApplicationId), is(notNullValue()));
    }

    private void setupData() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();
        particulars = STRING.next();
        applicantReceivedDate = now().toLocalDate().toString();
        applicationType = STRING.next();
        appeal = Boolean.TRUE;
        applicantAppellantFlag = Boolean.TRUE;
        paymentReference = STRING.next();
        applicantSynonym = STRING.next();
        applicantFirstName = STRING.next();
        applicantLastName = STRING.next();
        applicantNationality = STRING.next();
        applicantRemandStatus = STRING.next();
        applicantRepresentation = STRING.next();
        interpreterLanguageNeeds = STRING.next();
        applicantDoB = PAST_LOCAL_DATE.next();
        applicantAddress1 = STRING.next();
        applicantAddress2 = STRING.next();
        applicantAddress3 = STRING.next();
        applicantAddress4 = STRING.next();
        applicantAddress5 = STRING.next();
        applicantPostCode = POST_CODE.next();
        applicationReference = RandomStringUtils.randomAlphanumeric(4).toUpperCase() + RandomStringUtils.randomNumeric(7);
        respondentDefendantId = randomUUID().toString();
        respondentOrganisationName = STRING.next();
        respondentOrganisationAddress1 = STRING.next();
        respondentOrganisationAddress2 = STRING.next();
        respondentOrganisationAddress3 = STRING.next();
        respondentOrganisationAddress4 = STRING.next();
        respondentOrganisationAddress5 = STRING.next();
        respondentOrganisationPostcode = POST_CODE.next();
        respondentRepresentativeFirstName = STRING.next();
        respondentRepresentativeLastName = STRING.next();
        respondentRepresentativePosition = STRING.next();
        prosecutionCaseId = randomUUID().toString();
        prosecutionAuthorityId = SURREY_AUTHORITY_ID;
        prosecutionAuthorityCode = "SURRPF";
        prosecutionAuthorityReference = STRING.next();
    }
}
