package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsForDefendantMatching;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.matchDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubInitiateHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.RestHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

public class CaseLsmInfoIT extends AbstractIT {
    private static final String PROGRESSION_QUERY_CASE_LSM_INFO = "application/vnd.progression.query.case-lsm-info+json";

    private MessageConsumer publicEventConsumerForProsecutionCaseCreated = publicEvents
            .createPublicConsumer("public.progression.prosecution-case-created");

    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private String prosecutionCaseId_1;
    private String defendantId_1;
    private String masterDefendantId_1;
    private String prosecutionCaseId_2;
    private String defendantId_2;
    private String materialIdActive;
    private String materialIdDeleted;
    private String referralReasonId;
    private String listedStartDateTime;
    private String earliestStartDateTime;
    private String defendantDOB;
    private String hearingId;
    private String courtCentreId;


    @Before
    public void setUp() {
        stubInitiateHearing();
        prosecutionCaseId_1 = randomUUID().toString();
        defendantId_1 = randomUUID().toString();
        masterDefendantId_1 = randomUUID().toString();
        prosecutionCaseId_2 = randomUUID().toString();
        defendantId_2 = randomUUID().toString();
        materialIdActive = randomUUID().toString();
        materialIdDeleted = randomUUID().toString();
        referralReasonId = randomUUID().toString();
        listedStartDateTime = ZonedDateTimes.fromString("2019-06-30T18:32:04.238Z").toString();
        earliestStartDateTime = ZonedDateTimes.fromString("2019-05-30T18:32:04.238Z").toString();
        defendantDOB = LocalDate.now().minusYears(15).toString();
        hearingId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @Test
    public void shouldVerifyCaseLsmInfo() throws IOException {
        // initiation of case
        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_1, defendantId_1, masterDefendantId_1, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        Matcher[] prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_1, defendantId_1, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_1, prosecutionCaseMatchers);

        initiateCourtProceedingsForDefendantMatching(prosecutionCaseId_2, defendantId_2, defendantId_2, materialIdActive, materialIdDeleted, referralReasonId, listedStartDateTime, earliestStartDateTime, defendantDOB);
        verifyInMessagingQueueForProsecutionCaseCreated();
        prosecutionCaseMatchers = getProsecutionCaseMatchers(prosecutionCaseId_2, defendantId_2, emptyList());
        pollProsecutionCasesProgressionFor(prosecutionCaseId_2, prosecutionCaseMatchers);

        // match defendant2 associated to case 2
        matchDefendant(prosecutionCaseId_2, defendantId_2, prosecutionCaseId_1, defendantId_1, masterDefendantId_1);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        prosecutionCaseId_2, hearingId, defendantId_2, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(randomUUID().toString())
                        .build());

        poll(requestParams(getReadUrl(String.format("/prosecutioncases/%s/lsm-info", prosecutionCaseId_2)), PROGRESSION_QUERY_CASE_LSM_INFO).withHeader(USER_ID, randomUUID()))
                .timeout(RestHelper.TIMEOUT, TimeUnit.SECONDS)
                .until(
                        status().is(OK),
                        payload().isJson(allOf(withJsonPath("$.matchedDefendantCases[*].caseId", hasItems(prosecutionCaseId_1, prosecutionCaseId_2)),
                                withJsonPath("$.matchedDefendantCases[*].defendants[0].firstName", hasItem(equalTo("Harry"))),
                                withJsonPath("$.matchedDefendantCases[*].defendants[0].middleName", hasItem(equalTo("Jack"))),
                                withJsonPath("$.matchedDefendantCases[*].defendants[0].lastName", hasItem(equalTo("Kane Junior"))),
                                withJsonPath("$.matchedDefendantCases[*].defendants[0].id", hasItems(defendantId_1, defendantId_2)),
                                withJsonPath("$.matchedDefendantCases[*].defendants[0].masterDefendantId", hasItem(equalTo(masterDefendantId_1))),
                                withJsonPath("$.matchedDefendantCases[*].defendants[0].offences[0].offenceTitle", hasItem(equalTo("ROBBERY")))
                        ))
                );
    }

    private void verifyInMessagingQueueForProsecutionCaseCreated() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(publicEventConsumerForProsecutionCaseCreated);
        assertTrue(message.isPresent());
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return new StringToJsonObjectConverter().convert(strPayload);
    }
}
