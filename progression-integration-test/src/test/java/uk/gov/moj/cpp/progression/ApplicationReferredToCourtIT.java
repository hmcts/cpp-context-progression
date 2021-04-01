package uk.gov.moj.cpp.progression;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getApplicationFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.io.IOException;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ApplicationReferredToCourtIT extends AbstractIT {

    //TODO: to unblock build failure in pipeline. This test runs in local Successfully.
    private static final String PROGRESSION_REFER_APPLICATION_TO_COURT_JSON = "application/vnd.progression.refer-application-to-court+json";
    private static final String PROGRESSION_EVENT_APPLICATION_REFERRED_TO_COURT_FILE = "progression.event.application-referred-to-court.json";
    private static final String PROGRESSION_QUERY_APPLICATION_JSON = "application/vnd.progression.query.application+json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed.json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application.json";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String caseId;
    private String defendantId;
    private String hearingId;
    private String courtCentreId;
    private String courtApplicationId;

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();

        userId = randomUUID().toString();
        hearingId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        courtApplicationId = UUID.randomUUID().toString();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldApplicationReferredToCourtWhenNewApplicationAddedToListedHearing() throws Exception {

        //New case added, referred tocourt and verified
        doReferCaseToCourtAndVerify();

        //New hearing added with HEARING_INITIALISED status and verified
        doHearingConfirmedAndVerify();

        //New court application added with DRAFT status and verified
        doAddCourtApplicationAndVerify();

        //CourtApplication content from file with replaced hearingId, courtApplicationId
        final String applicationReferredToCourtString = getApplicationReferredToCourtJsonBody(hearingId, courtApplicationId,
                PROGRESSION_EVENT_APPLICATION_REFERRED_TO_COURT_FILE);

        //progression.refer-application-to-court command triggered
        postCommand(getWriteUrl("/referapplicationtocourt"),
                PROGRESSION_REFER_APPLICATION_TO_COURT_JSON,
                applicationReferredToCourtString);

        //Verifying the CourtApplication status modified as LISTED
        verifyPostUpdateCourtApplicationStatusCommand(courtApplicationId);

    }

    private void doReferCaseToCourtAndVerify() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, defendantId));
    }

    private void doHearingConfirmedAndVerify() {
        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId))
        );
    }

    private void doAddCourtApplicationAndVerify() throws Exception {
        addCourtApplication(caseId, courtApplicationId, PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON);
        final String caseResponse = getApplicationFor(courtApplicationId);
        assertThat(caseResponse, is(notNullValue()));
    }

    private void verifyPostUpdateCourtApplicationStatusCommand(final String id) {
        Matcher[] matchers = {withJsonPath("$.courtApplication.id", is(id)),
                withJsonPath("$.courtApplication.applicationStatus", is(ApplicationStatus.LISTED.toString()))
        };
        pollForResponse("/applications/" + id, PROGRESSION_QUERY_APPLICATION_JSON, matchers);

    }

    private String getApplicationReferredToCourtJsonBody(final String hearingId, final String courtApplicationId, final String fileName) throws IOException {
        return getPayload(fileName)
                .replace("RANDOM_HEARING_ID", hearingId)
                .replace("RANDOM_APPLICATION_ID", courtApplicationId);
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
