package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.ACTIVE;
import static uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum.INACTIVE;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for CHD-2474: verifies that proceedingsConcluded and caseStatus are
 * recalculated correctly when a judicial result is removed from an offence via a hearing amendment.
 */
@SuppressWarnings("java:S2699")
class HearingAmendmentProceedingsConcludedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String HEARING_RESULTED_INITIAL_FIXTURE = "public.events.hearing.hearing-resulted-case-updated.json";
    private static final String HEARING_RESULTED_AMENDMENT_DELETED_FIXTURE = "public.events.hearing.hearing-resulted-amendment-deleted.json";
    private static final String HEARING_RESULTED_TWO_DEF_INITIAL_FIXTURE = "public.events.hearing.hearing-resulted-two-defendants-two-offences-initial.json";
    private static final String HEARING_RESULTED_TWO_DEF_AMENDMENT_FIXTURE = "public.events.hearing.hearing-resulted-two-defendants-two-offences-amendment.json";

    // Offence IDs are hardcoded in the two-defendant fixtures
    private static final String D1_OFFENCE_1_ID = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1"; // amended (result deleted)
    private static final String D2_OFFENCE_1_ID = "4789ab16-0bb7-4ef1-87ef-c936bf0364f1"; // unchanged

    private final JmsMessageProducerClient messageProducerClientPublic =
            newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final JmsMessageProducerClient messageProducerClientPublic2 =
            newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter =
            new StringToJsonObjectConverter();

    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String d1Id;
    private String d2Id;
    private String newCourtCentreId;
    private String bailStatusCode;
    private String bailStatusDescription;
    private String bailStatusId;

    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        d1Id = randomUUID().toString();
        d2Id = randomUUID().toString();
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        bailStatusCode = "C";
        bailStatusDescription = "Remanded into Custody";
        bailStatusId = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    }

    @Test
    public void shouldResetProceedingsConcludedAndCaseStatusWhenResultRemovedViaAmendment()
            throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        // Initial result share: offence has a FINAL judicial result → all concluded → INACTIVE
        final JsonEnvelope initialShareEnvelope = envelopeFrom(
                buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId),
                buildHearingPayload(HEARING_RESULTED_INITIAL_FIXTURE));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, initialShareEnvelope);

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(INACTIVE.getDescription())),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", equalTo(true)));

        // Amendment re-share: same offence, but result deleted (judicialResults=[]) → must revert
        final JsonEnvelope amendmentEnvelope = envelopeFrom(
                buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId),
                buildHearingPayload(HEARING_RESULTED_AMENDMENT_DELETED_FIXTURE));
        messageProducerClientPublic2.sendMessage(PUBLIC_HEARING_RESULTED_V2, amendmentEnvelope);

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(ACTIVE.getDescription())),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded",
                        equalTo(false)));
    }

    @Test
    public void shouldResetProceedingsConcludedOnlyForAmendedOffenceAcrossTwoDefendantsTwoOffencesEach()
            throws Exception {
        final String case2Id = randomUUID().toString();
        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(case2Id, d1Id, d2Id);
        final String hearing2Id = pollCaseAndGetHearingForDefendant(case2Id, d1Id);

        // Initial share: all 4 offences FINAL → all concluded → INACTIVE
        final JsonEnvelope initialEnvelope = envelopeFrom(
                buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId),
                buildTwoDefendantHearingPayload(HEARING_RESULTED_TWO_DEF_INITIAL_FIXTURE, case2Id, hearing2Id));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, initialEnvelope);

        pollProsecutionCasesProgressionFor(case2Id,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(INACTIVE.getDescription())),
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + d1Id + "')].proceedingsConcluded",
                        hasItem(true)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + d2Id + "')].proceedingsConcluded",
                        hasItem(true)));

        // Amendment: D1O1 result deleted → D1.proceedingsConcluded=false, D2 unchanged, case=ACTIVE
        final JsonEnvelope amendmentEnvelope = envelopeFrom(
                buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId),
                buildTwoDefendantHearingPayload(HEARING_RESULTED_TWO_DEF_AMENDMENT_FIXTURE, case2Id, hearing2Id));
        messageProducerClientPublic2.sendMessage(PUBLIC_HEARING_RESULTED_V2, amendmentEnvelope);

        pollProsecutionCasesProgressionFor(case2Id,
                withJsonPath("$.prosecutionCase.caseStatus", equalTo(ACTIVE.getDescription())),
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + d1Id + "')].proceedingsConcluded",
                        hasItem(false)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + d2Id + "')].proceedingsConcluded",
                        hasItem(true)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + d1Id + "')].offences[?(@.id=='" + D1_OFFENCE_1_ID + "')].proceedingsConcluded",
                        hasItem(false)),
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + d2Id + "')].offences[?(@.id=='" + D2_OFFENCE_1_ID + "')].proceedingsConcluded",
                        hasItem(true)));
    }

    private JsonObject buildHearingPayload(final String fixturePath) {
        return stringToJsonObjectConverter.convert(
                getPayload(fixturePath)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", newCourtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription));
    }

    private JsonObject buildTwoDefendantHearingPayload(final String fixturePath,
                                                       final String targetCaseId,
                                                       final String targetHearingId) {
        return stringToJsonObjectConverter.convert(
                getPayload(fixturePath)
                        .replaceAll("CASE_ID", targetCaseId)
                        .replaceAll("HEARING_ID", targetHearingId)
                        .replaceAll("DEFENDANT_ID_ONE", d1Id)
                        .replaceAll("DEFENDANT_ID_TWO", d2Id)
                        .replaceAll("COURT_CENTRE_ID", newCourtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription));
    }
}
