package uk.gov.moj.cpp.progression.cotr;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrAccessControl.mockCourtsUserCotrAccessControl;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrAccessControl.mockDefenceUserCotrAccessControl;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrAccessControl.mockProsecutionUserCotrAccessControl;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.addCaseProsecutor;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.addFurtherInfoDefenceCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.addFurtherInfoForProsecutionCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.changeCotrDefendants;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.createCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.queryAndVerifyCotrForm;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.serveDefendantCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.serveProsecutionCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.updateReviewNotes;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyCaseId;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyChangeDefendantsAddedCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyChangeDefendantsRemovedCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyCotrAndGetCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyCotrCreated;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyInMessagingQueue;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyServeDefendant;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyServeProsecution;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyUpdatingReviewNotes;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForCotrDetails;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForCotrTrialHearings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForGetTrialReadinessHearingDetails;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForSearchTrialReadiness;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStructuredPetQueryForCotr;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedDefendantsForDefenceOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.stub.MaterialStub.stubMaterialMetadata;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.studCotrFormServedNotificationCms;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubCotrReviewNotes;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getCotrDetailsMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getCotrTrialHearingsMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.CotrPdfContent;
import uk.gov.justice.core.courts.CreateCotr;
import uk.gov.justice.core.courts.Defence;
import uk.gov.justice.core.courts.Fields;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ServeDefendantCotr;
import uk.gov.justice.core.courts.Summary;
import uk.gov.justice.progression.courts.AddFurtherInfoDefenceCotrCommand;
import uk.gov.justice.progression.courts.AddFurtherInfoProsecutionCotr;
import uk.gov.justice.progression.courts.ChangeDefendantsCotr;
import uk.gov.justice.progression.courts.CotrNotes;
import uk.gov.justice.progression.courts.ReviewNoteType;
import uk.gov.justice.progression.courts.ReviewNotes;
import uk.gov.justice.progression.courts.UpdateReviewNotes;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.PolarQuestion;
import uk.gov.moj.cpp.progression.command.ServeProsecutionCotr;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.RestHelper;
import uk.gov.moj.cpp.progression.stub.DefenceStub;
import uk.gov.moj.cpp.progression.stub.DirectionsManagementStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;
import uk.gov.moj.cpp.progression.stub.MaterialStub;
import uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class CotrIT {

    private static final String DEFENCE_USER_ID = UUID.randomUUID().toString();
    private static final String PROSECUTOR_USER_ID = UUID.randomUUID().toString();
    private static final String COURTS_USER_ID = UUID.randomUUID().toString();

    private static final String COTR_CREATED_PRIVATE_EVENT = "progression.event.cotr-created";
    private static final String COTR_CREATED_PUBLIC_EVENT = "public.progression.cotr-created";

    private static final String SERVE_PROSECUTOR_COTR_CREATED_PUBLIC_EVENT = "public.progression.prosecution-cotr-served";
    private static final String SERVE_PROSECUTOR_COTR_CREATED_PRIVATE_EVENT = "progression.event.prosecution-cotr-served";

    private static final String SERVE_DEFENDANT_COTR_PUBLIC_EVENT = "public.progression.serve-defendant-cotr";
    private static final String SERVE_DEFENDANT_COTR_PRIVATE_EVENT = "progression.event.defendant-cotr-served";

    private static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED = "public.prosecutioncasefile.cps-serve-cotr-submitted";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED = "public.prosecutioncasefile.cps-update-cotr-submitted";

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();


    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS = "progression.command.initiate-court-proceedings.json";
    private static final String PROGRESSION_QUERY_TRIAL_HEARINGS_FOR_PROSECUTION_CASE = "application/vnd.progression.query.cotr-trial-hearings+json";

    private static final String FURTHER_INFO_FOR_PROSECUTION_COTR_ADDED_PRIVATE_EVENT = "progression.event.further-info-for-prosecution-cotr-added";
    private static final String FURTHER_INFO_FOR_PROSECUTION_COTR_ADDED_PUBLIC_EVENT = "public.progression.further-info-for-prosecution-cotr-added";

    private static final String REVIEW_NOTES_UPDATED_PRIVATE_EVENT = "progression.event.review-notes-updated";
    private static final String REVIEW_NOTES_UPDATED_PUBLIC_EVENT = "public.progression.cotr-review-notes-updated";

    private static final String DEFENDANT_ADDED_TO_COTR_PRIVATE_EVENT = "progression.event.defendant-added-to-cotr";
    private static final String DEFENDANT_REMOVED_FROM_COTR_PRIVATE_EVENT = "progression.event.defendant-removed-from-cotr";
    private static final String DEFENDANTS_CHANGED_IN_COTR_PUBLIC_EVENT = "public.progression.defendants-changed-in-cotr";
    public static final String PROGRESSION_QUERY_GET_CASE_HEARINGS = "application/vnd.progression.query.casehearings+json";

    private static final String DOCUMENT_TEXT = STRING.next();
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static final JmsMessageConsumerClient consumerForCotrCreated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(COTR_CREATED_PRIVATE_EVENT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForPublicForCotrCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(COTR_CREATED_PUBLIC_EVENT).getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForServeProsecutionCotrCreated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(SERVE_PROSECUTOR_COTR_CREATED_PRIVATE_EVENT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForPublicServeProsecutionCotrCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(SERVE_PROSECUTOR_COTR_CREATED_PUBLIC_EVENT).getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForFurtherInfoForProsecutionCotrAdded = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(FURTHER_INFO_FOR_PROSECUTION_COTR_ADDED_PRIVATE_EVENT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForPublicEventFurtherInfoForProsecutionCotrAdded = newPublicJmsMessageConsumerClientProvider().withEventNames(FURTHER_INFO_FOR_PROSECUTION_COTR_ADDED_PUBLIC_EVENT).getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForReviewNotesUpdated = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(REVIEW_NOTES_UPDATED_PRIVATE_EVENT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForPublicEventReviewNotesUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(REVIEW_NOTES_UPDATED_PUBLIC_EVENT).getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForDefendantAddedToCotr = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(DEFENDANT_ADDED_TO_COTR_PRIVATE_EVENT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForDefendantRemovedFromCotr = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(DEFENDANT_REMOVED_FROM_COTR_PRIVATE_EVENT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForPublicEventDefendantsChangedIdCotr = newPublicJmsMessageConsumerClientProvider().withEventNames(DEFENDANTS_CHANGED_IN_COTR_PUBLIC_EVENT).getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForPublicServeDefendantCotr = newPublicJmsMessageConsumerClientProvider().withEventNames(SERVE_DEFENDANT_COTR_PUBLIC_EVENT).getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForServeDefendantCotr = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(SERVE_DEFENDANT_COTR_PRIVATE_EVENT).getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForPublicCourtDocumentAdded = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-document-added").getMessageConsumerClient();
    private static final JmsMessageConsumerClient consumerForCourDocumentNotified = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-document-send-to-cps").getMessageConsumerClient();

    private static final JmsMessageConsumerClient consumerForPetFormCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.pet-form-created").getMessageConsumerClient();
    private static final JmsMessageConsumerClient publicEventConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
    private static final JmsMessageConsumerClient privateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.cps-prosecutor-updated").getMessageConsumerClient();
    private static final JmsMessageConsumerClient caseProsecutorUpdatedPrivateEventsConsumer = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.case-cps-prosecutor-updated").getMessageConsumerClient();
    private String caseId;
    private String materialIdActive;
    private String materialIdDeleted;
    private String defendantId;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;

    @BeforeAll
    public static void setUpClass() {
        stubDocumentCreate(DOCUMENT_TEXT);
        MaterialStub.stubMaterialMetadata();
        studCotrFormServedNotificationCms();
        stubCotrReviewNotes();
    }

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        defendantId = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
    }

    @Test
    @Disabled("DD-33449")
    public void shouldTestCotrCommands() throws Exception {

        final UUID caseId = randomUUID();
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");

        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId.toString(), defendantId1.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        pollForCotrTrialHearings(caseId.toString(), getCotrTrialHearingsMatchers(hearingId, Collections.emptyList()));

        // createCotr
        final UUID cotrId = randomUUID();
        final String cotrIdString = cotrId.toString();
        final CreateCotr createCotr = CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withCaseId(caseId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withDefendantIds(Arrays.asList(defendantId1, defendantId2))
                .build();

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        createCotr(createCotr, DEFENCE_USER_ID);
        final JsonObject cotrEvent = verifyCotrAndGetCotr(consumerForCotrCreated, cotrIdString);
        verifyCotrCreated(cotrEvent, createCotr);

        verifyCotrAndGetCotr(consumerForPublicForCotrCreated, cotrIdString);
        queryAndVerifyCotrForm(caseId, cotrId);

        pollForCotrDetails(caseId.toString(), getCotrDetailsMatchers(cotrIdString, hearingId, Collections.emptyList()));

        // ServeProsecutionCotr
        mockProsecutionUserCotrAccessControl(PROSECUTOR_USER_ID);
        ServeProsecutionCotr serveProsecutionCotr = ServeProsecutionCotr.serveProsecutionCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withApplyForThePtrToBeVacated(PolarQuestion.polarQuestion().withAnswer("Y").withDetails("details").withAttentionRequired(Boolean.TRUE).build())
                .withIsTheTimeEstimateCorrect(PolarQuestion.polarQuestion().withAnswer("Y").withDetails("details").withAttentionRequired(Boolean.TRUE).build())
                .withHasAllDisclosureBeenProvided(PolarQuestion.polarQuestion().withAnswer("Y").withDetails("details").withAttentionRequired(Boolean.TRUE).build())
                .withSubmissionId(randomUUID())
                .build();
        UsersAndGroupsStub.stubGetOrganisationDetailForTypes("stub-data/usersgroups.get-organisations-details.json", PROSECUTOR_USER_ID);
        serveProsecutionCotr(serveProsecutionCotr, PROSECUTOR_USER_ID);
        final JsonObject serveProsecutionCotrEvent = verifyCotrAndGetCotr(consumerForServeProsecutionCotrCreated, cotrIdString);
        verifyServeProsecution(serveProsecutionCotrEvent, serveProsecutionCotr);

        verifyCotrAndGetCotr(consumerForPublicServeProsecutionCotrCreated, cotrIdString);

        // ServeDefendantCotr
        addCaseProsecutor(caseId.toString(), privateEventsConsumer, caseProsecutorUpdatedPrivateEventsConsumer);
        mockCourtsUserCotrAccessControl(COURTS_USER_ID);
        ServeDefendantCotr serveDefendantCotr = ServeDefendantCotr.serveDefendantCotr()
                .withCotrId(cotrId)
                .withDefendantId(defendantId1)
                .withServedByName("name")
                .withDefendantFormData("formData")
                .withPdfContent(CotrPdfContent.cotrPdfContent()
                        .withHeading("heading")
                        .withSubHeading("subheading")
                        .withSummary(Arrays.asList(Summary.summary().withTitle("summaryTile").withTitleDescription("withDesc")
                                .withFields(Arrays.asList(Fields.fields().withLabel("Label1").withValue("value").withDetails("details")
                                        .build())).build()))
                        .withDefence(Arrays.asList(Defence.defence().withTitle("defenceTile").withTitleDescription("withDesc")
                                .withFields(Arrays.asList(Fields.fields().withLabel("Label1").withValue("value").withDetails("details")
                                        .build())).build()))
                        .build())
                .build();

        serveDefendantCotr(serveDefendantCotr, COURTS_USER_ID);
        final JsonObject serveDefendantCotrEvent = verifyCotrAndGetCotr(consumerForServeDefendantCotr, cotrIdString);
        verifyServeDefendant(serveDefendantCotrEvent, serveDefendantCotr);
        verifyCaseId(serveDefendantCotrEvent, caseId.toString());
        verifyCotrAndGetCotr(consumerForPublicServeDefendantCotr, cotrIdString);
        verifyInMessagingQueue(consumerForPublicCourtDocumentAdded);
        verifyInMessagingQueue(consumerForCourDocumentNotified);
        // verifyCotrFormServedNotifyCms(); failing on jenkins, need investigation
        final AddFurtherInfoDefenceCotrCommand addFurtherInfoDefenceCotr = AddFurtherInfoDefenceCotrCommand.addFurtherInfoDefenceCotrCommand()
                .withDefendantId(defendantId1)
                .withMessage("message")
                .withIsCertificationReady(Boolean.FALSE)
                .withAddedBy(randomUUID())
                .withAddedByName("erica")
                .build();

        addFurtherInfoDefenceCotr(addFurtherInfoDefenceCotr, PROSECUTOR_USER_ID, createCotr.getCotrId().toString());
        // archive COTR
//        archiveCotr(createCotr.getCotrId().toString(), COURTS_USER_ID);
//        verifyCotrAndGetCotr(consumerForCotrArchive, cotrIdString);
//        verifyCotrAndGetCotr(consumerForPublicCotrArchive, cotrIdString);
    }

    @Test
    public void shouldFetchTrialHearingsForAProsecutionCase() throws Exception {

        final String prosecutionCaseId = randomUUID().toString();
        final String materialIdActive = randomUUID().toString();
        final String materialIdDeleted = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String referralReasonId = randomUUID().toString();
        final String listedStartDateTime = ZonedDateTimes.fromString("2022-06-30T18:32:04.238Z").toString();
        final String earliestStartDateTime = ZonedDateTimes.fromString("2022-05-30T18:32:04.238Z").toString();
        final String defendantDOB = LocalDate.now().minusYears(15).toString();

        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, prosecutionCaseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);

        poll(requestParams(getReadUrl("/prosecutioncases/" + prosecutionCaseId + "/cotr-trial-hearings"), PROGRESSION_QUERY_TRIAL_HEARINGS_FOR_PROSECUTION_CASE)
                .withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.trialHearings.length()", is(1))
                        )));
    }

    @Test
    public void shouldRaisePublicEventWhenAddingFurtherInfoForProsecutionCotr() throws Exception {
        final UUID caseId = randomUUID();
        final UUID cotrId = randomUUID();
        final String cotrIdString = cotrId.toString();
        final String message = "Prosecution Cotr Further Info";
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");

        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId.toString(), defendantId1.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        // createCotr
        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        final CreateCotr createCotr = CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withHearingId(UUID.fromString(hearingId))
                .withCaseId(caseId)
                .withDefendantIds(Arrays.asList(defendantId1, defendantId2))
                .build();

        createCotr(createCotr, DEFENCE_USER_ID);
        final JsonObject cotrEvent = verifyCotrAndGetCotr(consumerForCotrCreated, cotrIdString);
        verifyCotrCreated(cotrEvent, createCotr);

        verifyCotrAndGetCotr(consumerForPublicForCotrCreated, cotrIdString);

        mockProsecutionUserCotrAccessControl(PROSECUTOR_USER_ID);

        final AddFurtherInfoProsecutionCotr addFurtherInfoProsecutionCotr = AddFurtherInfoProsecutionCotr.addFurtherInfoProsecutionCotr()
                .withCotrId(cotrId)
                .withMessage(message)
                .withIsCertificationReady(Boolean.FALSE)
                .withAddedBy(randomUUID())
                .withAddedByName("erica")
                .build();

        addFurtherInfoForProsecutionCotr(addFurtherInfoProsecutionCotr, PROSECUTOR_USER_ID);
        final JsonObject cotrInPrivateEvent = verifyCotrAndGetCotr(consumerForFurtherInfoForProsecutionCotrAdded, cotrIdString);
        assertThat(cotrInPrivateEvent.getString("message"), is(addFurtherInfoProsecutionCotr.getMessage()));

        verifyCotrAndGetCotr(consumerForPublicEventFurtherInfoForProsecutionCotrAdded, cotrIdString);
    }

    @Test
    public void shouldRaisePublicEventWhenAddingDefendantToCotr() throws Exception {
        final UUID caseId = randomUUID();
        final UUID cotrId = randomUUID();
        final String cotrIdString = cotrId.toString();
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");

        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId.toString(), defendantId1.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        // createCotr
        final CreateCotr createCotr = CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withCaseId(caseId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withDefendantIds(Arrays.asList(defendantId1))
                .withSubmissionId(cotrId)
                .withHasAllEvidenceToBeReliedOnBeenServed(PolarQuestion.polarQuestion()
                        .withDetails("HasAllEvidenceToBeReliedOnBeenServed")
                        .withAnswer("Yes")
                        .build())
                .build();

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        createCotr(createCotr, DEFENCE_USER_ID);
        final JsonObject cotrFromEvent = verifyCotrAndGetCotr(consumerForCotrCreated, cotrIdString);
        verifyCotrCreated(cotrFromEvent, createCotr);

        verifyCotrAndGetCotr(consumerForPublicForCotrCreated, cotrIdString);

        final ChangeDefendantsCotr changeDefendantsCotr = ChangeDefendantsCotr.changeDefendantsCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withAddedDefendantIds(Arrays.asList(defendantId2))
                .build();

        changeCotrDefendants(changeDefendantsCotr, DEFENCE_USER_ID);
        final JsonObject cotrIdInPrivateEvent = verifyCotrAndGetCotr(consumerForDefendantAddedToCotr, cotrIdString);
        verifyChangeDefendantsAddedCotr(cotrIdInPrivateEvent, changeDefendantsCotr);

        verifyCotrAndGetCotr(consumerForPublicEventDefendantsChangedIdCotr, cotrIdString);
    }

    @Test
    public void shouldRaisePublicEventWhenRemovingDefendantFromCotr() throws Exception {
        final UUID caseId = randomUUID();
        final UUID cotrId = randomUUID();
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");
        final String cotrIdString = cotrId.toString();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId.toString(), defendantId1.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        // createCotr
        final CreateCotr createCotr = CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withCaseId(caseId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withDefendantIds(Arrays.asList(defendantId1, defendantId2))
                .build();
        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        createCotr(createCotr, DEFENCE_USER_ID);
        final JsonObject cotrFromEvent = verifyCotrAndGetCotr(consumerForCotrCreated, cotrIdString);
        verifyCotrCreated(cotrFromEvent, createCotr);

        verifyCotrAndGetCotr(consumerForPublicForCotrCreated, cotrIdString);

        final ChangeDefendantsCotr changeDefendantsCotr = ChangeDefendantsCotr.changeDefendantsCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withRemovedDefendantIds(Arrays.asList(defendantId2))
                .build();
        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);
        changeCotrDefendants(changeDefendantsCotr, DEFENCE_USER_ID);
        final JsonObject cotrInPrivateEvent = verifyCotrAndGetCotr(consumerForDefendantRemovedFromCotr, cotrIdString);
        verifyChangeDefendantsRemovedCotr(cotrInPrivateEvent, changeDefendantsCotr);

        verifyCotrAndGetCotr(consumerForPublicEventDefendantsChangedIdCotr, cotrIdString);
    }

    @Test
    public void shouldRaisePublicEventWhenUpdatingReviewNotes() throws Exception {
        final UUID caseId = randomUUID();
        final UUID cotrId = randomUUID();
        final String cotrIdString = cotrId.toString();
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");
        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId.toString(), defendantId1.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        // createCotr
        final CreateCotr createCotr = CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withCaseId(caseId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withDefendantIds(Arrays.asList(defendantId1, defendantId2))
                .build();

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        createCotr(createCotr, DEFENCE_USER_ID);
        final JsonObject cotrFromEvent = verifyCotrAndGetCotr(consumerForCotrCreated, cotrIdString);
        verifyCotrCreated(cotrFromEvent, createCotr);

        verifyCotrAndGetCotr(consumerForPublicForCotrCreated, cotrIdString);

        final UpdateReviewNotes updateReviewNotes = UpdateReviewNotes.updateReviewNotes()
                .withCotrId(cotrId)
                .withCotrNotes(Arrays.asList(CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.CASE_PROGRESSION)
                        .withReviewNotes(Arrays.asList(ReviewNotes.reviewNotes()
                                .withId(UUID.fromString("05acfc09-f672-41b9-afa0-acc74e4b2b8a"))
                                .withComment("Value 1")
                                .build()))
                        .build()))
                .build();

        mockCourtsUserCotrAccessControl(COURTS_USER_ID);
        updateReviewNotes(updateReviewNotes, COURTS_USER_ID);
        final JsonObject cotrInPrivateEvent = verifyCotrAndGetCotr(consumerForReviewNotesUpdated, cotrIdString);
        verifyUpdatingReviewNotes(cotrInPrivateEvent, updateReviewNotes);

        verifyCotrAndGetCotr(consumerForPublicEventReviewNotesUpdated, cotrIdString);
    }

    @Test
    public void shouldSearchTrialReadiness() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");
        final String courtCentreId = "88cdf36e-93e4-41b0-8277-17d9dba7f06f";
        final LocalDate startDate = LocalDate.now();
        final LocalDate endDate = LocalDate.now();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId.toString(), defendantId1.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        ListingStub.stubListingCotrSearch("stub-data/listing.search.hearings.json", hearingId);
        DirectionsManagementStub.stubCaseDirectionsList("stub-data/directionmanagement.query.case-directions-list.json");

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);

        final String response = pollForSearchTrialReadiness(courtCentreId, startDate.toString(), endDate.toString(), "N");
        final JsonObject responseObject = stringToJsonObjectConverter.convert(response);
        assertThat(responseObject.getJsonArray("trialReadinessHearings").getJsonObject(0).getString("id"), is(hearingId));

    }

    @Test
    public void shouldGetTrialReadinessHearingDetails() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID formId = randomUUID();
        final UUID petId = randomUUID();

        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId.toString(), defendantId1.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        DirectionsManagementStub.stubCaseDirectionsList("stub-data/directionmanagement.query.case-directions-list.json");
        DefenceStub.stubForDefendantIdpcMetadata("stub-data/defence.query.defendant.idpc.metadata.json", defendantId1.toString());

        final String responseForCaseQuery = pollProsecutionCasesProgressionFor(caseId.toString());
        final JsonObject caseObject = stringToJsonObjectConverter.convert(responseForCaseQuery);

        final String offenceId = caseObject.getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getString("id");

        final JsonObject payload = createObjectBuilder()
                .add("petId", petId.toString())
                .add("caseId", caseId.toString())
                .add("formId", formId.toString())
                .add("petDefendants", createArrayBuilder().add(createObjectBuilder().add("defendantId", defendantId1.toString())
                        .build()))
                .add("petFormData", createObjectBuilder().build().toString())
                .build();

        setupLoggedInUsersPermissionQueryStub();
        setupMaterialStructuredPetQueryForCotr(petId.toString());
        stubDocumentCreate(DOCUMENT_TEXT);
        stubMaterialMetadata();

        final Response responseForCreatePetForm = postCommand(getWriteUrl("/pet"), "application/vnd.progression.create-pet-form+json", payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));
        verifyInMessagingQueueForPetFormCreated();

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId1);

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);

        final String response = pollForGetTrialReadinessHearingDetails(hearingId);
        final JsonObject responseObject = stringToJsonObjectConverter.convert(response);
        assertThat(responseObject.getJsonObject("trialSummary"), is(notNullValue()));

    }

    private void verifyInMessagingQueueForPetFormCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPetFormCreated);
        assertTrue(message.isPresent());
    }

    public static void queryAndVerifyPetCaseDetail(final UUID caseId, final UUID petId, final UUID defendantId) {
        poll(requestParams(AbstractTestHelper.getReadUrl(String.format("/prosecutioncases/%s/pet", caseId)),
                "application/vnd.progression.query.pets-for-case+json")
                .withHeader("CJSCPPUID", randomUUID()))
                .timeout(30, SECONDS)
                .until(status().is(OK),
                        payload().isJson(
                                Matchers.allOf(
                                        withJsonPath("$.pets[0].defendants[0].caseId", Matchers.is(caseId.toString())),
                                        withJsonPath("$.pets[0].defendants[0].defendantId", is(defendantId.toString())),
                                        withJsonPath("$.pets[0].petId", is(petId.toString()))
                                )));
    }

    private static String getProgressionCaseHearings(final String caseId, final Matcher... matchers) {
        return poll(requestParams(getReadUrl("/prosecutioncases/" + caseId),
                PROGRESSION_QUERY_GET_CASE_HEARINGS)
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
    }

    private static String getCotrCaseDetails(final String caseId, final Matcher... matchers) {
        String response = poll(requestParams(getReadUrl("/prosecutioncases/" + caseId + "/cotr-details-prosecutioncase"),
                "application/vnd.progression.query.cotr.details.prosecutioncase+json")
                .withHeader(USER_ID, randomUUID()))
                .timeout(40, TimeUnit.SECONDS)
                .until(status().is(OK), payload().isJson(allOf(matchers)))
                .getPayload();
        return response;

    }

    @Test
    public void shouldCreateServetCpsCotrForm() throws IOException {

        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when
        verifyInMessagingQueueForProsecutionCaseCreated();

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.hearings.length()", is(1)),
                withJsonPath("$.hearings[0].id", is(notNullValue())),
                withJsonPath("$.hearings[0].courtCentre.id", is("88cdf36e-93e4-41b0-8277-17d9dba7f06f")),
                withJsonPath("$.hearings[0].courtCentre.name", is("Lavender Hill Magistrate's Court")),
                withJsonPath("$.hearings[0].courtCentre.roomId", is("9e4932f7-97b2-3010-b942-ddd2624e4dd8")),
                withJsonPath("$.hearings[0].courtCentre.roomName", is("Courtroom 01")),
                withJsonPath("$.hearings[0].hearingDays[0].sittingDay", is(notNullValue()))
        ));

        UsersAndGroupsStub.stubGetOrganisationDetailForTypes("stub-data/usersgroups.get-organisations-details.json", PROSECUTOR_USER_ID);

        final String cpsDefendantId = randomUUID().toString();
        final JsonObject cpsServeCotrSubmittedPublicEvent = buildPayloadForCpsServeCotrSubmitted(cpsDefendantId);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, randomUUID()), cpsServeCotrSubmittedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, publicEventEnvelope);

        assertThat(getPrivateEventsCotrCreatedConsumer(), is(notNullValue()));
        assertThat(getPrivateEventsCotrServedConsumer(), is(notNullValue()));
        final JsonEnvelope publicEvent = getPublicEventsCotrCreatedConsumer();
        assertThat(publicEvent, is(notNullValue()));
        assertThat(publicEvent.payloadAsJsonObject(), notNullValue());

        final Matcher[] caseWithCpsDefendantIdMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", CoreMatchers.is(cpsDefendantId))
                )
        );

        final String updatedResponseForCaseQuery = pollProsecutionCasesProgressionFor(caseId, caseWithCpsDefendantIdMatchers);
    }


    @Test
    public void shouldUpdateServetCpsCotrForm() throws IOException {

        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when
        verifyInMessagingQueueForProsecutionCaseCreated();

        getProgressionCaseHearings(caseId, anyOf(
                withJsonPath("$.hearings.length()", is(1)),
                withJsonPath("$.hearings[0].id", is(notNullValue())),
                withJsonPath("$.hearings[0].courtCentre.id", is("88cdf36e-93e4-41b0-8277-17d9dba7f06f")),
                withJsonPath("$.hearings[0].courtCentre.name", is("Lavender Hill Magistrate's Court")),
                withJsonPath("$.hearings[0].courtCentre.roomId", is("9e4932f7-97b2-3010-b942-ddd2624e4dd8")),
                withJsonPath("$.hearings[0].courtCentre.roomName", is("Courtroom 01")),
                withJsonPath("$.hearings[0].hearingDays[0].sittingDay", is(notNullValue()))
        ));

        //CPS - Create COTR
        UsersAndGroupsStub.stubGetOrganisationDetailForTypes("stub-data/usersgroups.get-organisations-details.json", PROSECUTOR_USER_ID);

        final String cpsDefendantId = randomUUID().toString();
        final JsonObject cpsServeCotrSubmittedPublicEvent = buildPayloadForCpsServeCotrSubmitted(cpsDefendantId);

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, randomUUID()), cpsServeCotrSubmittedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, publicEventEnvelope);

        verifyInMessagingQueueForCotrCreated();
        final JsonEnvelope jsonCotrCreatedEnvelope = getPrivateEventsCotrCreatedConsumer();
        assertThat(jsonCotrCreatedEnvelope, is(notNullValue()));
        final JsonObject privatePayload = jsonCotrCreatedEnvelope.payloadAsJsonObject();
        final String submissionId = privatePayload.getString("submissionId");

        final JsonObject cpsServeCotrUpdatePublicEvent = buildPayloadForCpsUpdateCotrSubmitted(submissionId);

        final JsonEnvelope publicEventUpdateEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED, randomUUID()), cpsServeCotrUpdatePublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED, publicEventUpdateEnvelope);

        assertThat(getPrivateEventsCotrServedConsumer(), is(notNullValue()));
        final JsonEnvelope publicEvent = getPublicEventsCotrServedConsumer();
        assertThat(publicEvent, is(notNullValue()));
        final JsonObject eventPayload = publicEvent.payloadAsJsonObject();
        assertThat(eventPayload, notNullValue());

        String updateCotr = getCotrCaseDetails(caseId, anyOf(
                withJsonPath("$.cotrDetails[0].id", is(notNullValue()))
        ));

    }

    private JsonObject buildPayloadForCpsServeCotrSubmitted(final String cpsDefendantId) throws IOException {
        final String inputEvent = getPayload("stub-data/cps-serve-cotr-submitted.json")
                .replace("%CASE_ID%", caseId)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%CPS_DEFENDANT_ID%", cpsDefendantId)
                .replace("%SUBMISSION_ID%", randomUUID().toString());


        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        return readData;
    }

    private JsonObject buildPayloadForCpsUpdateCotrSubmitted(final String submissionId) throws IOException {
        final String inputEvent = getPayload("stub-data/cps-serve-cotr-update.json")
                .replace("%CASE_ID%", caseId)
                .replace("%SUBMISSION_ID%", randomUUID().toString())
                .replace("%COTR_ID%", submissionId);


        final JsonObject readData = stringToJsonObjectConverter.convert(inputEvent);
        return readData;
    }

    private void verifyInMessagingQueueForCotrCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPublicForCotrCreated);
        assertTrue(message.isPresent());
    }


    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumer);
        assertTrue(message.isPresent());
    }

    public JsonEnvelope getPrivateEventsCotrCreatedConsumer() {
        return retrieveMessage(consumerForCotrCreated).orElse(null);
    }

    public JsonEnvelope getPublicEventsCotrCreatedConsumer() {
        return retrieveMessage(consumerForPublicForCotrCreated).orElse(null);
    }

    public JsonEnvelope getPrivateEventsCotrServedConsumer() {
        return retrieveMessage(consumerForServeProsecutionCotrCreated).orElse(null);
    }

    public JsonEnvelope getPublicEventsCotrServedConsumer() {
        return retrieveMessage(consumerForPublicServeProsecutionCotrCreated).orElse(null);
    }
}
