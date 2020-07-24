package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationWithDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.ejectCaseExtractPdf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.extractHearingIdFromProsecutionCasesProgression;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtExtractPdf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("squid:S1607")
public class CourtExtractIT extends AbstractIT {

    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String CROWN_COURT_EXTRACT = "CrownCourtExtract";
    private static final String CERTIFICATE_OF_CONVICTION = "CertificateOfConviction";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final MessageConsumer messageConsumerProsecutionCaseDefendantListingStatusChanged = privateEvents
            .createConsumer("progression.event.prosecutionCase-defendant-listing-status-changed");
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application.json";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_WITH_DEFENDANT_JSON = "progression.command.create-court-application-with-defendant.json";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String caseId;
    private String defendantId;
    private String userId;
    private String hearingId;
    private String courtCentreId;
    private String courtApplicationId;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        messageConsumerProsecutionCaseDefendantListingStatusChanged.close();
    }

    public static void verifyInMessagingQueueForCasesReferredToCourts() {
        final String referredToCourt = "public.progression.prosecution-cases-referred-to-court";
        final MessageConsumer messageConsumerClientPublicForReferToCourtOnHearingInitiated = publicEvents.createConsumer(referredToCourt);

        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerClientPublicForReferToCourtOnHearingInitiated);
        assertTrue(message.isPresent());
    }

    @Before
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        userId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();

        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
    }


    @Ignore("CPI-301 - Flaky IT, temporarily ignored for release")
    @Test
    public void shouldGetCourtExtract_whenExtractTypeIsCrownCourtExtract() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String prosecutionCasesResponse = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(prosecutionCasesResponse);
        hearingId = extractHearingIdFromProsecutionCasesProgression(prosecutionCasesJsonObject, defendantId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        assertEquals(caseId, prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getString("id"));

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, hearingId, CROWN_COURT_EXTRACT);
        // then
        assertThat(documentContentResponse, is(notNullValue()));
    }

    @Ignore("CPI-301 - Flaky IT, temporarily ignored for release")
    @Test
    public void shouldGetCourtExtract_whenExtractTypeIsCertificateOfConviction() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String prosecutionCasesResponse = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(prosecutionCasesResponse);
        hearingId = extractHearingIdFromProsecutionCasesProgression(prosecutionCasesJsonObject, defendantId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        assertEquals(caseId, prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getString("id"));

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, "", CERTIFICATE_OF_CONVICTION);
        // then
        assertThat(documentContentResponse, is(notNullValue()));
    }

    @Ignore("CPI-301 - Flaky IT, temporarily ignored for release")
    @Test
    public void shouldGetCourtExtract_whenLinkedApplicationAdded() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String prosecutionCasesResponse = pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
        final JsonObject prosecutionCasesJsonObject = getJsonObject(prosecutionCasesResponse);
        hearingId=extractHearingIdFromProsecutionCasesProgression(prosecutionCasesJsonObject,defendantId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        doAddCourtApplicationAndVerify(true);//Appeal pending scenario

        assertEquals(caseId, prosecutionCasesJsonObject.getJsonObject("hearingsAtAGlance").getString("id"));

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, "", CERTIFICATE_OF_CONVICTION);
        // then
        assertThat(documentContentResponse, is(notNullValue()));
    }

    @Ignore("CPI-301 - Flaky IT, temporarily ignored for release")
    @Test
    public void shouldGetCourtExtract_whenUnresultedCaseWithLikedApplicationIsEjected() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        doAddCourtApplicationAndVerify(false);

        // when
        final String documentContentResponse = ejectCaseExtractPdf(caseId, defendantId);

        // then
        assertThat(documentContentResponse, is(notNullValue()));
    }

    @Ignore("CPI-301 - Flaky IT, temporarily ignored for release")
    @Test
    public void shouldExtractCrownCourtFromResultedHearingWithPlea() throws Exception {
        final String publicHearingResulted = "public.hearing.resulted";
        final String newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));

        hearingId = doVerifyProsecutionCaseDefendantListingStatusChanged();

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId);
        sendMessage(messageProducerClientPublic, PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        verifyInMessagingQueueForCasesReferredToCourts();

        sendMessage(messageProducerClientPublic,
                publicHearingResulted, getHearingJsonObject(publicHearingResulted + ".json", caseId,
                        hearingId, defendantId, newCourtCentreId), metadataBuilder()
                        .withId(randomUUID())
                        .withName(publicHearingResulted)
                        .withUserId(userId)
                        .build());


        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", CoreMatchers.is(caseId)),

                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", CoreMatchers.is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", CoreMatchers.is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", CoreMatchers.is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", CoreMatchers.is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", CoreMatchers.is(55))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, hearingId, CROWN_COURT_EXTRACT);
        assertThat(documentContentResponse, is(notNullValue()));
    }

    private String doVerifyProsecutionCaseDefendantListingStatusChanged(){
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(messageConsumerProsecutionCaseDefendantListingStatusChanged);
        final JsonObject prosecutionCaseDefendantListingStatusChanged = message.get();
        return prosecutionCaseDefendantListingStatusChanged.getJsonObject("hearing").getString("id");
    }

    private void doAddCourtApplicationAndVerify(boolean withDefendant) throws Exception {
        // Creating first application for the case
        if (withDefendant) {
            addCourtApplicationWithDefendant(caseId, courtApplicationId, defendantId, PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_WITH_DEFENDANT_JSON);
        } else {
            addCourtApplication(caseId, courtApplicationId, PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON);
        }
        pollForApplication(courtApplicationId, withJsonPath("$", notNullValue()));
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        return stringToJsonObjectConverter.convert(strPayload);
    }
}

