package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.ConvictionDateHelper;

import java.io.IOException;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class ConvictionDateIT extends AbstractIT {

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private static final String NEW_COURT_CENTRE_ID = fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

    private static final String BAIL_STATUS_CODE = "C";
    private static final String BAIL_STATUS_DESCRIPTION = "Remanded into Custody";
    private static final String BAIL_STATUS_ID = "2593cf09-ace0-4b7d-a746-0703a29f33b5";
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static String userId;

    private ConvictionDateHelper helper;
    private String caseId;
    private String defendantId;
    private String offenceId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        userId = randomUUID().toString();
        offenceId = UUID.fromString("3789ab16-0bb7-4ef1-87ef-c936bf0364f1").toString();
        helper = new ConvictionDateHelper(caseId, offenceId, null);

    }

    @Test
    public void shouldUpdateCourtApplication() throws Exception {
        // given
        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, "applications/progression.initiate-court-proceedings-for-standalone-application.json");
        pollForApplication(courtApplicationId);

        helper = new ConvictionDateHelper(null, null, courtApplicationId);
        // when
        helper.addConvictionDate();

        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.courtApplication.convictionDate", is("2017-02-02"))
        };
        pollForApplication(courtApplicationId, convictionAddedMatchers);

        helper.removeConvictionDate();

        pollForApplication(courtApplicationId, withoutJsonPath("$.courtApplication.convictionDate"));
    }

    @Test
    public void shouldRetainTheJudicialResultsWhenConvictionDateIsUpdatedV2() throws IOException, JSONException {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);;

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingWithSingleCaseJsonObject("public.events.hearing.hearing-resulted-and-hearing-at-a-glance-updated.json", caseId,
                hearingId, defendantId, offenceId, NEW_COURT_CENTRE_ID, BAIL_STATUS_CODE, BAIL_STATUS_DESCRIPTION, BAIL_STATUS_ID));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);
        pollHearingWithStatus(hearingId, "HEARING_RESULTED");

        helper.addConvictionDate();
        final Matcher[] convictionAddedMatchers = {
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].chargeDate", is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].convictionDate", is("2017-02-02"))
        };
        pollProsecutionCasesProgressionFor(caseId, convictionAddedMatchers);
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