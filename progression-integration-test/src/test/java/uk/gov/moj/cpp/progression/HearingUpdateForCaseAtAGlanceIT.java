package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HearingUpdateForCaseAtAGlanceIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String caseId;
    private String defendantId;
    private String newCourtCentreId;
    private String newCourtCentreName;

    @BeforeEach
    public void setUp() {
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        newCourtCentreId = randomUUID().toString();
        newCourtCentreName = "Lavender Hill Magistrate's Court";
    }

    @Test
    public void shouldUpdateCaseAtAGlance() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId,
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='" + defendantId + "')]", CoreMatchers.notNullValue()));

        final JmsMessageConsumerClient publicHearingDetailChangedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.hearing-detail-changed").getMessageConsumerClient();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), getHearingJsonObject("public.listing.hearing-updated.json", caseId,
                hearingId, defendantId, newCourtCentreId, newCourtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);

        assertThat(retrieveMessageBody(publicHearingDetailChangedConsumer).isPresent(), is(true));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(newCourtCentreId)));
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

