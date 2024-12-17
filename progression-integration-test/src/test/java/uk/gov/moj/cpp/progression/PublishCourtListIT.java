package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.CorrespondenceStub.stubForCorrespondenceCaseContacts;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PublishCourtListIT extends AbstractIT {

    private static final String PUBLIC_EVENT_COURT_LIST_PUBLISHED = "public.listing.court-list-published";

    private static final String DOCUMENT_TEXT = STRING.next();

    private String defenceOrganisationEmail;
    private String defenceAdvocateEmail;
    private String prosecutorEmail;

    private String caseId;
    private String defendantId1;
    private String defendantId2;
    private String userId;
    private String caseUrn;

    private UUID prosecutionAuthorityId;

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    @BeforeEach
    public void setup() {
        NotificationServiceStub.setUp();
        stubDocumentCreate(DOCUMENT_TEXT);

        defenceOrganisationEmail = randomAlphanumeric(15)+"-defenceorg@email.com";
        defenceAdvocateEmail = randomAlphanumeric(15)+"-defenceadvocate@email.com";
        prosecutorEmail = randomAlphanumeric(15)+"-prosecutor@email.com";

        caseId = randomUUID().toString();
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        userId = randomUUID().toString();
        caseUrn = generateUrn();

        prosecutionAuthorityId = randomUUID();

        givenDefendantAdvocateIsPresentInCorrespondence(caseId, defendantId1, defendantId2);
    }

    @Test
    public void shouldRaiseSingleEmailNotificationWithAttachmentForDefenceOrganisationForDraftSingleHearing() {
        givenDefendantsAreRepresentedByDefenceOrganisation(defendantId1, defendantId2);

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-final-single-hearing.json");

        thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
    }

    @Test
    public void shouldRaiseSingleEmailNotificationWithAttachmentForDefenceOrganisationForDraftSingleHearing_WithMandatoryFieldsOnly() {
        givenDefendantsAreRepresentedByDefenceOrganisation(defendantId1, defendantId2);

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-final-single-hearing_with_mandatory_fields_only.json");

        thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
    }

    @Test
    public void shouldRaiseSingleEmailNotificationWithAttachmentForDefenceOrganisationForFinalSingleHearing() {
        givenDefendantsAreRepresentedByDefenceOrganisation(defendantId1, defendantId2);

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-final-single-hearing.json");

        thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
    }

    @Test
    public void shouldRaiseEmailNotificationWithAttachmentForDefenceOrganisationAndForProsecutorForWarnSingleHearing() throws Exception {
        givenDefendantIsRepresentedByDefenceOrganisation(defendantId1);
        andProsecutionCaseIsInCrownCourt();
        andNonCPSProsecutorIsPresent();

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-warn-single-hearing.json");

        thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
        andProsecutorIsNotifiedByEmail();
    }

    @Test
    public void shouldRaiseEmailNotificationWithAttachmentForDefenceOrganisationAndForProsecutorForFirmSingleHearing() throws Exception {
        givenDefendantIsRepresentedByDefenceOrganisation(defendantId1);
        andProsecutionCaseIsInCrownCourt();
        andNonCPSProsecutorIsPresent();

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-firm-single-hearing.json");

        thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
        andProsecutorIsNotifiedByEmail();
    }

    private void givenDefendantsAreRepresentedByDefenceOrganisation(final String... defendantIds) {
        for (final String defendantId : defendantIds) {
            givenDefendantIsRepresentedByDefenceOrganisation(defendantId);
        }
    }

    private void givenDefendantIsRepresentedByDefenceOrganisation(final String defendantId) {
        final String payload = getPayload("stub-data/defence.get-associated-organisation-random-email.json").replace("RANDOM_EMAIL", defenceOrganisationEmail);
        final JsonObject payloadAsJsonObject = new StringToJsonObjectConverter().convert(payload);
        stubForAssociatedOrganisation(payloadAsJsonObject, defendantId);
    }

    private void givenDefendantAdvocateIsPresentInCorrespondence(final String caseId, final String defendantId1, final String defendantId2) {
        final String payload = getPayload("stub-data/correspondence.query.contacts.json")
                .replace("RANDOM_EMAIL", defenceAdvocateEmail)
                .replace("CASE_ID", caseId)
                .replace("DEFENDANT_ID_1", defendantId1)
                .replace("DEFENDANT_ID_2", defendantId2);
        final JsonObject payloadAsJsonObject = new StringToJsonObjectConverter().convert(payload);
        stubForCorrespondenceCaseContacts(payloadAsJsonObject);
    }

    private void andNonCPSProsecutorIsPresent() {
        final String payload = getPayload("restResource/referencedata.query.prosecutor-noncps-random-email.json")
                .replaceAll("RANDOM_EMAIL", prosecutorEmail)
                .replaceAll("RANDOM_PROSECUTOR_ID", prosecutionAuthorityId.toString());
        final JsonObject payloadAsJsonObject = new StringToJsonObjectConverter().convert(payload);
        stubQueryProsecutorData(payloadAsJsonObject, prosecutionAuthorityId, randomUUID());
    }

    private void whenListingRaisesCourtListPublishedEvent(final String eventLocation) {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENT_COURT_LIST_PUBLISHED, userId), getCourtListPublishedPayloadAsJsonObject(eventLocation));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_COURT_LIST_PUBLISHED, publicEventEnvelope);
    }

    private UUID thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail() {
        final List<String> expectedDefendantOrgEmailDetails = newArrayList(defenceOrganisationEmail);
        verifyEmailNotificationIsRaisedWithAttachment(expectedDefendantOrgEmailDetails);
        final List<String> expectedDefenceAdvocateEmailDetails = newArrayList(defenceAdvocateEmail);
        verifyEmailNotificationIsRaisedWithAttachment(expectedDefenceAdvocateEmailDetails);
        return null;
    }

    private UUID andProsecutorIsNotifiedByEmail() {
        final List<String> expectedEmailDetails = newArrayList(prosecutorEmail);
        verifyEmailNotificationIsRaisedWithAttachment(expectedEmailDetails);
        return null;
    }

    private void andProsecutionCaseIsInCrownCourt() throws IOException, JSONException {
        addProsecutionCaseToCrownCourt();

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));
    }

    private JsonObject getCourtListPublishedPayloadAsJsonObject(final String path) {
        final String strPayload = getPayload(path)
                .replaceAll("DEFENDANT_ID_1", defendantId1)
                .replaceAll("DEFENDANT_ID_2", defendantId2)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("CASE_URN", caseUrn);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    public Response addProsecutionCaseToCrownCourt() throws IOException, JSONException {

        final String payload = getPayload("progression.command.prosecution-case-refer-to-court-random-prosecutor.json")
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replaceAll("RANDOM_PROSECUTION_AUTHORITY_ID", prosecutionAuthorityId.toString())
                .replaceAll("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId1);

        return postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                payload);
    }

}
