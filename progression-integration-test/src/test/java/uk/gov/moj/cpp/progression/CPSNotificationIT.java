package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.util.CaseProsecutorUpdateHelper;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.google.common.collect.Lists.newArrayList;
import com.google.common.io.Resources;
import static java.util.UUID.randomUUID;
import org.apache.commons.lang3.RandomStringUtils;
import static org.junit.jupiter.api.Assertions.fail;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCPSCivilProsecutionCaseToCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCPSProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addCivilProsecutionCaseToCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubQueryProsecutorData;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyNoEmailNotificationIsRaised;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;

class CPSNotificationIT extends AbstractIT {
    private static final String PUBLIC_DEFENCE_RECORD_INSTRUCTED = "public.defence.event.record-instruction-details";
    private static final String PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED = "public.defence.defence-organisation-disassociated";
    private static final String PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE = "public.defence.event.record-instruction-details.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed-cps-notification.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(CPSNotificationIT.class.getCanonicalName());

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String ORGANISATION_ID = randomUUID().toString();
    private static final String ORGANISATION_NAME = "Smith Associates Ltd." + RandomStringUtils.randomAlphanumeric(10);
    private final String futureHearingDate = LocalDate.now().plusYears(1) + "T09:30:00.000Z";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;
    private String courtCentreName;

    @BeforeEach
    public void setUp() {
        resetAllRequests();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        stubGetOrganisationDetails(ORGANISATION_ID, ORGANISATION_NAME);
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor.json", randomUUID());
    }

    @Test
    void shouldNotifyCPSWhenDefenceAssociatedWithCPSCriminalCase() throws JSONException {
        addCPSProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        // Instruct
        final JsonObject recordInstructedPublicEvent =
                getInstructedJsonObject(PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE, caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventInstructedEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_RECORD_INSTRUCTED, userId), recordInstructedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_RECORD_INSTRUCTED, publicEventInstructedEnvelope);

        // notify by email
        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    @Test
    void shouldNotifyCPSWhenDefenceAssociatedWithCPSCivilCase() {
        addCPSCivilProsecutionCaseToCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonObject recordInstructedPublicEvent =
                getInstructedJsonObject(PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE, caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventInstructedEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_RECORD_INSTRUCTED, userId), recordInstructedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_RECORD_INSTRUCTED, publicEventInstructedEnvelope);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    @Test
    void shouldNotNotifyCPSWhenProsecutorIsNotCPSAndDefenceAssociatedWithCivilCase() {
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps.json", randomUUID());
        addCivilProsecutionCaseToCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonObject recordInstructedPublicEvent =
                getInstructedJsonObject(PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE, caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventInstructedEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_RECORD_INSTRUCTED, userId), recordInstructedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_RECORD_INSTRUCTED, publicEventInstructedEnvelope);

        verifyNoEmailNotificationIsRaised();
    }

    @Test
    void shouldNotifyCPSWhenDefenceDisassociatedFromCPSCriminalCase() throws JSONException {
        addCPSProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope disassociationEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, userId), buildDisassociationPayload());
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, disassociationEnvelope);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    @Test
    void shouldNotifyCPSWhenDefenceDisassociatedFromCPSCivilCase() {
        addCPSCivilProsecutionCaseToCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope disassociationEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, userId), buildDisassociationPayload());
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, disassociationEnvelope);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    @Test
    void shouldNotNotifyCPSWhenProsecutorIsNotCPSAndDefenceDisassociatedFromCivilCase() {
        stubQueryProsecutorData("/restResource/referencedata.query.prosecutor-noncps.json", randomUUID());
        addCivilProsecutionCaseToCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope disassociationEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, userId), buildDisassociationPayload());
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, disassociationEnvelope);

        verifyNoEmailNotificationIsRaised();
    }

    @Test
    void shouldNotifyCPSWhenDefenceAssociatedWithNonCPSCriminalCase() throws JSONException {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonObject recordInstructedPublicEvent =
                getInstructedJsonObject(PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE, caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventInstructedEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_RECORD_INSTRUCTED, userId), recordInstructedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_RECORD_INSTRUCTED, publicEventInstructedEnvelope);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    @Test
    void shouldNotifyCPSWhenNonCPSCivilCaseUpdatedWithCPSProsecutor() {
        addCivilProsecutionCaseToCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        new CaseProsecutorUpdateHelper(caseId).updateCaseProsecutor();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonObject recordInstructedPublicEvent =
                getInstructedJsonObject(PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE, caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        final JsonEnvelope publicEventInstructedEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_RECORD_INSTRUCTED, userId), recordInstructedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_RECORD_INSTRUCTED, publicEventInstructedEnvelope);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    @Test
    void shouldNotifyCPSWhenCPSCivilCaseUpdatedToAnotherCPSProsecutor() {
        addCPSCivilProsecutionCaseToCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        new CaseProsecutorUpdateHelper(caseId).updateCaseProsecutor();

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope disassociationEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, userId), buildDisassociationPayload());
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, disassociationEnvelope);

        // "test@test.com" is the cpsMcEmailAddress from the updated prosecutor ref data stub (MAGISTRATES hearing)
        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME, "test@test.com"));
    }

    @Test
    void shouldNotifyCPSWhenDefenceDisassociatedFromNonCPSCriminalCase() throws JSONException {
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(hearingId);

        final JsonEnvelope disassociationEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, userId), buildDisassociationPayload());
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_ORGANISATION_DISASSOCIATED, disassociationEnvelope);

        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    private JsonObject buildDisassociationPayload() {
        return createObjectBuilder()
                .add("caseId", caseId)
                .add("defendantId", defendantId)
                .add("organisationId", ORGANISATION_ID)
                .add("userId", userId)
                .add("endDate", "2020-01-01T00:00:00.000Z")
                .add("isLAA", false)
                .build();
    }

    private JsonObject getInstructedJsonObject(final String path, final String caseId, final String hearingId,
                                               final String defendantId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("ORGANISATION_ID", ORGANISATION_ID)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId)
                .replaceAll("COURT_CENTRE_NAME", courtCentreName)
                .replaceAll("FUTURE_HEARING_DATE", futureHearingDate);
        LOGGER.info("Payload: {}", strPayload);
        LOGGER.info("COURT_CENTRE_ID = {}", courtCentreId);
        LOGGER.info("COURT_CENTRE_NAME = {}", courtCentreName);
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
}

