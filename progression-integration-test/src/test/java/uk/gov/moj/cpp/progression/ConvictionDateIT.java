package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageAsJsonObject;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.util.FeatureToggleUtil.enableAmendReshareFeature;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.util.ConvictionDateHelper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class ConvictionDateIT extends AbstractIT {

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
            .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");

    private static final String NEW_COURT_CENTRE_ID = fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

    private static final String BAIL_STATUS_CODE = "C";
    private static final String BAIL_STATUS_DESCRIPTION = "Remanded into Custody";
    private static final String BAIL_STATUS_ID = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static String userId;

    private ConvictionDateHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;

    @BeforeClass
    public static void setUpClass() {
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
    }

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        userId = randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        helper = new ConvictionDateHelper(caseId, offenceId, null);
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendant() throws Exception {
        enableAmendReshareFeature(false);

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")));

        // when
        helper.addConvictionDate();

        // then
        helper.verifyInActiveMQForConvictionDateChanged();

        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].convictionDate", is("2017-02-02"))
        };
        pollProsecutionCasesProgressionFor(caseId, convictionAddedMatchers);

        helper.removeConvictionDate();

        helper.verifyInActiveMQForConvictionDateRemoved();

        pollProsecutionCasesProgressionFor(caseId, withoutJsonPath("$.prosecutionCase.defendants[0].offences[0].convictionDate"));
    }

    @Test
    public void shouldUpdateOffenceUnderCourtApplicationCase() throws Exception {
        enableAmendReshareFeature(false);

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")));

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-appeal-application.json");

        pollForApplication(courtApplicationId);

        helper = new ConvictionDateHelper(null, "28b3d444-ae80-4920-a70f-ef01e128188e", courtApplicationId);

        // when
        helper.addConvictionDate();

        // then
        helper.verifyInActiveMQForConvictionDateChanged();

        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].convictionDate", is("2017-02-02"))
        };
        pollForApplication(courtApplicationId, convictionAddedMatchers);

        helper.removeConvictionDate();

        helper.verifyInActiveMQForConvictionDateRemoved();

        pollForApplication(courtApplicationId, withoutJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].convictionDate"));
    }

    @Test
    public void shouldUpdateOffenceUnderCourtApplicationCourtOrder() throws Exception {
        enableAmendReshareFeature(false);

        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        pollProsecutionCasesProgressionFor(caseId, withJsonPath("$.prosecutionCase.defendants[0].personDefendant.personDetails.firstName", is("Harry")));

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-court-order-linked-application.json");
        pollForApplication(courtApplicationId);

        helper = new ConvictionDateHelper(null, "28b3d444-ae80-4920-a70f-ef01e128188e", courtApplicationId);

        // when
        helper.addConvictionDate();

        // then
        helper.verifyInActiveMQForConvictionDateChanged();

        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.convictionDate", is("2017-02-02"))
        };
        pollForApplication(courtApplicationId, convictionAddedMatchers);

        helper.removeConvictionDate();

        helper.verifyInActiveMQForConvictionDateRemoved();

        pollForApplication(courtApplicationId, withoutJsonPath("$.courtApplication.courtOrder.courtOrderOffences[0].offence.convictionDate"));
    }

    @Test
    public void shouldUpdateCourtApplication() throws Exception {
        enableAmendReshareFeature(false);

        // given
        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-standalone-application.json");
        pollForApplication(courtApplicationId);

        helper = new ConvictionDateHelper(null, null, courtApplicationId);
        // when
        helper.addConvictionDate();

        // then
        helper.verifyInActiveMQForConvictionDateChanged();

        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.courtApplication.convictionDate", is("2017-02-02"))
        };
        pollForApplication(courtApplicationId, convictionAddedMatchers);

        helper.removeConvictionDate();

        helper.verifyInActiveMQForConvictionDateRemoved();

        pollForApplication(courtApplicationId, withoutJsonPath("$.courtApplication.convictionDate"));
    }

    @Test
    public void shouldRetainTheJudicialResultsWhenConvictionDateIsUpdated() throws IOException {
        enableAmendReshareFeature(false);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject("public.hearing.resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchers());

        helper.addConvictionDate();
        helper.verifyInActiveMQForConvictionDateChanged();
        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].convictionDate", is("2017-02-02"))
        };
        pollProsecutionCasesProgressionFor(caseId, convictionAddedMatchers);

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchers());
    }

    @Test
    public void shouldRetainTheJudicialResultsWhenConvictionDateIsUpdatedV2() throws IOException {
        enableAmendReshareFeature(true);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final String hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingWithSingleCaseJsonObject("public.events.hearing.hearing-resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchers());

        helper.addConvictionDate();
        helper.verifyInActiveMQForConvictionDateChanged();
        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].convictionDate", is("2017-02-02"))
        };
        pollProsecutionCasesProgressionFor(caseId, convictionAddedMatchers);

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchers());
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private JsonObject getHearingWithSingleCaseJsonObject(final String path, final String caseId, final String hearingId,
                                                          final String defendantId, final String offenceId, final String courtCentreId,
                                                          final String bailStatusCode, final String bailStatusDescription, final String bailStatusId) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("OFFENCE_ID", offenceId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("BAIL_STATUS_ID", bailStatusId)
                        .replaceAll("BAIL_STATUS_CODE", bailStatusCode)
                        .replaceAll("BAIL_STATUS_DESCRIPTION", bailStatusDescription)
        );
    }

    private Matcher[] getHearingAtAGlanceMatchers() {
        return new Matcher[]{
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.label", equalTo("Surcharge")),
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.judicialResultId", notNullValue())

        };
    }

}