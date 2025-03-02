package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;

import static com.google.common.collect.Lists.newArrayList;
import com.google.common.io.Resources;
import static com.jayway.jsonpath.Criteria.where;
import com.jayway.jsonpath.Filter;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import org.json.JSONException;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffencesWithSpecificUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateOffencesHelper.OFFENCE_CODE;

public class HearingUpdatedIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String PUBLIC_HEARING_DETAIL_CHANGED = "public.hearing-detail-changed";

    private static final String PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-allocated-hearing";
    private static final String PUBLIC_PROGRESSION_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING = "public.progression.offences-removed-from-existing-allocated-hearing";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private final JmsMessageConsumerClient messageConsumerClientPublicForHearingDetailChanged = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_HEARING_DETAIL_CHANGED).getMessageConsumerClient();
    private final JmsMessageConsumerClient messageConsumerClientPublicOffenceRemovedFromHearing = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING).getMessageConsumerClient();
    private final JmsMessageConsumerClient messageConsumerClientProgressionPublicOffenceRemovedFromHearing = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING).getMessageConsumerClient();

    private static final String OFFENCE_ID = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String OFFENCE_ID2 = "4789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String caseId;
    private String defendantId;
    private String userId;
    private String courtCentreId;
    private String hearingId;
    private String caseUrnAlsoActingAsRandomReferences;

    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        caseUrnAlsoActingAsRandomReferences = generateUrn();
    }

    @Test
    public void shouldUpdateHearing() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(hearingId);
        final JsonEnvelope publicEventConfirmedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)));

        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObject(hearingId, updatedCourtCentreId);
        final JsonEnvelope publicEventUpdatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventUpdatedEnvelope);

        final Filter updatedHearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(updatedCourtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedHearingIdFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicForHearingDetailChanged);
    }


    @Test
    public void shouldUpdateHearingWhenDefendantMatched() throws Exception {
        final String prosecutionCaseId_1 = randomUUID().toString();
        final String defendantId_1 = randomUUID().toString();
        final String masterDefendantId_1 = randomUUID().toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        getHearingForDefendant(hearingId, new Matcher[]{
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].masterDefendantId", is(defendantId))
        });

        matchDefendant(caseId, defendantId, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", equalTo(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].masterDefendantId", equalTo(masterDefendantId_1)));

        getHearingForDefendant(hearingId, new Matcher[]{
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].masterDefendantId", is(masterDefendantId_1))
        });
    }

    @Test
    public void shouldUpdateHearingWhenCaseOffenceHasBeenUpdated() throws JSONException {
        final UUID offenceId1 = fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1");
        final UUID offenceId2 = randomUUID();
        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId, offenceId2);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed-one-defendant-two-offences-ids.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("RANDOM_OFFENCE_ID_2", offenceId2.toString())
        );
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath(
                        "$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)));

        // when remove an offence from hearing
        final JsonObject hearingOffenceRemovedJson = getOffenceRemovedFromExistngHearingJsonObject(hearingId, offenceId1.toString());
        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, userId), hearingOffenceRemovedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, publicEventEnvelope);
        getHearingForDefendant(hearingId, new Matcher[]{withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences.length()", equalTo(1))});

        // when update offences of case
        ProsecutionCaseUpdateOffencesHelper helper = new ProsecutionCaseUpdateOffencesHelper(caseId, defendantId, offenceId1.toString());
        helper.updateMultipleOffences(offenceId1.toString(), offenceId2.toString(), OFFENCE_CODE);
        verifyProbationHearingCommandInvoked(newArrayList(hearingId, "TFL123"));
    }

    @Test
    @Disabled("DD-33449")
    public void shouldHearingWithApplicationWhenLinkedApplicationToHearing() throws Exception {
        String courtCentreName = "Lavender Hill Magistrate's Court";

        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffencesWithSpecificUrn(caseId, defendantId, caseUrnAlsoActingAsRandomReferences);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        String courtApplicationId = randomUUID().toString();
        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtApplicationId, randomUUID().toString(), caseUrnAlsoActingAsRandomReferences, courtCentreId, courtCentreName);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application-adjorn.json");
        verifyProbationHearingCommandInvoked(newArrayList(hearingId, courtApplicationId));
    }

    @Test
    public void shouldRaiseProbationEventWhenAllocationChanged() throws Exception {
        final String applicationId = randomUUID().toString();
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplication(applicationId);

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final JsonEnvelope publicEventConfirmedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-application-with-linked-case.json",
                applicationId, hearingId, caseId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        pollForApplicationStatus(applicationId, "LISTED");
        verifyProbationHearingCommandInvoked(newArrayList(hearingId, "176a Lavender Hill", "B01LY00", "Sentence"));

        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedForApplicationJsonObject(hearingId, updatedCourtCentreId, applicationId);

        final JsonEnvelope publicEventUpdatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventUpdatedEnvelope);

        verifyProbationHearingCommandInvoked(newArrayList(hearingId, "d9bff7d8-6168-4163-ad77-3b98d61de174", "Application", "cf73207f-3ced-488a-82a0-3fba79c2ce05"));
    }

    @Test()
    public void shouldUpdateHearingWhenHearingListedWithListingNumber() throws IOException, JMSException, JSONException {
        String courtCentreName = "Lavender Hill Magistrate's Court";
        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId, caseUrnAlsoActingAsRandomReferences);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), randomUUID().toString(), caseUrnAlsoActingAsRandomReferences, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(1))
        );

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", is(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].listingNumber", is(1)));

        verifyProbationHearingCommandInvoked(newArrayList(hearingId));
    }

    @Test()
    public void shouldUpdateHearingWhenHearingListedWithListingNumberForSomeOffences() throws JMSException, JSONException {
        String courtCentreName = "Lavender Hill Magistrate's Court";
        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffencesWithSpecificUrn(caseId, defendantId, caseUrnAlsoActingAsRandomReferences);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, randomUUID().toString(), randomUUID().toString(), caseUrnAlsoActingAsRandomReferences, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForHearing(hearingId,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", is(defendantId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].listingNumber", is(1)),
                withoutJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[1]")
        );

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].id", is(defendantId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is(OFFENCE_ID)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].listingNumber", is(1)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is(OFFENCE_ID2)),
                withoutJsonPath("$.prosecutionCase.defendants[0].offences[1].listingNumber"));

        verifyProbationHearingCommandInvoked(newArrayList(hearingId));
        verifyInMessagingQueue(messageConsumerClientProgressionPublicOffenceRemovedFromHearing);
    }

    @Test
    public void shouldRemoveOffenceFromHearing() throws Exception {
        final String offenceId1 = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        final String offenceId2 = "4789ab16-0bb7-4ef1-87ef-c936bf0364f1";

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingConfirmedWithTwoOffencesJsonObject(hearingId, offenceId1, offenceId2);
        final JsonEnvelope publicEventConfirmedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter offencesFilter = filter(where("offences").size(2)
                .and("offence[0].id").is(offenceId1)
                .and("offence[1].id").is(offenceId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", offencesFilter)));

        final JsonObject hearingOffenceRemovedJson = getOffenceRemovedFromExistngHearingJsonObject(hearingId, offenceId1);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, userId), hearingOffenceRemovedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, publicEventEnvelope);

        final Filter updatedHearingIdFilter = filter(where("id").is(hearingId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter updatedOffencesFilter = filter(where("offences").size(1)
                .and("offence[0].id").is(offenceId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedHearingIdFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", updatedOffencesFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicOffenceRemovedFromHearing);
    }

    @Test
    public void shouldRemoveWholeDefendant() throws Exception {
        final String offenceId1 = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId, defendantId1, defendantId2);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId1);

        final JsonObject hearingConfirmedJson = getHearingConfirmedWithTwoDefendantsJsonObject(hearingId, defendantId1, defendantId2);
        final JsonEnvelope publicEventConfirmedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventConfirmedEnvelope);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter defendantsFilter = filter(where("defendants").size(2)
                .and("defendants[0].id").is(defendantId1)
                .and("defendants[1].id").is(defendantId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", defendantsFilter)));

        final JsonObject hearingOffenceRemovedJson = getOffenceRemovedFromExistngHearingJsonObject(hearingId, offenceId1);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, userId), hearingOffenceRemovedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_SELECTED_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING, publicEventEnvelope);

        final Filter updatedDefendantsFilter = filter(where("id").is(hearingId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));
        final Filter updatedOffencesFilter = filter(where("defendants").size(1)
                .and("defendants[0].id").is(defendantId2));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedDefendantsFilter)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[0].defendants[0]", updatedOffencesFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicOffenceRemovedFromHearing);
    }

    @Test
    public void shouldUpdateHearing_SendNotificationToParties() throws Exception {

        final JmsMessageConsumerClient messageConsumerEmailRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.email-requested").getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerPrintRequestPrivateEvent = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.print-requested").getMessageConsumerClient();

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps-no-email.json", randomUUID());

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingConfirmedJsonObject(hearingId);
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        final Filter hearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(courtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", hearingIdFilter)));

        final String updatedCourtCentreId = randomUUID().toString();
        final JsonObject hearingUpdatedJson = getHearingUpdatedJsonObjectWithNotificationFlagTrue(hearingId, updatedCourtCentreId);

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, userId), hearingUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, publicEventEnvelope);

        final Filter updatedHearingIdFilter = filter(where("id").is(hearingId)
                .and("courtCentre.id").is(updatedCourtCentreId)
                .and("hearingListingStatus").is("HEARING_INITIALISED"));

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath(compile("$.hearingsAtAGlance.hearings[?]", updatedHearingIdFilter)));
        verifyInMessagingQueue(messageConsumerClientPublicForHearingDetailChanged);
        doVerifyNotificationPrivateEvent(messageConsumerEmailRequestPrivateEvent, caseId);
        doVerifyNotificationPrivateEvent(messageConsumerPrintRequestPrivateEvent, caseId);
    }

    private void doVerifyNotificationPrivateEvent(final JmsMessageConsumerClient messageConsumerProgressionCommandEmail, final String caseId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerProgressionCommandEmail);
        assertThat(message.get(), Matchers.notNullValue());
        final JsonObject progressionCommandNotificationEvent = message.get();
        assertThat(progressionCommandNotificationEvent.getString("caseId", EMPTY), is(caseId));
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

    private JsonObject getHearingConfirmedWithTwoOffencesJsonObject(final String hearingId, final String offenceId1, final String offenceId2) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed-with-two-offences.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("OFFENCE_ID_1", offenceId1)
                        .replaceAll("OFFENCE_ID_2", offenceId2)
        );
    }

    private JsonObject getHearingConfirmedWithTwoDefendantsJsonObject(final String hearingId, final String defendantId1, final String defendantId2) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-confirmed-one-case-two-defendants.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("DEFENDANT_ID_1", defendantId1)
                        .replaceAll("DEFENDANT_ID_2", defendantId2)
        );
    }


    private JsonObject getOffenceRemovedFromExistngHearingJsonObject(final String hearingId, final String offenceIdToRemove) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.hearing.selected-offences-removed-from-existing-hearing.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("OFFENCE_ID_TO_REMOVE", offenceIdToRemove)
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

    private JsonObject getHearingUpdatedJsonObjectWithNotificationFlagTrue(final String hearingId, final String courtCentreId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated-notification-flag-true.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
        );
    }

    private JsonObject getHearingUpdatedForApplicationJsonObject(final String hearingId, final String courtCentreId, final String applicationId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated-for-application.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("APPLICATION_ID", applicationId)
        );
    }

    private static void verifyInMessagingQueue(final JmsMessageConsumerClient consumer) {
        final Optional<JsonObject> message = retrieveMessageBody(consumer);
        assertThat(message.isPresent(), is(true));
    }

    private JsonObject getHearingWithStandAloneApplicationJsonObject(final String path, final String applicationId, final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId,
                                            final String adjournedHearingId, final String reference,
                                            final String courtCentreId, final String courtCentreName) {
        return getHearingJsonObject(path, caseId, hearingId, defendantId, applicationId, adjournedHearingId, reference, courtCentreId, courtCentreName, "2020-01-01");
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId,
                                            final String adjournedHearingId, final String reference,
                                            final String courtCentreId, final String courtCentreName,
                                            final String orderedDate) {
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
                        .replaceAll("OFFENCE_ID", "3789ab16-0bb7-4ef1-87ef-c936bf0364f1")
                        .replaceAll("ORDERED_DATE", orderedDate)
        );
    }

}
