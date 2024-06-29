package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentCaseLevel;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentDefendantLevel;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_COURT_DOCUMENT_SHARED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_COURT_DOCUMENT_SHARE_FAILED;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addRemoveCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocuments;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.shareCourtDocument;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.test.matchers.ElementAtListMatcher.first;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.core.courts.CourtDocument;
import uk.gov.justice.core.courts.CourtDocumentIndex;
import uk.gov.justice.courts.progression.query.Courtdocuments;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.util.FileUtil;
import uk.gov.moj.cpp.progression.util.QueryUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ShareCourtDocumentIT extends AbstractIT {

    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCH = "/courtdocumentsearch?caseId={0}&defendantId={1}&hearingId={2}";
    private static final String MAGISTRATES_USER_GROUP_ID = "dd8dcdcf-58d1-4e45-8450-40b0f569a7e7";
    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";
    private static final String UPLOAD_USER_ID = "5e1cc18c-76dc-47dd-99c1-d6f87385edf1";
    private static final String PRIVATE_COURT_DOCUMENT_SHARED_EVENT_V2= "progression.event.court-document-shared-v2";
    private static final String PRIVATE_DUPLICATE_SHARE_COURT_DOCUMENT_REQUEST_RECEIVED_EVENT = "progression.event.duplicate-share-court-document-request-received";
    private static final String PRIVATE_COURT_DOCUMENT_SHARE_FAILED_EVENT = "progression.event.court-document-share-failed";

    private static final MessageConsumer messageConsumerClientPrivateForCourtDocumentShared = privateEvents.createPrivateConsumer(PRIVATE_COURT_DOCUMENT_SHARED_EVENT_V2);
    private static final MessageConsumer messageConsumerClientPrivateForDuplicateShareCourtDocumentRequestReceivedEvent = privateEvents.createPrivateConsumer(PRIVATE_DUPLICATE_SHARE_COURT_DOCUMENT_REQUEST_RECEIVED_EVENT);
    private static final MessageConsumer messageConsumerClientPrivateForCourtDocumentShareFailedEvent = privateEvents.createPrivateConsumer(PRIVATE_COURT_DOCUMENT_SHARE_FAILED_EVENT);
    private static final MessageConsumer messageConsumerCourtDocumentShareFailedPublicEvent = publicEvents.createPublicConsumer(PUBLIC_COURT_DOCUMENT_SHARE_FAILED);
    private static final MessageConsumer messageConsumerCourtDocumentSharedPublicEvent = publicEvents.createPublicConsumer(PUBLIC_COURT_DOCUMENT_SHARED);

    private static String caseId;
    private static String defendantLevelDocumentId1;
    private static String defendantLevelDocumentId2;
    private static String caseLevelDocumentId;
    private static String defendantId1;
    private static String defendantId2;

    @BeforeClass
    public static void setup() {
        setupAsAuthorisedUser(fromString(UPLOAD_USER_ID));
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-specific-magistrate-groups-by-user.json");
    }

    @Before
    public void createVariables() {
        caseId = randomUUID().toString();
        defendantLevelDocumentId1 = randomUUID().toString();
        defendantLevelDocumentId2 = randomUUID().toString();
        caseLevelDocumentId = randomUUID().toString();
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");

    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithNoCaseLevelDocs() throws IOException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document-defendant-level.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);
        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        final BeanMatcher<Courtdocuments> pregeneratedResultMatcher = isBean(Courtdocuments.class)
                .withValue(cds -> cds.getDocumentIndices().size(), 1)
                .with(Courtdocuments::getDocumentIndices, first(Is.is(isBean(CourtDocumentIndex.class)
                        .with(CourtDocumentIndex::getDocument, isBean(CourtDocument.class)
                                .withValue(cd -> cd.getCourtDocumentId().toString(), defendantLevelDocumentId1)
                                .withValue(cd -> cd.getDocumentCategory().getDefendantDocument().getProsecutionCaseId().toString(), caseId)
                                .withValue(cd -> cd.getDocumentCategory().getDefendantDocument().getDefendants().get(0).toString(), defendantId1)
                        )
                )));

        final String queryUrl = getReadUrl(MessageFormat.format(PROGRESSION_QUERY_COURTDOCUMENTSSEARCH, caseId, defendantId1, HEARING_ID_TYPE_TRIAL));
        final RequestParams preGeneratedRequestParams = requestParams(queryUrl,
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(CPP_UID_HEADER.getName(), USER_ID)
                .build();

        QueryUtil.waitForQueryMatch(preGeneratedRequestParams, 45, pregeneratedResultMatcher, Courtdocuments.class);

    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithCaseLevelDocs() throws IOException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);

        addCourtDocumentCaseLevel("progression.add-court-document-case-level.json", caseId, caseLevelDocumentId);


        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(caseLevelDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_TRIAL);

        final String expectedPayload = getExpectedPayloadForCourtDocumentShared(caseLevelDocumentId, defendantLevelDocumentId1, defendantId1, caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithMultiDefendantWithHearingTypeTrial() throws IOException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);


        addCourtDocumentCaseLevel("progression.add-court-document-case-level.json", caseId, caseLevelDocumentId);

        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(caseLevelDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_TRIAL);

        final String expectedPayload = getExpectedPayloadForCourtDocumentShared(caseLevelDocumentId, defendantLevelDocumentId1, defendantId1, caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private String getExpectedPayloadForCourtDocumentShared(final String caseLeveldocId, final String defendantLeveldocId1, final String defendantId1, final String caseId) {
        return FileUtil.getPayload("expected/expected.progression.query-courtdocuments-for-shared-documents.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLeveldocId)
                .replace("DEFENDANT-LEVEL-COURT-DOCUMENT-ID", defendantLeveldocId1)
                .replace("DEFENDANT_ID", defendantId1)
                .replace("CASE-ID", caseId);
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithMultiDefendantWithHearingTypeTrialOfIssue() throws IOException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        addCourtDocumentCaseLevel(resourceCaseLevel, caseId, caseLevelDocumentId);

        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL_OF_ISSUE, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(caseLevelDocumentId, HEARING_ID_TYPE_TRIAL_OF_ISSUE, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_TRIAL_OF_ISSUE);

        final String expectedPayload = getExpectedPayloadForCourtDocumentShared(caseLevelDocumentId, defendantLevelDocumentId1, defendantId1, caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithNonTrialHearingType() throws IOException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        final String courtDocumentCaseLevel = addCourtDocumentCaseLevel(resourceCaseLevel, caseId, caseLevelDocumentId);
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);

        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final JsonObject actualDocumentJson = new StringToJsonObjectConverter().convert(getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_NON_TRIAL));

        assertThat(actualDocumentJson.getJsonArray("documentIndices").size(), is(2));
    }


    @Test
    public void shouldRaiseDuplicateShareCourtDocumentRequestReceivedWhenDocumentAlreadySharedWithMagistratesAndPublicSuccessMessage() throws IOException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document-defendant-level.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);


        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForDuplicateShareCourtDocumentRequestReceivedEvent);
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

    }

    @Test
    public void shouldRaiseShareCourtDocumentFailedWhenTryToShareDocumentWhichIsAlreadyRemoved() throws Exception {

        final String materialIdActive = randomUUID().toString();
        final String materialIdDeleted = randomUUID().toString();
        final String referraReasonId = randomUUID().toString();
        final String hearingId = "2daefec3-2f76-8109-82d9-2e60544a6c02";
        final String userId = "dd8dcdcf-58d1-4e45-8450-40b0f569a7e7";

        addProsecutionCaseToCrownCourt(caseId, defendantId1, materialIdActive, materialIdDeleted, defendantLevelDocumentId1, referraReasonId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, emptyList()));

        setupAsAuthorisedUser(UUID.fromString(userId), "stub-data/usersgroups.get-support-groups-by-user.json");

        addRemoveCourtDocument(defendantLevelDocumentId1, materialIdActive, true, UUID.fromString(userId));

        assertTrue(getCourtDocumentsByCase(randomUUID().toString(), caseId).contains("{\"documentIndices\":[]}"));

        shareCourtDocument(defendantLevelDocumentId1, hearingId, userId, "progression.share-court-document.json");

        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShareFailedEvent);
        verifyInMessagingQueue(messageConsumerCourtDocumentShareFailedPublicEvent);
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("documentIndices[0].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[1].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[1].document.documentTypeRBAC", (o1, o2) -> true)
        );
    }


    private static void verifyInMessagingQueue(final MessageConsumer messageConsumer) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumer);
        assertTrue(message.isPresent());
    }

}
