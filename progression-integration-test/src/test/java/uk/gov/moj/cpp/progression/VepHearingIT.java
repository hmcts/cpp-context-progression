package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import io.restassured.response.Response;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.assertThatRequestIsAccepted;
import static uk.gov.moj.cpp.progression.helper.RestHelper.postCommand;
import static uk.gov.moj.cpp.progression.stub.DefenceStub.stubForAssociatedOrganisation;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorDataForGivenProsecutionAuthorityId;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorsByOucode;
import static uk.gov.moj.cpp.progression.stub.VejHearingStub.stubVejHearing;
import static uk.gov.moj.cpp.progression.stub.VejHearingStub.stubVejHearingDeleted;
import static uk.gov.moj.cpp.progression.stub.VejHearingStub.verifyHearingCommandInvoked;
import static uk.gov.moj.cpp.progression.stub.VejHearingStub.verifyHearingDeletedCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class VepHearingIT extends AbstractIT {
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_EVENTS_LISTING_HEARING_DELETED = "public.events.listing.hearing-deleted";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;

    private String ouCode;
    private String prosecutionAuthorityId;

    @BeforeEach
    public void setUp() {
        stubVejHearing();
        stubVejHearingDeleted();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        ouCode = randomAlphanumeric(7);
        prosecutionAuthorityId = randomUUID().toString();
        removeStub(get(urlMatching("/referencedata-service/query/api/rest/referencedata/prosecutors.*")));
        // test specific stub to ensure prosecutor data is unique and consistent
        stubQueryProsecutorDataForGivenProsecutionAuthorityId("restResource/referencedata.query.police.prosecutor.json", prosecutionAuthorityId, ouCode);
        stubQueryProsecutorsByOucode("restResource/referencedata.query.police.prosecutor.json", prosecutionAuthorityId, ouCode);
    }

    @Test
    public void shouldRaiseVejCommandWhenCNotificationFromListingIndicatesHearingConfirmedOrDeleted() throws Exception {
        stubForAssociatedOrganisation("stub-data/defence.get-associated-organisation.json", defendantId);

        addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.police.hearing-confirmed.json", caseId, hearingId, defendantId, courtCentreId, courtCentreName);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        verifyHearingCommandInvoked(asList(caseId, hearingId));

        final JsonObject hearingDeletedJson = getHearingMarkedAsDeletedObject(hearingId);
        final JsonEnvelope publicEventDeletedEnvelope = envelopeFrom(buildMetadata(PUBLIC_EVENTS_LISTING_HEARING_DELETED, userId), hearingDeletedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_EVENTS_LISTING_HEARING_DELETED, publicEventDeletedEnvelope);

        verifyHearingDeletedCommandInvoked(asList(caseId, hearingId));
    }

    private String createReferProsecutionCaseToCrownCourtJsonBody(final String caseId, final String defendantId, final String materialIdOne,
                                                                  final String materialIdTwo, final String courtDocumentId, final String referralId,
                                                                  final String caseUrn, final String prosecutionAuthorityId, final String ouCode, final String filePath) {
        return getPayload(filePath)
                .replaceAll("RANDOM_CASE_ID", caseId)
                .replace("RANDOM_REFERENCE", caseUrn)
                .replaceAll("RANDOM_DEFENDANT_ID", defendantId)
                .replace("RANDOM_DOC_ID", courtDocumentId)
                .replace("RANDOM_MATERIAL_ID_ONE", materialIdOne)
                .replace("RANDOM_MATERIAL_ID_TWO", materialIdTwo)
                .replace("RANDOM_REFERRAL_ID", referralId)
                .replace("RR_ORDERED_DATE", LocalDate.now().toString())
                .replace("RANDOM_PROSECUTION_AUTHORITY_ID", prosecutionAuthorityId)
                .replace("RANDOM_OU_CODE", ouCode);
    }

    private JsonObject getHearingMarkedAsDeletedObject(final String hearingId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.listing.hearing-deleted.json")
                        .replaceAll("HEARING_ID", hearingId)
        );
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

    public void addProsecutionCaseToCrownCourtWithOneGrownDefendantAndTwoOffences(final String caseId, final String defendantId) throws JSONException {
        final JSONObject jsonPayload = new JSONObject(createReferProsecutionCaseToCrownCourtJsonBody(caseId, defendantId, randomUUID().toString(),
                randomUUID().toString(), randomUUID().toString(), randomUUID().toString(), generateUrn(), prosecutionAuthorityId, ouCode, "progression.command.police.prosecution-case-refer-to-court-one-grown-defendant-two-offences.json"));
        jsonPayload.getJSONObject("courtReferral").remove("courtDocuments");
        final String caseCreationPayload = jsonPayload.toString();
        final Response response = postCommand(getWriteUrl("/refertocourt"),
                "application/vnd.progression.refer-cases-to-court+json",
                caseCreationPayload);
        assertThatRequestIsAccepted(response);
    }

}
