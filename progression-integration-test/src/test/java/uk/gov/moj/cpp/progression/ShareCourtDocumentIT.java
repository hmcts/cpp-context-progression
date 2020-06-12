package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_COURT_DOCUMENT_SHARED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_COURT_DOCUMENT_SHARE_FAILED;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addRemoveCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocuments;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsByCase;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.shareCourtDocument;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
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
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.util.FileUtil;
import uk.gov.moj.cpp.progression.util.QueryUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.core.Is;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ShareCourtDocumentIT extends AbstractIT {

    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCH = "/courtdocumentsearch?caseId={0}&defendantId={1}&hearingId={2}";
    private static final String MAGISTRATES_USER_GROUP_ID = "dd8dcdcf-58d1-4e45-8450-40b0f569a7e7";
    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";
    private static final String UPLOAD_USER_ID = "5e1cc18c-76dc-47dd-99c1-d6f87385edf1";
    private static final String PRIVATE_COURT_DOCUMENT_SHARED_EVENT = "progression.event.court-document-shared";
    private static final String PRIVATE_DUPLICATE_SHARE_COURT_DOCUMENT_REQUEST_RECEIVED_EVENT = "progression.event.duplicate-share-court-document-request-received";
    private static final String PRIVATE_COURT_DOCUMENT_SHARE_FAILED_EVENT = "progression.event.court-document-share-failed";

    private static final MessageConsumer messageConsumerClientPrivateForCourtDocumentShared = privateEvents.createConsumer(PRIVATE_COURT_DOCUMENT_SHARED_EVENT);
    private static final MessageConsumer messageConsumerClientPrivateForDuplicateShareCourtDocumentRequestReceivedEvent = privateEvents.createConsumer(PRIVATE_DUPLICATE_SHARE_COURT_DOCUMENT_REQUEST_RECEIVED_EVENT);
    private static final MessageConsumer messageConsumerClientPrivateForCourtDocumentShareFailedEvent = privateEvents.createConsumer(PRIVATE_COURT_DOCUMENT_SHARE_FAILED_EVENT);
    private static final MessageConsumer messageConsumerCourtDocumentShareFailedPublicEvent = publicEvents.createConsumer(PUBLIC_COURT_DOCUMENT_SHARE_FAILED);
    private static final MessageConsumer messageConsumerCourtDocumentSharedPublicEvent = publicEvents.createConsumer(PUBLIC_COURT_DOCUMENT_SHARED);



    @BeforeClass
    public static void setup() {
        setupAsAuthorisedUser(fromString(UPLOAD_USER_ID));
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-specific-magistrate-groups-by-user.json");
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithNoCaseLevelDocs() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        String resource = "progression.add-court-document.json";

        final String courtDocument = addCourtDocument(caseId, docId, defendantId, resource);
        final JsonObject courtDocumentJson = new StringToJsonObjectConverter().convert(courtDocument);
        final String courtDocumentId = courtDocumentJson.getJsonObject("courtDocument").getString("courtDocumentId");


        shareCourtDocument(courtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        final BeanMatcher<Courtdocuments> pregeneratedResultMatcher = isBean(Courtdocuments.class)
                .withValue(cds -> cds.getDocumentIndices().size(), 1)
                .with(Courtdocuments::getDocumentIndices, first(Is.is(isBean(CourtDocumentIndex.class)
                        .with(CourtDocumentIndex::getDocument, isBean(CourtDocument.class)
                                .withValue(cd -> cd.getCourtDocumentId().toString(), courtDocumentId)
                                .withValue(cd -> cd.getDocumentCategory().getDefendantDocument().getProsecutionCaseId().toString(), caseId)
                                .withValue(cd -> cd.getDocumentCategory().getDefendantDocument().getDefendants().get(0).toString(), defendantId)
                        )
                )));

        final String queryUrl = getReadUrl(MessageFormat.format(PROGRESSION_QUERY_COURTDOCUMENTSSEARCH, caseId, defendantId, HEARING_ID_TYPE_TRIAL));
        final RequestParams preGeneratedRequestParams = requestParams(queryUrl,
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON)
                .withHeader(CPP_UID_HEADER.getName(), USER_ID)
                .build();

        QueryUtil.waitForQueryMatch(preGeneratedRequestParams, 45, pregeneratedResultMatcher, Courtdocuments.class);

    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithCaseLevelDocs() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String docId1 = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        String resource = "progression.add-court-document.json";

        final String courtDocument = addCourtDocument(caseId, docId, defendantId, resource);
        final JsonObject courtDocumentJson = new StringToJsonObjectConverter().convert(courtDocument);
        final String courtDocumentId = courtDocumentJson.getJsonObject("courtDocument").getString("courtDocumentId");

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        final String courtDocumentCaseLevel = addCourtDocument(caseId, docId1, defendantId, resourceCaseLevel);
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);
        final String caseLevelCourtDocumentId = courtDocumentCaseLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");



        shareCourtDocument(courtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(caseLevelCourtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId, HEARING_ID_TYPE_TRIAL);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.query-courtdocuments-for-shared-documents.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLevelCourtDocumentId)
                .replace("DEFENDANT-LEVEL-COURT-DOCUMENT-ID", courtDocumentId)
                .replace("DEFENDANT_ID", defendantId)
                .replace("CASE-ID", caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithMultiDefendantWithHearingTypeTrial() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String docIDef2 = UUID.randomUUID().toString();
        final String docId1 = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String defendantId1 = UUID.randomUUID().toString();
        String resource = "progression.add-court-document.json";

        final String courtDocument = addCourtDocument(caseId, docId, defendantId, resource);
        final String courtDocumentDef2 = addCourtDocument(caseId, docIDef2, defendantId1, resource);
        final JsonObject courtDocumentJson = new StringToJsonObjectConverter().convert(courtDocument);
        final String courtDocumentId = courtDocumentJson.getJsonObject("courtDocument").getString("courtDocumentId");

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        final String courtDocumentCaseLevel = addCourtDocument(caseId, docId1, defendantId, resourceCaseLevel);
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);
        final String caseLevelCourtDocumentId = courtDocumentCaseLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        shareCourtDocument(courtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(caseLevelCourtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId, HEARING_ID_TYPE_TRIAL);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.query-courtdocuments-for-shared-documents.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLevelCourtDocumentId)
                .replace("DEFENDANT-LEVEL-COURT-DOCUMENT-ID", courtDocumentId)
                .replace("DEFENDANT_ID", defendantId)
                .replace("CASE-ID", caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithMultiDefendantWithHearingTypeTrialOfIssue() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String docIDef2 = UUID.randomUUID().toString();
        final String docId1 = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String defendantId1 = UUID.randomUUID().toString();
        String resource = "progression.add-court-document.json";

        final String courtDocument = addCourtDocument(caseId, docId, defendantId, resource);
        final String courtDocumentDef2 = addCourtDocument(caseId, docIDef2, defendantId1, resource);
        final JsonObject courtDocumentJson = new StringToJsonObjectConverter().convert(courtDocument);
        final String courtDocumentId = courtDocumentJson.getJsonObject("courtDocument").getString("courtDocumentId");

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        final String courtDocumentCaseLevel = addCourtDocument(caseId, docId1, defendantId, resourceCaseLevel);
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);
        final String caseLevelCourtDocumentId = courtDocumentCaseLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        shareCourtDocument(courtDocumentId, HEARING_ID_TYPE_TRIAL_OF_ISSUE, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(caseLevelCourtDocumentId, HEARING_ID_TYPE_TRIAL_OF_ISSUE, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId, HEARING_ID_TYPE_TRIAL_OF_ISSUE);

        final String expectedPayload = FileUtil.getPayload("expected/expected.progression.query-courtdocuments-for-shared-documents.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLevelCourtDocumentId)
                .replace("DEFENDANT-LEVEL-COURT-DOCUMENT-ID", courtDocumentId)
                .replace("DEFENDANT_ID", defendantId)
                .replace("CASE-ID", caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }


    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithNonTrialHearingType() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String docIDef2 = UUID.randomUUID().toString();
        final String docId1 = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String defendantId1 = UUID.randomUUID().toString();
        String resource = "progression.add-court-document.json";

        final String courtDocument = addCourtDocument(caseId, docId, defendantId, resource);
        final String courtDocumentDef2 = addCourtDocument(caseId, docIDef2, defendantId1, resource);
        final JsonObject courtDocumentJson = new StringToJsonObjectConverter().convert(courtDocument);
        final String courtDocumentId = courtDocumentJson.getJsonObject("courtDocument").getString("courtDocumentId");

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        final String courtDocumentCaseLevel = addCourtDocument(caseId, docId1, defendantId, resourceCaseLevel);
        final JsonObject courtDocumentCaseLevelJson = new StringToJsonObjectConverter().convert(courtDocumentCaseLevel);
        final String caseLevelCourtDocumentId = courtDocumentCaseLevelJson.getJsonObject("courtDocument").getString("courtDocumentId");

        shareCourtDocument(courtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        final JsonObject actualDocumentJson = new StringToJsonObjectConverter().convert(getCourtDocuments(USER_ID, caseId, defendantId, HEARING_ID_TYPE_NON_TRIAL));

        assertThat(actualDocumentJson.getJsonArray("documentIndices").size(), is(2));
    }


    @Test
    public void shouldRaiseDuplicateShareCourtDocumentRequestReceivedWhenDocumentAlreadySharedWithMagistratesAndPublicSuccessMessage() throws IOException {
        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        String resource = "progression.add-court-document.json";
        final String courtDocument = addCourtDocument(caseId, docId, defendantId, resource);
        final JsonObject courtDocumentJson = new StringToJsonObjectConverter().convert(courtDocument);
        final String courtDocumentId = courtDocumentJson.getJsonObject("courtDocument").getString("courtDocumentId");


        shareCourtDocument(courtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForCourtDocumentShared);

        shareCourtDocument(courtDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerClientPrivateForDuplicateShareCourtDocumentRequestReceivedEvent);
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

    }

    @Test
    public void shouldRaiseShareCourtDocumentFailedWhenTryToShareDocumentWhichIsAlreadyRemoved() throws Exception {

        final String caseId = UUID.randomUUID().toString();
        final String docId = UUID.randomUUID().toString();
        final String defendantId = UUID.randomUUID().toString();
        final String materialIdActive = randomUUID().toString();
        final String materialIdDeleted = randomUUID().toString();
        final String referraReasonId = randomUUID().toString();
        final String hearingId = "2daefec3-2f76-8109-82d9-2e60544a6c02";
        final String userId = "dd8dcdcf-58d1-4e45-8450-40b0f569a7e7";

        addProsecutionCaseToCrownCourt(caseId, defendantId, materialIdActive, materialIdDeleted, docId, referraReasonId);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, emptyList()));

        setupAsAuthorisedUser(UUID.fromString(userId), "stub-data/usersgroups.get-support-groups-by-user.json");

        addRemoveCourtDocument(docId, materialIdActive, true, UUID.fromString(userId));

        assertTrue(getCourtDocumentsByCase(UUID.randomUUID().toString(), caseId).contains("{\"documentIndices\":[]}"));

        shareCourtDocument(docId, hearingId, userId, "progression.share-court-document.json");

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

    private String addCourtDocument( final String caseId , final String docId , final String defendantId, final String resourceAddCourtDocument) throws IOException {

        String body = prepareAddCourtDocumentPayload(docId, caseId, defendantId, resourceAddCourtDocument);

        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        return getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true))
        ));
    }

    private String prepareAddCourtDocumentPayload(final String docId, final String caseId, final String defendantId, final String addCourtDocumentResource) throws IOException {
        String body = Resources.toString(Resources.getResource(addCourtDocumentResource),
                Charset.defaultCharset());
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId);
        return body;
    }

    private static void verifyInMessagingQueue(final MessageConsumer messageConsumer) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumer);
        assertTrue(message.isPresent());
    }

}
