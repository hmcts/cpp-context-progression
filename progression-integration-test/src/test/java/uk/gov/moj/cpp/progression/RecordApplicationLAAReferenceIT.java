package uk.gov.moj.cpp.progression;

import com.jayway.jsonpath.ReadContext;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.ProsecutionCaseUpdateDefendantHelper;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplicationUpdate;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplicationOnly;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordApplicationLAAReferenceOnApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.recordApplicationLAAReferenceWithDescription;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatusWithStatusDescription;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.removeStub;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

public class RecordApplicationLAAReferenceIT extends AbstractIT {
    private static final String PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED = "public.progression.application-offences-updated";
    private static final String PROGRESSION_APPLICATION_OFFENCES_UPDATED_FOR_HEARING = "progression.event.application-laa-reference-updated-for-hearing";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final JmsMessageConsumerClient messageConsumerClientPrivateForLaaReferenceUpdatedForHearing = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_APPLICATION_OFFENCES_UPDATED_FOR_HEARING).getMessageConsumerClient();

    private String applicationId;
    private String subjectId;
    private String offenceLevelStatus;
    private String statusId;
    private String offenceId;
    private String statusCode;
    private String statusDescription;
    private String applicationReference;
    private String hearingId;
    private String userId;
    private static final String organisationName = "Greg Associates Ltd.";
    private final String organisationId = UUID.randomUUID().toString();

    @BeforeAll
    public static void setup() {
        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatchWithEmptyResults();
    }

    @Test
    void shouldRaisePublicEventWhenApplicationIsFoundForRecordLAAReferenceForApplication() throws IOException, JSONException {
        applicationId = randomUUID().toString();
        subjectId = randomUUID().toString();
        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        hearingId = randomUUID().toString();
        statusCode = "G2";
        userId = randomUUID().toString();
        statusDescription = "Desc";
        offenceLevelStatus = "Pending";
        statusId = "2daefbc3-2f72-8109-82d9-2e60544a6c04";
        applicationReference = "XX12345";
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", statusCode, statusDescription);
        //Given
        String caseId= randomUUID().toString();
        String defendantId= randomUUID().toString();
        String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId,courtCentreId, "Lavender Hill Magistrate's Court");

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);


        intiateCourtProceedingForApplicationUpdate(applicationId, subjectId, offenceId, defendantId, caseId,"applications/progression.initiate-court-proceedings-for-application-update2.json");
        pollForApplication(applicationId);

        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReferenceForHearing = newPublicJmsMessageConsumerClientProvider().withEventNames(PROGRESSION_APPLICATION_OFFENCES_UPDATED_FOR_HEARING).getMessageConsumerClient();

        //Record LAA reference
        //When
        recordApplicationLAAReferenceWithDescription(applicationId, subjectId, offenceId, statusCode, statusDescription, applicationReference);

        //Then
        pollForCourtApplicationOnly(applicationId, getApplicationMatchers());
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, buildProsecutionCaseLaaMatchers()));
        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, getApplicationMatchers());
        verifyInMessagingQueueForApplicationOffenceUpdated(messageConsumerClientPublicForRecordLAAReference, offenceId, subjectId, applicationId);
        verifyInMessagingQueue(messageConsumerClientPrivateForLaaReferenceUpdatedForHearing);
    }

    @Test
    void shouldRaisePublicEventWhenApplicationIsFoundForRecordLAAReferenceForApplicationOnApplication() throws IOException, JSONException {
        applicationId = randomUUID().toString();
        subjectId = randomUUID().toString();
        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        hearingId = randomUUID().toString();
        statusCode = "G2";
        userId = randomUUID().toString();
        statusDescription = "Desc";
        offenceLevelStatus = "Pending";
        statusId = "2daefbc3-2f72-8109-82d9-2e60544a6c04";
        applicationReference = "XX12345";
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", statusCode, statusDescription);
        //Given
        String caseId= randomUUID().toString();
        String defendantId= randomUUID().toString();
        String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId,courtCentreId, "Lavender Hill Magistrate's Court");

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);


        intiateCourtProceedingForApplication(applicationId, caseId, defendantId, subjectId, hearingId, "applications/progression.initiate-court-proceedings-for-application.json");
        pollForApplication(applicationId);
        //Record LAA reference
        //When
        recordApplicationLAAReferenceOnApplication(applicationId, statusCode, statusDescription, applicationReference);

        //Then
        pollForCourtApplicationOnly(applicationId, getApplicationOnApplicationMatchers());
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, List.of()));
        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, getApplicationOnApplicationMatchers());
        verifyInMessagingQueue(messageConsumerClientPrivateForLaaReferenceUpdatedForHearing);
        //verify laaReference is updated on hearing
        pollForHearing(hearingId, withJsonPath("$.hearing.courtApplications[0].laaApplnReference.applicationReference", is(applicationReference)));
        //send an address update for the defendant
        ProsecutionCaseUpdateDefendantHelper prosecutionCaseUpdateDefendantHelper = new ProsecutionCaseUpdateDefendantHelper(caseId, defendantId);
        prosecutionCaseUpdateDefendantHelper.updateDefendantWithAddressInfo("NN7 7NN");
        //verify laaReference not wiped out on hearing and application
        pollForHearing(hearingId, withJsonPath("$.hearing.courtApplications[0].laaApplnReference.applicationReference", is(applicationReference)));
        pollForCourtApplicationOnly(applicationId, getApplicationOnApplicationMatchers());
    }

    private List<Matcher<? super ReadContext>> buildProsecutionCaseLaaMatchers() {
        return newArrayList(
                withJsonPath("$.prosecutionCase.defendants[*].legalAidStatus", hasItem(equalTo(offenceLevelStatus))),
                withJsonPath("$.prosecutionCase.defendants[*].offences[*].laaApplnReference.applicationReference", hasItem(equalTo(applicationReference))),
                withJsonPath("$.prosecutionCase.defendants[*].offences[*].laaApplnReference.offenceLevelStatus", hasItem(equalTo(offenceLevelStatus)))
        );
    }

    private void verifyInMessagingQueue(final JmsMessageConsumerClient messageConsumer) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumer);
        assertTrue(message.isPresent());
    }

    @SafeVarargs
    private void verifyInitiateCourtProceedingsViewStoreUpdated(final String applicationId, final Matcher<? super ReadContext>... matchers) {
        pollForResponse("/court-proceedings/application/" + applicationId,
                "application/vnd.progression.query.court-proceedings-for-application+json",
                randomUUID().toString(),
                matchers);
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
                                            final String defendantId, final String courtCentreId, final String courtCentreName, final String applicationId) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private static void verifyInMessagingQueueForApplicationOffenceUpdated(final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReference, String offenceId, String subjectId, String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForRecordLAAReference);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getString("offenceId"), is(offenceId));
        assertThat(message.get().getString("subjectId"), is(subjectId));
        assertThat(message.get().getString("applicationId"), is(applicationId));
    }

    private static void verifyInMessagingQueueForApplicationOffenceUpdatedForHearing(final JmsMessageConsumerClient messageConsumerClientPublicForRecordLAAReferenceForHearing,String hearingId, String offenceId, String subjectId, String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(messageConsumerClientPublicForRecordLAAReferenceForHearing);
        assertThat(message.isPresent(), is(true));
        assertThat(message.get().getString("offenceId"), is(offenceId));
        assertThat(message.get().getString("hearingId"), is(hearingId));
        assertThat(message.get().getString("subjectId"), is(subjectId));
        assertThat(message.get().getString("applicationId"), is(applicationId));
    }

    private Matcher[] getApplicationMatchers() {
        final List<Matcher> matchers = newArrayList(withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.subject.id", is(subjectId)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].id", is(offenceId)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].laaApplnReference.statusCode", is(statusCode)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].laaApplnReference.statusDescription", is(statusDescription)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].laaApplnReference.offenceLevelStatus", is(offenceLevelStatus)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].laaApplnReference.applicationReference", is(applicationReference)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].laaApplnReference.statusId", is(statusId))
        );
        return matchers.toArray(new Matcher[0]);
    }

    private Matcher[] getApplicationOnApplicationMatchers() {
        final List<Matcher> matchers = newArrayList(withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.laaApplnReference.statusCode", is(statusCode)),
                withJsonPath("$.courtApplication.laaApplnReference.statusDescription", is(statusDescription)),
                withJsonPath("$.courtApplication.laaApplnReference.offenceLevelStatus", is(offenceLevelStatus)),
                withJsonPath("$.courtApplication.laaApplnReference.applicationReference", is(applicationReference)),
                withJsonPath("$.courtApplication.laaApplnReference.statusId", is(statusId))
        );
        return matchers.toArray(new Matcher[0]);
    }

    private JsonObject getApplicationHearingJsonObject(final String path, final String hearingId,
                                                       final String applicationId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }

    @AfterAll
    public static void teardownOnce() {
        removeStub();
    }
}
