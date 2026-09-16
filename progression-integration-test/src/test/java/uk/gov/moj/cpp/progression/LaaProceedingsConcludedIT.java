package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithTwoDefendantsThreeOffencesEach;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithTwoDefendantsTwoOffencesEach;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.LaaAPIMServiceStub.verifyLaaProceedingsConcludedCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * DD-42137 - A defendant's proceedings are concluded only when all of their
 * offenses are concluded, regardless of other defendants on the case.
 * The change triggers the LAA notification flow via APIM.
 */
public class LaaProceedingsConcludedIT extends AbstractIT {

    private static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String userId;
    private String caseId;
    private String defendantId;
    private String defendant1Id;
    private String defendant2Id;
    private String hearingId;
    private String courtCentreId;
    private String courtCentreName;
    private String reportingRestrictionId;

    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        defendant1Id = randomUUID().toString();
        defendant2Id = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        reportingRestrictionId = randomUUID().toString();
    }

    /**
     * Example 1 & 2 - Notify LAA only for defendants whose proceedings are fully concluded.
     * Defendant 2 is notified even though defendant 1 still has an open offense.
     */
    @Test
    public void shouldNotifyOnlyTheDefendantWhoseProceedingsAreFullyConcluded() throws Exception {
        addProsecutionCaseToCrownCourtWithTwoDefendantsThreeOffencesEach(caseId, defendant1Id, defendant2Id);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendant1Id);

        sendHearingResultedTwoDefendants("public.hearing.resulted-two-defendants-one-open-one-concluded.json", "2021-11-23");

        // defendant 2 (fully concluded) is notified exactly once ...
        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendant2Id));
        // ... and defendant 1 (still has an open offense) never appears in any LAA notification.
        verifyLaaProceedingsConcludedCommandInvoked(0, newArrayList(hearingId, caseId, defendant1Id));
    }

    /**
     * A defendant is notified when all their offenses are concluded in the same hearing.
     */
    @Test
    public void shouldNotifyLaaWhenSingleDefendantOffencesAllConclude() throws Exception {
        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        sendHearingResulted("public.hearing.resulted-defendant-proceeding-concluded-with-all-offences-resulted-final.json", "2021-11-23");

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId));
        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.caseStatus", is("INACTIVE")));
    }

    /**
     * A defendant is not notified when any of their offenses is still open.
     */
    @Test
    public void shouldNotTriggerLaaWhenSingleDefendantHasOneOffenceConcludedAndOneNotConcluded() throws Exception {
        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        sendHearingResulted("public.hearing.resulted-defendant-proceeding-concluded-with-offence1-concluded.json", "2021-11-23");

        verifyLaaProceedingsConcludedCommandInvoked(0, newArrayList(hearingId, caseId, defendantId));
    }

    /**
     * A partially concluded defendant is not notified, while another fully concluded
     * defendant on the same case is notified.
     */
    @Test
    public void shouldNotNotifyThePartiallyConcludedDefendantWhenTheOtherDefendantIsFullyConcluded() throws Exception {
        addProsecutionCaseToCrownCourtWithTwoDefendantsTwoOffencesEach(caseId, defendant1Id, defendant2Id);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendant1Id);

        sendHearingResultedTwoDefendants("public.hearing.resulted-two-defendants-partial-and-concluded.json", "2021-11-23");

        // defendant 1 (one offense concluded, one still not) is never notified ...
        verifyLaaProceedingsConcludedCommandInvoked(0, newArrayList(hearingId, caseId, defendant1Id));
        // ... while defendant 2 (both offenses concluded) is notified exactly once.
        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendant2Id));
    }

    /**
     * Do not notify LAA when the defendant has no legal aid.
     */
    @Test
    public void shouldNotTriggerLaaWhenSingleDefendantHasNoLegalAid() throws Exception {
        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        sendHearingResulted("public.hearing.resulted-defendant-proceeding-concluded-with-all-offences-resulted-final-no-legal-aid.json", "2021-11-23");

        verifyLaaProceedingsConcludedCommandInvoked(0, newArrayList(hearingId, caseId, defendantId));
    }

    /**
     * Do not notify LAA when both defendants have no legal aid.
     */
    @Test
    public void shouldNotTriggerLaaWhenBothDefendantsHaveNoLegalAid() throws Exception {
        addProsecutionCaseToCrownCourtWithTwoDefendantsTwoOffencesEach(caseId, defendant1Id, defendant2Id);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendant1Id);

        sendHearingResultedTwoDefendants("public.hearing.resulted-two-defendants-no-legal-aid.json", "2021-11-23");

        verifyLaaProceedingsConcludedCommandInvoked(0, newArrayList(hearingId, caseId, defendant1Id));
        verifyLaaProceedingsConcludedCommandInvoked(0, newArrayList(hearingId, caseId, defendant2Id));
    }

    private void sendHearingResultedTwoDefendants(final String payloadFile, final String orderedDate) {
        final JsonObject hearingResultedJson = stringToJsonObjectConverter.convert(
                getPayload(payloadFile)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID_ONE", defendant1Id)
                        .replaceAll("DEFENDANT_ID_TWO", defendant2Id)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("ORDERED_DATE", orderedDate)
        );
        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), hearingResultedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
    }

    private void sendHearingResulted(final String payloadFile, final String orderedDate) {
        final JsonObject hearingResultedJson = stringToJsonObjectConverter.convert(
                getPayload(payloadFile)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId)
                        .replaceAll("ORDERED_DATE", orderedDate)
        );
        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), hearingResultedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
    }
}
