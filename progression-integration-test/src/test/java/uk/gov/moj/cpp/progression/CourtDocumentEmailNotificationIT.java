package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPrivateJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.helper.StubUtil.setupMaterialStub;
import static uk.gov.moj.cpp.progression.it.framework.ContextNameProvider.CONTEXT_NAME;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetOrganisationById;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

@SuppressWarnings("squid:S1607")
public class CourtDocumentEmailNotificationIT extends AbstractIT {

    private static final String USER_GROUP_NOT_PRESENT_DROOL = randomUUID().toString();
    private static final String USER_GROUP_NOT_PRESENT_RBAC = randomUUID().toString();
    private final JmsMessageConsumerClient consumerForProgressionCommandEmail = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.email-requested").getMessageConsumerClient();
    private final JmsMessageConsumerClient consumerForProgressionSendToCpsFlag = newPrivateJmsMessageConsumerClientProvider(CONTEXT_NAME).withEventNames("progression.event.send-to-cps-flag-updated").getMessageConsumerClient();
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.events.hearing.hearing-resulted";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String caseId;
    private String docId;
    private String defendantId1;
    private String hearingId;
    private String courtCentreId;
    private String userId;

    @BeforeAll
    public static void init() {
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_DROOL), "stub-data/usersgroups.get-invalid-groups-by-user.json");
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
    }

    @BeforeEach
    public void setup() {
        final String materialId = "5e1cc18c-76dc-47dd-99c1-d6f87385edf1";
        setupMaterialStub(materialId);
        stubGetOrganisationById(REST_RESOURCE_REF_DATA_GET_ORGANISATION_JSON);
        caseId = randomUUID().toString();
        defendantId1 = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        userId = randomUUID().toString();
        docId = randomUUID().toString();
    }

    @Test
    public void shouldGenerateEmailNotificationEventWhenCourtDocumentAdded_DefenceDisclosureToggledOff() throws IOException, JSONException {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("defenceDisclosure", false);
        FeatureStubber.clearCache(CONTEXT_NAME);
        FeatureStubber.stubFeaturesFor(CONTEXT_NAME, features);

        addProsecutionCaseToCrownCourt(caseId, defendantId1);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId1);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject("public.listing.hearing-confirmed.json",
                caseId, hearingId, defendantId1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        new CaseProsecutorUpdateHelper(caseId).updateCaseProsecutor();


        Matcher[] caseUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(equalTo(courtCentreId))),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true)),
                withJsonPath("$.prosecutionCase.prosecutor", notNullValue())
        };

        pollProsecutionCasesProgressionFor(caseId, caseUpdatedMatchers);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED + ".json", caseId,
                hearingId, defendantId1, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED, publicEventResultedEnvelope);

        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(courtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId1)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

        addCourtDocument("expected/expected.progression.add-court-document.json", courtCentreId);

        verifyForProgressionCommandEmail();
    }

    @Test
    public void shouldGenerateAPINotificationEventWhenCourtDocumentAdded_DefenceDisclosureToggledOn() throws IOException, JSONException {
        final ImmutableMap<String, Boolean> features = ImmutableMap.of("defenceDisclosure", true);
        FeatureStubber.clearCache(CONTEXT_NAME);
        FeatureStubber.stubFeaturesFor(CONTEXT_NAME, features);

        addProsecutionCaseToCrownCourt(caseId, defendantId1);
        Matcher[] caseCreatedMatchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withoutJsonPath("$.prosecutionCase.prosecutor")
        };

        pollProsecutionCasesProgressionFor(caseId, caseCreatedMatchers);

        new CaseProsecutorUpdateHelper(caseId).updateCaseProsecutor();
        Matcher[] caseUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.prosecutionCase.prosecutor", notNullValue())
        };

        pollProsecutionCasesProgressionFor(caseId, caseUpdatedMatchers);

        addCourtDocument("expected/expected.progression.add-court-document.json", courtCentreId);
        verifyForProgressionDocumentSentToCpsFlag();
    }


    private void addCourtDocument(final String expectedPayloadPath, final String courtCentreId) throws IOException, JSONException {
        //Given
        final String body = prepareAddCourtDocumentPayloadWithOneDefendant();
        //When
        final Response writeResponse = postCommand(getWriteUrl("/courtdocument/" + docId),
                "application/vnd.progression.add-court-document+json",
                body);
        assertThat(writeResponse.getStatusCode(), equalTo(HttpStatus.SC_ACCEPTED));

        //Then
        final String actualDocument = getCourtDocumentFor(docId, allOf(
                withJsonPath("$.courtDocument.courtDocumentId", equalTo(docId)),
                withJsonPath("$.courtDocument.containsFinancialMeans", equalTo(true)),
                withJsonPath("$.courtDocument.sendToCps", equalTo(true)))
        );


        final String expectedPayload = getPayload(expectedPayloadPath)
                .replace("COURT-DOCUMENT-ID", docId)
                .replace("DEFENDENT-ID", defendantId1)
                .replace("CASE-ID", caseId)
                .replace("DOCUMENT-TYPE-ID", courtCentreId);

        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(LENIENT,
                new Customization("courtDocument.materials", (o1, o2) -> true),
                new Customization("courtDocument.documentTypeId", (o1, o2) -> true)
        );

    }

    private String prepareAddCourtDocumentPayloadWithOneDefendant() {
        String body = getPayload("progression.add-court-document-def-level.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId1)
                .replaceAll("%RANDOM_DOC_TYPE%", randomUUID().toString());
        return body;
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId1, final String courtCentreId) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId1)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("REPORTING_RESTRICTION_ID", randomUUID().toString())
                .replaceAll("ORDERED_DATE", "2023-01-01");
        return stringToJsonObjectConverter.convert(strPayload);
    }


    private void verifyForProgressionDocumentSentToCpsFlag() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForProgressionSendToCpsFlag);
        assertThat(message, notNullValue());
        assertThat(message.get().get("courtDocument"), notNullValue());
    }


    private void verifyForProgressionCommandEmail() {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForProgressionCommandEmail);
        assertThat(message, notNullValue());
        assertThat(message.get(), isJson(withJsonPath("$.caseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
        assertThat(message.get(), isJson(withJsonPath("$.notifications[0].templateId",
                Matchers.hasToString(Matchers.containsString("85fe515a-ff7a-4d16-acbd-cf93d0c75f57")))));
        assertThat(message.get(), isJson(withJsonPath("$.notifications[0].sendToAddress",
                Matchers.hasToString(Matchers.containsString("abc@xyz.co.uk")))));
    }

}
