package uk.gov.moj.cpp.progression;

import com.google.common.io.Resources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getCourtExtractPdf;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutionCaseAtAGlanceFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.getProsecutioncasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.createMockEndpoints;
import static uk.gov.moj.cpp.progression.helper.RestHelper.getJsonObject;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.assertProsecutionCase;

@SuppressWarnings("squid:S1607")
public class CourtExtractIT {

    private String caseId;
    private String defendantId;
    private String userId;
    private String hearingId;
    private String courtCentreId;

    private static final String DOCUMENT_TEXT = STRING.next();
    private static final String CROWN_COURT_EXTRACT = "CrownCourtExtract";
    private static final String CERTIFICATE_OF_CONVICTION = "CertificateOfConviction";

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Before
    public void setUp() throws IOException {
        caseId = UUID.randomUUID().toString();
        defendantId = UUID.randomUUID().toString();
        userId = randomUUID().toString();
        hearingId = randomUUID().toString();
        courtCentreId = UUID.randomUUID().toString();

        createMockEndpoints();
        DocumentGeneratorStub.stubDocumentCreate(DOCUMENT_TEXT);
        HearingStub.stubInitiateHearing();
    }

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Test
    public void shouldGetCourtExtract_whenExtractTypeIsCrownCourtExtract() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        assertProsecutionCase(getJsonObject(getProsecutioncasesProgressionFor(caseId)).getJsonObject
                ("prosecutionCase"), caseId, defendantId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        getProsecutionCaseAtAGlanceFor(caseId);

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, hearingId, CROWN_COURT_EXTRACT);
        // then
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }

    @Test
    public void shouldGetCourtExtract_whenExtractTypeIsCertificateOfConviction() throws Exception {
        // given
        addProsecutionCaseToCrownCourt(caseId, defendantId);
        assertProsecutionCase(getJsonObject(getProsecutioncasesProgressionFor(caseId)).getJsonObject
                ("prosecutionCase"), caseId, defendantId);

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());

        getProsecutionCaseAtAGlanceFor(caseId);

        // when
        final String documentContentResponse = getCourtExtractPdf(caseId, defendantId, "", CERTIFICATE_OF_CONVICTION);
        // then
        assertThat(documentContentResponse, equalTo(DOCUMENT_TEXT));
    }

    private JsonObject getHearingJsonObject(final String path, final String caseId, final String hearingId,
                                            final String defendantId, final String courtCentreId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("CASE_ID", caseId)
                .replaceAll("HEARING_ID", hearingId)
                .replaceAll("DEFENDANT_ID", defendantId)
                .replaceAll("COURT_CENTRE_ID", courtCentreId);
        System.out.println(strPayload);
        System.out.println("COURT_CENTRE_ID==" + courtCentreId);
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

