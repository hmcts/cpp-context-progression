package uk.gov.moj.cpp.progression.helper;

import static com.jayway.jsonassert.JsonAssert.with;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.compile;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.Cleaner.closeSilently;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getCaseByUrn;
import static uk.gov.moj.cpp.progression.helper.DefaultRequests.getDefendantsByCaseId;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.EVENT_SELECTOR_DEFENDANT_ADDITION_FAILED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDED;
import static uk.gov.moj.cpp.progression.helper.EventSelector.PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDITION_FAILED;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder;
import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDefendantHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddDefendantHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.progression.command.add-defendant+json";

    public static final String GET_CASE_DEFENDANTS_MEDIA_TYPE = "application/vnd.progression.query.case-defendants+json";

    private final MessageConsumerClient privateEventDefendantAdditionFailedConsumer = new MessageConsumerClient();
    private final MessageConsumerClient publicEventDefendantAdditionFailedConsumer = new MessageConsumerClient();

    private final String VALUE_OFFENCES_ID = UUID.randomUUID().toString();
    private final String VALUE_DEFENDANT_ID = UUID.randomUUID().toString();

    private static final String VALUE_POLICE_DEFENDANT_ID = "BS2CM01ADEF1";
    private static final String VALUE_POLICE_OFFENCE_ID = "A00PCD7073";
    private static final String VALUE_YEAR = "09";
    private static final String VALUE_ORGANISATION_UNIT = "XX45GD00";
    private static final String VALUE_NUMBER = "10000000001";
    private static final String VALUE_CHECK_DIGIT = "A";
    private static final String VALUE_OFFENCE_SEQUENCE = "1";
    private static final String VALUE_ASN_SEQUENCE_NUMBER = "1";
    private static final String VALUE_CJS_CODE = "OF61131";
    private static final String VALUE_REASON = "131";
    private static final String VALUE_DESCRIPTION = "description";
    private static final String VALUE_WORDING = "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith";
    private static final String VALUE_CATEGORY = "Either Way";
    private static final String VALUE_START_DATE = "2010-08-01";
    private static final String VALUE_END_DATE = "2010-08-21";
    private static final String VALUE_ARREST_DATE = "2010-08-21";
    private static final String VALUE_CHARGE_DATE = "2011-08-01";
    private static final String PERSON_TITLE = "Mr";
    private static final String PERSON_FIRST_NAME = "John";
    private static final String PERSON_LAST_NAME = "Doe";
    private static final String PERSON_DATE_OF_BIRTH = "1971-12-21";
    private static final String PERSON_NATIONALITY = "British";
    private static final String PERSON_GENDER = "Male";
    private static final String ADDRESS_LINE_1 = "Address Line 1";
    private static final String ADDRESS_LINE_2 = "Address Line 2";
    private static final String ADDRESS_LINE_3 = "Address Line 3";
    private static final String ADDRESS_LINE_4 = "Address Line 4";
    private static final String POST_CODE = "Post Code";
    private static final String ADDRESS_ID = UUID.randomUUID().toString();

    private final String caseId;
    private final String caseUrn;
    private String request;
    private final String personId;

    public AddDefendantHelper(final String caseId) {
        this.caseId = caseId;
        privateEventsConsumer = QueueUtil.privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANT_ADDED);
        privateEventDefendantAdditionFailedConsumer.startConsumer(EVENT_SELECTOR_DEFENDANT_ADDITION_FAILED, STRUCTURE_EVENT_TOPIC);
        publicConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDED, PUBLIC_ACTIVE_MQ_TOPIC);
        publicEventDefendantAdditionFailedConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_DEFENDANT_ADDITION_FAILED, PUBLIC_ACTIVE_MQ_TOPIC);
        personId = UUID.randomUUID().toString();
        caseUrn = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }


    /*
      Currently the optional fields are: arrest date, end date and charge date
     */
    public String addMinimalDefendant() {
        final JsonObject defendantAsJsonObject = createObjectBuilder()
                .add("defendantId", VALUE_DEFENDANT_ID)
                .add("person", createObjectBuilder()
                        .add("id", personId)
                        .add("title", PERSON_TITLE)
                        .add("firstName", PERSON_FIRST_NAME)
                        .add("lastName", PERSON_LAST_NAME)
                        .add("dateOfBirth", PERSON_DATE_OF_BIRTH)
                        .add("nationality", PERSON_NATIONALITY)
                        .add("gender", PERSON_GENDER)
                        .add("address", createObjectBuilder()
                                .add("address1", ADDRESS_LINE_1)
                                .add("address2", ADDRESS_LINE_2)
                                .add("address3", ADDRESS_LINE_3)
                                .add("address4", ADDRESS_LINE_4)
                                .add("postCode", POST_CODE).build()).build())
                .add("caseUrn", caseUrn)
                .add("policeDefendantId", VALUE_POLICE_DEFENDANT_ID)
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", VALUE_OFFENCES_ID)
                                .add("policeOffenceId", VALUE_POLICE_OFFENCE_ID)
                                .add("cpr", createObjectBuilder()
                                        .add("defendantOffender", createObjectBuilder()
                                                .add("year", VALUE_YEAR)
                                                .add("organisationUnit", VALUE_ORGANISATION_UNIT)
                                                .add("number", VALUE_NUMBER)
                                                .add("checkDigit", VALUE_CHECK_DIGIT)
                                                .build())
                                        .add("cjsCode", VALUE_CJS_CODE)
                                        .add("offenceSequence", VALUE_OFFENCE_SEQUENCE)
                                        .build())
                                .add("asnSequenceNumber", VALUE_ASN_SEQUENCE_NUMBER)
                                .add("cjsCode", VALUE_CJS_CODE)
                                .add("reason", VALUE_REASON)
                                .add("description", VALUE_DESCRIPTION)
                                .add("wording", VALUE_WORDING)
                                .add("category", VALUE_CATEGORY)
                                .add("startDate", VALUE_START_DATE)
                                .build())
                        .build())
                .build();

        request = defendantAsJsonObject.toString();

        makePostCall(getWriteUrl("/cases/" + caseId + "/defendant"), WRITE_MEDIA_TYPE, request);

        return request;
    }

    /*
      The optional fields are included, namely, arrest date, end date and charge date
     */
    public void addFullDefendant() {
        addFullDefendant(VALUE_DEFENDANT_ID);
    }

    public void addFullDefendant(final String defendantId) {
        addFullDefendant(defendantId, VALUE_POLICE_DEFENDANT_ID);
    }

    /*
          The optional fields are included, namely, arrest date, end date and charge date
         */
    public void addFullDefendant(final String defendantId, final String policeDefendantId) {
        final JsonObject defendantAsJsonObject = createObjectBuilder()
                .add("defendantId", defendantId)
                .add("person", createObjectBuilder()
                        .add("id", personId)
                        .add("title", PERSON_TITLE)
                        .add("firstName", PERSON_FIRST_NAME)
                        .add("lastName", PERSON_LAST_NAME)
                        .add("dateOfBirth", PERSON_DATE_OF_BIRTH)
                        .add("nationality", PERSON_NATIONALITY)
                        .add("gender", PERSON_GENDER)
                        .add("address", createObjectBuilder()
                                .add("address1", ADDRESS_LINE_1)
                                .add("address2", ADDRESS_LINE_2)
                                .add("address3", ADDRESS_LINE_3)
                                .add("address4", ADDRESS_LINE_4)
                                .add("postCode", POST_CODE).build()).build())
                .add("caseUrn", caseUrn)
                .add("policeDefendantId", policeDefendantId)
                .add("offences", createArrayBuilder()
                        .add(createObjectBuilder()
                                .add("id", VALUE_OFFENCES_ID)
                                .add("policeOffenceId", VALUE_POLICE_OFFENCE_ID)
                                .add("cpr", createObjectBuilder()
                                        .add("defendantOffender", createObjectBuilder()
                                                .add("year", VALUE_YEAR)
                                                .add("organisationUnit", VALUE_ORGANISATION_UNIT)
                                                .add("number", VALUE_NUMBER)
                                                .add("checkDigit", VALUE_CHECK_DIGIT)
                                                .build())
                                        .add("cjsCode", VALUE_CJS_CODE)
                                        .add("offenceSequence", VALUE_OFFENCE_SEQUENCE)
                                        .build())
                                .add("asnSequenceNumber", VALUE_ASN_SEQUENCE_NUMBER)
                                .add("cjsCode", VALUE_CJS_CODE)
                                .add("reason", VALUE_REASON)
                                .add("description", VALUE_DESCRIPTION)
                                .add("wording", VALUE_WORDING)
                                .add("category", VALUE_CATEGORY)
                                .add("startDate", VALUE_START_DATE)
                                // ..and the optional fields
                                .add("endDate", VALUE_END_DATE)
                                .add("arrestDate", VALUE_ARREST_DATE)
                                .add("chargeDate", VALUE_CHARGE_DATE)
                                .build())
                        .build())
                .build();

        request = defendantAsJsonObject.toString();

        makePostCall(getWriteUrl("/cases/" + caseId + "/defendant"), WRITE_MEDIA_TYPE, request);
    }


    public void verifyInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);

        assertThat(jsonResponse.get("defendantId"), is(jsRequest.getString("defendantId")));
        //assertThat(jsonResponse.get("personId"), is(jsRequest.getString("personId")));
        assertThat(jsonResponse.get("policeDefendantId"), is(jsRequest.getString("policeDefendantId")));
        assertThat(jsonResponse.get("offences[0].policeOffenceId"), is(jsRequest.getString("offences[0].policeOffenceId")));
    }

    public void verifyInPublicTopic() {
        final String defendantAddedEvent = publicConsumer.retrieveMessage().orElse(null);

        assertThat(defendantAddedEvent, notNullValue());

        with(defendantAddedEvent)
                .assertThat("$.caseId", is(caseId))
                .assertThat("$.defendantId", is(VALUE_DEFENDANT_ID));
    }

    public void verifyFailureMessageInPrivateTopic() {
        verifyFailureMessageInPrivateTopic(VALUE_DEFENDANT_ID);
    }

    public void verifyFailureMessageInPrivateTopic(final String defendantId) {
        final String defendantAdditionFailedEvent = privateEventDefendantAdditionFailedConsumer.retrieveMessage().orElse(null);

        assertThat(defendantAdditionFailedEvent, notNullValue());

        with(defendantAdditionFailedEvent)
                .assertThat("$.description", notNullValue())
                .assertThat("$.defendantId", is(defendantId))
                .assertThat("$.caseId", is(caseId));
    }


    public void verifyFailureMessageInPublicTopic() {
        verifyFailureMessageInPublicTopic(VALUE_DEFENDANT_ID);
    }

    public void verifyFailureMessageInPublicTopic(final String defendantId) {
        final String defendantAdditionFailedEvent = publicEventDefendantAdditionFailedConsumer.retrieveMessage().orElse(null);

        assertThat(defendantAdditionFailedEvent, notNullValue());

        with(defendantAdditionFailedEvent)
                .assertThat("$.description", notNullValue())
                .assertThat("$.defendantId", is(defendantId))
                .assertThat("$.caseId", is(caseId));
    }

    public void verifyMinimalDefendantAdded() {
        final JsonPath jsRequest = new JsonPath(request);

        final Filter personIdFilter = filter(where("person.id").is(jsRequest.get("person.id")));

        final RequestParamsBuilder getDefendantsByCaseId = getDefendantsByCaseId(caseId);

        final List<RequestParamsBuilder> endPointsToTest = asList(getDefendantsByCaseId);

        endPointsToTest.forEach(endPoint ->
                poll(endPoint)
                        .timeout(20L, SECONDS)
                        .until(
                                status().is(OK),
                                payload()
                                        .isJson(matchMinimalDefendant(personIdFilter)))
        );
    }

    public void verifySearchForCaseByURN() {

        final RequestParamsBuilder caseByUrn = getCaseByUrn(caseUrn);

        final List<RequestParamsBuilder> endPointsToTest = asList(caseByUrn);

        endPointsToTest.forEach(endPoint ->
                poll(endPoint)
                        .until(
                                status().is(OK),
                                payload()
                                        .isJson(allOf(
                                                withJsonPath("$.caseUrn", is(caseUrn)))
                                        ))
        );
    }

    public void verifyFullDefendantAdded() {
        final JsonPath jsRequest = new JsonPath(request);

        final Filter personIdFilter = filter(where("personId").is(jsRequest.get("personId")));

        final RequestParamsBuilder getDefendantsByCaseId = getDefendantsByCaseId(caseId);

        final List<RequestParamsBuilder> endPointsToTest = asList(getDefendantsByCaseId);

        endPointsToTest.forEach(endPoint ->
                poll(endPoint)
                        .until(
                                status().is(OK),
                                payload()
                                        .isJson(matchFullDefendant(personIdFilter)))
        );
    }


    private Matcher<ReadContext> matchFullDefendant(final Filter personIdFilter) {
        return allOf(
                withJsonPath(compile("$.defendants[?]", personIdFilter), hasSize(1)),
                withJsonPath(compile("$.defendants[?].defendantId", personIdFilter), contains(VALUE_DEFENDANT_ID)),

                withJsonPath(compile("$.defendants[?].offences", personIdFilter), hasSize(1)),
                withJsonPath(compile("$.defendants[?].offences[0].id", personIdFilter), contains(VALUE_OFFENCES_ID)),
                withJsonPath(compile("$.defendants[?].offences[0].wording", personIdFilter), contains(VALUE_WORDING)),
                withJsonPath(compile("$.defendants[?].offences[0].policeOffenceId", personIdFilter), contains(VALUE_POLICE_OFFENCE_ID)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderYear", personIdFilter), contains(VALUE_YEAR)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderOrganisationUnit", personIdFilter), contains(VALUE_ORGANISATION_UNIT)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderNumber", personIdFilter), contains(VALUE_NUMBER)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderCheckDigit", personIdFilter), contains(VALUE_CHECK_DIGIT)),
                withJsonPath(compile("$.defendants[?].offences[0].offenceCode", personIdFilter), contains(VALUE_CJS_CODE)),
                withJsonPath(compile("$.defendants[?].offences[0].cjsCode", personIdFilter), contains(VALUE_CJS_CODE)),
                withJsonPath(compile("$.defendants[?].offences[0].offenceSequenceNumber", personIdFilter), contains(Integer.parseInt(VALUE_ASN_SEQUENCE_NUMBER))),
                withJsonPath(compile("$.defendants[?].offences[0].reason", personIdFilter), contains(VALUE_REASON)),
                withJsonPath(compile("$.defendants[?].offences[0].category", personIdFilter), contains(VALUE_CATEGORY)),
                withJsonPath(compile("$.defendants[?].offences[0].startDate", personIdFilter), contains(VALUE_START_DATE)),

                withJsonPath(compile("$.defendants[?].policeDefendantId", personIdFilter), contains(VALUE_POLICE_DEFENDANT_ID)),
                withJsonPath(compile("$.defendants[?].caseId", personIdFilter), contains(caseId)),

                // assert the optional fields
                withJsonPath(compile("$.defendants[?].offences[0].chargeDate", personIdFilter), contains(VALUE_CHARGE_DATE)),
                withJsonPath(compile("$.defendants[?].offences[0].arrestDate", personIdFilter), contains(VALUE_ARREST_DATE)),
                withJsonPath(compile("$.defendants[?].offences[0].endDate", personIdFilter), contains(VALUE_END_DATE))
        );
    }

    private Matcher<ReadContext> matchMinimalDefendant(final Filter personIdFilter) {
        return allOf(
                withJsonPath(compile("$.defendants[?]", personIdFilter), hasSize(1)),
                withJsonPath(compile("$.defendants[?].defendantId", personIdFilter), contains(VALUE_DEFENDANT_ID)),

                withJsonPath(compile("$.defendants[?].offences", personIdFilter), hasSize(1)),
                withJsonPath(compile("$.defendants[?].offences[0].id", personIdFilter), contains(VALUE_OFFENCES_ID)),
                withJsonPath(compile("$.defendants[?].offences[0].wording", personIdFilter), contains(VALUE_WORDING)),
                withJsonPath(compile("$.defendants[?].offences[0].policeOffenceId", personIdFilter), contains(VALUE_POLICE_OFFENCE_ID)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderYear", personIdFilter), contains(VALUE_YEAR)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderOrganisationUnit", personIdFilter), contains(VALUE_ORGANISATION_UNIT)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderNumber", personIdFilter), contains(VALUE_NUMBER)),
                withJsonPath(compile("$.defendants[?].offences[0].cprDefendantOffenderCheckDigit", personIdFilter), contains(VALUE_CHECK_DIGIT)),
                withJsonPath(compile("$.defendants[?].offences[0].offenceCode", personIdFilter), contains(VALUE_CJS_CODE)),
                withJsonPath(compile("$.defendants[?].offences[0].cjsCode", personIdFilter), contains(VALUE_CJS_CODE)),
                withJsonPath(compile("$.defendants[?].offences[0].offenceSequenceNumber", personIdFilter), contains(Integer.parseInt(VALUE_ASN_SEQUENCE_NUMBER))),
                withJsonPath(compile("$.defendants[?].offences[0].reason", personIdFilter), contains(VALUE_REASON)),
                withJsonPath(compile("$.defendants[?].offences[0].category", personIdFilter), contains(VALUE_CATEGORY)),
                withJsonPath(compile("$.defendants[?].offences[0].startDate", personIdFilter), contains(VALUE_START_DATE)),

                withJsonPath(compile("$.defendants[?].policeDefendantId", personIdFilter), contains(VALUE_POLICE_DEFENDANT_ID)),
                withJsonPath(compile("$.defendants[?].caseId", personIdFilter), contains(caseId))
        );
    }

    public String getPersonId() {
        return personId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getDefendantId() {
        return VALUE_DEFENDANT_ID;
    }

    public String getOffenceId() {
        return VALUE_OFFENCES_ID;
    }

    @Override
    public void close() {
        super.close();
        closeSilently(privateEventDefendantAdditionFailedConsumer);
        closeSilently(publicConsumer);
        closeSilently(publicEventDefendantAdditionFailedConsumer);
    }
}
