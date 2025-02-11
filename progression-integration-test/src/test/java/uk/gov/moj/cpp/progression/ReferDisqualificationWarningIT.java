package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.DocumentGenerationHelper.validateEnglishReferalDisqualifyWarning;
import static uk.gov.moj.cpp.progression.helper.DocumentGenerationHelper.validateWelshReferalDisqualifyWarning;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToMagsCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentsPerCase;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStub;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.pollDocumentGenerationRequest;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubEnforcementArea;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryDocumentTypeData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryEthinicityData;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryReferralReasons;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.moj.cpp.progression.helper.DocumentGenerationHelper;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.jms.JMSException;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class ReferDisqualificationWarningIT extends AbstractIT {

    public static final String EMPTY_PAGE_TEMPLATE_NAME = "EmptyPage";
    private static final String ENGLISH_TEMPLATE_NAME = "NPE_RefferalDisqualificationWarning";
    private static final String WELSH_TEMPLATE_NAME = "NPB_RefferalDisqualificationWarning";
    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private String caseId;
    private String defendantId;
    private String referralReasonId;
    private Path testResourceBasePath;
    private String materialId;
    private String caseUrn;
    private String courtDocumentId;
    private String postCode;

    @BeforeEach
    public void setUp() {

        testResourceBasePath = Paths.get("referral-disqualify-warning");
        stubQueryReferralReasons("/restResource/referencedata.query.referral-disqualification-reasons.json", randomUUID());
        stubEnforcementArea("/restResource/referencedata.query.enforcement-area-v1.json");
        stubQueryDocumentTypeData("/restResource/ref-data-orders-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
    }

    @AfterEach
    public void tearDown() throws JMSException {
        stubQueryEthinicityData("/restResource/ref-data-ethnicities.json", randomUUID());
        stubQueryReferralReasons("/restResource/referencedata.query.referral-disqualification-reasons.json", randomUUID());
        stubQueryDocumentTypeData("/restResource/ref-data-orders-document-type.json");
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
    }

    @Test
    public void shouldGetProsecutionCaseWithReferralDisqualifyWarning() throws Exception {
        caseId = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        caseUrn = generateUrn();
        materialId = randomUUID().toString();
        setupMaterialStub(materialId);

        postCode = getPostCode(true, true);

        DocumentGenerationHelper.givenCaseIsReferredToMags(postCode,
                ENGLISH_TEMPLATE_NAME,
                EMPTY_PAGE_TEMPLATE_NAME,
                WELSH_TEMPLATE_NAME);

        addProsecutionCaseToMagsCourt(caseId, defendantId, referralReasonId, caseUrn, postCode);

        verifyEnglishWelshDisqualifyWarningIsGenerated(caseUrn, true, true);
        verifyMaterialCreated();
        verifyAddCourtDocument("460fbe94-c002-11e8-a355-529269fb1459", courtDocumentId);

        Matcher[] matchers = new Matcher[]{
                withJsonPath("$.documentIndices", is(notNullValue())),
                withJsonPath("$.documentIndices", hasSize(greaterThan(0))),
                withJsonPath("$.documentIndices[0].caseIds[0]", is(caseId)),
                withJsonPath("$.documentIndices[0].document.name", is(notNullValue())),
                withJsonPath("$.documentIndices[0].document.materials[0].userGroups", is(notNullValue()))
        };

        getCourtDocumentsPerCase(randomUUID().toString(), caseId, matchers);

    }

    private void verifyEnglishWelshDisqualifyWarningIsGenerated(final String caseUrn, final Boolean hasPostcode, final Boolean hasWelshPostcode) throws Exception {
        final Optional<JSONObject> welshDocumentGenerationRequest = pollDocumentGenerationRequest(WELSH_TEMPLATE_NAME);
        assertThat(welshDocumentGenerationRequest.isPresent(), is(true));
        validateWelshReferalDisqualifyWarning(welshDocumentGenerationRequest.get(), testResourceBasePath.resolve("referral-disqualify-warning-welsh-parameters.json"), caseUrn);

        final Optional<JSONObject> englishDocumentGenerationRequest = pollDocumentGenerationRequest(ENGLISH_TEMPLATE_NAME);
        assertThat(englishDocumentGenerationRequest.isPresent(), is(true));
        validateEnglishReferalDisqualifyWarning(englishDocumentGenerationRequest.get(), testResourceBasePath.resolve("referral-disqualify-warning-english-parameters.json"), caseUrn, hasWelshPostcode, hasPostcode);
    }

    private String getPostCode(Boolean hasWelshPostcode, Boolean hasPostcode) {
        return hasPostcode ? hasWelshPostcode ? "CF10 1BY" : "W1T 1JY" : null;
    }

    private void verifyAddCourtDocument(final String documentTypeId, final String courtDocumentId) throws IOException, JSONException {
        //Given
        final String body = prepareAddCourtDocumentPayload();

        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + courtDocumentId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Then
        final String actualDocument = getCourtDocumentFor(courtDocumentId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(courtDocumentId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)),
                withJsonPath("$.courtDocument.sendToCps", equalTo(true)))
        );

        final String expectedPayload = getPayload("expected/expected.progression.add-disqualify-court-document.json")
                .replace("COURT-DOCUMENT-ID", courtDocumentId)
                .replace("DEFENDENT-ID", defendantId)
                .replace("DOCUMENT-TYPE-ID", documentTypeId)
                .replace("CASE-ID", caseId)
                .replace("MATERIAL-ID", materialId)
                .replaceAll("%UPLOADDATETIME%", ZONE_DATETIME_FORMATTER.format(ZonedDateTime.now()));

        assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-disqualify-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", courtDocumentId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("%RANDOM_MATERIAL_ID%", materialId);
        return body;
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("courtDocument.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }

}

