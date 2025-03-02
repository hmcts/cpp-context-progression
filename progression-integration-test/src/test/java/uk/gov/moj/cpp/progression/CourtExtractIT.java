package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import org.hamcrest.CoreMatchers;
import static org.hamcrest.CoreMatchers.hasItem;
import org.hamcrest.Matcher;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCourtApplicationWithDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.ejectCaseExtractPdf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtExtractPdf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollForApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusResulted;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub.getCrownCourtExtractDocumentRequestByDefendant;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

@SuppressWarnings("squid:S1607")
public class CourtExtractIT extends AbstractIT {
    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";

    private static final String CROWN_COURT_EXTRACT = "CrownCourtExtract";
    private static final String CERTIFICATE_OF_CONVICTION = "CertificateOfConviction";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_JSON = "progression.command.create-court-application.json";
    private static final String PROGRESSION_COMMAND_CREATE_COURT_APPLICATION_WITH_DEFENDANT_JSON = "progression.command.create-court-application-with-defendant.json";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String caseId;
    private String defendantId;
    private String userId;
    private String hearingId;
    private String courtCentreId;
    private String courtApplicationId;
    private String reportingRestrictionId;

    @BeforeEach
    public void setUp() {
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        userId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
        courtApplicationId = randomUUID().toString();
        reportingRestrictionId = randomUUID().toString();
    }

    @Test
    public void shouldGetCourtExtract_whenExtractTypeIsCertificateOfConviction() throws Exception {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject("public.listing.hearing-confirmed.json",
                caseId, hearingId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        // when
        String documentContentResponse = getCourtExtractPdf(caseId, defendantId, "", CERTIFICATE_OF_CONVICTION);
        // then
        assertThat(documentContentResponse, is(notNullValue()));

        documentContentResponse = getCourtExtractPdf(caseId, defendantId, hearingId, CROWN_COURT_EXTRACT);
        // then
        assertThat(documentContentResponse, is(notNullValue()));
        verifyContentsInCrownCourtExtractPayloadUsedForPdfGeneration();
    }

    @Test
    public void shouldGetCourtExtract_whenLinkedApplicationAdded() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject("public.listing.hearing-confirmed.json",
                caseId, hearingId, defendantId, courtCentreId));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        doAddCourtApplicationAndVerify(true);//Appeal pending scenario

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, "", CERTIFICATE_OF_CONVICTION);
        // then
        assertThat(documentContentResponse, is(notNullValue()));
    }

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

    @Test
    public void shouldExtractCrownCourtFromResultedHearingWithPleaV2() throws Exception {
        final String newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final JsonObject payload = getHearingJsonObject(PUBLIC_HEARING_RESULTED_V2 + ".json", caseId,
                hearingId, defendantId, newCourtCentreId, reportingRestrictionId, "2021-03-29");

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), payload);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope);
        pollHearingWithStatusResulted(hearingId);

        Matcher[] personDefendantOffenceUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", CoreMatchers.is(caseId)),

                withJsonPath("$.hearingsAtAGlance.hearings.[*].type.description", hasItem("Sentence")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(newCourtCentreId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].defendants.[*].id", hasItem(defendantId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourtDefendantIds[0]", hasItem(defendantId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.name", hasItem("Derby Youth Court")),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.courtCode", hasItem(5647)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].youthCourt.youthCourtId", hasItem("a4cd3c1d-be10-410d-a50d-e260cdbf6d19")),

                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.custodyTimeLimit", CoreMatchers.is("2018-01-01")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.timeLimit", CoreMatchers.is("2018-09-10")),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.bailStatus.custodyTimeLimit.daysSpent", CoreMatchers.is(44)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.timeLimit", CoreMatchers.is("2018-09-14")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].custodyTimeLimit.daysSpent", CoreMatchers.is(55)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].orderIndex", CoreMatchers.is(2)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].indictmentParticular", CoreMatchers.is("offence-indictmentParticular")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].id", CoreMatchers.is(reportingRestrictionId)),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].judicialResultId", CoreMatchers.is("0f5b8757-e588-4b7f-806a-44dc0eb0e75e")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].label", CoreMatchers.is("Reporting Restriction Label")),
                withJsonPath("$.prosecutionCase.defendants[0].offences[0].reportingRestrictions[0].orderedDate", CoreMatchers.is("2020-10-20"))
        };

        pollProsecutionCasesProgressionFor(caseId, personDefendantOffenceUpdatedMatchers);

        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, hearingId, CROWN_COURT_EXTRACT);
        assertThat(documentContentResponse, is(notNullValue()));
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

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId,
                                            final String reportingRestrictionId, final String orderedDate) {
        final String strPayload = getPayload(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("ORDERED_DATE", orderedDate)
                .replaceAll("REPORTING_RESTRICTION_ID", reportingRestrictionId);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private void verifyContentsInCrownCourtExtractPayloadUsedForPdfGeneration() throws JSONException {
        final Optional<JSONObject> documentRequest = getCrownCourtExtractDocumentRequestByDefendant(defendantId);
        assertThat(documentRequest.isPresent(), is(true));
        final JSONObject crownCourtExtractPayload = documentRequest.get();

        assertThat(crownCourtExtractPayload.has("prosecutingAuthority"), is(true));
        assertThat(crownCourtExtractPayload.getJSONObject("defendant").getString("arrestSummonsNumber"), is("arrest123"));
    }
}

