package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentCaseLevel;
import static uk.gov.moj.cpp.progression.helper.AddCourtDocumentHelper.addCourtDocumentDefendantLevel;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_COURT_DOCUMENT_SHARED;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocuments;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.shareCourtDocument;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.FileUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;

import javax.json.JsonObject;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ShareCourtDocumentIT extends AbstractIT {

    private static final String PROGRESSION_QUERY_COURTDOCUMENTSSEARCH = "/courtdocumentsearch?caseId={0}&defendantId={1}&hearingId={2}";
    private static final String MAGISTRATES_USER_GROUP_ID = "dd8dcdcf-58d1-4e45-8450-40b0f569a7e7";
    private static final String USER_ID = "07e9cd55-0eff-4eb3-961f-0d83e259e415";
    private static final String UPLOAD_USER_ID = "5e1cc18c-76dc-47dd-99c1-d6f87385edf1";

    private static final JmsMessageConsumerClient messageConsumerCourtDocumentSharedPublicEvent = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_COURT_DOCUMENT_SHARED).getMessageConsumerClient();
    private static String caseId;
    private static String defendantLevelDocumentId1;
    private static String caseLevelDocumentId;
    private static String defendantId1;
    private static String defendantId2;

    @BeforeAll
    public static void setup() {
        setupAsAuthorisedUser(fromString(UPLOAD_USER_ID));
        setupAsAuthorisedUser(fromString(USER_ID), "stub-data/usersgroups.get-specific-magistrate-groups-by-user.json");
    }

    @BeforeEach
    public void createVariables() {
        caseId = randomUUID().toString();
        defendantLevelDocumentId1 = randomUUID().toString();
        caseLevelDocumentId = randomUUID().toString();
        defendantId1 = randomUUID().toString();
        defendantId2 = randomUUID().toString();
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithNoCaseLevelDocs() throws IOException, JSONException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document-defendant-level.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);
        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        pollForResponse(MessageFormat.format(PROGRESSION_QUERY_COURTDOCUMENTSSEARCH, caseId, defendantId1, HEARING_ID_TYPE_TRIAL),
                APPLICATION_VND_PROGRESSION_QUERY_SEARCH_COURTDOCUMENTS_JSON,
                USER_ID.toString(),
                withJsonPath("$.documentIndices[0].document.courtDocumentId", is(defendantLevelDocumentId1)),
                withJsonPath("$.documentIndices[0].document.documentCategory.defendantDocument.prosecutionCaseId", is(caseId)),
                withJsonPath("$.documentIndices[0].document.documentCategory.defendantDocument.defendants[0]", is(defendantId1))
        );
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithCaseLevelDocs() throws IOException, JSONException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);

        addCourtDocumentCaseLevel("progression.add-court-document-case-level.json", caseId, caseLevelDocumentId);


        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        shareCourtDocument(caseLevelDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_TRIAL);

        final String expectedPayload = getExpectedPayloadForCourtDocumentShared(caseLevelDocumentId, defendantLevelDocumentId1, defendantId1, caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithMultiDefendantWithHearingTypeTrial() throws IOException, JSONException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);


        addCourtDocumentCaseLevel("progression.add-court-document-case-level.json", caseId, caseLevelDocumentId);

        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        shareCourtDocument(caseLevelDocumentId, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_TRIAL);

        final String expectedPayload = getExpectedPayloadForCourtDocumentShared(caseLevelDocumentId, defendantLevelDocumentId1, defendantId1, caseId);

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithMultiDefendantWithHearingTypeTrialOfIssue() throws IOException, JSONException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        addCourtDocumentCaseLevel(resourceCaseLevel, caseId, caseLevelDocumentId);

        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL_OF_ISSUE, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        shareCourtDocument(caseLevelDocumentId, HEARING_ID_TYPE_TRIAL_OF_ISSUE, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        final String actualDocument = getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_TRIAL_OF_ISSUE);

        final String expectedPayload = getExpectedPayloadForCourtDocumentShared(caseLevelDocumentId, defendantLevelDocumentId1, defendantId1, caseId);
        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    @Test
    public void shouldMakeCourtDocumentSharedWithMagistratesForGivenDefendantsOnlyWithNonTrialHearingType() throws IOException, JSONException {
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId1, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));
        addCourtDocumentDefendantLevel("progression.add-court-document.json", defendantLevelDocumentId1, defendantId1, defendantId2, caseId);

        String resourceCaseLevel = "progression.add-court-document-case-level.json";
        addCourtDocumentCaseLevel(resourceCaseLevel, caseId, caseLevelDocumentId);

        shareCourtDocument(defendantLevelDocumentId1, HEARING_ID_TYPE_TRIAL, MAGISTRATES_USER_GROUP_ID, "progression.share-court-document.json");
        verifyInMessagingQueue(messageConsumerCourtDocumentSharedPublicEvent);

        final JsonObject actualDocumentJson = new StringToJsonObjectConverter().convert(getCourtDocuments(USER_ID, caseId, defendantId1, HEARING_ID_TYPE_NON_TRIAL));
        assertThat(actualDocumentJson.getJsonArray("documentIndices").size(), is(2));
    }

    private String getExpectedPayloadForCourtDocumentShared(final String caseLeveldocId, final String defendantLeveldocId1, final String defendantId1, final String caseId) {
        return FileUtil.getPayload("expected/expected.progression.query-courtdocuments-for-shared-documents.json")
                .replace("CASE-LEVEL-COURT-DOCUMENT-ID", caseLeveldocId)
                .replace("DEFENDANT-LEVEL-COURT-DOCUMENT-ID", defendantLeveldocId1)
                .replace("DEFENDANT_ID", defendantId1)
                .replace("CASE-ID", caseId);
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("documentIndices[0].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[1].document.materials[0].uploadDateTime", (o1, o2) -> true),
                new Customization("documentIndices[0].document.documentTypeRBAC", (o1, o2) -> true),
                new Customization("documentIndices[1].document.documentTypeRBAC", (o1, o2) -> true)
        );
    }

    private static void verifyInMessagingQueue(final JmsMessageConsumerClient messageConsumer) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumer);
        assertThat(message.isPresent(), is(true));
    }

}
