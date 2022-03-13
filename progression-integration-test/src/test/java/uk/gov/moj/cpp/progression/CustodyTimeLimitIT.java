package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedCaseDefendantsOrganisation;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.util.FeatureToggleUtil.enableAmendReshareFeature;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;


import com.jayway.restassured.response.Response;
import java.io.IOException;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.AwaitUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class CustodyTimeLimitIT extends AbstractIT {

    private static final String HEARING_QUERY = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_HEARING_RESULTED_WITH_CTL_EXTENSION = "public.events.hearing.hearing-resulted-with-ctl-extension";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createPublicProducer();
    private static final MessageConsumer prosecutionCaseDefendantListingStatusChanged = privateEvents.createPrivateConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final MessageConsumer extendCustodyTimeLimitResulted = privateEvents.createPrivateConsumer("progression.events.extend-custody-time-limit-resulted");
    private static final MessageConsumer custodyTimeLimitExtended = privateEvents.createPrivateConsumer("progression.events.custody-time-limit-extended");
    private static final MessageConsumer custodyTimeLimitExtendedPublicEvent = publicEvents.createPublicConsumer("public.events.progression.custody-time-limit-extended");

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String offenceId;
    private String newCourtCentreId;
    private String bailStatusCode;
    private String bailStatusDescription;
    private String bailStatusId;
    private static final String docId = randomUUID().toString();
    private static final String cpsDefendantId = randomUUID().toString();

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        bailStatusCode = "C";
        bailStatusDescription = "Remanded into Custody";
        bailStatusId = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        prosecutionCaseDefendantListingStatusChanged.close();
        extendCustodyTimeLimitResulted.close();
        custodyTimeLimitExtended.close();
        custodyTimeLimitExtendedPublicEvent.close();
    }

    @Test
    public void shouldUpdateOffenceCustodyTimeLimitAndCpsDefendantId() throws Exception {
        stubForAssociatedCaseDefendantsOrganisation("stub-data/defence.get-associated-case-defendants-organisation.json", caseId);
        final String extendedCustodyTimeLimit = "2022-09-10";

        enableAmendReshareFeature(true);
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        verifyAddCourtDocumentForCase();

        final String initialHearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        final String hearingIdForCTLExtension = randomUUID().toString();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_WITH_CTL_EXTENSION + ".json", caseId,
                        hearingIdForCTLExtension, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        final Matcher[] defendantMatchers = new Matcher[] {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", equalTo(extendedCustodyTimeLimit)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.isCtlExtended", equalTo(true)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", equalTo(44))
        };

        pollProsecutionCasesProgressionFor(caseId, defendantMatchers);

        final int ctlExpiryCountDown = (int) DAYS.between(LocalDate.now(), LocalDate.parse(extendedCustodyTimeLimit));
        final Matcher[] prosecutionCasesProgressionForCAAG = new Matcher[]{
                withJsonPath("$.defendants[0].id", is(defendantId)),
                withJsonPath("$.defendants[0].ctlExpiryDate", is(extendedCustodyTimeLimit)),
                withJsonPath("$.defendants[0].ctlExpiryCountDown", is(ctlExpiryCountDown)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.timeLimit", is(extendedCustodyTimeLimit)),
                withJsonPath("$.defendants[0].caagDefendantOffences[0].custodyTimeLimit.daysSpent", is(44))
        };

        pollProsecutionCasesProgressionForCAAG(caseId, prosecutionCasesProgressionForCAAG);

        verifyInMessagingQueueForExtendCustodyTimeLimitResulted(hearingIdForCTLExtension, caseId, offenceId, extendedCustodyTimeLimit);
        verifyInMessagingQueueForCustodyTimeLimitExtended(initialHearingId, offenceId, extendedCustodyTimeLimit);
        verifyInMessagingQueueForCustodyTimeLimitExtendedPublicEvent(initialHearingId, offenceId, extendedCustodyTimeLimit);
        verifyOffenceUpdatedWithCustodyTimeLimitAndCpsDefendantId(initialHearingId, extendedCustodyTimeLimit);

    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = AwaitUtil.awaitAndRetrieveMessageAsJsonObject(prosecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
    }

    private static void verifyInMessagingQueueForExtendCustodyTimeLimitResulted(final String hearingId, final String caseId, final String offenceId, final String extendedCustodyTimeLimit) {

        final Optional<JsonObject> message = AwaitUtil.awaitAndRetrieveMessageAsJsonObject(extendCustodyTimeLimitResulted);
        assertTrue(message.isPresent());

        final JsonObject extendCustodyTimeLimitResulted = message.get();
        assertThat(extendCustodyTimeLimitResulted.getString("hearingId"), is(hearingId));
        assertThat(extendCustodyTimeLimitResulted.getString("caseId"), is(caseId));
        assertThat(extendCustodyTimeLimitResulted.getString("offenceId"), is(offenceId));
        assertThat(extendCustodyTimeLimitResulted.getString("extendedTimeLimit"), is(extendedCustodyTimeLimit));

    }

    private static void verifyInMessagingQueueForCustodyTimeLimitExtended(final String hearingId, final String offenceId, final String extendedCustodyTimeLimit) {

        final Optional<JsonObject> message = AwaitUtil.awaitAndRetrieveMessageAsJsonObject(custodyTimeLimitExtended);
        assertTrue(message.isPresent());

        final JsonObject custodyTimeLimitExtended = message.get();
        assertThat(custodyTimeLimitExtended.getJsonArray("hearingIds").getString(0), is(hearingId));
        assertThat(custodyTimeLimitExtended.getString("offenceId"), is(offenceId));
        assertThat(custodyTimeLimitExtended.getString("extendedTimeLimit"), is(extendedCustodyTimeLimit));

    }

    private static void verifyInMessagingQueueForCustodyTimeLimitExtendedPublicEvent(final String hearingId, final String offenceId, final String extendedCustodyTimeLimit) {

        final Optional<JsonObject> message = AwaitUtil.awaitAndRetrieveMessageAsJsonObject(custodyTimeLimitExtendedPublicEvent);
        assertTrue(message.isPresent());

        final JsonObject publicEvent = message.get();
        assertThat(publicEvent.getJsonArray("hearingIds").getString(0), is(hearingId));
        assertThat(publicEvent.getString("offenceId"), is(offenceId));
        assertThat(publicEvent.getString("extendedTimeLimit"), is(extendedCustodyTimeLimit));

    }

    private void verifyOffenceUpdatedWithCustodyTimeLimitAndCpsDefendantId(final String hearingId, final String extendedCustodyTimeLimit) {

        final Matcher[] hearingMatchers = {
                withJsonPath("$", notNullValue()),
                withJsonPath("$.hearing.id", Matchers.is(hearingId))
        };

        final String dbHearing =  pollForResponse("/hearingSearch/" + hearingId, HEARING_QUERY, hearingMatchers);
        final JsonObject hearing = stringToJsonObjectConverter.convert(dbHearing);
        final JsonObject custodyTimeLimit = hearing.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getJsonObject("custodyTimeLimit");
        final String actualCpsDefendantId = hearing.getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getString("cpsDefendantId");
        assertThat(custodyTimeLimit.getBoolean("isCtlExtended"), is(true));
        assertThat(custodyTimeLimit.getString("timeLimit"), is(extendedCustodyTimeLimit));
        assertThat(actualCpsDefendantId, is(cpsDefendantId));

    }

    private void verifyAddCourtDocumentForCase() throws IOException {
        final String body = prepareAddCourtDocumentPayload();
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document-v2+json", body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId,
                singletonList(withJsonPath("$.prosecutionCase.defendants[0].cpsDefendantId", is(cpsDefendantId)))));
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document-for-case-v2.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID1%", defendantId)
                .replaceAll("%RANDOM_CPS_DEFENDANT_ID%", cpsDefendantId);

        return body;
    }
}

