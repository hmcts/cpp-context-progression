package uk.gov.moj.cpp.progression.summons;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.MaterialHelper.sendEventToConfirmMaterialAdded;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.verifyMaterialCreated;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyCreateLetterRequested;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetDocumentsTypeAccess;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryReferralReasons;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.getLanguagePrefix;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.sendPublicEventToConfirmHearingForInitiatedCase;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyCaseDocumentAddedToCdes;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyMaterialRequestRecordedAndExtractMaterialId;
import static uk.gov.moj.cpp.progression.summons.SummonsHelper.verifyTemplatePayloadValues;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RequestSjpCaseReferredSummonsIT extends AbstractIT {

    private static final String PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED = "progression.event.nows-material-request-recorded";

    private static final String SJP_REFERRAL_ID = "2daefec3-2f76-8109-82d9-2e60544a6c02";

    private String caseId = randomUUID().toString();
    private String courtDocumentId = randomUUID().toString();
    private String materialIdActive = randomUUID().toString();
    private String materialIdDeleted = randomUUID().toString();
    private String caseUrn = generateUrn();

    private static final JmsMessageConsumerClient nowsMaterialRequestRecordedConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PRIVATE_EVENT_NOWS_MATERIAL_REQUEST_RECORDED).getMessageConsumerClient();
    private static final String DOCUMENT_TEXT = STRING.next();

    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final List<String> SJP_REFERRED_DEFENDANT_NAME = newArrayList("Harry", "Jack", "Kane Junior");

    private static final Map<String, List<List<String>>> DEFENDANT_NAME_MAP = ImmutableMap.of(
            DEFENDANT_ID, of(newArrayList(SJP_REFERRED_DEFENDANT_NAME))
    );

    public static Stream<Arguments> sjpSpecifications() {
        return Stream.of(
                // welsh court hearing
                Arguments.of(false),
                Arguments.of(true)
        );
    }

    @BeforeEach
    public void setUp() {

        stubInitiateHearing();
        stubDocumentCreate(DOCUMENT_TEXT);
        IdMapperStub.setUp();
        NotificationServiceStub.setUp();
        stubGetDocumentsTypeAccess("/restResource/get-all-document-type-access.json");
        stubQueryReferralReasons("/restResource/referencedata.query.referral-reasons-by-id.json", randomUUID());


        caseId = randomUUID().toString();
        courtDocumentId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        caseUrn = generateUrn();
    }

    @MethodSource("sjpSpecifications")
    @ParameterizedTest
    public void shouldGenerateSummonsForReferredCases(final boolean isWelsh) throws Exception {
        addProsecutionCaseToCrownCourt(caseId, DEFENDANT_ID, materialIdActive, materialIdDeleted, courtDocumentId, SJP_REFERRAL_ID, caseUrn);
        verifySummonsGeneratedOnHearingConfirmed(isWelsh);

        verifyMaterialCreated();
        final UUID materialId = verifyMaterialRequestRecordedAndExtractMaterialId(nowsMaterialRequestRecordedConsumer);
        sendEventToConfirmMaterialAdded(materialId);

        verifyCreateLetterRequested(of("letterUrl", materialId.toString()));
    }

    private void verifySummonsGeneratedOnHearingConfirmed(final boolean isWelsh) {
        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, DEFENDANT_ID,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId == '" + DEFENDANT_ID + "')].hearingIds[0]", hasSize(greaterThanOrEqualTo(1))));

        sendPublicEventToConfirmHearingForInitiatedCase(hearingId, DEFENDANT_ID, "3789ab16-0bb7-4ef1-87ef-c936bf0364f1", caseId, isWelsh);

        verifyCaseDocumentAddedToCdes(DEFENDANT_ID, caseId, 1);

        final String defendantTemplateName = "SP" + getLanguagePrefix(isWelsh) + "_SjpReferral";
        final List<String> defendantName = DEFENDANT_NAME_MAP.get(DEFENDANT_ID).get(0);

        verifyTemplatePayloadValues(defendantTemplateName, "SJP_REFERRAL", caseUrn, defendantName.get(0), defendantName.get(0), defendantName.get(0));
    }

}