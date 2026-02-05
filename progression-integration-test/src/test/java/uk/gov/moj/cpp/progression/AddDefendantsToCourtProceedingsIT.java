package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ListingStub.setupListingAnyAllocationQuery;
import static uk.gov.moj.cpp.progression.stub.ListingStub.setupListingAnyFutureAllocationQuery;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.AddDefendantsToCourtProceedings;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.Utilities;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDefendantsToCourtProceedingsIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddDefendantsToCourtProceedingsIT.class);

    static final String PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";
    static final String PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_CASE = "public.progression.defendants-added-to-case";
    private static final String PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON = "application/vnd.progression.add-defendants-to-court-proceedings+json";

    private final JmsMessageConsumerClient messageConsumerClientPublicCourtProceedings = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS).getMessageConsumerClient();
    private final JmsMessageConsumerClient messageConsumerClientPublicCase = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANTS_ADDED_TO_CASE).getMessageConsumerClient();
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private String caseId;
    private String defendantId;
    private String offenceId;
    private String caseUrn;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        offenceId = randomUUID().toString();
        caseUrn = generateUrn();
    }

    @Test
    public void shouldInvokeDefendantsAddedToCaseWithoutListingRequests() throws Exception {
        final String offenceId3 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String defendantId3 = randomUUID().toString();

        setupListingAnyAllocationQuery(caseUrn, LocalDate.now().toString(), "stub-data/listing.any-allocation.search.hearings.json");

        //Create prosecution case
        addProsecutionCaseToCrownCourt(caseId, defendantId, caseUrn);
        verifyPostListCourtHearing(caseId, defendantId);

        //Create payload for
        final String startDateTime = ZonedDateTime.now().plusWeeks(2).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(
                true, caseId, defendantId, defendantId2, offenceId, startDateTime);
        final String addDefendantsToCourtProceedingsJson = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        //Post command progression.add-defendants-to-court-proceedings
        postCommand(getWriteUrl("/adddefendantstocourtproceedings"),
                PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON,
                addDefendantsToCourtProceedingsJson);

        //Verify the defendants and check the duplicate is not added
        verifyDefendantsAddedInViewStore(caseId, defendantId2);

        verifyPostListCourtHearing(caseId, defendantId2);
    }

    @Test
    public void shouldListHearingRequestsInvokePublicMessage() throws Exception {

        final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
        final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";

        final String userId = randomUUID().toString();
        final String courtCentreId = "3d2cf089-63ec-4bbf-a330-402540f200ba";
        final String defendantId2 = randomUUID().toString();
        final String startDateTime = ZonedDateTime.now().plusWeeks(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
        setupListingAnyFutureAllocationQuery(caseUrn, startDateTime.substring(0, 10), "stub-data/listing.any-allocation.search.future-hearings.json", startDateTime);

        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final JsonObject prosecutionCaseJson = getJsonObject(pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId)));

        final Optional<JsonValue> defendantHearing = prosecutionCaseJson.getJsonObject("hearingsAtAGlance")
                .getJsonArray("defendantHearings")
                .stream().filter(def1 -> ((JsonObject) def1).getString("defendantId").equals(defendantId))
                .findFirst();

        String hearingId = randomUUID().toString();

        if (defendantHearing.isPresent()) {
            hearingId = ((JsonObject) defendantHearing.get()).getJsonArray("hearingIds").get(0).toString().replaceAll("\"", "");
            final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(caseId, hearingId, defendantId, courtCentreId);

            final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
            messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        }

        //otherwise it creates new hearing with status as HEARING_INITIALISED
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(caseId, hearingId, defendantId, courtCentreId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        //Create payload for
        final AddDefendantsToCourtProceedings addDefendantsToCourtProceedings = buildAddDefendantsToCourtProceedings(
                true, caseId, defendantId, defendantId2, offenceId, startDateTime);
        final String addDefendantsToCourtProceedingsJson = Utilities.JsonUtil.toJsonString(addDefendantsToCourtProceedings);

        //Post command progression.add-defendants-to-court-proceedings
        postCommand(getWriteUrl("/adddefendantstocourtproceedings"),
                PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_JSON,
                addDefendantsToCourtProceedingsJson);

        //Verify the defendants and check the duplicate is not added
        verifyDefendantsAddedInViewStore(caseId, defendantId2);

        //Verify public.progression.defendants-added-to-court-proceedings message in the public queue
        verifyInMessagingQueueForDefendantsAddedToCourtHearings(caseId, defendantId2);
        verifyInMessagingQueueForDefendantsAddedToCase(caseId, defendantId2);
    }

    private void verifyInMessagingQueueForDefendantsAddedToCourtHearings(final String caseId, final String defendantId) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerClientPublicCourtProceedings, isJson(Matchers.allOf(
                withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].prosecutionCaseId", is(caseId)),
                withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].defendantId", is(defendantId))
        )));
        assertNotNull(message);
    }

    private void verifyInMessagingQueueForDefendantsAddedToCase(final String caseId, final String defendantId) {
        final JsonPath message = retrieveMessageAsJsonPath(messageConsumerClientPublicCase, isJson(Matchers.allOf(
                withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].prosecutionCaseId", is(caseId)),
                withJsonPath("$.listHearingRequests[0].listDefendantRequests[0].defendantId", is(defendantId))
        )));
        assertNotNull(message);
    }

    private void verifyDefendantsAddedInViewStore(final String caseId, final String defendantId) {

        LOGGER.info("Verifying for defendants added to caseId {} and defendant id {}", caseId, defendantId);

        Matcher[] matchers = {
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + defendantId + "')]", notNullValue()),
                withJsonPath("$.prosecutionCase.defendants[?(@.id=='" + defendantId + "')].prosecutionCaseId", hasItem(caseId)),
        };

        pollProsecutionCasesProgressionFor(caseId, matchers);
    }

    private AddDefendantsToCourtProceedings buildAddDefendantsToCourtProceedings(
            final boolean forAdded, final String caseId, final String defendantId,
            final String defendantId2, final String offenceId, final String startDateTime) {

        final List<Defendant> defendantsList = new ArrayList<>();

        final Offence offence = Offence.offence()
                .withId(UUID.fromString(offenceId))
                .withOffenceDefinitionId(randomUUID())
                .withOffenceCode("TFL123")
                .withOffenceTitle("TFL Ticket Dodger")
                .withWording("TFL ticket dodged")
                .withStartDate(LocalDate.of(2019, 5, 1))
                .withCount(0)
                .build();

        //past duplicate defendant
        final Defendant defendant = Defendant.defendant()
                .withId(UUID.fromString(defendantId))
                .withMasterDefendantId(UUID.fromString(defendantId))
                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();
        defendantsList.add(defendant);

        //Add defendant
        final Defendant defendant2 = Defendant.defendant()
                .withId(UUID.fromString(defendantId2))
                .withMasterDefendantId(UUID.fromString(defendantId2))
                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();

        if (forAdded) {
            defendantsList.add(defendant2);
        }

        //Duplicate defendant in current payload
        final Defendant defendant3 = Defendant.defendant()
                .withId(UUID.fromString(defendantId2))
                .withMasterDefendantId(UUID.fromString(defendantId2))
                .withCourtProceedingsInitiated(ZonedDateTime.now(ZoneId.of("UTC")))
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withOffences(Collections.singletonList(offence))
                .build();

        if (forAdded) {
            defendantsList.add(defendant3);
        }

        final ListDefendantRequest listDefendantRequest2 = ListDefendantRequest.listDefendantRequest()
                .withProsecutionCaseId(UUID.fromString(caseId))
                .withDefendantOffences(Collections.singletonList(UUID.fromString(offenceId)))
                .withDefendantId(defendant2.getId())
                .build();

        final HearingType hearingType = HearingType.hearingType().withId(randomUUID()).withDescription("TO_JAIL").build();
        final CourtCentre courtCentre = CourtCentre.courtCentre().withId(UUID.fromString("3d2cf089-63ec-4bbf-a330-402540f200ba")).withName("Court Name 5").build();

        final ListHearingRequest listHearingRequest = ListHearingRequest.listHearingRequest()
                .withCourtCentre(courtCentre).withHearingType(hearingType)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withListDefendantRequests(Collections.singletonList(listDefendantRequest2))
                .withEarliestStartDateTime(ZonedDateTime.now().plusWeeks(1))
                .withListedStartDateTime(ZonedDateTime.parse(startDateTime))
                .withEstimateMinutes(20)
                .build();

        return AddDefendantsToCourtProceedings
                .addDefendantsToCourtProceedings()
                .withDefendants(defendantsList)
                .withListHearingRequests(Collections.singletonList(listHearingRequest))
                .build();

    }


    private JsonObject getHearingConfirmedJsonObject(final String caseId, final String hearingId, final String defendantId, final String courtCentreId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.listing.hearing-confirmed.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingUpdatedJsonObject(final String caseId, final String hearingId, final String defendantId, final String courtCentreId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.listing.hearing-updated.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }


}
