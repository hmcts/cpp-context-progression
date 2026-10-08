package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.APPLICATION_VND_PROGRESSION_REFER_CASES_TO_COURT_JSON;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.createReferProsecutionCaseToCrownCourtJsonBody;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.LaaAPIMServiceStub.verifyLaaProceedingsConcludedCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class LaaProceedingsConcludedIT extends AbstractIT {

    private static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";

    private static final String REFER_TO_COURT_LAA_ONE_DEFENDANT_TWO_OFFENCES_FIXTURE = "progression.command.prosecution-case-refer-to-court-laa-imp-fo-two-offences.json";
    private static final String REFER_TO_COURT_LAA_TWO_DEFENDANTS_TWO_OFFENCES_EACH_FIXTURE = "progression.command.prosecution-case-refer-to-court-laa-two-defendants-two-offences-each.json";

    private static final String HEARING_RESULTED_ONE_DEFENDANT_IMP_AND_FO_FIXTURE = "public.hearing.resulted-laa-proceedings-concluded-imp-and-fo.json";
    private static final String HEARING_RESULTED_LAA_TWO_DEFENDANTS_PARTIAL_FIXTURE = "public.hearing.resulted-laa-two-defendants-defendant-two-partially-resulted.json";
    private static final String HEARING_RESULTED_LAA_TWO_DEFENDANTS_FULL_FIXTURE = "public.hearing.resulted-laa-two-defendants-both-fully-resulted.json";
    private static final String HEARING_RESULTED_NO_LAA_TWO_DEFENDANTS_PARTIAL_FIXTURE = "public.hearing.no-laa-two-defendants-defendant-two-partially-resulted.json";

    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String userId;
    private String caseId;
    private String defendantId;
    private String defendantIdOne;
    private String defendantIdTwo;
    private String hearingId;
    private String courtCentreId;
    private String courtCentreName;
    private String reportingRestrictionId;

    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        defendantIdOne = randomUUID().toString();
        defendantIdTwo = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtCentreName = "Lavender Hill Crown Court";
        reportingRestrictionId = randomUUID().toString();
    }

    @Test
    public void shouldConcludeProceedingsForOneLegalAidDefendantWhenBothOffencesAreResulted() throws Exception {

        setUpOneDefendantCaseAndSendHearingResulted(HEARING_RESULTED_ONE_DEFENDANT_IMP_AND_FO_FIXTURE);

        final Matcher[] proceedingsConcludedMatchers = {
                withJsonPath("$.prosecutionCase.caseStatus", is("INACTIVE")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].laaApplnReference.offenceLevelStatus", is("Granted")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].label", is("Imprisonment")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].laaApplnReference.offenceLevelStatus", is("Granted")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].judicialResults[0].label", is("Fine")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true))
        };

        pollProsecutionCasesProgressionFor(caseId, proceedingsConcludedMatchers);

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId));
    }

    @Test
    public void shouldConcludeProceedingsOnlyForTheFullyResultedLegalAidDefendantWhenTheOtherIsPartiallyResulted() throws Exception {

        setUpTwoDefendantCaseAndSendHearingResulted(HEARING_RESULTED_LAA_TWO_DEFENDANTS_PARTIAL_FIXTURE);

        final Matcher[] proceedingsConcludedMatchers = {
                withJsonPath("$.prosecutionCase.caseStatus", is("ACTIVE")),
                // defendant 1: both IMP and FO resulted -> concludes
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].label", is("Imprisonment")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].judicialResults[0].label", is("Fine")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true)),

                // defendant 2: IMP resulted, FO never resulted -> does not conclude
                withJsonPath("$.prosecutionCase.defendants[1].offences[0].judicialResults[0].label", is("Imprisonment")),
                withJsonPath("$.prosecutionCase.defendants[1].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[1].offences[1].proceedingsConcluded", is(false)),
                withJsonPath("$.prosecutionCase.defendants[1].proceedingsConcluded", is(false))
        };

        pollProsecutionCasesProgressionFor(caseId, proceedingsConcludedMatchers);
    }

    @Test
    public void scenario3_shouldConcludeProceedingsIndependentlyOfLegalAidStatusWhenOneDefendantIsPartiallyResulted() throws Exception {

        // Same offence/result pattern as Scenario 2, but neither defendant has Legal Aid - proves
        // proceedings-concluded is computed purely from judicial results, not gated on LAA status.
        setUpTwoDefendantCaseAndSendHearingResulted(HEARING_RESULTED_NO_LAA_TWO_DEFENDANTS_PARTIAL_FIXTURE);

        final Matcher[] proceedingsConcludedMatchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].label", is("Imprisonment")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].judicialResults[0].label", is("Fine")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[1].offences[0].judicialResults[0].label", is("Imprisonment")),
                withJsonPath("$.prosecutionCase.defendants[1].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[1].offences[1].proceedingsConcluded", is(false)),
                withJsonPath("$.prosecutionCase.defendants[1].proceedingsConcluded", is(false))
        };

        pollProsecutionCasesProgressionFor(caseId, proceedingsConcludedMatchers);
    }

    @Test
    public void scenario4_shouldConcludeProceedingsForBothLegalAidDefendantsWhenBothAreFullyResulted() throws Exception {

        setUpTwoDefendantCaseAndSendHearingResulted(HEARING_RESULTED_LAA_TWO_DEFENDANTS_FULL_FIXTURE);

        final Matcher[] proceedingsConcludedMatchers = {
                withJsonPath("$.prosecutionCase.caseStatus", is("INACTIVE")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].judicialResults[0].label", is("Imprisonment")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].judicialResults[0].label", is("Fine")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[1].offences[0].judicialResults[0].label", is("Imprisonment")),
                withJsonPath("$.prosecutionCase.defendants[1].offences[0].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[1].offences[1].judicialResults[0].label", is("Fine")),
                withJsonPath("$.prosecutionCase.defendants[1].offences[1].proceedingsConcluded", is(true)),
                withJsonPath("$.prosecutionCase.defendants[1].proceedingsConcluded", is(true))
        };

        pollProsecutionCasesProgressionFor(caseId, proceedingsConcludedMatchers);

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantIdOne));
        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantIdTwo));
    }

    private void setUpOneDefendantCaseAndSendHearingResulted(final String hearingResultedFixture) throws Exception {

        final String referToCourtPayload = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId,
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(),
                generateUrn(), REFER_TO_COURT_LAA_ONE_DEFENDANT_TWO_OFFENCES_FIXTURE);
        postCommand(getWriteUrl("/refertocourt"), APPLICATION_VND_PROGRESSION_REFER_CASES_TO_COURT_JSON, referToCourtPayload);

        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final String payload = getPayload(hearingResultedFixture)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("ORDERED_DATE", "2021-11-23")
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);
        final JsonObject hearingResultedJson = stringToJsonObjectConverter.convert(payload);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), hearingResultedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
    }

    private void setUpTwoDefendantCaseAndSendHearingResulted(final String hearingResultedFixture) throws Exception {

        final String referToCourtPayload = createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantIdOne, defendantIdTwo,
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(),
                generateUrn(), REFER_TO_COURT_LAA_TWO_DEFENDANTS_TWO_OFFENCES_EACH_FIXTURE);
        postCommand(getWriteUrl("/refertocourt"), APPLICATION_VND_PROGRESSION_REFER_CASES_TO_COURT_JSON, referToCourtPayload);

        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantIdOne);

        final String payload = getPayload(hearingResultedFixture)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID_ONE", defendantIdOne)
                .replaceAll("DEFENDANT_ID_TWO", defendantIdTwo)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("ORDERED_DATE", "2021-11-23")
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);
        final JsonObject hearingResultedJson = stringToJsonObjectConverter.convert(payload);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), hearingResultedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
    }
}
