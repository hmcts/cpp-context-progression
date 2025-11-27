package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForApplicationAtAGlance;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationForApplicationAtAGlance;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeAccessQueryData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.MockCourtApplication;

import javax.jms.JMSException;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LinkedApplicationExtractIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application-aaag.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed.json";
    private String caseId;
    private String userId;
    private String hearingId;
    private String courtApplicationId;
    private String courtCentreId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        hearingId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();
        userId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        defendantId = randomUUID().toString();
        stubQueryDocumentTypeAccessQueryData("/restResource/ref-data-document-type-for-standalone.json");
    }

    @AfterAll
    public static void tearDown() throws JMSException {
        stubQueryDocumentTypeData("/restResource/ref-data-document-type.json");
    }

    @Test
    public void shouldVerifyLinkedApplicationsInApplicationAtAGlance() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId),
                getHearingJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE, caseId, hearingId, defendantId, courtCentreId));
        publicMessageProducerClient.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);


        final String firstApplicationId = courtApplicationId;
        doAddCourtApplicationAndVerify(PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON, randomUUID().toString(), new MockCourtApplication());
        verifyApplicationAtAGlance(courtApplicationId);

        final String linkedApplicationId = courtApplicationId;
        doAddCourtApplicationAndVerify(PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON, firstApplicationId, new MockCourtApplication());
        verifyApplicationAtAGlance(courtApplicationId);

        verifyLinkedApplicationHearingsForCourt(linkedApplicationId, defendantId);
    }

    private void verifyLinkedApplicationHearingsForCourt(final String applicationId, final String defendantId) {
        pollForApplicationHearingsForCourtExtract(applicationId, defendantId,
                withJsonPath("$.linkedApplicationHearings.length()", CoreMatchers.is(0))
        );
    }

    public static void pollForApplicationHearingsForCourtExtract(final String applicationId, final String defendantId, final Matcher... matchers) {
        pollForResponse("/applications/" + applicationId + "/defendants/" + defendantId,
                "application/vnd.progression.query.linked-application-hearings-for-court-extract+json",
                randomUUID().toString(),
                matchers
        );
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

    private void verifyApplicationAtAGlance(final String applicationId) {
        pollForApplicationAtAGlance(applicationId,
                withJsonPath("$.applicationId", equalTo(applicationId)));
    }

    private void doAddCourtApplicationAndVerify(final String filename, final String parentApplicationId, final MockCourtApplication courtApplication) {

        addCourtApplicationForApplicationAtAGlance(caseId,
                courtApplicationId,
                courtApplication.getParticulars(),
                courtApplication.getApplicantReceivedDate(),
                courtApplication.getApplicationType(),
                courtApplication.getAppeal(),
                courtApplication.getApplicantAppellantFlag(),
                courtApplication.getPaymentReference(),
                courtApplication.getApplicantSynonym(),
                courtApplication.getApplicantFirstName(),
                courtApplication.getApplicantLastName(),
                courtApplication.getApplicantNationality(),
                courtApplication.getApplicantRemandStatus(),
                courtApplication.getApplicantRepresentation(),
                courtApplication.getInterpreterLanguageNeeds(),
                courtApplication.getApplicantDoB(),
                courtApplication.getApplicantAddress1(),
                courtApplication.getApplicantAddress2(),
                courtApplication.getApplicantAddress3(),
                courtApplication.getApplicantAddress4(),
                courtApplication.getApplicantAddress5(),
                courtApplication.getApplicantPostCode(),
                courtApplication.getApplicationReference(),
                courtApplication.getRespondentDefendantId(),
                courtApplication.getRespondentOrganisationName(),
                courtApplication.getRespondentOrganisationAddress1(),
                courtApplication.getRespondentOrganisationAddress2(),
                courtApplication.getRespondentOrganisationAddress3(),
                courtApplication.getRespondentOrganisationAddress4(),
                courtApplication.getRespondentOrganisationAddress5(),
                courtApplication.getRespondentOrganisationPostcode(),
                courtApplication.getRespondentRepresentativeFirstName(),
                courtApplication.getRespondentRepresentativeLastName(),
                courtApplication.getRespondentRepresentativePosition(),
                courtApplication.getProsecutionCaseId(),
                courtApplication.getProsecutionAuthorityId(),
                courtApplication.getProsecutionAuthorityCode(),
                courtApplication.getProsecutionAuthorityReference(),
                parentApplicationId,
                filename);

        final String caseResponse = getApplicationFor(courtApplicationId);
        assertThat(caseResponse, is(notNullValue()));
    }
}
