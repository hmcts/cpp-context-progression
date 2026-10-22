package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.addBreachApplicationForExistingHearing;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.AbstractIT;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BreachApplicationIT extends AbstractIT {

    private static final String PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PUBLIC_PROGRESSION_EVENT_APPLICATION_PROCEEDINGS_EDITED = "public.progression.event.application-proceedings-edited";
    private static final String EDITED_RESPONDENT_ADDRESS_1 = "1 Edited Street";
    private static final String EDITED_RESPONDENT_POSTCODE = "SW1A 1AA";

    @BeforeAll
    public static void setUpClass() {
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
    }

    @Test
    public void shouldCreateLinkedApplicationWithBreachOrder() throws Exception {
        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-breach-application.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.type.breachType", is("GENERIC_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseId", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].prosecutionCaseIdentifier.caseURN", is("TFL4359536")),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out time reasons for Breach order"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldCreateApplicationWithBreachOrderFromExistingHearing() throws Exception {


        final String caseId = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();


        addProsecutionCaseToCrownCourt(caseId, masterDefendantId);

        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, masterDefendantId);

        addBreachApplicationForExistingHearing(hearingId, masterDefendantId, "applications/progression.add-breach-application.json");
        final String casePayload = pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.linkedApplicationsSummary", hasSize(1)));
        final String applicationId = new StringToJsonObjectConverter().convert(casePayload).getJsonArray("linkedApplicationsSummary").getJsonObject(0).getString("applicationId");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("CJ03506")),
                withJsonPath("$.courtApplication.type.linkType", is("LINKED")),
                withJsonPath("$.courtApplication.type.breachType", is("COMMISSION_OF_NEW_OFFENCE_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.respondents[0].id", notNullValue()),
                withJsonPath("$.courtApplication.courtOrder", notNullValue()),
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }

    @Test
    public void shouldUpdateExistingHearingWhenBreachApplicationAddedToItIsEdited() throws Exception {
        final String caseId = randomUUID().toString();
        final String masterDefendantId = randomUUID().toString();
        final JmsMessageConsumerClient hearingExtendedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENTS_HEARING_EXTENDED).getMessageConsumerClient();

        // 1. Create a case and its hearing
        addProsecutionCaseToCrownCourt(caseId, masterDefendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, masterDefendantId);

        // 2. Add a commission of new offence breach application to the hearing; it is referred to the hearing without being linked to it
        addBreachApplicationForExistingHearing(hearingId, masterDefendantId, "applications/progression.add-breach-application.json");
        final String casePayload = pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.linkedApplicationsSummary", hasSize(1)));
        final String applicationId = new StringToJsonObjectConverter().convert(casePayload).getJsonArray("linkedApplicationsSummary").getJsonObject(0).getString("applicationId");
        assertThat(retrieveMessageAsJsonPath(hearingExtendedConsumer, isJson(allOf(
                withJsonPath("$.hearingId", is(hearingId)),
                withJsonPath("$.courtApplication.id", is(applicationId))))), notNullValue());

        // 3. Edit the application as the UI does, from its court proceedings: change the respondent's address
        final JmsMessageConsumerClient applicationProceedingsEditedConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_EVENT_APPLICATION_PROCEEDINGS_EDITED).getMessageConsumerClient();
        final String courtProceedings = pollForResponse("/court-proceedings/application/" + applicationId,
                "application/vnd.progression.query.court-proceedings-for-application+json",
                randomUUID().toString(),
                withJsonPath("$.courtApplication.id", is(applicationId)));
        postCommand(getWriteUrl("/initiate-application"),
                "application/vnd.progression.edit-court-proceedings-for-application+json",
                getBreachApplicationWithEditedRespondentAddressJson(courtProceedings));

        // 4. The hearing the breach application was added to receives the edited application
        final JsonPath hearingExtended = retrieveMessageAsJsonPath(hearingExtendedConsumer, isJson(allOf(
                withJsonPath("$.hearingId", is(hearingId)),
                withJsonPath("$.courtApplication.respondents[0].masterDefendant.personDefendant.personDetails.address.address1", is(EDITED_RESPONDENT_ADDRESS_1)))));
        assertThat(hearingExtended, notNullValue());
        assertThat(hearingExtended.getString("courtApplication.id"), is(applicationId));
        assertThat(hearingExtended.getString("courtApplication.respondents[0].masterDefendant.personDefendant.personDetails.address.postcode"), is(EDITED_RESPONDENT_POSTCODE));
        assertThat(hearingExtended.get("hearingDays"), nullValue());
        assertThat(hearingExtended.get("courtCentre"), nullValue());

        // 5. The public application-proceedings-edited event carries the edited application and does not expose the internal hearingIds
        final JsonPath applicationProceedingsEdited = retrieveMessageAsJsonPath(applicationProceedingsEditedConsumer, isJson(withJsonPath("$.courtApplication.id", is(applicationId))));
        assertThat(applicationProceedingsEdited, notNullValue());
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].masterDefendant.personDefendant.personDetails.address.address1"), is(EDITED_RESPONDENT_ADDRESS_1));
        assertThat(applicationProceedingsEdited.getString("courtApplication.respondents[0].masterDefendant.personDefendant.personDetails.address.postcode"), is(EDITED_RESPONDENT_POSTCODE));
        assertThat(applicationProceedingsEdited.get("hearingIds"), nullValue());
    }

    private String getBreachApplicationWithEditedRespondentAddressJson(final String courtProceedings) {
        final JsonObject courtProceedingsJson = new StringToJsonObjectConverter().convert(courtProceedings);
        final JsonObject courtApplication = courtProceedingsJson.getJsonObject("courtApplication");
        final JsonObject respondent = courtApplication.getJsonArray("respondents").getJsonObject(0);
        final JsonObject masterDefendant = respondent.getJsonObject("masterDefendant");
        final JsonObject personDefendant = masterDefendant.getJsonObject("personDefendant");
        final JsonObject personDetails = personDefendant.getJsonObject("personDetails");
        final JsonObjectBuilder address = personDetails.containsKey("address")
                ? createObjectBuilder(personDetails.getJsonObject("address"))
                : createObjectBuilder();

        final JsonObject editedRespondent = createObjectBuilder(respondent)
                .add("masterDefendant", createObjectBuilder(masterDefendant)
                        .add("personDefendant", createObjectBuilder(personDefendant)
                                .add("personDetails", createObjectBuilder(personDetails)
                                        .add("address", address
                                                .add("address1", EDITED_RESPONDENT_ADDRESS_1)
                                                .add("postcode", EDITED_RESPONDENT_POSTCODE)))))
                .build();

        return createObjectBuilder()
                .add("courtApplication", createObjectBuilder(courtApplication)
                        .add("respondents", createArrayBuilder().add(editedRespondent)))
                .add("courtHearing", courtProceedingsJson.getJsonObject("courtHearing"))
                .build()
                .toString();
    }

    @Test
    public void shouldCreateStandaloneApplicationWithBreachOrder() throws Exception {
        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, "applications/progression.initiate-court-proceedings-for-stand-alone-breach-application.json");

        final Matcher[] applicationMatchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.type.code", is("AS14518")),
                withJsonPath("$.courtApplication.type.linkType", is("STANDALONE")),
                withJsonPath("$.courtApplication.type.breachType", is("GENERIC_BREACH")),
                withJsonPath("$.courtApplication.applicationStatus", is("DRAFT")),
                withJsonPath("$.courtApplication.applicant.id", notNullValue()),
                withJsonPath("$.courtApplication.subject.id", notNullValue()),
                withJsonPath("$.courtApplication.outOfTimeReasons", is("Out time reasons for Breach order"))
        };

        pollForCourtApplication(applicationId, applicationMatchers);
    }
}
