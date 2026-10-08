package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusResulted;
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
 * CONFIRMED reproduction for a reported defect: a single-defendant, single-offence LAA case,
 * where the offence's own judicial result is category FINAL (an IMP sentence), is notified to the
 * LAA APIM endpoint with {@code isConcluded:false} / {@code proceedingsConcluded:false} whenever
 * the hearing-resulted event's hearing-level {@code defendantJudicialResults} list (a SEPARATE,
 * parallel representation of judicial results, independent of the offence's own nested
 * {@code judicialResults}) also carries an entry for the SAME offence id that is not category
 * FINAL and does not terminate proceedings.
 * <p>
 * Root cause: {@code DefendantHelper.isConcluded(Offence, List&lt;DefendantJudicialResult&gt;,
 * List&lt;JudicialResult&gt;)} independently evaluates (1) the offence's own judicialResults,
 * (2) the hearing-level defendantJudicialResults for the same offence id, and (3) defendant-level
 * defendantCaseJudicialResults for the same offence id, then ANDs the three together. Any one
 * non-empty, non-concluding source for that offence id vetoes the whole result to false - even
 * when the offence's own result is genuinely FINAL. This call chain feeds
 * {@code progression.event.laa-defendant-proceeding-concluded-changed} and gates the LAA APIM
 * notification, verified here against the real WireMock request body (not just that a
 * notification fired).
 */
public class LaaProceedingsConcludedDefectIT extends AbstractIT {

    private static final String PUBLIC_EVENTS_HEARING_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String userId;
    private String caseId;
    private String defendantId;
    private String hearingId;
    private String courtCentreId;
    private String courtCentreName;
    private String reportingRestrictionId;

    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        reportingRestrictionId = randomUUID().toString();
    }

    @Test
    public void shouldNotifyLaaWhenSingleOffenceHasFinalJudicialResultAndLaaReference() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(
                "public.events.hearing.hearing-resulted-with-defendantjudicialresults-at-defendant-level.json", caseId,
                hearingId, defendantId, courtCentreId, courtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatusResulted(hearingId);

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId, "\"isConcluded\":true", "\"proceedingsConcluded\":true"));
    }

    /**
     * CONFIRMED reproduction: same offence, same FINAL judicial result and LAA reference as the
     * baseline test above, but the hearing-resulted event ALSO carries a non-final entry in the
     * hearing-level {@code defendantJudicialResults} list for the SAME offence id
     * (hearing.getDefendantJudicialResults(), passed through ProgressionService.updateCase -&gt;
     * progression.command.hearing-resulted-update-case -&gt;
     * DefendantHelper.isConcluded(Offence, List&lt;DefendantJudicialResult&gt;, List&lt;JudicialResult&gt;)).
     * Verified against a live WireMock-captured request: the LAA notification still fires, but with
     * {@code isConcluded:false} / {@code proceedingsConcluded:false} - exactly the reported defect.
     */
    @Test
    public void shouldReportNotConcludedWhenStaleNonFinalHearingLevelJudicialResultExistsForSameOffence() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(
                "public.events.hearing.hearing-resulted-with-stale-hearing-level-judicial-result.json", caseId,
                hearingId, defendantId, courtCentreId, courtCentreName, reportingRestrictionId, "2021-03-29"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatusResulted(hearingId);

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId, "\"isConcluded\":false", "\"proceedingsConcluded\":false"));
    }

    /**
     * Isolation test using the EXACT real FO (Fine) judicial result reported as working, with
     * NO hearing-level defendantJudicialResults and NO defendantCaseJudicialResults in play at
     * all - i.e. only the offence's own judicialResults, which is what DefendantHelper.isConcluded's
     * "offenceLevelConcluded" check reads. If this still concludes true, it shows the offence's own
     * FO judicial result content is not, by itself, anything unusual.
     */
    @Test
    public void shouldConcludeWithRealFoJudicialResultAloneNoOtherSources() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(
                "public.events.hearing.hearing-resulted-real-fo-only.json", caseId,
                hearingId, defendantId, courtCentreId, courtCentreName, reportingRestrictionId, "2026-10-08"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatusResulted(hearingId);

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId, "\"isConcluded\":true", "\"proceedingsConcluded\":true"));
    }

    /**
     * Isolation test using the EXACT real IMP (Imprisonment) judicial result reported as
     * FAILING, with NO hearing-level defendantJudicialResults and NO defendantCaseJudicialResults
     * in play - same isolation as the FO test above. If this ALSO concludes true here (unlike
     * the real-world failure), it proves the offence's own IMP judicialResults content is NOT,
     * by itself, the cause - the divergence the user saw must come from something outside what's
     * visible in the generated LAA event (i.e. the hearing-level or case-level lists), not from
     * getJudicialResults() on the offence itself.
     */
    @Test
    public void shouldConcludeWithRealImpJudicialResultAloneNoOtherSources() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, userId), getHearingJsonObject(
                "public.events.hearing.hearing-resulted-real-imp-only.json", caseId,
                hearingId, defendantId, courtCentreId, courtCentreName, reportingRestrictionId, "2026-10-08"));
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, publicEventResultedEnvelope);
        pollHearingWithStatusResulted(hearingId);

        verifyLaaProceedingsConcludedCommandInvoked(1, newArrayList(hearingId, caseId, defendantId, "\"isConcluded\":true", "\"proceedingsConcluded\":true"));
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName,
                                            final String reportingRestrictionId, final String orderedDate) {
        final String payload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("ORDERED_DATE", orderedDate)
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);

        return stringToJsonObjectConverter.convert(payload);
    }
}
