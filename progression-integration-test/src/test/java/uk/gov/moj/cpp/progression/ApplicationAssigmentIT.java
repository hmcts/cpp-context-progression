package uk.gov.moj.cpp.progression;

import static java.util.UUID.randomUUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.Utilities.listenFor;
import static uk.gov.moj.cpp.progression.Utilities.makeCommand;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.referBoxWorkApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.test.matchers.BeanMatcher.isBean;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.verifyInMessagingQueueForBoxWorkReferred;
import static uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper.verifyPostBoxWorkApplicationReferredHearing;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.AssignedUser;
import uk.gov.justice.core.courts.BoxworkAssignmentChanged;
import uk.gov.justice.core.courts.CourtApplication;

import uk.gov.justice.courts.progression.query.Application;
import uk.gov.justice.progression.courts.ChangeBoxworkAssignment;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper;
import uk.gov.moj.cpp.progression.helper.QueueUtil;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.test.matchers.BeanMatcher;
import uk.gov.moj.cpp.progression.test.matchers.MapStringToTypeMatcher;
import uk.gov.moj.cpp.progression.util.QueryUtil;
import uk.gov.moj.cpp.progression.util.ReferBoxWorkApplicationHelper;

import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class ApplicationAssigmentIT extends AbstractIT {

    private UUID courtApplicationId;

    private static final String DOCUMENT_TEXT = STRING.next();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private static final String PROGRESSION_QUERY_APPLICATION_PROPERTY = "progression.query.application";
    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final MessageConsumer consumerForCourtApplicationCreated = publicEvents.createConsumer(COURT_APPLICATION_CREATED);
    private static final String PROGRESSION_QUERY_APPLICATION_JSON = "application/vnd.progression.query.application+json";
    public static final String PROGRESSION_COMMAND_REFER_BOXWORK_APPLICATION_JSON = "progression.command.refer-boxwork-application.json";
    private ReferBoxWorkApplicationHelper helper;
    private String hearingId;


    @Before
    public void setUp() {
        super.setUp();
        courtApplicationId = randomUUID();
        hearingId = UUID.randomUUID().toString();
        helper = new ReferBoxWorkApplicationHelper();
        createMockEndpoints();
        HearingStub.stubInitiateHearing();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        consumerForCourtApplicationCreated.close();
    }

    @Test
    public void shouldAssignBoxworkApplication() throws Exception {

        addStandaloneCourtApplication(courtApplicationId.toString(), randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        final RequestParams applicationQueryRequest = requestParams(getURL(PROGRESSION_QUERY_APPLICATION_PROPERTY,
                courtApplicationId.toString()),
                PROGRESSION_QUERY_APPLICATION_JSON)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();

        final BeanMatcher<Application> preAssignResultMatcher = isBean(Application.class)
                .withValue(Application::getAssignedUser, null)
                .with(Application::getCourtApplication, isBean(CourtApplication.class)
                        .withValue(CourtApplication::getId, courtApplicationId));

        QueryUtil.waitForQueryMatch(applicationQueryRequest, 45, preAssignResultMatcher, Application.class);

        checkUserAssigned(applicationQueryRequest);


    }

    private void checkUserAssigned(final RequestParams applicationQueryRequest) {
        Utilities.EventListener publicEventResulted = listenFor("public.progression.boxwork-assignment-changed")
          .withFilter(MapStringToTypeMatcher.convertStringTo(BoxworkAssignmentChanged.class, isBean(BoxworkAssignmentChanged.class)
               .with(BoxworkAssignmentChanged::getApplicationId, is(courtApplicationId))));


        ChangeBoxworkAssignment changeBoxworkAssignmentCommand ;

        changeBoxworkAssignmentCommand = new ChangeBoxworkAssignment(courtApplicationId, UUID.randomUUID());

        makeCommand(requestSpec, "progression.change-boxwork-assignment")
                .ofType("application/vnd.progression.change-boxwork-assignment+json")
                .withPayload(changeBoxworkAssignmentCommand)
                .executeSuccessfully();

        publicEventResulted.waitFor();

        final BeanMatcher<Application> postAssignResultMatcher = isBean(Application.class)
                .with(Application::getAssignedUser, isBean(AssignedUser.class)
                        .withValue(AssignedUser::getUserId, changeBoxworkAssignmentCommand.getUserId())
                        .withValue(AssignedUser::getFirstName, "testy")

                )
                .with(Application::getCourtApplication, isBean(CourtApplication.class)
                        .withValue(CourtApplication::getId, courtApplicationId));


        QueryUtil.waitForQueryMatch(applicationQueryRequest, 45, postAssignResultMatcher, Application.class);


        changeBoxworkAssignmentCommand = new ChangeBoxworkAssignment(courtApplicationId, null);

        makeCommand(requestSpec, "progression.change-boxwork-assignment")
                .ofType("application/vnd.progression.change-boxwork-assignment+json")
                .withPayload(changeBoxworkAssignmentCommand)
                .executeSuccessfully();


        publicEventResulted.waitFor();

        final BeanMatcher<Application> postAssignResultMatcherUnassign = isBean(Application.class)
                .withValue(Application::getAssignedUser, null);

        QueryUtil.waitForQueryMatch(applicationQueryRequest, 45, postAssignResultMatcherUnassign, Application.class);
    }

    @Test
    public void shouldReferBoxWorkApplicationForHearing() throws Exception {


        addStandaloneCourtApplication(courtApplicationId.toString(),  UUID.randomUUID().toString(), new CourtApplicationsHelper().new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");

        verifyInMessagingQueueForStandaloneCourtApplicationCreated();

        referBoxWorkApplication(courtApplicationId.toString(), hearingId, PROGRESSION_COMMAND_REFER_BOXWORK_APPLICATION_JSON);

        verifyPostBoxWorkApplicationReferredHearing(courtApplicationId.toString());

        verifyInMessagingQueueForBoxWorkReferred();

        final RequestParams applicationQueryRequest = requestParams(getURL(PROGRESSION_QUERY_APPLICATION_PROPERTY,
                courtApplicationId.toString()),
                PROGRESSION_QUERY_APPLICATION_JSON)
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();
        checkUserAssigned(applicationQueryRequest);

    }

    private static void verifyInMessagingQueueForStandaloneCourtApplicationCreated() {
        final Optional<JsonObject> message = QueueUtil.retrieveMessageAsJsonObject(consumerForCourtApplicationCreated);
        String arnResponse = message.get().getString("arn");
        assertTrue(message.isPresent());
        assertTrue(arnResponse.length() == 10);
    }
}
