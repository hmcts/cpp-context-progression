package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourt;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.stub.ReferenceDataStub.stubGetOrganisationById;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;

import java.util.Arrays;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class HearingConfirmedForCaseAtAGlanceIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();
    private static final String MAGISTRATES_JURISDICTION_TYPE = "MAGISTRATES";

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private String userId;
    private String hearingId;
    private String caseId;
    private String defendantId;
    private String courtCentreId;

    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
        stubGetOrganisationById(REST_RESOURCE_REF_DATA_GET_ORGANISATION_JSON);
    }

    @Before
    public void setUp() {
        DocumentGeneratorStub.stubDocumentCreate(STRING.next());
        stubGetOrganisationById(REST_RESOURCE_REF_DATA_GET_ORGANISATION_WITHOUT_POSTCODE_JSON);
        HearingStub.stubInitiateHearing();
        userId = randomUUID().toString();
        caseId = randomUUID().toString();
        defendantId = randomUUID().toString();
        courtCentreId = randomUUID().toString();
    }

    @SuppressWarnings("squid:S1607")
    @Ignore
    @Test
    public void shouldUpdateCaseAtAGlance() throws Exception {

        addProsecutionCaseToCrownCourt(caseId, defendantId);
        hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId, defendantId, getProsecutionCaseMatchers(caseId, defendantId, Arrays.asList(
                withJsonPath("$.hearingsAtAGlance.defendantHearings[?(@.defendantId=='"+ defendantId +"')]", notNullValue())
        )));

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, getHearingJsonObject("public.listing.hearing-confirmed.json",
                        caseId, hearingId, defendantId, courtCentreId), JsonEnvelope.metadataBuilder()
                        .withId(randomUUID())
                        .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                        .withUserId(userId)
                        .build());


        Matcher[] caseUpdatedMatchers = {
                withJsonPath("$.prosecutionCase.id", equalTo(caseId)),
                withJsonPath("$.hearingsAtAGlance.hearings.[*].courtCentre.id", hasItem(equalTo(courtCentreId))),
                withJsonPath("$.prosecutionCase.defendants[0].isYouth", equalTo(true)),
                withJsonPath("$.hearingsAtAGlance.latestHearingJurisdictionType", equalTo(MAGISTRATES_JURISDICTION_TYPE))
        };

        pollProsecutionCasesProgressionFor(caseId, caseUpdatedMatchers);
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
}

