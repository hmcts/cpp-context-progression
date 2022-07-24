package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneDefendantAndTwoOffences;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForLegalEntityDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchersForLegalEntity;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.util.PleadOnlineHelper;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.jsonpath.ReadContext;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PleadOnlineIT extends AbstractIT {

    private final MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents.createPublicConsumer("public.progression.prosecution-case-created");
    private PleadOnlineHelper pleadOnlineHelper;

    private static final MessageConsumer messageConsumerOnlinePleaRecorded = QueueUtil.privateEvents.createPrivateConsumer("progression.event.online-plea-recorded");


    @Before
    public void setUp() throws Exception {
        setupLoggedInUsersPermissionQueryStub();

        pleadOnlineHelper = new PleadOnlineHelper();
    }

    @Test
    public void shouldSubmitOnlinePleaRequestForIndividual() throws IOException {

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

        final JsonPath matchers = QueueUtil.retrieveMessage(messageConsumerOnlinePleaRecorded, isJson(Matchers.allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.pleadOnline.defendantId", is(defendantId.toString()))
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

        final JsonPath matchers = QueueUtil.retrieveMessage(messageConsumerOnlinePleaRecorded, isJson(Matchers.allOf(
                withJsonPath("$.caseId", is(caseId.toString())),
                withJsonPath("$.pleadOnline.defendantId", is(defendantId.toString()))
        )));
        Assert.assertNotNull(matchers);


    }

    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
    }


}
