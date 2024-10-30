package uk.gov.moj.cpp.progression;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasToString;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.CorrespondenceStub.stubForCorrespondenceCaseContacts;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithAttachment;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyNoLetterRequested;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueReaderUtil;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PublishCourtListIT extends AbstractIT {

    private static final String PUBLIC_EVENT_COURT_LIST_PUBLISHED = "public.listing.court-list-published";
    private static final String PRIVATE_EVENT_EMAIL_REQUESTED = "progression.event.email-requested";

    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String DEFENCE_ORGANISATION_EMAIL = "email@email.com";
    private static final String DEFENCE_ADVOCATE_EMAIL = "defenceAdvocate@organisation.com";
    private static final String PROSECUTOR_EMAIL = "test@test.com";
    private final String caseId = randomUUID().toString();
    private final String defendantId1 = randomUUID().toString();
    private final String defendantId2 = randomUUID().toString();
    private final String userId = randomUUID().toString();
    private final String caseUrn = generateUrn();

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static JmsMessageConsumerClient messageConsumerClientEmailRequested;
    private static final QueueReaderUtil queueReaderUtil = new QueueReaderUtil();

    @BeforeAll
    public static void setupOnce()  {
        messageConsumerClientEmailRequested = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PRIVATE_EVENT_EMAIL_REQUESTED).getMessageConsumerClient();
        queueReaderUtil.startListeningToPrivateEvents(messageConsumerClientEmailRequested, PRIVATE_EVENT_EMAIL_REQUESTED);
    }


    @BeforeEach
    public void setup() {
        NotificationServiceStub.setUp();
        stubDocumentCreate(DOCUMENT_TEXT);
        givenDefendantAdvocateIsPresentInCorrespondence(caseId, defendantId1,defendantId2);
    }

    @Test
    public void shouldRaiseSingleEmailNotificationWithAttachmentForDefenceOrganisationForDraftSingleHearing() {
        givenDefendantsAreRepresentedByDefenceOrganisation(defendantId1, defendantId2);

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-final-single-hearing.json");

        final UUID materialId = thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
        andNoPostalNotificationIsRaised(materialId);
    }

    @Test
    public void shouldRaiseSingleEmailNotificationWithAttachmentForDefenceOrganisationForDraftSingleHearing_WithMandatoryFieldsOnly() {
        givenDefendantsAreRepresentedByDefenceOrganisation(defendantId1, defendantId2);

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-final-single-hearing_with_mandatory_fields_only.json");

        final UUID materialId = thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
        andNoPostalNotificationIsRaised(materialId);
    }

    @Test
    public void shouldRaiseSingleEmailNotificationWithAttachmentForDefenceOrganisationForFinalSingleHearing() {
        givenDefendantsAreRepresentedByDefenceOrganisation(defendantId1, defendantId2);

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-final-single-hearing.json");

        final UUID materialId = thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
        andNoPostalNotificationIsRaised(materialId);
    }

    @Test
    public void shouldRaiseEmailNotificationWithAttachmentForDefenceOrganisationAndForProsecutorForWarnSingleHearing() throws Exception, JSONException {
        givenDefendantIsRepresentedByDefenceOrganisation(defendantId1);
        andProsecutionCaseIsPresentInCrownCourt();
        andNonCPSProsecutorIsPresent();

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-warn-single-hearing.json");

        final UUID defenceOrganisationMaterialId = thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
        final UUID prosecutorMaterialId = andProsecutorIsNotifiedByEmail();
        andNoPostalNotificationIsRaised(defenceOrganisationMaterialId);
        andNoPostalNotificationIsRaised(prosecutorMaterialId);
    }

    @Test
    public void shouldRaiseEmailNotificationWithAttachmentForDefenceOrganisationAndForProsecutorForFirmSingleHearing() throws Exception, JSONException {
        givenDefendantIsRepresentedByDefenceOrganisation(defendantId1);
        andProsecutionCaseIsPresentInCrownCourt();
        andNonCPSProsecutorIsPresent();

        whenListingRaisesCourtListPublishedEvent("public.listing.court-list-published-firm-single-hearing.json");

        final UUID defenceOrganisationMaterialId = thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail();
        final UUID prosecutorMaterialId = andProsecutorIsNotifiedByEmail();
        andNoPostalNotificationIsRaised(defenceOrganisationMaterialId);
        andNoPostalNotificationIsRaised(prosecutorMaterialId);
    }

    private void givenDefendantsAreRepresentedByDefenceOrganisation(final String... defendantIds) {
        for (final String defendantId : defendantIds) {
            givenDefendantIsRepresentedByDefenceOrganisation(defendantId);
        }
    }

    private void givenDefendantIsRepresentedByDefenceOrganisation(final String defendantId) {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
    }

    private void givenDefendantAdvocateIsPresentInCorrespondence(final String caseId, final String defendantId1, final String defendantId2) {
        stubForCorrespondenceCaseContacts("stub-data/correspondence.query.contacts.json", caseId, defendantId1, defendantId2);
    }

    private void andProsecutionCaseIsPresentInCrownCourt() throws IOException, JSONException {
        addProsecutionCaseToCrownCourtAndVerify(caseId, defendantId1, caseUrn);
    }

    private static void andNonCPSProsecutorIsPresent() {
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps.json", randomUUID());
    }

    private void whenListingRaisesCourtListPublishedEvent(final String eventLocation) {
        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_EVENT_COURT_LIST_PUBLISHED, userId), getContentsAsJsonObject(eventLocation));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENT_COURT_LIST_PUBLISHED, publicEventEnvelope);
    }

    private UUID thenDefenceOrganisationAndDefenceAdvocateIsNotifiedByEmail() {
        final UUID materialIdDefenceOrg = verifyEmailRequestedPrivateEvent(DEFENCE_ORGANISATION_EMAIL);
        final List<String> expectedDefendantOrgEmailDetails = newArrayList(DEFENCE_ORGANISATION_EMAIL);
        verifyEmailNotificationIsRaisedWithAttachment(expectedDefendantOrgEmailDetails, materialIdDefenceOrg);
        final UUID materialIdDefenceAdvocate = verifyEmailRequestedPrivateEvent(DEFENCE_ADVOCATE_EMAIL);
        final List<String> expectedDefenceAdvocateEmailDetails = newArrayList(DEFENCE_ADVOCATE_EMAIL);
        verifyEmailNotificationIsRaisedWithAttachment(expectedDefenceAdvocateEmailDetails, materialIdDefenceAdvocate);
        return materialIdDefenceOrg;
    }

    private UUID andProsecutorIsNotifiedByEmail() {
        final UUID materialId = verifyEmailRequestedPrivateEvent(PROSECUTOR_EMAIL);
        final List<String> expectedEmailDetails = newArrayList(PROSECUTOR_EMAIL);
        verifyEmailNotificationIsRaisedWithAttachment(expectedEmailDetails, materialId);
        return materialId;
    }

    private static void andNoPostalNotificationIsRaised(final UUID materialId) {
        verifyNoLetterRequested(of(materialId.toString()));
    }

    private UUID verifyEmailRequestedPrivateEvent(final String emailAddress) {
        final Optional<JsonObject> message = queueReaderUtil.retrieveMessageBody(PRIVATE_EVENT_EMAIL_REQUESTED, emailAddress);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get(), isJson(withJsonPath("$.notifications[0].sendToAddress",
                hasToString(containsString(emailAddress)))));

        return fromString(message.get().getString("materialId"));
    }

    private void addProsecutionCaseToCrownCourtAndVerify(final String caseId, final String defendantId, final String urn) throws IOException, JSONException {
        addProsecutionCaseToCrownCourt(caseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));
    }

    private JsonObject getContentsAsJsonObject(final String path) {
        final String strPayload = getPayload(path)
                .replaceAll("DEFENDANT_ID_1", defendantId1)
                .replaceAll("DEFENDANT_ID_2", defendantId2)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("CASE_URN", caseUrn);
        return stringToJsonObjectConverter.convert(strPayload);
    }

}
