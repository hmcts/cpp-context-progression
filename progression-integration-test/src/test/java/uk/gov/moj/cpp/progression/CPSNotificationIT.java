package uk.gov.moj.cpp.progression;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatus;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.NotificationServiceStub.verifyEmailNotificationIsRaisedWithoutAttachment;
import static uk.gov.moj.cpp.progression.stub.UsersAndGroupsStub.stubGetOrganisationDetails;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;

import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.io.Resources;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CPSNotificationIT extends AbstractIT {
    private static final String PUBLIC_DEFENCE_RECORD_INSTRUCTED = "public.defence.event.record-instruction-details";
    private static final String PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE = "public.defence.event.record-instruction-details.json";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED_FILE = "public.listing.hearing-confirmed-cps-notification.json";
    private static final Logger LOGGER = LoggerFactory.getLogger(CPSNotificationIT.class.getCanonicalName());


    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
    private static final String ORGANISATION_ID = "f8254db1-1683-483e-afb3-b87fde5a0a26";
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
        IdMapperStub.setUp();
        HearingStub.stubInitiateHearing();
        DocumentGeneratorStub.stubDocumentCreate(STRING.next());
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();
        courtCentreName = "Lavender Hill Magistrate's Court";
        stubGetOrganisationDetails(ORGANISATION_ID, ORGANISATION_NAME);
    }

    @Test
    public void shouldNotifyCPS() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), getInstructedJsonObject(PUBLIC_LISTING_HEARING_CONFIRMED_FILE,
                caseId, hearingId, defendantId, courtCentreId, courtCentreName));
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatus(hearingId, "HEARING_INITIALISED");

        // Instruct
        final JsonObject recordInstructedPublicEvent =
                getInstructedJsonObject(PUBLIC_DEFENCE_RECORD_INSTRUCTED_FILE, caseId, hearingId, defendantId, courtCentreId, courtCentreName);
        Thread.sleep(1000 * 5);
        final JsonEnvelope publicEventInstructedEnvelope = envelopeFrom(buildMetadata(PUBLIC_DEFENCE_RECORD_INSTRUCTED, userId), recordInstructedPublicEvent);
        messageProducerClientPublic.sendMessage(PUBLIC_DEFENCE_RECORD_INSTRUCTED, publicEventInstructedEnvelope);

        // notify by email
        verifyEmailNotificationIsRaisedWithoutAttachment(newArrayList(ORGANISATION_NAME));
    }

    private JsonObject getInstructedJsonObject(final String path, final String caseId, final String hearingId,
                                               final String defendantId, final String courtCentreId, final String courtCentreName) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
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

