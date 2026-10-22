package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.JsonPath.parse;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.CaseHearingsQueryHelper.pollForHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetLatestHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.updateCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"squid:S1607", "squid:S2925"})
public class UpdateCourtApplicationIT extends AbstractIT {
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String PUBLIC_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PUBLIC_PROGRESSION_EVENT_APPLICATION_PROCEEDINGS_EDITED = "public.progression.event.application-proceedings-edited";
    private static final String LINKED_APPLICATION_WITH_RESPONDENT_REFERRED_TO_EXISTING_HEARING = "applications/progression.initiate-court-proceedings-for-generic-linked-application.json";
    private static final String EDITED_RESPONDENT_ADDRESS_1 = "1 Edited Street";
    private static final String EDITED_RESPONDENT_POSTCODE = "SW1A 1AA";
    private static final String EDITED_RESPONDENT_FIRST_NAME = "Edited";
    private static final String EDITED_RESPONDENT_LAST_NAME = "Respondent";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String caseId;
    private String defendantId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateCourtApplicationAndGetConfirmation() throws Exception {

        // when
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollCaseAndGetHearingForDefendant(caseId, defendantId);

        String applicationId = randomUUID().toString();
        String applicantId = UUID.fromString("88cdf36e-93e4-41b0-8277-17d9dba7f06f").toString();

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");

        Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out of times reasons for linked application test")),
                withJsonPath("$.courtApplication.applicationReference", notNullValue()),
        };

        pollForApplication(applicationId, applicationMatchers);

        updateCourtApplication(applicationId, applicantId, caseId, defendantId, "", "progression.command.update-court-application-with-court-order.json");

        Matcher[] updatedApplicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("updated out of time reasons")),
                withJsonPath("$.courtApplication.courtOrder.id", notNullValue()),
        };

        verifyInitiateCourtProceedingsViewStoreUpdated(applicationId, updatedApplicationMatchers);
    }

    @Test
    public void shouldRaiseProbationEventWhenUpdateCourtApplication() throws Exception {
        final String applicationId = randomUUID().toString();

        addStandaloneCourtApplication(applicationId, UUID.randomUUID().toString(), new CourtApplicationsHelper.CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()), getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-application-with-linked-case.json",
                applicationId, hearingId, caseId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForApplicationStatus(applicationId, "LISTED");

        verifyProbationHearingCommandInvoked(newArrayList(hearingId, "\"a\""));

        // hearing-confirmed does not link the application to the hearing; hearing-updated with courtApplicationIds does,
        // and the edit only updates hearings linked to the application
        final JsonEnvelope hearingUpdatedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_UPDATED, randomUUID()), getHearingUpdatedForApplicationJsonObject(hearingId, courtCentreId, applicationId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_UPDATED, hearingUpdatedEnvelope);
        pollForCourtApplication(applicationId, withJsonPath("$.hearings[*].id", hasItem(hearingId)));

        final JmsMessageConsumerClient applicationProceedingsEditedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_APPLICATION_PROCEEDINGS_EDITED).getMessageConsumerClient();

        String applicantId = UUID.fromString("88cdf36e-93e4-41b0-8277-17d9dba7f06f").toString();
        updateCourtApplication(applicationId, applicantId, caseId, defendantId, hearingId, "progression.command.update-court-application-with-hearing.json");
        verifyProbationHearingCommandInvoked(newArrayList(hearingId, "\"b\""));

        final JsonPath applicationProceedingsEdited = retrieveMessageAsJsonPath(applicationProceedingsEditedConsumer, isJson(withJsonPath("$.courtApplication.id", is(applicationId))));
        assertThat(applicationProceedingsEdited, notNullValue());
        assertThat(applicationProceedingsEdited.getString("courtApplication.outOfTimeReasons"), is("b"));
        assertThat(applicationProceedingsEdited.get("hearingIds"), nullValue());
    }

    @Test
    public void shouldUpdateApplicationOnHearingsNotResultedWhenApplicationEdited() throws Exception {
        final String applicationId = randomUUID().toString();
        final String caseUrn = generateUrn();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";

        // 1. Create a case and its first hearing, and confirm the hearing in listing
        addProsecutionCaseToCrownCourt(caseId, defendantId, caseUrn);
        final String resultedHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope hearingConfirmedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()),
                getHearingJsonObject("public.listing.hearing-confirmed.json", resultedHearingId, applicationId, caseUrn, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedEnvelope);

        // 2. Refer an application with a respondent to the first hearing; the referral links the application to that hearing
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, resultedHearingId, LINKED_APPLICATION_WITH_RESPONDENT_REFERRED_TO_EXISTING_HEARING);
        pollForCourtApplication(applicationId,
                withJsonPath("$.courtApplication.applicationStatus", is("LISTED")),
                withJsonPath("$.hearings[*].id", hasItem(resultedHearingId)));

        // 3. Result the first hearing with a next hearing for the application. The payload has no prosecution cases,
        // so the next hearing is created for the application only and linked to it (see gap 326 for hearings with cases)
        final JsonEnvelope hearingResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, randomUUID()),
                getHearingJsonObject("public.events.hearing.hearing-resulted.application-adjourned-to-new-hearing.json", resultedHearingId, applicationId, caseUrn, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, hearingResultedEnvelope);
        pollForHearing(resultedHearingId, withJsonPath("$.hearingListingStatus", is("HEARING_RESULTED")));

        // 4. Wait for the new hearing to be linked to the application and take its id
        final String application = pollForCourtApplication(applicationId, withJsonPath("$.hearings.length()", is(2)));
        final List<String> applicationHearingIds = parse(application).read("$.hearings[*].id");
        final String newHearingId = applicationHearingIds.stream()
                .filter(hearingId -> !hearingId.equals(resultedHearingId))
                .findFirst()
                .orElseThrow();

        // 5. Edit the application: change the respondent's name and address
        final JmsMessageConsumerClient hearingExtendedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).getMessageConsumerClient();
        final JmsMessageConsumerClient applicationProceedingsEditedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_APPLICATION_PROCEEDINGS_EDITED).getMessageConsumerClient();

        postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.edit-court-proceedings-for-application+json",
                getApplicationWithEditedRespondentNameAndAddressJson(applicationId, resultedHearingId));

        // 6. The new hearing receives the edited application with the respondent's new name and address, without hearing days, court centre or box work flag
        final JsonPath newHearingExtended = retrieveMessageAsJsonPath(hearingExtendedConsumer, isJson(withJsonPath("$.hearingId", is(newHearingId))));
        assertThat(newHearingExtended, notNullValue());
        assertThat(newHearingExtended.getString("courtApplication.id"), is(applicationId));
        assertThat(newHearingExtended.getString("courtApplication.respondents[0].personDetails.firstName"), is(EDITED_RESPONDENT_FIRST_NAME));
        assertThat(newHearingExtended.getString("courtApplication.respondents[0].personDetails.lastName"), is(EDITED_RESPONDENT_LAST_NAME));
        assertThat(newHearingExtended.getString("courtApplication.respondents[0].personDetails.address.address1"), is(EDITED_RESPONDENT_ADDRESS_1));
        assertThat(newHearingExtended.getString("courtApplication.respondents[0].personDetails.address.postcode"), is(EDITED_RESPONDENT_POSTCODE));
        assertThat(newHearingExtended.get("hearingDays"), nullValue());
        assertThat(newHearingExtended.get("courtCentre"), nullValue());
        assertThat(newHearingExtended.get("isBoxWorkRequest"), nullValue());

        // 7. The resulted hearing is not updated
        assertThat(retrieveMessageAsJsonPath(hearingExtendedConsumer, isJson(withJsonPath("$.hearingId", is(resultedHearingId)))), nullValue());

        // 8. The public application-proceedings-edited event carries the edited application and does not expose the internal hearingIds
        final JsonPath applicationProceedingsEdited = retrieveMessageAsJsonPath(applicationProceedingsEditedConsumer, isJson(withJsonPath("$.courtApplication.id", is(applicationId))));
        assertThat(applicationProceedingsEdited, notNullValue());
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].personDetails.firstName"), is(EDITED_RESPONDENT_FIRST_NAME));
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].personDetails.lastName"), is(EDITED_RESPONDENT_LAST_NAME));
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].personDetails.address.address1"), is(EDITED_RESPONDENT_ADDRESS_1));
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].personDetails.address.postcode"), is(EDITED_RESPONDENT_POSTCODE));
        assertThat(applicationProceedingsEdited.get("hearingIds"), nullValue());
    }

    @Test
    public void shouldUpdateApplicationOnNextHearingWithCasesWhenApplicationEdited() throws Exception {
        final String applicationId = randomUUID().toString();
        final String caseUrn = generateUrn();
        final String courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        final String courtCentreName = "Lavender Hill Magistrate's Court";

        // 1. Create a case and its first hearing, and confirm the hearing in listing
        addProsecutionCaseToCrownCourt(caseId, defendantId, caseUrn);
        final String resultedHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope hearingConfirmedEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID()),
                getHearingJsonObject("public.listing.hearing-confirmed.json", resultedHearingId, applicationId, caseUrn, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedEnvelope);

        // 2. Refer an application linked to the case, with a respondent, to the first hearing; the referral links the application to that hearing
        initiateCourtProceedingsForCourtApplication(applicationId, caseId, resultedHearingId, LINKED_APPLICATION_WITH_RESPONDENT_REFERRED_TO_EXISTING_HEARING);
        pollForCourtApplication(applicationId,
                withJsonPath("$.courtApplication.applicationStatus", is("LISTED")),
                withJsonPath("$.hearings[*].id", hasItem(resultedHearingId)));

        // 3. Result the first hearing with a next hearing for the application. The resulted hearing carries the case, so the
        // next hearing is listed as a case hearing and registered against the application instead of being linked to it
        final JsonEnvelope hearingResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, randomUUID()),
                getHearingJsonObject("public.events.hearing.hearing-resulted.application-and-case-adjourned-to-new-hearing.json", resultedHearingId, applicationId, caseUrn, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, hearingResultedEnvelope);
        pollForHearing(resultedHearingId, withJsonPath("$.hearingListingStatus", is("HEARING_RESULTED")));

        // 4. Take the new hearing from the case, as it is not linked to the application in the view store
        final String newHearingId = pollCaseAndGetLatestHearingForDefendant(caseId, defendantId, 2, singletonList(resultedHearingId));
        pollForCourtApplication(applicationId, withJsonPath("$.hearings.length()", is(1)));

        // 5. Edit the application: change the respondent's address
        final JmsMessageConsumerClient hearingExtendedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).getMessageConsumerClient();
        final JmsMessageConsumerClient applicationProceedingsEditedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_APPLICATION_PROCEEDINGS_EDITED).getMessageConsumerClient();

        postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.edit-court-proceedings-for-application+json",
                getApplicationWithEditedRespondentAddressJson(applicationId, resultedHearingId));

        // 6. The new hearing receives the edited application with the respondent's new address
        final JsonPath newHearingExtended = retrieveMessageAsJsonPath(hearingExtendedConsumer, isJson(withJsonPath("$.hearingId", is(newHearingId))));
        assertThat(newHearingExtended, notNullValue());
        assertThat(newHearingExtended.getString("courtApplication.id"), is(applicationId));
        assertThat(newHearingExtended.getString("courtApplication.respondents[0].personDetails.address.address1"), is(EDITED_RESPONDENT_ADDRESS_1));
        assertThat(newHearingExtended.getString("courtApplication.respondents[0].personDetails.address.postcode"), is(EDITED_RESPONDENT_POSTCODE));
        assertThat(newHearingExtended.get("hearingDays"), nullValue());
        assertThat(newHearingExtended.get("courtCentre"), nullValue());
        assertThat(newHearingExtended.get("isBoxWorkRequest"), nullValue());

        // 7. The resulted hearing is not updated
        assertThat(retrieveMessageAsJsonPath(hearingExtendedConsumer, isJson(withJsonPath("$.hearingId", is(resultedHearingId)))), nullValue());

        // 8. The public application-proceedings-edited event carries the edited application and does not expose the internal hearingIds
        final JsonPath applicationProceedingsEdited = retrieveMessageAsJsonPath(applicationProceedingsEditedConsumer, isJson(withJsonPath("$.courtApplication.id", is(applicationId))));
        assertThat(applicationProceedingsEdited, notNullValue());
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].personDetails.address.address1"), is(EDITED_RESPONDENT_ADDRESS_1));
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].personDetails.address.postcode"), is(EDITED_RESPONDENT_POSTCODE));
        assertThat(applicationProceedingsEdited.get("hearingIds"), nullValue());
    }

    @SafeVarargs
    private void verifyInitiateCourtProceedingsViewStoreUpdated(final String applicationId, final Matcher<? super ReadContext>... matchers) {
        pollForResponse("/court-proceedings/application/" + applicationId,
                "application/vnd.progression.query.court-proceedings-for-application+json",
                randomUUID().toString(),
                matchers);
    }

    private JsonObject getHearingWithStandAloneApplicationJsonObject(final String path, final String applicationId, final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getHearingUpdatedForApplicationJsonObject(final String hearingId, final String courtCentreId, final String applicationId) {
        return stringToJsonObjectConverter.convert(
                getPayload("public.listing.hearing-updated-for-application.json")
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("APPLICATION_ID", applicationId));
    }

    private JsonObject getHearingJsonObject(final String path, final String hearingId, final String applicationId, final String reference,
                                            final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("ADJOURNED_ID", randomUUID().toString())
                        .replaceAll("APPLICATION_REF", reference)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("OFFENCE_ID", "3789ab16-0bb7-4ef1-87ef-c936bf0364f1")
                        .replaceAll("ORDERED_DATE", "2020-01-01"));
    }

    private String getApplicationWithEditedRespondentAddressJson(final String applicationId, final String hearingId) {
        return getApplicationWithEditedRespondentJson(applicationId, hearingId, false);
    }

    private String getApplicationWithEditedRespondentNameAndAddressJson(final String applicationId, final String hearingId) {
        return getApplicationWithEditedRespondentJson(applicationId, hearingId, true);
    }

    private String getApplicationWithEditedRespondentJson(final String applicationId, final String hearingId, final boolean editName) {
        final JsonObject initiatedApplication = stringToJsonObjectConverter.convert(getPayload(LINKED_APPLICATION_WITH_RESPONDENT_REFERRED_TO_EXISTING_HEARING)
                .replace("APPLICATION_ID", applicationId)
                .replaceAll("CASE_ID", caseId)
                .replace("HEARING_ID", hearingId));
        final JsonObject courtApplication = initiatedApplication.getJsonObject("courtApplication");
        final JsonObject respondent = courtApplication.getJsonArray("respondents").getJsonObject(0);
        final JsonObject personDetails = respondent.getJsonObject("personDetails");

        final JsonObjectBuilder editedPersonDetails = createObjectBuilder(personDetails)
                .add("address", createObjectBuilder(personDetails.getJsonObject("address"))
                        .add("address1", EDITED_RESPONDENT_ADDRESS_1)
                        .add("postcode", EDITED_RESPONDENT_POSTCODE));
        if (editName) {
            editedPersonDetails
                    .add("firstName", EDITED_RESPONDENT_FIRST_NAME)
                    .add("lastName", EDITED_RESPONDENT_LAST_NAME);
        }

        final JsonObject editedRespondent = createObjectBuilder(respondent)
                .add("personDetails", editedPersonDetails)
                .build();
        final JsonObject editedCourtApplication = createObjectBuilder(courtApplication)
                .add("respondents", createArrayBuilder().add(editedRespondent))
                .build();

        return createObjectBuilder(initiatedApplication)
                .add("courtApplication", editedCourtApplication)
                .build()
                .toString();
    }

}

