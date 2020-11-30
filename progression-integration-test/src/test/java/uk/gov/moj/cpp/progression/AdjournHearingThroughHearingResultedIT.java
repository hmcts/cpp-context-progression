package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addLinkedCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsForNonExistingBookingId;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class AdjournHearingThroughHearingResultedIT extends AbstractIT {

    private static final String PROGRESSION_EXTEND_HEARING_JSON = "application/vnd.progression.extend-hearing+json";
    private static final String APPLICATION_REFERRED_AND_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged =
            privateEvents.createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer consumerForCourtApplicationCreated =
            publicEvents.createConsumer("public.progression.court-application-created");
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;
    private String applicationId;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
        consumerForCourtApplicationCreated.close();
    }

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        applicationId = randomUUID().toString();
    }

    @Test
    public void shouldAdjournApplicationToNewHearing() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, randomUUID().toString(), reference, courtCentreId, courtCentreName);
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        String courtApplicationId = randomUUID().toString();
        addLinkedCourtApplication(caseId, defendantId, courtApplicationId, "progression.command.create-court-application-for-adjournment.json");
        verifyInMessagingQueueForCourtApplicationCreated(reference + "-1");

        Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.linkedCaseId", is(caseId))
        };

        pollForApplication(courtApplicationId, applicationMatchers);

        final String extendedHearingString = getReferApplicationToCourtJsonPayload(hearingId, courtApplicationId, caseId, defendantId, reference,
                "progression.command.refer-application-to-court-for-adjournment.json");

        //progression.extend-hearing command triggered
        postCommand(getWriteUrl("/referapplicationtocourt"),
                PROGRESSION_EXTEND_HEARING_JSON,
                extendedHearingString);

        //Verifying the public.progression.events.hearing-extended event in the MQ topic
        verifyPostHearingExtendedEvent(hearingId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted.application-adjourned-to-next-hearing.json", caseId,
                        hearingId, defendantId, applicationId, adjournedHearingId, reference, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        verifyPostHearingExtendedEvent(adjournedHearingId);

    }

    @Test
    public void shouldAdjournApplicationToNewHearingInMagistrate() throws Exception {
        stubGetProvisionalBookedSlotsForNonExistingBookingId();
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        String response = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        JsonObject prosecutionCasesJsonObject = getJsonObject(response);
        String reference = prosecutionCasesJsonObject.getJsonObject("prosecutionCase").getJsonObject("prosecutionCaseIdentifier").getString("prosecutionAuthorityReference");

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, randomUUID().toString(), reference, courtCentreId, courtCentreName);
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        String courtApplicationId = randomUUID().toString();
        addLinkedCourtApplication(caseId, defendantId, courtApplicationId, "progression.command.create-court-application-for-adjournment.json");
        verifyInMessagingQueueForCourtApplicationCreated(reference + "-1");

        Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(courtApplicationId)),
                withJsonPath("$.courtApplication.linkedCaseId", is(caseId))
        };

        pollForApplication(courtApplicationId, applicationMatchers);

        final String extendedHearingString = getReferApplicationToCourtJsonPayload(hearingId, courtApplicationId, caseId, defendantId, reference,
                "progression.command.refer-application-to-court-for-adjournment.json");

        //progression.extend-hearing command triggered
        postCommand(getWriteUrl("/referapplicationtocourt"),
                PROGRESSION_EXTEND_HEARING_JSON,
                extendedHearingString);

        //Verifying the public.progression.events.hearing-extended event in the MQ topic
        verifyPostHearingExtendedEvent(hearingId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String adjournedHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted.application-adjourned-to-next-hearing-in-mag-with-non-existing-booking-ref.json", caseId,
                        hearingId, defendantId, applicationId, adjournedHearingId, reference, newCourtCentreId, newCourtCentreName), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        verifyNoPostHearingExtendedEvent();

        final Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].id", hasItem(hearingId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.hearingsAtAGlance.courtApplications[0].applicant.defendant.isYouth", is(true)),
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendants[0].address.address2", is("Address 2 Had No Provisional booking ID"))};

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);
    }


    private void verifyPostHearingExtendedEvent(final String hearingId) {
        final MessageConsumer hearingExtendedConsumer = publicEvents.createConsumer(APPLICATION_REFERRED_AND_HEARING_EXTENDED);
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(hearingExtendedConsumer);
        assertTrue(message.isPresent());
        assertThat(message.get().getString("hearingId"), equalTo(hearingId));
    }

    private String getReferApplicationToCourtJsonPayload(final String hearingId,
                                                         final String courtApplicationId,
                                                         final String prosecutionCaseId,
                                                         final String defendantId,
                                                         final String applicationReference,
                                                         final String fileName) throws IOException {
        return Resources.toString(Resources.getResource(fileName), Charset.defaultCharset())
                .replace("RANDOM_HEARING_ID", hearingId)
                .replace("RANDOM_APPLICATION_ID", courtApplicationId)
                .replace("RANDOM_CASE_ID", prosecutionCaseId)
                .replace("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_REFERENCE", applicationReference);

    }

    private static void verifyInMessagingQueueForCourtApplicationCreated(String arn) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent());
        String arnResponse = message.get().getString("arn");
        assertThat(arnResponse, equalTo(arn));
    }

     private String doVerifyProsecutionCaseDefendantListingStatusChanged(){
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId,
                                            final String adjournedHearingId, final String reference,
                                            final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("ADJOURNED_ID", adjournedHearingId)
                        .replaceAll("APPLICATION_REF", reference)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

    private void verifyNoPostHearingExtendedEvent() {
        final MessageConsumer hearingExtendedConsumer = publicEvents.createConsumer(APPLICATION_REFERRED_AND_HEARING_EXTENDED);
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(hearingExtendedConsumer);
        assertThat(message.isPresent(), is(false));
    }
}

