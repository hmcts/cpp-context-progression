package uk.gov.moj.cpp.progression.cotr;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.progression.courts.ChangeDefendantsCotr.changeDefendantsCotr;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
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
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyCotrAndGetCotr;
import static uk.gov.moj.cpp.progression.cotr.cotrHelper.CotrHelper.verifyInMessagingQueue;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForCaseHearings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForCotrDetails;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForCotrTrialHearings;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForGetTrialReadinessHearingDetails;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForSearchTrialReadiness;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStructuredPetQueryForCotr;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedDefendantsForDefenceOrganisation;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForDefendantIdpcMetadata;
import static uk.gov.moj.cpp.progression.stub.DirectionsManagementStub.stubCaseDirectionsList;
import static uk.gov.moj.cpp.progression.stub.ListingStub.stubListingCotrSearch;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.stubCotrFormServedNotificationCms;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubCotrReviewNotes;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetailForTypes;
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
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.PolarQuestion;
import uk.gov.moj.cpp.progression.command.ServeProsecutionCotr;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import io.restassured.response.Response;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CotrIT extends AbstractIT {

    private static final String DEFENCE_USER_ID = randomUUID().toString();
    private static final String PROSECUTOR_USER_ID = randomUUID().toString();
    private static final String COURTS_USER_ID = randomUUID().toString();

    private static final String PROGRESSION_PUBLIC_EVENT_COTR_CREATED = "public.progression.cotr-created";
    private static final String PROGRESSION_PUBLIC_EVENT_PROSECUTOR_COTR_SERVED = "public.progression.prosecution-cotr-served";
    private static final String PROGRESSION_PUBLIC_EVENT_SERVE_DEFENDANT_COTR = "public.progression.serve-defendant-cotr";
    private static final String PROGRESSION_PUBLIC_EVENT_COTR_REVIEW_NOTES_UPDATED = "public.progression.cotr-review-notes-updated";
    private static final String PROGRESSION_PUBLIC_EVENT_DEFENDANTS_CHANGED_IN_COTR = "public.progression.defendants-changed-in-cotr";
    private static final String PROGRESSION_PUBLIC_EVENT_FURTHER_INFO_FOR_PROSECUTION_COTR_ADDED = "public.progression.further-info-for-prosecution-cotr-added";

    private static final String PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED = "public.prosecutioncasefile.cps-serve-cotr-submitted";
    private static final String PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED = "public.prosecutioncasefile.cps-update-cotr-submitted";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final String PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS = "progression.command.initiate-court-proceedings.json";
    private static final String PROGRESSION_QUERY_TRIAL_HEARINGS_FOR_PROSECUTION_CASE = "application/vnd.progression.query.cotr-trial-hearings+json";

    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private final JmsMessageConsumerClient consumerForPublicForCotrCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(PROGRESSION_PUBLIC_EVENT_COTR_CREATED).getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPublicServeProsecutionCotrCreated = newPublicJmsMessageConsumerClientProvider().withEventNames(PROGRESSION_PUBLIC_EVENT_PROSECUTOR_COTR_SERVED).getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPublicEventFurtherInfoForProsecutionCotrAdded = newPublicJmsMessageConsumerClientProvider().withEventNames(PROGRESSION_PUBLIC_EVENT_FURTHER_INFO_FOR_PROSECUTION_COTR_ADDED).getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPublicEventReviewNotesUpdated = newPublicJmsMessageConsumerClientProvider().withEventNames(PROGRESSION_PUBLIC_EVENT_COTR_REVIEW_NOTES_UPDATED).getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPublicEventDefendantsChangedIdCotr = newPublicJmsMessageConsumerClientProvider().withEventNames(PROGRESSION_PUBLIC_EVENT_DEFENDANTS_CHANGED_IN_COTR).getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPublicServeDefendantCotr = newPublicJmsMessageConsumerClientProvider().withEventNames(PROGRESSION_PUBLIC_EVENT_SERVE_DEFENDANT_COTR).getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForPublicCourtDocumentAdded = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.court-document-added").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForCourDocumentNotified = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.court-document-send-to-cps").getMessageConsumerClient();

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
        stubCotrFormServedNotificationCms();
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
    public void shouldTestCotrCommands() throws Exception {

        final UUID caseId = randomUUID();
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");

        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        pollProsecutionCasesProgressionFor(caseId.toString(), getProsecutionCaseMatchers(caseId.toString(), defendantId1.toString()));

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId.toString(), defendantId1.toString());

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
        stubGetOrganisationDetailForTypes("stub-data/usersgroups.get-organisations-details.json", PROSECUTOR_USER_ID);
        serveProsecutionCotr(serveProsecutionCotr, PROSECUTOR_USER_ID);

        verifyCotrAndGetCotr(consumerForPublicServeProsecutionCotrCreated, cotrIdString);

        // ServeDefendantCotr
        addCaseProsecutor(caseId.toString());
        mockCourtsUserCotrAccessControl(COURTS_USER_ID);
        ServeDefendantCotr serveDefendantCotr = ServeDefendantCotr.serveDefendantCotr()
                .withCotrId(cotrId)
                .withDefendantId(defendantId1)
                .withServedByName("name")
                .withDefendantFormData("formData")
                .withPdfContent(CotrPdfContent.cotrPdfContent()
                        .withHeading("heading")
                        .withSubHeading("subheading")
                        .withSummary(singletonList(Summary.summary().withTitle("summaryTile").withTitleDescription("withDesc")
                                .withFields(singletonList(Fields.fields().withLabel("Label1").withValue("value").withDetails("details")
                                        .build())).build()))
                        .withDefence(singletonList(Defence.defence().withTitle("defenceTile").withTitleDescription("withDesc")
                                .withFields(singletonList(Fields.fields().withLabel("Label1").withValue("value").withDetails("details")
                                        .build())).build()))
                        .build())
                .build();

        serveDefendantCotr(serveDefendantCotr, COURTS_USER_ID);
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

        pollForResponse("/prosecutioncases/" + prosecutionCaseId + "/cotr-trial-hearings",
                PROGRESSION_QUERY_TRIAL_HEARINGS_FOR_PROSECUTION_CASE,
                randomUUID().toString(),
                withJsonPath("$.trialHearings.length()", is(1))
        );
    }

    @Test
    public void shouldRaisePublicEventsWhenPerformingBasicOperationsToCotr() throws Exception {
        /**
         * This test verifies the following:
         *  - Create COTR form
         *  - Add a defendant to the COTR form
         *  - Remove a defendant from the COTR form
         *  - Add further info for prosecution COTR
         *  - Update review notes on COTR form
         */

        final UUID caseId = randomUUID();
        final UUID cotrId = randomUUID();
        final String cotrIdString = cotrId.toString();
        final UUID defendantId1 = UUID.fromString("bd8d80d0-e995-40fb-9f59-340a53a1a688");
        final UUID defendantId2 = UUID.fromString("c46ca4a8-39ae-440d-9016-e12e936313e3");

        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId.toString(), defendantId1.toString(), defendantId2.toString());
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId.toString(), defendantId1.toString());

        // Create a COTR form
        final CreateCotr createCotr = CreateCotr.createCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withCaseId(caseId)
                .withCaseUrn("CaseUrn")
                .withHearingDate(LocalDate.now().toString())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCenter("Lavender hill mags")
                .withDefendantIds(List.of(defendantId1))
                .withSubmissionId(cotrId)
                .withHasAllEvidenceToBeReliedOnBeenServed(PolarQuestion.polarQuestion()
                        .withDetails("HasAllEvidenceToBeReliedOnBeenServed")
                        .withAnswer("Yes")
                        .build())
                .build();

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        createCotr(createCotr, DEFENCE_USER_ID);
        verifyCotrAndGetCotr(consumerForPublicForCotrCreated, cotrIdString);

        // Add defendant 2 to COTR form
        final ChangeDefendantsCotr changeAddDefendantToCotr = changeDefendantsCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withAddedDefendantIds(List.of(defendantId2))
                .build();

        changeCotrDefendants(changeAddDefendantToCotr, DEFENCE_USER_ID);

        verifyCotrAndGetCotr(consumerForPublicEventDefendantsChangedIdCotr, cotrIdString, defendantId2.toString());

        // Remove defendant 2 from COTR form
        final ChangeDefendantsCotr changeRemoveDefendantToCotr = changeDefendantsCotr()
                .withCotrId(cotrId)
                .withHearingId(UUID.fromString(hearingId))
                .withRemovedDefendantIds(List.of(defendantId1))
                .build();
        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);
        stubForAssociatedDefendantsForDefenceOrganisation("stub-data/defence.query.get-associated-defendants.json", DEFENCE_USER_ID);
        changeCotrDefendants(changeRemoveDefendantToCotr, DEFENCE_USER_ID);

        verifyCotrAndGetCotr(consumerForPublicEventDefendantsChangedIdCotr, cotrIdString, defendantId1.toString());

        // Add further prosecution info to COTR form
        final AddFurtherInfoProsecutionCotr addFurtherInfoProsecutionCotr = AddFurtherInfoProsecutionCotr.addFurtherInfoProsecutionCotr()
                .withCotrId(cotrId)
                .withMessage("Prosecution Cotr Further Info")
                .withIsCertificationReady(Boolean.FALSE)
                .withAddedBy(randomUUID())
                .withAddedByName("erica")
                .build();

        addFurtherInfoForProsecutionCotr(addFurtherInfoProsecutionCotr, PROSECUTOR_USER_ID);

        verifyCotrAndGetCotr(consumerForPublicEventFurtherInfoForProsecutionCotrAdded, cotrIdString);

        // Update review notes on COTR form
        final UpdateReviewNotes updateReviewNotes = UpdateReviewNotes.updateReviewNotes()
                .withCotrId(cotrId)
                .withCotrNotes(singletonList(CotrNotes.cotrNotes()
                        .withReviewNoteType(ReviewNoteType.CASE_PROGRESSION)
                        .withReviewNotes(singletonList(ReviewNotes.reviewNotes()
                                .withId(UUID.fromString("05acfc09-f672-41b9-afa0-acc74e4b2b8a"))
                                .withComment("Value 1")
                                .build()))
                        .build()))
                .build();

        mockCourtsUserCotrAccessControl(COURTS_USER_ID);
        updateReviewNotes(updateReviewNotes, COURTS_USER_ID);
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
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId.toString(), defendantId1.toString());

        stubListingCotrSearch("stub-data/listing.search.hearings.json", hearingId);
        stubCaseDirectionsList("stub-data/directionmanagement.query.case-directions-list.json");

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
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId.toString(), defendantId1.toString());

        stubCaseDirectionsList("stub-data/directionmanagement.query.case-directions-list.json");
        stubForDefendantIdpcMetadata("stub-data/defence.query.defendant.idpc.metadata.json", defendantId1.toString());

        pollProsecutionCasesProgressionFor(caseId.toString());

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

        final Response responseForCreatePetForm = postCommand(getWriteUrl("/pet"), "application/vnd.progression.create-pet-form+json", payload.toString());
        assertThat(responseForCreatePetForm.getStatusCode(), is(ACCEPTED.getStatusCode()));

        //query pets by caseId
        queryAndVerifyPetCaseDetail(caseId, petId, defendantId1);

        mockDefenceUserCotrAccessControl(DEFENCE_USER_ID);

        pollForGetTrialReadinessHearingDetails(hearingId,
                withJsonPath("$.trialSummary", is(notNullValue())));
    }

    @Test
    public void shouldCreateServeCpsCotrForm() throws IOException {

        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when
        pollForCaseHearings(caseId,
                withJsonPath("$.hearings.length()", is(1)),
                withJsonPath("$.hearings[0].id", is(notNullValue())),
                withJsonPath("$.hearings[0].courtCentre.id", is("88cdf36e-93e4-41b0-8277-17d9dba7f06f")),
                withJsonPath("$.hearings[0].courtCentre.name", is("Lavender Hill Magistrate's Court")),
                withJsonPath("$.hearings[0].courtCentre.roomId", is("9e4932f7-97b2-3010-b942-ddd2624e4dd8")),
                withJsonPath("$.hearings[0].courtCentre.roomName", is("Courtroom 01")),
                withJsonPath("$.hearings[0].hearingDays[0].sittingDay", is(notNullValue()))
        );

        stubGetOrganisationDetailForTypes("stub-data/usersgroups.get-organisations-details.json", PROSECUTOR_USER_ID);

        final String cpsDefendantId = randomUUID().toString();
        final JsonObject cpsServeCotrSubmittedPublicEvent = buildPayloadForCpsServeCotrSubmitted(cpsDefendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, randomUUID()), cpsServeCotrSubmittedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, publicEventEnvelope);

        final JsonEnvelope publicEvent = getPublicEventsCotrCreatedConsumer();
        assertThat(publicEvent, is(notNullValue()));
        assertThat(publicEvent.payloadAsJsonObject(), notNullValue());

        final Matcher[] caseWithCpsDefendantIdMatchers = getProsecutionCaseMatchers(caseId, defendantId,
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", CoreMatchers.is(cpsDefendantId))
                )
        );

        pollProsecutionCasesProgressionFor(caseId, caseWithCpsDefendantIdMatchers);
    }

    @Test
    public void shouldUpdateServeCpsCotrForm() throws IOException {

        //given
        initiateCourtProceedings(PROGRESSION_COMMAND_INITIATE_COURT_PROCEEDINGS, caseId, defendantId, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        //when

        pollForCaseHearings(caseId,
                withJsonPath("$.hearings.length()", is(1)),
                withJsonPath("$.hearings[0].id", is(notNullValue())),
                withJsonPath("$.hearings[0].courtCentre.id", is("88cdf36e-93e4-41b0-8277-17d9dba7f06f")),
                withJsonPath("$.hearings[0].courtCentre.name", is("Lavender Hill Magistrate's Court")),
                withJsonPath("$.hearings[0].courtCentre.roomId", is("9e4932f7-97b2-3010-b942-ddd2624e4dd8")),
                withJsonPath("$.hearings[0].courtCentre.roomName", is("Courtroom 01")),
                withJsonPath("$.hearings[0].hearingDays[0].sittingDay", is(notNullValue()))
        );

        //CPS - Create COTR
        stubGetOrganisationDetailForTypes("stub-data/usersgroups.get-organisations-details.json", PROSECUTOR_USER_ID);

        final String cpsDefendantId = randomUUID().toString();
        final JsonObject cpsServeCotrSubmittedPublicEvent = buildPayloadForCpsServeCotrSubmitted(cpsDefendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, randomUUID()), cpsServeCotrSubmittedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_SERVE_COTR_SUBMITTED, publicEventEnvelope);

        final JsonObject cotrEvent = verifyAndRetrieveCotrCreatedPublicEvent();
        final String submissionId = cotrEvent.getString("submissionId");

        final JsonObject cpsServeCotrUpdatePublicEvent = buildPayloadForCpsUpdateCotrSubmitted(submissionId);

        final JsonEnvelope publicEventUpdateEnvelope = envelopeFrom(buildMetadata(PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED, randomUUID()), cpsServeCotrUpdatePublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_PROSECUTIONCASEFILE_CPS_UPDATE_COTR_SUBMITTED, publicEventUpdateEnvelope);

        final JsonEnvelope publicEvent = getPublicEventsCotrServedConsumer();
        assertThat(publicEvent, is(notNullValue()));
        final JsonObject eventPayload = publicEvent.payloadAsJsonObject();
        assertThat(eventPayload, notNullValue());

        pollForCotrCaseDetails(caseId, anyOf(
                withJsonPath("$.cotrDetails[0].id", is(notNullValue()))
        ));

    }

    public void queryAndVerifyPetCaseDetail(final UUID caseId, final UUID petId, final UUID defendantId) {
        pollForResponse(format("/prosecutioncases/%s/pet", caseId),
                "application/vnd.progression.query.pets-for-case+json",
                randomUUID().toString(),
                withJsonPath("$.pets[0].defendants[0].caseId", Matchers.is(caseId.toString())),
                withJsonPath("$.pets[0].defendants[0].defendantId", is(defendantId.toString())),
                withJsonPath("$.pets[0].petId", is(petId.toString()))
        );
    }

    private String pollForCotrCaseDetails(final String caseId, final Matcher... matchers) {
        return pollForResponse("/prosecutioncases/" + caseId + "/cotr-details-prosecutioncase",
                "application/vnd.progression.query.cotr.details.prosecutioncase+json",
                randomUUID().toString(),
                matchers);

    }

    private JsonObject buildPayloadForCpsServeCotrSubmitted(final String cpsDefendantId) {
        final String inputEvent = getPayload("stub-data/cps-serve-cotr-submitted.json")
                .replace("%CASE_ID%", caseId)
                .replace("%DEFENDANT_ID%", defendantId)
                .replace("%CPS_DEFENDANT_ID%", cpsDefendantId)
                .replace("%SUBMISSION_ID%", randomUUID().toString());


        return stringToJsonObjectConverter.convert(inputEvent);
    }

    private JsonObject buildPayloadForCpsUpdateCotrSubmitted(final String submissionId) {
        final String inputEvent = getPayload("stub-data/cps-serve-cotr-update.json")
                .replace("%CASE_ID%", caseId)
                .replace("%SUBMISSION_ID%", randomUUID().toString())
                .replace("%COTR_ID%", submissionId);


        return stringToJsonObjectConverter.convert(inputEvent);
    }

    private JsonObject verifyAndRetrieveCotrCreatedPublicEvent() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForPublicForCotrCreated);
        assertTrue(message.isPresent());
        return message.get();
    }

    public JsonEnvelope getPublicEventsCotrCreatedConsumer() {
        return retrieveMessage(consumerForPublicForCotrCreated).orElse(null);
    }

    public JsonEnvelope getPublicEventsCotrServedConsumer() {
        return retrieveMessage(consumerForPublicServeProsecutionCotrCreated).orElse(null);
    }
}
