package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithCommittingCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.stub.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsForExistingBookingId;
import static uk.gov.moj.cpp.progression.stub.AzureScheduleServiceStub.stubGetProvisionalBookedSlotsForNonExistingBookingId;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.ListingStub.verifyPostListCourtHearingWithCommittingCourt;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


import org.hamcrest.Matchers;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonArray;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class HearingResultedIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED = "public.hearing.hearing-offence-verdict-updated";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression" +
            ".prosecution-cases-referred-to-court";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents
            .createPublicConsumer(PUBLIC_PROGRESSION_EVENT_PROSECUTION_CASES_REFERRED_TO_COURT);
    private static final MessageConsumer messageConsumerHearingPopulatedToProbationCaseWorker = privateEvents.createPrivateConsumer("progression.events.hearing-populated-to-probation-caseworker");

    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
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
    private String reportingRestrictionId;

    private static final String PROGRESSION_EVENT_DEFENDANT_LISTING_STATUS_CHANGED ="progression.event.prosecutionCase-defendant-listing-status-changed-v2";
    private static final String PROGRESSION_EVENT_DEFENDANT_PROCEEDING_CONCLUDED_CHANGED="progression.event.defendant-proceeding-concluded-changed";


    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForReferToCourtOnHearingInitiated.close();
        messageConsumerHearingPopulatedToProbationCaseWorker.close();
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

    @Before
    public void setUp() {
        while(QueueUtil.retrieveMessageAsString(messageConsumerHearingPopulatedToProbationCaseWorker, 1L).isPresent());
        cleanViewStoreTables();
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        applicationId = randomUUID().toString();
        reportingRestrictionId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateCaseAtAGlance() throws Exception {
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED + ".json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);

        }


        final String custodyTimeLimit = "2018-09-14";
        final Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourtDefendantIds[0]", hasItem(defendantId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.name", hasItem("Derby Youth Court")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.courtCode", hasItem(5647)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.youthCourtId", hasItem("a4cd3c1d-be10-410d-a50d-e260cdbf6d19")),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is(custodyTimeLimit)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].judicialResultId", is("0f5b8757-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", is("Reporting Restriction Label")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", is("2020-10-20")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].previousDaysHeldInCustody", is(5)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].dateHeldInCustodySince", is("2018-08-10")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].indicatedPlea.offenceId", is("00000000-0000-0000-0000-000000000000")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].indicatedPlea.indicatedPleaDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].indicatedPlea.indicatedPleaValue", is("INDICATED_GUILTY")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].indicatedPlea.source", is("ONLINE"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

        final int ctlExpiryCountDown = (int) DAYS.between(LocalDate.now(), LocalDate.parse(custodyTimeLimit));
        final Matcher[] prosecutionCasesProgressionForCAAG = new Matcher[]{
                withJsonPath("$.defendants[0].id", is(defendantId)),
                withJsonPath("$.defendants[0].ctlExpiryDate", is(custodyTimeLimit)),
                withJsonPath("$.defendants[0].ctlExpiryCountDown", is(ctlExpiryCountDown)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.timeLimit", is(custodyTimeLimit)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.daysSpent", is(55)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].indicatedPlea.offenceId", is("00000000-0000-0000-0000-000000000000")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].indicatedPlea.indicatedPleaDate", is("2018-01-01")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].indicatedPlea.indicatedPleaValue", is("INDICATED_GUILTY")),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].indicatedPlea.source", is("ONLINE"))
        };
        pollProsecutionCasesProgressionForCAAG(caseId, prosecutionCasesProgressionForCAAG);

    }

    @Test
    public void shouldUpdateOffenceVerdictWhenRaisedPublicEvent() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();
        metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED)
                .withUserId(userId)
                .build();
        final JsonObject hearingVerdictUpdatedJson = getVerdictPublicEventPayload(hearingId);
        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, hearingVerdictUpdatedJson, metadata);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict.offenceId", Matchers.is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].verdict.verdictDate", Matchers.is("2017-02-20"))
        );

    }


    @Test
    public void shouldUpdateVerdictOfApplication() throws Exception {
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-applications-only.json",
                        applicationId, hearingId, caseId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForApplicationStatus(applicationId, "LISTED");

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED)
                .withUserId(userId)
                .build();
        final JsonObject hearingVerdictUpdatedJson = getVerdictPublicEventPayloadForApplication(hearingId, applicationId);
        sendMessage(messageProducerClientPublic, PUBLIC_HEARING_HEARING_OFFENCE_VERDICT_UPDATED, hearingVerdictUpdatedJson, metadata);


        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.courtApplications[0].id", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.applicationId", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].verdict.verdictDate", Matchers.is("2017-02-20"))
        );

    }

    @Test
    public void shouldRaiseHearingPopulatedToProbationCaseWorkerWhenPublicListingHearingConfirmed() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
            verifyInMessagingQueueForHearingPopulatedToProbationCaseWorker(hearingId, messageConsumerHearingPopulatedToProbationCaseWorker);
        }
    }

    @Test
    public void shouldKeepCpsOrganisationUpdateCaseAtAGlance() throws Exception {

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED + ".json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }


        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.cpsOrganisation", is("A01")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourtDefendantIds[0]", hasItem(defendantId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.name", hasItem("Derby Youth Court")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.courtCode", hasItem(5647)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.youthCourtId", hasItem("a4cd3c1d-be10-410d-a50d-e260cdbf6d19")),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

    }


    @Test
    public void shouldUpdateCaseAtAGlanceForNonExistingProvisionalBookingReference() throws Exception {
        stubGetProvisionalBookedSlotsForNonExistingBookingId();
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-with-next-hearing-in-mag-with-non-existing-booking-ref.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }


        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.name", hasItem(newCourtCentreName)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

    }

    @Test
    public void shouldUpdateHearingResulted() throws Exception {
        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            addProsecutionCaseToCrownCourt(caseId, defendantId);
            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-applications-only.json",
                        applicationId, hearingId, caseId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForApplicationStatus(applicationId, "LISTED");

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            sendMessage(messageProducerClientPublic,
                    PUBLIC_HEARING_RESULTED, getHearingWithStandAloneApplicationJsonObject("public.hearing.resulted-with-standalone-application.json",
                            applicationId, hearingId, caseId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final String hearingIdQueryResult = pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON);
        final JsonObject hearingJsonObject = getJsonObject(hearingIdQueryResult);
        JsonObject courtApplicationInHearing = hearingJsonObject.getJsonObject("hearing").getJsonArray("courtApplications").getJsonObject(0);
        assertThat(courtApplicationInHearing.getString("id"), equalTo(applicationId));
        final JsonObject hearingCourtCentre = hearingJsonObject.getJsonObject("hearing").getJsonObject("courtCentre");
        assertThat(hearingCourtCentre.getString("id"), equalTo(courtCentreId));

    }

    @Test
    public void shouldUseExistedCommittingCourtWhenHearingResultedSendDifferentCommittingCourt() throws Exception {
        stubGetProvisionalBookedSlotsForExistingBookingId();
        final String listedStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2020-12-15T18:32:04.238Z").toString();
        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {
            initiateCourtProceedingsWithCommittingCourt(caseId, defendantId, listedStartDateTime, earliestStartDateTime);
            pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

            hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        try (final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
                .createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed-v2")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

            doVerifyProsecutionCaseDefendantListingStatusChanged(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        }

        verifyInMessagingQueueForCasesReferredToCourts();

        try (final MessageConsumer messageConsumerProsecutionCaseResulted = privateEvents
                .createPrivateConsumer("progression.event.prosecution-cases-resulted")) {

            sendMessage(messageProducerClientPublic,
                    PUBLIC_HEARING_RESULTED, getHearingJsonObject("public.hearing.resulted-with-crown-committing-court.json", caseId,
                            hearingId, defendantId, newCourtCentreId, newCourtCentreName, reportingRestrictionId), metadataBuilder()
                            .withId(randomUUID())
                            .withName(PUBLIC_HEARING_RESULTED)
                            .withUserId(userId)
                            .build());
            final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseResulted);
            final JsonObject prosecutionCaseResulted = message.get();
            assertThat(prosecutionCaseResulted.getJsonObject("hearing").getString("id"), is(hearingId));
            assertThat(prosecutionCaseResulted.getJsonObject("committingCourt").getString("courtHouseType"), is("MAGISTRATES"));

        }

        verifyPostListCourtHearingWithCommittingCourt(caseId, defendantId, "MAGISTRATES");
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged) {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
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
                                            final String reportingRestrictionId) {
        final String payload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);

        return stringToJsonObjectConverter.convert(payload);
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

    public static void verifyInMessagingQueueForCasesReferredToCourts() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }


    private JsonObject getVerdictPublicEventPayload(final String hearingId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-verdict-updated.json")
                .replaceAll("HEARING_ID", hearingId);
        return stringToJsonObjectConverter.convert(strPayload);
    }
    private JsonObject getVerdictPublicEventPayloadForApplication(final String hearingId, final String applicationId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-verdict-updated-for-application.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }
}

