package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplicationUpdateForRepOrder;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplicationOnly;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.receiveRepresentationOrderForApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisationForRepresenationOrder;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubLegalStatusWithStatusDescription;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.removeStub;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryExactMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UnifiedSearchStub.stubUnifiedSearchQueryPartialMatchWithEmptyResults;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetGroupsForLoggedInQuery;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetUsersAndGroupsQueryForSystemUsers;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayloadAsJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ReceiveRepresentationOrderForApplicationIT extends AbstractIT {

    private static final String PUBLIC_APPLICATION_ORGANISATION_CHANGED = "public.progression.application-organisation-changed";
    private static final String PUBLIC_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED = "public.progression.application-offences-updated";
    private static final String PUBLIC_ASSOCIATE_DEFENCE_ORGANISATION = "public.progression.defence-organisation-for-laa-associated";
    private static final String PUBLIC_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA = "public.progression.defendant-laa-contract-associated";
    private static final String PUBLIC_DISASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA = "public.progression.defence-organisation-for-laa-disassociated";
    private static final String PROGRESSION_APPLICATION_OFFENCES_UPDATED_FOR_HEARING = "progression.event.application-laa-reference-updated-for-hearing";
    private static final String PROGRESSION_APPLICATION_REPORDER_UPDATED_FOR_HEARING = "progression.event.application-rep-order-updated-for-hearing";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";



    private String applicationId;
    private String subjectId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String offenceId;
    private String statusCode;
    private String statusDescription;
    private String applicationReference;
    private String userId;
    private final String laaContractNumber = "LAA3456";
    private String organisationName = "Greg Associates Ltd.";
    private final String organisationId = UUID.randomUUID().toString();

    @BeforeAll
    public static void setup() {
        removeStub();
        stubUnifiedSearchQueryExactMatchWithEmptyResults();
        stubUnifiedSearchQueryPartialMatchWithEmptyResults();
    }

    @Test
    void shouldRaisePublicEventWhenApplicationIsFoundForReceiveRepresentationOrderForApplicationWithOrganisation() throws IOException, JSONException {
        applicationId = randomUUID().toString();
        subjectId = randomUUID().toString();
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        subjectId = randomUUID().toString();
        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        userId = "test";
        statusCode = "G2";
        statusDescription = "Desc";
        applicationReference = "AS145197659";
        stubGetOrganisationDetails(organisationId, organisationName);
        stubGetUsersAndGroupsQueryForSystemUsers(userId);
        stubGetGroupsForLoggedInQuery(userId);
        stubLegalStatusWithStatusDescription("/restResource/ref-data-legal-statuses.json", statusCode, statusDescription);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        //Given
        stubForAssociatedOrganisationForRepresenationOrder(getPayloadAsJsonObject("stub-data/defence.get-associated-organisation-reporder.json"), defendantId, organisationId, organisationName);
        String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId,courtCentreId, "Lavender Hill Magistrate's Court");

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        intiateCourtProceedingForApplicationUpdateForRepOrder(applicationId, subjectId, offenceId, caseId, defendantId, applicationReference, laaContractNumber, "applications/progression.initiate-court-proceedings-for-application-reporder.json");
        pollForApplication(applicationId);

        final JmsMessageConsumerClient messageConsumerClientPublicForOrganisationChanged = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_APPLICATION_ORGANISATION_CHANGED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForOrganisationChangedDefence = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_CASE_DEFENDANT_CHANGED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForOrganisationLAAAssociated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_ASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForOrganisationDisassociated = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_DISASSOCIATE_DEFENCE_ORGANISATION_FOR_LAA).getMessageConsumerClient();

        final JmsMessageConsumerClient messageConsumerClientPublicForLAAReferenceChanged = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPublicForLAAReferenceChangedDefence = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_DEFENDANT_OFFENCES_UPDATED).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPrivateForLaaReferenceUpdatedForHearing = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_APPLICATION_OFFENCES_UPDATED_FOR_HEARING).getMessageConsumerClient();
        final JmsMessageConsumerClient messageConsumerClientPrivateForRepOrcerUpdatedForHearing = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames(PROGRESSION_APPLICATION_REPORDER_UPDATED_FOR_HEARING).getMessageConsumerClient();
        //When
        receiveRepresentationOrderForApplication(applicationId, subjectId, offenceId, statusCode, laaContractNumber, applicationReference, userId);

        //Then
        pollForCourtApplicationOnly(applicationId, getApplicationMatchers());

        //Verify
        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, getApplicationMatchers());
        verifyInMessagingQueueForApplication(messageConsumerClientPublicForLAAReferenceChanged);
        verifyInMessagingQueueForApplication(messageConsumerClientPublicForLAAReferenceChangedDefence);
        verifyInMessagingQueue(messageConsumerClientPrivateForLaaReferenceUpdatedForHearing);
        verifyInMessagingQueue(messageConsumerClientPrivateForRepOrcerUpdatedForHearing);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, buildProsecutionCaseLaaMatchers()));
    }

    private List<Matcher<? super ReadContext>> buildProsecutionCaseLaaMatchers() {
        return newArrayList(
                    withJsonPath("$.prosecutionCase.defendants[*].legalAidStatus", hasItem(equalTo("Pending"))),
                    withJsonPath("$.prosecutionCase.defendants[*].offences[*].laaApplnReference.applicationReference", hasItem(equalTo("AS145197659"))),
                    withJsonPath("$.prosecutionCase.defendants[*].offences[*].laaApplnReference.offenceLevelStatus", hasItem(equalTo("Pending")))
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

    private static void verifyInMessagingQueueForApplication(final JmsMessageConsumerClient jmsMessageConsumerClient) {
        final Optional<JsonObject> message = retrieveMessageBody(jmsMessageConsumerClient);
        assertThat(message.isPresent(), is(true));
    }

    private Matcher[] getApplicationMatchers() {
        final List<Matcher> matchers = newArrayList(withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.subject.id", is(subjectId)));
        return matchers.toArray(new Matcher[0]);
    }

    @AfterAll
    public static void teardownOnce() {
        removeStub();
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
