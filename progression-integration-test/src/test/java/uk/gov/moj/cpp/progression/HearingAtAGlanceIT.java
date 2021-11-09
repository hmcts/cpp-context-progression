package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.initiateCourtProceedingsWithoutCourtDocument;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionForCAAG;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryCpsProsecutorData;
import static uk.gov.moj.cpp.progression.util.FeatureToggleUtil.enableAmendReshareFeature;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.jayway.jsonpath.ReadContext;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class HearingAtAGlanceIT extends AbstractIT {

    private static final String NEW_COURT_CENTRE_ID = fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

    private static final String BAIL_STATUS_CODE = "C";
    private static final String BAIL_STATUS_DESCRIPTION = "Remanded into Custody";
    private static final String BAIL_STATUS_ID = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";

    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";

    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerClientPublicForHearingResultedCaseUpdated = publicEvents
            .createConsumer(PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED);
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
            .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static String userId;
    private static String hearingId;
    private static String caseId;
    private static String defendantId;
    private static String offenceId;

    @BeforeClass
    public static void setUpClass() {
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerClientPublicForHearingResultedCaseUpdated.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
    }

    private static void verifyInMessagingQueueForHearingResultedCaseUpdated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForHearingResultedCaseUpdated);
        assertTrue(message.isPresent());
        assertThat(message.get().getJsonObject("prosecutionCase").getString("caseStatus"), equalTo("INACTIVE"));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getBoolean("proceedingsConcluded"), equalTo(true));
        assertThat(message.get().getJsonObject("prosecutionCase").getString("cpsOrganisation"), equalTo("A02"));

        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0)
                .getJsonArray("judicialResults").getJsonObject(0).getString("judicialResultId"), equalTo("94d6e18a-4114-11ea-b77f-2e728ce88125"));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0)
                .getJsonArray("judicialResults").getJsonObject(0).getString("category"), equalTo("FINAL"));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0)
                .getJsonArray("judicialResults").getJsonObject(0).getString("category"), equalTo("FINAL"));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0)
                .getJsonArray("judicialResults").getJsonObject(0).getBoolean("alwaysPublished"), equalTo(false));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0)
                .getJsonArray("judicialResults").getJsonObject(0).getBoolean("urgent"), equalTo(false));
        assertThat(message.get().getJsonObject("prosecutionCase").getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0)
                .getJsonArray("judicialResults").getJsonObject(0).getString("resultText"), equalTo("resultText"));

    }

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        userId = randomUUID().toString();
    }

    @Test
    public void shouldSetJudiciaryResultsAtHearingsLevelForHearingAtAGlance() throws Exception {
        stubQueryCpsProsecutorData("/restResource/referencedata.query.prosecutor.by.oucode.json", randomUUID(), HttpStatus.SC_OK);
        enableAmendReshareFeature(false);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        //validate enrichment for refer-cases-to-court
        List<Matcher<? super ReadContext>> additionalMatchers = newArrayList(
                withJsonPath("$.prosecutionCase.prosecutionCaseIdentifier.majorCreditorCode", is("TFL2")),
                withJsonPath("$.prosecutionCase.prosecutionCaseIdentifier.prosecutionAuthorityOUCode", is("GB10056"))
        );

        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId, additionalMatchers));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject("public.hearing.resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchers());
        verifyInMessagingQueueForHearingResultedCaseUpdated();
    }

    @Test
    public void shouldRetainCurrentReportingRestrictionsAfterManuallyAddingOnewithHearing() throws Exception {

        enableAmendReshareFeature(true);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();
        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingWithSingleCaseJsonObject("public.events.hearing.hearing-resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getHearingAtaGlanceMatchersWithReportingRestrictions());
        verifyInMessagingQueueForHearingResultedCaseUpdated();

    }

    @Test
    public void shouldSetJudiciaryResultsAtHearingsLevelForHearingAtAGlanceV2() throws Exception {
        enableAmendReshareFeature(true);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingWithSingleCaseJsonObject("public.events.hearing.hearing-resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchers());
        verifyInMessagingQueueForHearingResultedCaseUpdated();
    }

    @Test
    public void shouldKeepCpsOrganisationForHearingAtAGlance() throws Exception {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("amendReshare", false);
        FeatureStubber.stubFeaturesFor(PROGRESSION_CONTEXT, features);
        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject("public.hearing.resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchersForCpsOrganisation());
        verifyInMessagingQueueForHearingResultedCaseUpdated();
    }

    @Test
    public void shouldKeepCpsOrganisationForHearingAtAGlanceV2() throws Exception {
        enableAmendReshareFeature(true);

        initiateCourtProceedingsWithoutCourtDocument(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingWithSingleCaseJsonObject("public.events.hearing.hearing-resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionFor(caseId, getHearingAtAGlanceMatchersForCpsOrganisation());
        verifyInMessagingQueueForHearingResultedCaseUpdated();
    }


    @Test
    public void shouldSetDefendantLevelJudiciaryResultsAndQuery() throws Exception {
        enableAmendReshareFeature(false);

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingWithSingleCaseJsonObject("public.hearing.resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionForCAAG(caseId, getCaseAtAGlanceMatchers());
        verifyInMessagingQueueForHearingResultedCaseUpdated();
    }


    @Test
    public void shouldSetDefendantLevelJudiciaryResultsAndQueryV2() throws Exception {
        enableAmendReshareFeature(true);

        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED_V2, getHearingWithSingleCaseJsonObject("public.events.hearing.hearing-resulted-and-hearing-at-a-glance-updated.json", caseId,
                        hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED_V2)
                        .withUserId(userId)
                        .build());

        pollProsecutionCasesProgressionForCAAG(caseId, getCaseAtAGlanceMatchers());
        verifyInMessagingQueueForHearingResultedCaseUpdated();
    }
    private String doVerifyProsecutionCaseDefendantListingStatusChanged() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private Matcher[] getHearingAtAGlanceMatchers() {
        return new Matcher[]{
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.label", equalTo("Surcharge")),
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.judicialResultId", notNullValue())

        };
    }

    private Matcher[] getHearingAtaGlanceMatchersWithReportingRestrictions() {
        return new Matcher[]{
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.label", equalTo("Surcharge")),
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.judicialResultId", notNullValue()),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[*]", IsCollectionWithSize.hasSize(2))
        };
    }

    private Matcher[] getHearingAtAGlanceMatchersForCpsOrganisation() {
        return new Matcher[]{
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.label", equalTo("Surcharge")),
                withJsonPath("$.prosecutionCase.cpsOrganisation", equalTo("A01")),
                withJsonPath("$.hearingsAtAGlance.hearings[0].defendantJudicialResults[0].judicialResult.judicialResultId", notNullValue())

        };
    }

    private Matcher[] getCaseAtAGlanceMatchers() {
        return new Matcher[]{
                withJsonPath("$.defendants[0].defendantJudicialResults[0].label", equalTo("Surcharge")),
                withJsonPath("$.defendants[0].defendantJudicialResults[0].judicialResultId", notNullValue()),
                withJsonPath("$.defendants[0].defendantCaseJudicialResults[0].label", equalTo("Costs to Crown Prosecution Service")),
                withJsonPath("$.defendants[0].defendantCaseJudicialResults[0].judicialResultId", notNullValue())

        };
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

}

