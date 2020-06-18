package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingUpdatedIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String PUBLIC_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);
    private static final MessageConsumer messageConsumerClientPublicForHearingDetailChanged = publicEvents
            .createConsumer(PUBLIC_HEARING_DETAIL_CHANGED);


    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String caseId;
    private String defendantId;
    private String userId;
    private String courtCentreId;
    private String hearingId;

    @Before
    public void setUp() {
        stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
    }

    @Test
    public void shouldUpdateHearing() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId));

        final Metadata hearingConfirmedMetadata = createMetadata(PUBLIC_LISTING_HEARING_CONFIRMED);
        final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(hearingId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, hearingConfirmedMetadata);

        verifyInMessagingQueue(messageConsumerClientPublicForReferToCourtOnHearingInitiated);

        pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(courtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].hearingListingStatus", equalTo("HEARING_INITIALISED"))
        });

        final String updatedCourtCentreId = randomUUID().toString();
        final Metadata hearingUpdatedMetadata = createMetadata(PUBLIC_LISTING_HEARING_UPDATED);
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, updatedCourtCentreId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_UPDATED, hearingUpdatedJson, hearingUpdatedMetadata);

        pollProsecutionCasesProgressionFor(caseId, new Matcher[]{
                withJsonPath("$.hearingsAtAGlance.hearings[0].courtCentre.id", equalTo(updatedCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].hearingListingStatus", equalTo("HEARING_INITIALISED"))
        });
        verifyInMessagingQueue(messageConsumerClientPublicForHearingDetailChanged);

    }


    private Metadata createMetadata(final String eventName) {
        return JsonEnvelope.metadataBuilder()
                .withId(randomUUID())
                .withName(eventName)
                .withUserId(userId)
                .build();
    }

    private JsonObject getHearingConfirmedJsonObject(final String hearingId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingUpdatedJsonObject(final String hearingId, final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private static void verifyInMessagingQueue(final MessageConsumer consumer) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumer);
        assertTrue(message.isPresent());
    }

}
