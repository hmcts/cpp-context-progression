package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForLegalEntityDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchersForLegalEntity;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.progression.util.PleadOnlineHelper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PleadOnlineIT extends AbstractIT {

    private static final JmsMessageConsumerClient publicEventConsumerForProsecutionCaseCreated = newPublicJmsMessageConsumerClientProvider().withEventNames("public.progression.prosecution-case-created").getMessageConsumerClient();
    private PleadOnlineHelper pleadOnlineHelper;

    private static final JmsMessageConsumerClient messageConsumerOnlinePleaRecorded = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.online-plea-recorded").getMessageConsumerClient();
    private static final JmsMessageConsumerClient messageConsumerOnlinePleaPcqVisitedRecorded = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.online-plea-pcq-visited-recorded").getMessageConsumerClient();

    @BeforeEach
    public void setUp() {
        setupLoggedInUsersPermissionQueryStub();

        pleadOnlineHelper = new PleadOnlineHelper();
    }

    @Test
    public void shouldSubmitOnlinePleaRequestForIndividual() throws IOException, JSONException {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )

        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        Response response = pleadOnlineHelper.submitOnlinePlea(caseId.toString(), defendantId.toString(), "progression.command.online-plea-request-individual.json");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final JsonPath matchers = retrieveMessageAsJsonPath(messageConsumerOnlinePleaRecorded, isJson(Matchers.allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.pleadOnline.defendantId", is(defendantId.toString()))
        )));
        Assert.assertNotNull(matchers);


    }

    //CCT-1324
    @Test
    public void shouldRaiseOnlinePleaPcqVisitedRequestForIndividual() throws IOException, JSONException {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences(caseId.toString(), defendantId.toString());
        final Matcher[] caseWithOffenceMatchers = getProsecutionCaseMatchers(caseId.toString(), defendantId.toString(),
                newArrayList(
                        withJsonPath("$.prosecutionCase.defendants[0].offences[0].id", is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                        withJsonPath("$.prosecutionCase.defendants[0].offences[1].id", is("4789ab16-0bb7-4ef1-87ef-c936bf0364f1"))
                )
        );

        pollProsecutionCasesProgressionFor(caseId.toString(), caseWithOffenceMatchers);

        Response response = pleadOnlineHelper.submitOnlinePleaPcqVisited(caseId.toString(), defendantId.toString(), "progression.command.online-plea-pcq-visited-individual.json");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final JsonPath matchers = retrieveMessageAsJsonPath(messageConsumerOnlinePleaPcqVisitedRecorded, isJson(Matchers.allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.pleadOnlinePcqVisited.defendantId", is(defendantId.toString())),
                withJsonPath("$.pleadOnlinePcqVisited.pcqId", is("9ec012ea-566a-4952-ad8d-b09a57030d95")),
                withJsonPath("$.pleadOnlinePcqVisited.urn", is("TFL12345467"))
        )));

        Assert.assertNotNull(matchers);
    }


    @Test
    public void shouldSubmitOnlinePleaRequestForCorporate() throws IOException {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        // initiation of first case
        initiateCourtProceedingsForLegalEntityDefendantMatching(caseId.toString(), defendantId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString(), ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString());
        verifyInMessagingQueueForProsecutionCaseCreated();
        List<Matcher<? super ReadContext>> customMatchers = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDateCode", is(4))
        );

        Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchersForLegalEntity(caseId.toString(), defendantId.toString(), customMatchers);
        pollProsecutionCasesProgressionFor(caseId.toString(), prosecutionCaseMatchers);

        Response response = pleadOnlineHelper.submitOnlinePlea(caseId.toString(), defendantId.toString(), "progression.command.online-plea-request-corporate.json");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final JsonPath matchers = retrieveMessageAsJsonPath(messageConsumerOnlinePleaRecorded, isJson(Matchers.allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.pleadOnline.defendantId", is(defendantId.toString()))
        )));
        Assert.assertNotNull(matchers);

    }

    //CCT-1324
    @Test
    public void shouldSubmitOnlinePleaPcqVisitedRequestForCorporate() throws IOException {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        // initiation of first case
        initiateCourtProceedingsForLegalEntityDefendantMatching(caseId.toString(), defendantId.toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString(), ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString());
        verifyInMessagingQueueForProsecutionCaseCreated();
        List<Matcher<? super ReadContext>> customMatchers = newArrayList(
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].offenceDateCode", is(4))
        );

        Matcher<? super ReadContext>[] prosecutionCaseMatchers = getProsecutionCaseMatchersForLegalEntity(caseId.toString(), defendantId.toString(), customMatchers);
        pollProsecutionCasesProgressionFor(caseId.toString(), prosecutionCaseMatchers);

        Response response = pleadOnlineHelper.submitOnlinePleaPcqVisited(caseId.toString(), defendantId.toString(), "progression.command.online-plea-pcq-visited-corporate.json");
        assertThat(response.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final JsonPath matchers = retrieveMessageAsJsonPath(messageConsumerOnlinePleaPcqVisitedRecorded, isJson(Matchers.allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.pleadOnlinePcqVisited.defendantId", is(defendantId.toString()))
        )));
        Assert.assertNotNull(matchers);

    }

    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageBody(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
    }


}
