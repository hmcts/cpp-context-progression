package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtDocumentFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;
import static uk.gov.moj.cpp.progression.util.WireMockStubUtils.setupAsAuthorisedUser;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.NotificationServiceStub;
import uk.gov.moj.cpp.progression.stub.ReferenceDataStub;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.jayway.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.comparator.CustomComparator;

public class CourtDocumentEmailNotificationIT extends AbstractIT {

    private String caseId;
    private String docId;
    private String defendantId;
    private String hearingId;
    private String courtCentreId;
    private String userId;

    private static final String USER_GROUP_NOT_PRESENT_DROOL = UUID.randomUUID().toString();
    private static final String USER_GROUP_NOT_PRESENT_RBAC = UUID.randomUUID().toString();
    private static final MessageConsumer consumerForCourDocumentSendToCps = privateEvents.createConsumer("progression.event.court-document-send-to-cps");
    private static final MessageConsumer consumerForProgressionCommandEmail = privateEvents.createConsumer("progression.event.email-requested");
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED = "public.hearing.resulted";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final MessageConsumer publicEventConsumer = publicEvents
            .createConsumer("public.progression.court-document-added");




    @BeforeClass
    public static void init() {

        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_DROOL), "stub-data/usersgroups.get-invalid-groups-by-user.json");
        setupAsAuthorisedUser(UUID.fromString(USER_GROUP_NOT_PRESENT_RBAC), "stub-data/usersgroups.get-invalid-rbac-groups-by-user.json");
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        consumerForCourDocumentSendToCps.close();
        consumerForProgressionCommandEmail.close();
        messageProducerClientPublic.close();
        publicEventConsumer.close();
    }


    @Before
    public void setup() {
        HearingStub.stubInitiateHearing();
        NotificationServiceStub.setUp();
        IdMapperStub.setUp();
        ReferenceDataStub.stubGetOrganisationById("/restResource/ref-data-get-organisation.json");
        caseId = randomUUID().toString();
        docId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        userId = randomUUID().toString();
    }

    @Test
    public void shouldGenerateNotificationEventWhenCourtDocumentAdded() throws IOException {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId, newArrayList(
                withJsonPath("$.hearingsAtAGlance.id", is(caseId))
        )));

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId))
        );

        Matcher[] caseUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(equalTo(courtCentreId))),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true))
        };

        pollProsecutionCasesProgressionFor(caseId, caseUpdatedMatchers);
        sendMessage(messageProducerClientPublic,
                PUBLIC_HEARING_RESULTED, getHearingJsonObject(PUBLIC_HEARING_RESULTED + ".json", caseId,
                        hearingId, defendantId, courtCentreId), metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_HEARING_RESULTED)
                        .withUserId(userId)
                        .build());


        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(courtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", is(55))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

        addCourtDocument("expected/expected.progression.add-court-document.json");
        verifyForCourtDocumentSentToCps();
        verifyForProgressionCommandEmail();
    }


    private void addCourtDocument(final String expectedPayloadPath) throws IOException {
        //Given
        final String body = prepareAddCourtDocumentPayload();
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
                .replace("COURT-DOCUMENT-ID", docId.toString())
                .replace("DEFENDENT-ID", defendantId.toString())
                .replace("CASE-ID", caseId.toString());

        JSONAssert.assertEquals(expectedPayload, actualDocument, getCustomComparator());
    }

    private CustomComparator getCustomComparator() {
        return new CustomComparator(STRICT,
                new Customization("courtDocument.materials[0].uploadDateTime", (o1, o2) -> true)
        );
    }

    private String prepareAddCourtDocumentPayload() {
        String body = getPayload("progression.add-court-document.json");
        body = body.replaceAll("%RANDOM_DOCUMENT_ID%", docId)
                .replaceAll("%RANDOM_CASE_ID%", caseId)
                .replaceAll("%RANDOM_DEFENDANT_ID%", defendantId);
        return body;
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


    private void verifyForCourtDocumentSentToCps() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourDocumentSendToCps);
        assertThat(message, notNullValue());
    }

    private void verifyForProgressionCommandEmail() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForProgressionCommandEmail);
        assertThat(message, notNullValue());
        assertThat(message.get(), isJson(withJsonPath("$.caseId",
                Matchers.hasToString(Matchers.containsString(caseId)))));
        assertThat(message.get(), isJson(withJsonPath("$.notifications[0].templateId",
                Matchers.hasToString(Matchers.containsString("85fe515a-ff7a-4d16-acbd-cf93d0c75f57")))));
        assertThat(message.get(), isJson(withJsonPath("$.notifications[0].sendToAddress",
                Matchers.hasToString(Matchers.containsString("abc@xyz.co.uk")))));
    }
}
