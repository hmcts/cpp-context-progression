package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import static com.google.common.io.Resources.getResource;
import com.jayway.jsonpath.ReadContext;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import io.restassured.path.json.JsonPath;
import static java.nio.charset.Charset.defaultCharset;
import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import org.apache.http.HttpStatus;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.hasSize;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.OpaHelper.pollForOpaPressList;
import static uk.gov.moj.cpp.progression.helper.OpaHelper.pollForOpaPublicList;
import static uk.gov.moj.cpp.progression.helper.OpaHelper.pollForOpaResultList;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtFirstHearing;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusResulted;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonPath;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.it.framework.util.ViewStoreCleaner.cleanViewStoreTables;
import static uk.gov.moj.cpp.progression.util.FeatureStubUtil.setFeatureToggle;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

@SuppressWarnings("all")
public class OnlinePleasAllocationIT extends AbstractIT {

    private static final String PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED = "public.defence.allocation-pleas-added";
    private static final String PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED = "public.defence.allocation-pleas-updated";
    private static final String ALLOCATION_PLEAS_ADDED = "public.defence.allocation-pleas-added.json";
    private static final String ALLOCATION_PLEAS_UPDATED = "public.defence.allocation-pleas-updated.json";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_HEARING_RESULTED_CASE_UPDATED_V2 = "public.events.hearing.first-hearing-resulted-case-updated";

    private static final String userId = randomUUID().toString();
    private static final String GENERATE_OPA_NOTICE = "progression.command.generate-opa-notice.json";

    private static final String offenceId = "3789ab16-0bb7-4ef1-87ef-c936bf0364f1";
    private static final String offenceId2 = randomUUID().toString();
    private static final String courtCentreId = "111bdd2a-6b7a-4002-bc8c-5c6f93844f40";
    private static final String newCourtCentreId = "999bdd2a-6b7a-4002-bc8c-5c6f93844f40";
    private static final String courtCentreName = "Lavender Hill Magistrate's Court";
    private static final String bailStatusCode = "C";
    private static final String bailStatusDescription = "Remanded into Custody";
    private static final String bailStatusId = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    private static final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(new ObjectMapperProducer().objectMapper());

    private static final String PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_GENERATED = "public.progression.public-list-opa-notice-generated";
    private static final String PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_GENERATED = "public.progression.press-list-opa-notice-generated";
    private static final String PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_GENERATED = "public.progression.result-list-opa-notice-generated";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED = "public.progression.hearing-resulted";

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private JmsMessageProducerClient messageProducerClientPublic;
    private JmsMessageConsumerClient generatedPublicListConsumer;
    private JmsMessageConsumerClient generatedPressListConsumer;
    private JmsMessageConsumerClient generatedResultListConsumer;

    @BeforeAll
    public static void beforeAllTests() {
        setFeatureToggle("OPA", true);
    }

    @BeforeEach
    public void setUp() {

        cleanViewStoreTables(List.of("press_list_opa_notice", "public_list_opa_notice", "result_list_opa_notice"));

        messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        generatedPublicListConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_PUBLIC_LIST_OPA_NOTICE_GENERATED).getMessageConsumerClient();
        generatedPressListConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_PRESS_LIST_OPA_NOTICE_GENERATED).getMessageConsumerClient();
        generatedResultListConsumer = newPublicJmsMessageConsumerClientProvider().withEventNames(PUBLIC_PROGRESSION_RESULT_LIST_OPA_NOTICE_GENERATED).getMessageConsumerClient();
    }

    @Test
    public void shouldAddAndUpdatePleasAllocation_DeactivateOpaNoticesAsHearingResultsAreNotShared() throws Exception {
        final UUID allocationId = randomUUID();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);

        pollAndVerifyAllOpaRegisterCleanedUp(caseId, defendantId, hearingId);

        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);

        JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        pollAndVerifyOpaRegisters(caseId, defendantId, hearingId);

        final JsonObject updatedOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_UPDATED);
        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED, userId), updatedOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_UPDATED, publicEventEnvelope);

        pollAndVerifyOpaRegisters(caseId, defendantId, hearingId);

        final io.restassured.response.Response publicResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-public-list-notice+json", "{}");
        assertThat(publicResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final io.restassured.response.Response pressResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-press-list-notice+json", "{}");
        assertThat(pressResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        final io.restassured.response.Response resultResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-result-list-notice+json", "{}");
        assertThat(publicResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        pollAndVerifyAllOpaRegisterCleanedUp(caseId, defendantId, hearingId);
    }

    @Test
    public void shouldGeneratePublicAndPressOpaNoticesOnHearingConfirmedAndResultNoticeOnHearingResulted() throws Exception {
        final UUID allocationId = randomUUID();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String hearingId = createCaseAndGetHearingId(caseId, defendantId, false);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.opa-hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final JsonObject addOnlinePleaAllocationEvent = buildOnlinePleaAllocationPayload(allocationId.toString(), caseId, defendantId, ALLOCATION_PLEAS_ADDED);
        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, userId), addOnlinePleaAllocationEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ALLOCATION_PLEAS_ADDED, publicEventEnvelope);

        pollAndVerifyOpaRegisters(caseId, defendantId, hearingId);

        final io.restassured.response.Response publicResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-public-list-notice+json", "{}");
        assertThat(publicResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        Matcher publicEventMatcher = isJson(Matchers.allOf(
                withJsonPath("$.defendantId", is(defendantId)),
                withJsonPath("$.hearingId", is(hearingId)))
        );
        verifyEventWithMatchers(generatedPublicListConsumer, publicEventMatcher);

        final io.restassured.response.Response pressResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-press-list-notice+json", "{}");
        assertThat(pressResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));
        verifyEventWithMatchers(generatedPressListConsumer, publicEventMatcher);


        // result hearing and chck opa result notice
        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingWithSingleCaseJsonObject(PUBLIC_HEARING_RESULTED_CASE_UPDATED_V2 + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, bailStatusCode, bailStatusDescription, bailStatusId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);
        pollHearingWithStatusResulted(hearingId);

        final io.restassured.response.Response resultResponse = postCommand(getWriteUrl("/opa-notice/request"),
                "application/vnd.progression.request-opa-result-list-notice+json", "{}");
        assertThat(resultResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));


        publicEventMatcher = isJson(Matchers.allOf(
                withJsonPath("$.defendantId", is(defendantId)),
                withJsonPath("$.hearingId", is(hearingId)),
                withJsonPath("$.notificationId", is(notNullValue())),
                withJsonPath("$.opaNotice.bailStatus", is("Conditional")),
                withJsonPath("$.opaNotice.caseUrn", is("PRCS232397VG")),
                withJsonPath("$.opaNotice.court", is("Liverpool Sreat Mags Court")),
                withJsonPath("$.opaNotice.firstHearingDate", is(notNullValue())),
                withJsonPath("$.opaNotice.firstName", is("samba")),
                withJsonPath("$.opaNotice.middleName", is("a")),
                withJsonPath("$.opaNotice.lastName", is("ramba")),
                withJsonPath("$.opaNotice.offences[0].allocationDecision", is("Defendant chooses trial by jury")),
                withJsonPath("$.opaNotice.offences[0].decisionDate", is("2023-10-08")),
                withJsonPath("$.opaNotice.offences[0].legislation", is("10 years punishment in jail")),
                withJsonPath("$.opaNotice.offences[0].title", is("Case worker murder"))));

        verifyEventWithMatchers(generatedResultListConsumer, publicEventMatcher);
    }

    private String createCaseAndGetHearingId(final String caseId, final String defendantId, final boolean isYouth) throws IOException, JSONException {
        addProsecutionCaseToCrownCourtFirstHearing(caseId, defendantId, isYouth);
        return pollCaseAndGetHearingForDefendant(caseId, defendantId);
    }

    private void verifyEventWithMatchers(final JmsMessageConsumerClient consumer, Matcher matcher) {
        final JsonPath messageBody = retrieveMessageAsJsonPath(consumer, matcher);
        assertThat(messageBody, notNullValue());
    }

    private void pollAndVerifyAllOpaRegisterCleanedUp(final String caseId, final String defendantId, final String hearingId) {
        pollForOpaPressList(getOpaMatcher(caseId, defendantId, hearingId, 0));

        pollForOpaPublicList(getOpaMatcher(caseId, defendantId, hearingId, 0));

        pollForOpaResultList(getOpaMatcher(caseId, defendantId, hearingId, 0));
    }

    private void pollAndVerifyOpaRegisters(final String caseId, final String defendantId, final String hearingId) {
        pollForOpaPressList(getOpaMatcher(caseId, defendantId, hearingId));

        pollForOpaPublicList(getOpaMatcher(caseId, defendantId, hearingId));

        pollForOpaResultList(getOpaMatcher(caseId, defendantId, hearingId));

    }

    private Matcher<? super ReadContext> getOpaMatcher(final String caseId, final String defendantId, final String hearingId) {
        return getOpaMatcher(caseId, defendantId, hearingId, 1);
    }

    private Matcher<? super ReadContext> getOpaMatcher(final String caseId, final String defendantId, final String hearingId, int size) {
        return withJsonPath(
                "$.opaNotices[?(@.caseId == '" + caseId + "' " +
                        "&& @.defendantId == '" + defendantId + "' " +
                        "&& @.hearingId == '" + hearingId + "')]", hasSize(size)
        );
    }

    private JsonObject buildOnlinePleaAllocationPayload(final String allocationId,
                                                        final String caseId,
                                                        final String defendantId,
                                                        final String resourceName) throws IOException {
        final String inputEvent = Resources.toString(getResource(resourceName), defaultCharset());

        return stringToJsonObjectConverter.convert(inputEvent
                .replaceAll("ALLOCATION_ID", allocationId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("OFFENCE_ID1", offenceId)
                .replaceAll("OFFENCE_ID2", offenceId2));
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
                        .replaceAll("SITTING_DAY_1", now().plus(5, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                        .replaceAll("SITTING_DAY_2", now().plus(6, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                        .replaceAll("SITTING_DAY_3", now().plus(7, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
                        .replaceAll("SITTING_DAY_4", now().plus(8, DAYS).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)))
        );
    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String courtCentreId, final String bailStatusCode,
                                                          final String bailStatusDescription, final String bailStatusId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
                        .replaceAll("SITTING_DAY", formatter.format(now().plusMonths(1)))
                        .replaceAll("SHARED_TIME", formatter.format(now().minusDays(1)))
        );
    }
}
