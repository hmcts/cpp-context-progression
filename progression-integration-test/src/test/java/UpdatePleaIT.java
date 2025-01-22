import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addStandaloneCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplicationStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.progression.helper.CourtApplicationsHelper.CourtApplicationRandomValues;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.nio.charset.Charset;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UpdatePleaIT extends AbstractIT {
    private static final String DOCUMENT_TEXT = STRING.next();
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final String PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED = "public.hearing.hearing-offence-plea-updated";
    protected static final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;
    private String applicationId;

    @BeforeAll
    public static void setUpClass() {
        stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
    }


    @BeforeEach
    public void setUp() {
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        applicationId = randomUUID().toString();
    }

    @Test
    public void shouldUpdateOffenceVerdictWhenRaisedPublicEvent() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        final JsonObject hearingVerdictUpdatedJson = getPleaPublicEventPayload(hearingId, caseId, defendantId);

        final JsonEnvelope publicEventEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED, userId), hearingVerdictUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED, publicEventEnvelope2);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].plea.offenceId", Matchers.is("3789ab16-0bb7-4ef1-87ef-c936bf0364f1")),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].offences[0].plea.pleaDate", Matchers.is("2017-05-06"))
        );

    }

    @Test
    public void shouldUpdateVerdictOfApplication() throws Exception {
        addStandaloneCourtApplication(applicationId, randomUUID().toString(), new CourtApplicationRandomValues(), "progression.command.create-standalone-court-application.json");
        pollForApplicationStatus(applicationId, "DRAFT");

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingWithStandAloneApplicationJsonObject("public.listing.hearing-confirmed-applications-only.json",
                applicationId, hearingId, caseId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForApplicationStatus(applicationId, "LISTED");

        final JsonObject hearingVerdictUpdatedJson = getPleaPublicEventPayloadForApplication(hearingId, applicationId);

        final JsonEnvelope publicEventEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED, userId), hearingVerdictUpdatedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_HEARING_OFFENCE_PLEA_UPDATED, publicEventEnvelope2);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", Matchers.is(hearingId)),
                withJsonPath("$.hearing.courtApplications[0].id", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].plea.applicationId", Matchers.is(applicationId)),
                withJsonPath("$.hearing.courtApplications[0].plea.pleaDate", Matchers.is("2017-05-06"))
        );

    }

    private JsonObject getHearingWithStandAloneApplicationJsonObject(final String path, final String applicationId, final String hearingId, final String caseId, final String defendantId, final String courtCentreId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }

    private JsonObject getPleaPublicEventPayload(final String hearingId, final String caseId, final String defendantId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-plea-updated.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("DEFENDANT_ID", defendantId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getPleaPublicEventPayloadForApplication(final String hearingId, final String applicationId) {
        final String strPayload = getPayloadForCreatingRequest("public.hearing.hearing-offence-plea-updated-for-application.json")
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("APPLICATION_ID", applicationId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId, final String courtCentreName) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
        );
    }
}
