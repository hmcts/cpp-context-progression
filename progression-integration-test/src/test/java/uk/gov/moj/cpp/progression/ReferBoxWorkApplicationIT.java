package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;

import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.referBoxWorkApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getQueryUri;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.verifyInMessagingQueueForBoxWorkReferred;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.verifyPostBoxWorkApplicationReferredHearing;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.util.Optional;
import java.util.UUID;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;

public class ReferBoxWorkApplicationIT {

    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
    private static final String PROGRESSION_QUERY_APPLICATION_JSON = "application/vnd.progression.query.application+json";
    public static final String PROGRESSION_COMMAND_REFER_BOXWORK_APPLICATION_JSON = "progression.command.refer-boxwork-application.json";
    private ReferBoxWorkApplicationHelper helper;
    private String applicationId;
    private String hearingId;

    @Before
    public void setUp() {
        applicationId = UUID.randomUUID().toString();
        hearingId = UUID.randomUUID().toString();
        helper = new ReferBoxWorkApplicationHelper();
        createMockEndpoints();
        HearingStub.stubInitiateHearing();


    }

    @Test
    public void shouldReferBoxWorkApplicationForHearing() throws Exception {

        addStandaloneCourtApplication(applicationId,  randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        verifyPostUpdateCourtApplicationStatus(applicationId,ApplicationStatus.DRAFT);

        referBoxWorkApplication(applicationId, hearingId, PROGRESSION_COMMAND_REFER_BOXWORK_APPLICATION_JSON);

        verifyPostBoxWorkApplicationReferredHearing(applicationId);

        verifyInMessagingQueueForBoxWorkReferred();

        verifyPostUpdateCourtApplicationStatus(applicationId,ApplicationStatus.IN_PROGRESS);
    }




    private static void verifyPostUpdateCourtApplicationStatus(final String id,final ApplicationStatus applicationStatus) {
        poll(requestParams(getQueryUri("/applications/"+ id), PROGRESSION_QUERY_APPLICATION_JSON)
                .withHeader(USER_ID, UUID.randomUUID()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtApplication.id", equalTo(id)),
                                withJsonPath("$.courtApplication.applicationStatus", equalTo(applicationStatus.toString()))
                        )));
    }


    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String arnResponse = message.get().getString("arn");
        assertTrue(message.isPresent());
        assertTrue(arnResponse.length() == 10);
    }

}

