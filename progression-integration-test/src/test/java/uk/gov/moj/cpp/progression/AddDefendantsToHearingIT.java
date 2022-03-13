package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.util.Utilities;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AddDefendantsToHearingIT extends AbstractIT {

    private static final String PUBLIC_HEARING_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT = "public.events.hearing.prosecution-case-created-in-hearing";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final MessageConsumer messageConsumerDefendantsAndListingHearingRequestsStoredPrivateEvent = privateEvents.createPrivateConsumer("progression.event.defendants-and-listing-hearing-requests-stored");
    private static final MessageConsumer messageConsumerDefendantsAddedToCourtProceedingsPrivateEvent = privateEvents.createPrivateConsumer("progression.event.defendants-added-to-court-proceedings");
    private static final MessageConsumer messageConsumerProsecutionCaseCreatedInHearingPrivateEvent = privateEvents.createPrivateConsumer("progression.event.prosecution-case-created-in-hearing");
    private static final MessageConsumer messageConsumerDefendantsAndListingHearingRequestsAddedPrivateEvent = privateEvents.createPrivateConsumer("progression.event.defendants-and-listing-hearing-requests-added");
    private static final MessageConsumer messageConsumerDefendantsAddedToCourtProceedingsPublicEvent = publicEvents.createPublicConsumer("public.progression.defendants-added-to-court-proceedings");
    private static final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Before
    public void setUp() {
        while(QueueUtil.retrieveMessageAsString(messageConsumerHearingPopulatedToProbationCaseWorker, 1L).isPresent());
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldStoreDefendantWhenProsecutionCaseHasBeenCreatedInHearing() throws IOException {

        final String userId = randomUUID().toString();
        final String prosecutionCaseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String offenceId = randomUUID().toString();
        final String courtCentreId = "f8254db1-1683-483e-afb3-b87fde5a0a26";
        final String urn = generateUrn();


        // add prosecution case
        addProsecutionCaseToCrownCourtAndVerify(prosecutionCaseId, defendantId, urn);
        final String hearingId =  pollProsecutionCasesProgressionAndReturnHearingId(prosecutionCaseId, defendantId, getProsecutionCaseMatchers(prosecutionCaseId, defendantId, Arrays.asList(
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='"+ defendantId +"')]", notNullValue()))));
        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", hearingId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        prosecutionCaseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());


        Matcher[] caseUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(prosecutionCaseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(equalTo(courtCentreId))),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true))
        };

        pollProsecutionCasesProgressionFor(prosecutionCaseId, caseUpdatedMatchers);

        // add defendants but prosecution case has not been created in hearing
        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(prosecutionCaseId, defendantId1, offenceId, courtCentreId);
        final String addDefendantsToCourtProceedingsJson = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        postCommand(getWriteUrl("/adddefendantstocourtproceedings"),
                "application/vnd.progression.add-defendants-to-court-proceedings+json",
                addDefendantsToCourtProceedingsJson);

        // verify events - DefendantsAddedToCourtProceedings and DefendantsAndListingHearingRequestsStored
        // store the defendants
        verifyInMessagingQueueForDefendantsAddedToCourtProceedings();
        verifyInMessagingQueueForDefendantsAndListingHearingRequestsStored();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT)
                .withUserId(userId)
                .build();

        final JsonObject prosecutionCaseCreatedInHearingJson = getProsecutionCaseCreatedInHearingObject(prosecutionCaseId);

        // prosecution case has been created in hearing
        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT, prosecutionCaseCreatedInHearingJson, metadata);

        // verify events - ProsecutionCaseCreatedInHearing, DefendantsAndListingHearingRequestsAdded and DefendantsAddedToCourtProceedingsPublicEvent
        // release the defendants
        verifyInMessagingQueueForProsecutionCaseCreatedInHearing();
        verifyInMessagingQueueForDefendantsAndListingHearingRequestsAdded();
        verifyInMessagingQueueForDefendantsAddedToCourtProceedingsPublicEvent();
        final JsonPath probationEventRaised = QueueUtil.retrieveMessage(messageConsumerHearingPopulatedToProbationCaseWorker, isJson(Matchers.allOf(
                        withJsonPath("$.hearing.id", CoreMatchers.is(hearingId)),
                        withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", CoreMatchers.is(defendantId1))
                )
        ));
        assertNotNull(probationEventRaised);
    }

    @Test
    public void shouldNotStoreDefendantWhenProsecutionCaseHasAlreadyBeenCreatedInHearing() throws IOException {

        final String userId = randomUUID().toString();
        final String prosecutionCaseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String offenceId = randomUUID().toString();
        final String courtCentreId = "f8254db1-1683-483e-afb3-b87fde5a0a26";
        final String urn = generateUrn();

        ListingStub.stubListingSearchHearingsQuery("stub-data/listing.search.hearings.json", UUID.randomUUID().toString());

        // add prosecution case
        addProsecutionCaseToCrownCourtAndVerify(prosecutionCaseId, defendantId, urn);

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT)
                .withUserId(userId)
                .build();

        final JsonObject prosecutionCaseCreatedInHearingJson = getProsecutionCaseCreatedInHearingObject(prosecutionCaseId);

        // prosecution case has been created in hearing
        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_PROSECUTION_CASE_CREATED_IN_HEARING_EVENT, prosecutionCaseCreatedInHearingJson, metadata);

        // verify events - ProsecutionCaseCreatedInHearing
        verifyInMessagingQueueForProsecutionCaseCreatedInHearing();


        // add defendants but prosecution case has not been created in hearing
        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(prosecutionCaseId, defendantId1, offenceId, courtCentreId);
        final String addDefendantsToCourtProceedingsJson = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        postCommand(getWriteUrl("/adddefendantstocourtproceedings"),
                "application/vnd.progression.add-defendants-to-court-proceedings+json",
                addDefendantsToCourtProceedingsJson);

        // verify events - DefendantsAddedToCourtProceedings, DefendantsAndListingHearingRequestsAdded and DefendantsAddedToCourtProceedingsPublicEvent
        verifyInMessagingQueueForDefendantsAddedToCourtProceedings();
        verifyInMessagingQueueForDefendantsAndListingHearingRequestsAdded();
        verifyInMessagingQueueForDefendantsAddedToCourtProceedingsPublicEvent();

    }


    private AddDefendantsToCourtProceedings buildAddDefendantsToCourtProceedings(final String prosecutionCaseId, final String defendantId, final String offenceId, final String courtCentreId) {

        final List<Defendant> defendants = new ArrayList<>();

        final Offence offence = Offence.offence()
                .withId(fromString(offenceId))
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 5, 1))
                .withCount(0)
                .build();

        final Defendant defendant = Defendant.defendant()
                .withId(fromString(defendantId))
                .withMasterDefendantId(fromString(defendantId))
                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                .withProsecutionCaseId(fromString(prosecutionCaseId))
                .withOffences(Collections.singletonList(offence))
                .build();
        defendants.add(defendant);

        final ListDefendantRequest listDefendantRequest = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(fromString(prosecutionCaseId))
                .withDefendantOffences(Collections.singletonList(fromString(offenceId)))
                .withDefendantId(defendant.getId())
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(fromString(courtCentreId)).withName("Court Name 5").build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(Collections.singletonList(listDefendantRequest))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(2))
                .withListedStartDateTime(ZonedDateTime.parse("2050-05-18T09:01:01.001Z"))
                .withEstimateMinutes(20)
                .build();

        return AddDefendantsToCourtProceedings
                .addDefendantsToCourtProceedings()
                .withDefendants(defendants)
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    public static void main(String[] args) {
        System.out.println(ZonedDateTime.now().plusDays(3));
    }

    private void addProsecutionCaseToCrownCourtAndVerify(final String caseId, final String defendantId, final String urn) throws IOException {
        PreAndPostConditionHelper.addProsecutionCaseToCrownCourt(caseId, defendantId, urn);

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.id", is(caseId)))));
    }

    private JsonObject getProsecutionCaseCreatedInHearingObject(final String prosecutionCaseId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.hearing.prosecution-case-created-in-hearing.json")
                        .replaceAll("PROSECUTION_CASE_ID", prosecutionCaseId)
        );
    }

    private static void verifyInMessagingQueueForDefendantsAndListingHearingRequestsStored() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerDefendantsAndListingHearingRequestsStoredPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForDefendantsAddedToCourtProceedings() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerDefendantsAddedToCourtProceedingsPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForProsecutionCaseCreatedInHearing() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseCreatedInHearingPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForDefendantsAndListingHearingRequestsAdded() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerDefendantsAndListingHearingRequestsAddedPrivateEvent);
        assertTrue(message.isPresent());
    }

    private static void verifyInMessagingQueueForDefendantsAddedToCourtProceedingsPublicEvent() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerDefendantsAddedToCourtProceedingsPublicEvent);
        assertTrue(message.isPresent());
    }


}
