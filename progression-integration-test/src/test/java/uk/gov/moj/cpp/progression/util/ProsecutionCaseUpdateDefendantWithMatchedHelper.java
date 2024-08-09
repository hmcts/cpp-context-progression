package uk.gov.moj.cpp.progression.util;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;


import java.time.LocalDate;
import uk.gov.moj.cpp.progression.helper.AbstractTestHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.google.common.io.Resources;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProsecutionCaseUpdateDefendantWithMatchedHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseUpdateDefendantWithMatchedHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.update-defendant-for-prosecution-case+json";
    private static final String WRITE_MEDIA_TYPE_OFFENCES = "application/vnd.progression.update-offences-for-prosecution-case+json";

    private static final String TEMPLATE_UPDATE_DEFENDANT_PAYLOAD = "progression.update-defendant-with-matched-for-prosecution-case.json";

    private static final String TEMPLATE_UPDATE_OFFENCES_PAYLOAD = "progression.update-offences-for-matched-defendants.json";


    private final MessageConsumer publicEventsCaseDefendantChanged =
            QueueUtil.publicEvents
                    .createPublicConsumer("public.progression.case-defendant-changed");

    public ProsecutionCaseUpdateDefendantWithMatchedHelper() {

        privateEventsConsumer = QueueUtil.privateEvents.createPrivateConsumer("progression.event.prosecution-case-defendant-updated");
    }

    public static Response initiateCourtProceedingsForMatchedDefendants(final String caseId,
                                                                        final String defendantId,
                                                                        final String masterDefendantId) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                Resources.toString(Resources.getResource("progression.command.initiate-court-proceedings-for-matched-defendants.json"),
                        Charset.defaultCharset())
                        .replaceAll("RANDOM_CASE_ID", caseId)
                        .replaceAll("RANDOM_REFERENCE", generateUrn())
                        .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                        .replaceAll("RANDOM_MASTER_DEFENDANT_ID", masterDefendantId)
        );

    }

    public static Response initiateCourtProceedingsWithCustodialEstablishment(final String caseId,
                                                                        final String defendantId,
                                                                        final String masterDefendantId) throws IOException {
        return postCommand(getWriteUrl("/initiatecourtproceedings"),
                "application/vnd.progression.initiate-court-proceedings+json",
                Resources.toString(Resources.getResource("progression.command.initiate-court-proceedings-with-custodial-establishment.json"),
                        Charset.defaultCharset())
                        .replaceAll("RANDOM_CASE_ID", caseId)
                        .replaceAll("RANDOM_REFERENCE", generateUrn())
                        .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                        .replaceAll("RANDOM_MASTER_DEFENDANT_ID", masterDefendantId)
        );

    }

    public static Matcher[] getUpdatedDefendantMatchers(final String rootPath, final String caseId, final String defendantId, List<Matcher> additionalMatchers) {
        List<Matcher> matchers = newArrayList(
                withJsonPath(rootPath+".id", is(caseId)),

                // defendant assertion
                withJsonPath(rootPath+".defendants[0].id", is(defendantId)),
                withJsonPath(rootPath+".defendants[0].prosecutionCaseId", is(caseId)),
                withJsonPath(rootPath+".defendants[0].prosecutionAuthorityReference", is("TFL12345-ABC")),
                withJsonPath(rootPath+".defendants[0].pncId", is("1234567")),

                // defendant offence assertion
                withJsonPath(rootPath+".defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),

                // assert person
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.title", is("DR")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.firstName", is("Harry")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.middleName", is("Jack")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.lastName", is("Kane")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.dateOfBirth", is("1995-01-01")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.nationalityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.additionalNationalityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.disabilityStatus", is("a")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.gender", is("MALE")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.interpreterLanguageNeeds", is("Hindi")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.documentationLanguageNeeds", is("WELSH")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.nationalInsuranceNumber", is("NH222222B")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.occupation", is("Footballer")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.occupationCode", is("F")),

                // person address
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.address.address1", is("22")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.address.address2", is("associated-address2")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.address.address3", is("Acacia Town")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.address.address4", is("Acacia City")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.address.address5", is("Acacia Country")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.address.postcode", is("CR7 0AA")),

                // person contact details
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.contact.home", is("123456")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.contact.work", is("7891011")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.contact.mobile", is("+45678910")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.contact.primaryEmail", is("contact-email@two.com")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.contact.secondaryEmail", is("harry.kane@hotmail.com")),
                withJsonPath(rootPath+".defendants[0].associatedPersons[0].person.contact.fax", is("3425678")),

                // person defendant details
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.title", is("DR")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.middleName", is("Jack")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.lastName", is("Kane Junior 2")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.gender", is("FEMALE")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.ethnicity.observedEthnicityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.ethnicity.selfDefinedEthnicityId", is("2daefec3-2f76-8109-82d9-2e60544a6c02")),


                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.address.address1", is("address2-1")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.address.address2", is("address2-2")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.address.address3", is("address2-3")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.address.address4", is("address2-4")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.address.postcode", is("W1W 1AA")),

                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.contact.home", is("123456")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.contact.primaryEmail", is("harry.kanejunior@spurs.co.uk-2")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.contact.secondaryEmail", is("harry.kanejunior@hotmail.com")),
                withJsonPath(rootPath+".defendants[0].personDefendant.personDetails.contact.fax", is("3425678")),

                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.name", is("Disneyland Paris-2")),
                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.incorporationNumber", is("Mickeymouse1")),
                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.address.address1", is("Disney Road-2")),
                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.address.address2", is("Disney Town")),
                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.address.address3", is("Disney District")),
                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.address.address4", is("Paris")),
                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.address.address5", is("France")),
                withJsonPath(rootPath+".defendants[0].personDefendant.employerOrganisation.address.postcode", is("CR7 0AA")),


                withJsonPath(rootPath+".defendants[0].personDefendant.bailStatus.id", is("2593cf09-ace0-4b7d-a746-0703a29f33b5")),

                withJsonPath(rootPath+".defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),

                withJsonPath(rootPath+".defendants[0].personDefendant.custodialEstablishment.custody", is("Prison-2")),
                withJsonPath(rootPath+".defendants[0].personDefendant.custodialEstablishment.id", is("a7f54d22-20a1-4154-8955-8e215818bcb5")),
                withJsonPath(rootPath+".defendants[0].personDefendant.custodialEstablishment.name", is("custody-name-2")),

                withJsonPath(rootPath+".defendants[0].personDefendant.driverNumber", is("AACC12345"))
        );

        matchers.addAll(additionalMatchers);

        return matchers.toArray(new Matcher[0]);

    }

    public void updateDefendant(String defendantId, String caseId, String matchedDefendantHearingId) {
        final String jsonString = getPayload(TEMPLATE_UPDATE_DEFENDANT_PAYLOAD);
        updateDefendant(jsonString, defendantId, caseId, matchedDefendantHearingId);
    }

    public void addOffenceToDefendant(String defendantId, String caseId, final String offenceId, final String offenceCode) {
        final String jsonString = getPayload(TEMPLATE_UPDATE_OFFENCES_PAYLOAD);
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("defendantId", defendantId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").put("prosecutionCaseId", caseId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(0).put("id", offenceId);
        jsonObjectPayload.getJSONObject("defendantCaseOffences").getJSONArray("offences").getJSONObject(0).put("offenceCode", offenceCode);

        String request = jsonObjectPayload.toString();
        request = request.replace("REPORTING_RESTRICTION_ORDERED_DATE", LocalDate.now().plusDays(1).toString());
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE_OFFENCES, request);
    }

    public void updateDefendant(final String jsonString, String defendantId, String caseId, String matchedDefendantHearingId) {
        final JSONObject jsonObjectPayload = new JSONObject(jsonString);
        jsonObjectPayload.getJSONObject("defendant").put("id", defendantId);
        jsonObjectPayload.getJSONObject("defendant").put("prosecutionCaseId", caseId);
        jsonObjectPayload.put("matchedDefendantHearingId", matchedDefendantHearingId);

        String request = jsonObjectPayload.toString();
        makePostCall(getWriteUrl("/prosecutioncases/" + caseId + "/defendants/" + defendantId), WRITE_MEDIA_TYPE, request);
    }

    /**
     * Retrieve message from queue and do additional verifications
     */
    @SuppressWarnings("squid:S2629")
    public void verifyInActiveMQ(String id) {

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);
        LOGGER.info("message in queue payload: {}", jsonResponse.prettify());

        assertThat(jsonResponse.getString("id"), is(id));
    }
    @SuppressWarnings("squid:S3655")
    public void verifyInMessagingQueueForDefendentChanged(String caseId) {
        final Optional<JsonObject> message =
                QueueUtil.retrieveMessageAsJsonObject(publicEventsCaseDefendantChanged);
        assertTrue(message.isPresent());
        assertThat(message.get(), isJson(withJsonPath("$.defendant.prosecutionCaseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
    }

    public void closePrivateEventConsumer() throws JMSException {
        privateEventsConsumer.close();
    }

}
