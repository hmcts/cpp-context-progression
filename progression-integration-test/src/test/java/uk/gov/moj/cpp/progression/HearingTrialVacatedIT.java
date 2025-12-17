package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.Matchers.is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class HearingTrialVacatedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    public static final String PUBLIC_LISTING_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";
    private String vacatedTrialReasonId;

    @BeforeEach
    public void setUp() {
        vacatedTrialReasonId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateVacatedTrialStatusWhenVacatedFromHearing() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonObject notVacatedTrialObject = getHearingJsonObject("public.message.hearing.trial-not-vacated.json", hearingId, null);

        final JsonEnvelope publicEventVacatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_TRIAL_VACATED, userId), notVacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_TRIAL_VACATED, publicEventVacatedEnvelope);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(false))
        );

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.hearing.trial-vacated.json", hearingId, vacatedTrialReasonId);

        final JsonEnvelope publicEventEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_HEARING_TRIAL_VACATED, userId), vacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_TRIAL_VACATED, publicEventEnvelope2);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(true))
        );
    }

    @Test
    public void shouldUpdateVacatedTrialStatusWhenVacatedFromListing() throws Exception {

        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), "Lavender Hill Magistrate's Court");

        final JsonEnvelope publicEventConfirmedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonObject notVacatedTrialObject = createObjectBuilder().add("hearingId", hearingId).add("allocated", true).add("isVacated", false).build();

        JsonEnvelope publicEventUpdatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, userId), notVacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, publicEventUpdatedEnvelope);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(false))
        );

        final JsonObject vacatedTrialObject = getHearingJsonObject("public.message.listing.trial-vacated-updated.json", hearingId, vacatedTrialReasonId);

        publicEventUpdatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, userId), vacatedTrialObject);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_VACATED_TRIAL_UPDATED, publicEventUpdatedEnvelope);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.isVacatedTrial", is(true))
        );
    }


    private JsonObject getHearingJsonObject(final String path, final String hearingId, final String vacatedTrialReasonId) {
        if (nonNull(vacatedTrialReasonId)) {
            final String strPayload = getPayload(path)
                    .replaceAll("HEARING_ID", hearingId)
                    .replaceAll("REASON_ID", vacatedTrialReasonId);
            return stringToJsonObjectConverter.convert(strPayload);
        } else {
            final String strPayload = getPayload(path)
                    .replaceAll("HEARING_ID", hearingId);
            return stringToJsonObjectConverter.convert(strPayload);
        }
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
}
