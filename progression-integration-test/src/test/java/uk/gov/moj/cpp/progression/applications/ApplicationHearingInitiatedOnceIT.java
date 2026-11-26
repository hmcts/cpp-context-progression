package uk.gov.moj.cpp.progression.applications;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.intiateCourtProceedingForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.pollForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.HearingStub.stubApplicationTimelineWithNoHearings;
import static uk.gov.moj.cpp.progression.stub.HearingStub.verifyInitiateCourtHearingSentOnce;
import static uk.gov.moj.cpp.progression.stub.HearingStub.verifyNoInitiateCourtHearingForApplication;
import static uk.gov.moj.cpp.progression.stub.IdMapperStub.stubForApplicationShortId;
import static uk.gov.moj.cpp.progression.stub.ListingStub.getPostListCourtHearing;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.progression.AbstractIT;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationHearingInitiatedOnceIT extends AbstractIT {

    private static final String COURT_APPEAL_APPLICATION_FIXTURE = "applications/progression.initiate-court-proceedings-for-court-appeal-application.json";
    private static final String STANDALONE_APPLICATION_FIXTURE = "progression.initiate-court-proceedings-for-application.json";
    private static final String HEARING_CONFIRMED_FIXTURE = "public.listing.hearing-confirmed-application-only-hearing.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String LINKED_APPLICATION_HEARINGS_FOR_COURT_EXTRACT = "application/vnd.progression.query.linked-application-hearings-for-court-extract+json";

    private final JmsMessageProducerClient publicMessageProducerClient = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String applicationId;
    private String caseId;
    private String defendantId;
    private String masterDefendantId;
    private String hearingId;

    @BeforeEach
    public void setUp() {
        applicationId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        masterDefendantId = randomUUID().toString();
        hearingId = randomUUID().toString();
    }

    @Test
    public void shouldInitiateApplicationHearingOnlyOnceListingHasConfirmedIt() throws Exception {
        stubForApplicationShortId(randomUUID().toString().replace("-", "").substring(0, 8), applicationId);
        initiateCourtProceedingsForCourtApplication(applicationId, COURT_APPEAL_APPLICATION_FIXTURE);
        pollForCourtApplication(applicationId, withJsonPath("$.courtApplication.applicationStatus", is("UN_ALLOCATED")));

        final JsonObject listedHearing = stringToJsonObjectConverter.convert(getPostListCourtHearing(applicationId))
                .getJsonArray("hearings").getJsonObject(0);
        hearingId = listedHearing.getString("id");
        verifyNoInitiateCourtHearingForApplication(applicationId);

        final JsonObject courtCentre = listedHearing.getJsonObject("courtCentre");
        final JsonEnvelope hearingConfirmed = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, randomUUID().toString()),
                hearingConfirmedPayload(courtCentre.getString("id"), courtCentre.getString("name")));
        publicMessageProducerClient.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmed);

        pollForApplicationStatus(applicationId, "LISTED");
        verifyInitiateCourtHearingSentOnce(hearingId);
    }

    @Test
    public void shouldReturnNoLinkedApplicationHearingsForCourtExtractWhileHearingContextHasNoneForTheApplication() throws Exception {
        stubApplicationTimelineWithNoHearings(applicationId);
        intiateCourtProceedingForApplication(applicationId, caseId, defendantId, masterDefendantId, hearingId, STANDALONE_APPLICATION_FIXTURE);
        pollForCourtApplication(applicationId, withJsonPath("$.courtApplication.id", is(applicationId)));

        pollForResponse("/applications/" + applicationId + "/defendants/" + defendantId,
                LINKED_APPLICATION_HEARINGS_FOR_COURT_EXTRACT,
                withJsonPath("$.linkedApplicationHearings.length()", is(0)));
    }

    private JsonObject hearingConfirmedPayload(final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(getPayload(HEARING_CONFIRMED_FIXTURE)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("APPLICATION_ID", applicationId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName));
    }
}
