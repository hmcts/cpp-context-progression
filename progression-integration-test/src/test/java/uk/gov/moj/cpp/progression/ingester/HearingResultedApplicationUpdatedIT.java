package uk.gov.moj.cpp.progression.ingester;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.AbstractIT;
import uk.gov.moj.cpp.unifiedsearch.test.util.constant.ApplicationExternalCreatorType;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import org.hamcrest.Matcher;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.applications.applicationHelper.ApplicationHelper.initiateCourtProceedingsForCourtApplication;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusResulted;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.UnifiedSearchIndexSearchHelper.findBy;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class HearingResultedApplicationUpdatedIT extends AbstractIT {

    private static final String PUBLIC_HEARING_RESULTED_V2 = "public.events.hearing.hearing-resulted";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_HEARING_RESULTED_TO_NEXT_HEARING_V2 = "ingestion/public.hearing.resulted.application-adjourned-to-next-hearing-with-application-case-V2.json";
    private static final String PUBLIC_HEARING_RESULTED_WITH_DRIVER_NUMBER = "public.hearing.resulted.application-with-driver-number.json";
    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private String applicationId;
    private String caseId;
    private String defendantId;
    private String hearingId;
    private String userId;
    private String courtCentreId;
    private String courtCentreName;
    private String newCourtCentreId;
    private String newCourtCentreName;
    private String caseUrnAlsoActingAsRandomReferences;

    @BeforeEach
    public void setup() {
        caseId = randomUUID().toString();
        applicationId = randomUUID().toString();
        defendantId = randomUUID().toString();
        userId = randomUUID().toString();
        courtCentreId = UUID.fromString("111bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        newCourtCentreId = UUID.fromString("999bdd2a-6b7a-4002-bc8c-5c6f93844f40").toString();
        newCourtCentreName = "Narnia Magistrate's Court";
        caseUrnAlsoActingAsRandomReferences = generateUrn();
    }

    @Test
    public void shouldIngestHearingResultedApplicationUpdatedEvent() throws Exception {

        //GIVEN - WHEN
        addProsecutionCaseToCrownCourt(caseId, defendantId, caseUrnAlsoActingAsRandomReferences);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, caseUrnAlsoActingAsRandomReferences, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "ingestion/progression.initiate-court-proceedings-for-generic-linked-application.json");
        addProsecutionCaseToCrownCourt(caseId, defendantId);

        final JsonEnvelope publicEventResultedEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_TO_NEXT_HEARING_V2, caseId,
                hearingId, defendantId, courtApplicationId, caseUrnAlsoActingAsRandomReferences, newCourtCentreId, newCourtCentreName, "2021-05-26"));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope);
        pollHearingWithStatusResulted(hearingId);

        final JsonEnvelope publicEventResultedEnvelope2 = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), getHearingJsonObject(PUBLIC_HEARING_RESULTED_TO_NEXT_HEARING_V2, caseId,
                hearingId, defendantId, courtApplicationId, caseUrnAlsoActingAsRandomReferences, newCourtCentreId, newCourtCentreName, "2021-05-25"));
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventResultedEnvelope2);

        // THEN
        final Matcher[] caseMatcher = {
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.applications[*].applicationStatus", hasItem(ApplicationStatus.FINALISED.toString())),
        };
        final Optional<JsonObject> courtApplicationResponseJsonObject = findBy(caseMatcher);
        assertTrue(courtApplicationResponseJsonObject.isPresent());

        final String outApplicationStatus = courtApplicationResponseJsonObject.get().getJsonArray("applications").getJsonObject(0).getJsonString("applicationStatus").getString();
        final String outapplicationExternalCreatorType = courtApplicationResponseJsonObject.get().getJsonArray("applications").getJsonObject(0).getJsonString("applicationExternalCreatorType").getString();
        final String outputCaseId = courtApplicationResponseJsonObject.get().getJsonString("caseId").getString();

        assertEquals(ApplicationStatus.FINALISED.toString(), outApplicationStatus);
        assertEquals(ApplicationExternalCreatorType.PROSECUTOR.name(), outapplicationExternalCreatorType);
        assertEquals(caseId, outputCaseId);
    }

    @Test
    public void shouldUpdateDriveNumberForApplication() throws Exception {

        //GIVEN - WHEN
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, hearingId, defendantId, applicationId, caseUrnAlsoActingAsRandomReferences, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);
        pollHearingWithStatusInitialised(hearingId);

        final String courtApplicationId = randomUUID().toString();
        initiateCourtProceedingsForCourtApplication(courtApplicationId, caseId, hearingId, "ingestion/progression.initiate-court-proceedings-for-generic-linked-application.json");

        final JsonObject hearingResultedEventJsonObject = getHearingJsonObject(PUBLIC_HEARING_RESULTED_WITH_DRIVER_NUMBER, caseId,
                hearingId, defendantId, courtApplicationId, caseUrnAlsoActingAsRandomReferences, newCourtCentreId, newCourtCentreName, "2021-05-26");

        publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_HEARING_RESULTED_V2, userId), hearingResultedEventJsonObject);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_RESULTED_V2, publicEventEnvelope);

        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.prosecutionCase.id", is(caseId)),
                withJsonPath("$.prosecutionCase.defendants[0].personDefendant.driverNumber", is("DVL12345"))
        );

    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId, final String reference,
                                            final String courtCentreId, final String courtCentreName) {
        return getHearingJsonObject(path, caseId, hearingId, defendantId, applicationId, reference, courtCentreId, courtCentreName, "2020-01-01");
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String applicationId, final String reference,
                                            final String courtCentreId, final String courtCentreName,
                                            final String orderedDate) {
        return stringToJsonObjectConverter.convert(
                getPayload(path)
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("APPLICATION_ID", applicationId)
                        .replaceAll("APPLICATION_REF", reference)
                        .replaceAll("COURT_CENTRE_ID", courtCentreId)
                        .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                        .replaceAll("OFFENCE_ID", "3789ab16-0bb7-4ef1-87ef-c936bf0364f1")
                        .replaceAll("ORDERED_DATE", orderedDate)
        );
    }
}
