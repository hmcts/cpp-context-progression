package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.retrieveMessageBody;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;

import java.util.Optional;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the CCT-2487 fix — applicationSource=MH suppresses offence selection.
 *
 * AC1: Case hearing (MH + ACTIVE case) → offences must NOT be stored on the court application.
 * AC2: Application hearing (MH + INACTIVE case, e.g. breach) → offences MUST be preserved.
 */
@SuppressWarnings("squid:S1607")
public class InitiateCourtProceedingsMHSourceIT extends AbstractIT {

    private static final String COURT_APPLICATION_CREATED = "public.progression.court-application-created";

    private static final String MH_ACTIVE_CASE_FIXTURE =
            "applications/progression.initiate-court-proceedings-mh-source-active-case.json";

    private static final String MH_INACTIVE_CASE_FIXTURE =
            "applications/progression.initiate-court-proceedings-mh-source-inactive-case.json";

    private final JmsMessageConsumerClient consumerForCourtApplicationCreated =
            newPublicJmsMessageConsumerClientProvider()
                    .withEventNames(COURT_APPLICATION_CREATED)
                    .getMessageConsumerClient();

    private String caseId;
    private String offenceId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        offenceId = randomUUID().toString();
    }

    /**
     * AC1: MH source + ACTIVE case hearing.
     *
     * The offences sent in the command must NOT be stored on the resulting court application.
     * The CCT-2487 offence-selection logic must not run.
     */
    @Test
    public void shouldNotStoreOffencesWhenApplicationSourceIsMHAndCaseIsActive() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, randomUUID().toString());
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, randomUUID().toString()));

        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, MH_ACTIVE_CASE_FIXTURE);

        verifyCourtApplicationCreatedEventPublished(applicationId);

        final Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.applicationStatus", notNullValue()),
                // offences must be absent — suppressed because source=MH and caseStatus=ACTIVE
                hasNoJsonPath("$.courtApplication.courtApplicationCases[0].offences")
        };

        pollForApplication(applicationId, matchers);
    }

    /**
     * AC2: MH source + INACTIVE case (application hearing, e.g. breach hearing).
     *
     * The offences sent in the command must be preserved on the resulting court application.
     * As-is behaviour applies for inactive cases regardless of source.
     */
    @Test
    public void shouldPreserveOffencesWhenApplicationSourceIsMHAndCaseIsInactive() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, randomUUID().toString());
        pollProsecutionCasesProgressionFor(caseId, getProsecutionCaseMatchers(caseId, randomUUID().toString()));

        final String applicationId = randomUUID().toString();

        initiateCourtProceedingsForCourtApplication(applicationId, caseId, MH_INACTIVE_CASE_FIXTURE);

        verifyCourtApplicationCreatedEventPublished(applicationId);

        // The offence id in the fixture is the literal placeholder OFFENCE_ID which is NOT
        // replaced in this flow (it is a fixed test UUID in the fixture). We assert the
        // offences array is present and non-empty instead.
        final Matcher[] matchers = {
                withJsonPath("$.courtApplication.id", is(applicationId)),
                withJsonPath("$.courtApplication.courtApplicationCases[0].caseStatus", is("INACTIVE")),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0]", notNullValue()),
                withJsonPath("$.courtApplication.courtApplicationCases[0].offences[0].offenceCode", is("CA03012"))
        };

        pollForApplication(applicationId, matchers);
    }

    private void verifyCourtApplicationCreatedEventPublished(final String applicationId) {
        final Optional<JsonObject> message = retrieveMessageBody(consumerForCourtApplicationCreated);
        assertTrue(message.isPresent(), "Expected court-application-created event on JMS topic");
        final String idFromEvent = message.get().getJsonObject("courtApplication").getString("id");
        assertThat(idFromEvent, equalTo(applicationId));
    }
}
