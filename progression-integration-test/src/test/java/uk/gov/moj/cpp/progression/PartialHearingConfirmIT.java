package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionAndReturnHearingId;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;
import static uk.gov.moj.cpp.progression.util.ReferProsecutionCaseToCrownCourtHelper.getProsecutionCaseMatchers;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

public class PartialHearingConfirmIT  extends AbstractIT{

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_PARTIAL_HEARING_CONFIRMED = "public.listing.partial-hearing-confirmed.json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";
    private static final MessageProducer messageProducerClientPublic = publicEvents.createProducer();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    @AfterClass
    public static void tearDown() throws JMSException {
        messageProducerClientPublic.close();
    }

    @Before
    public void setUp() {
        HearingStub.stubInitiateHearing();
        ListingStub.stubListCourtHearing();
        IdMapperStub.setUp();
    }

    @Test
    public void shouldPartialHearingConfirm() throws IOException {

        final List<String> caseIds = new ArrayList<>();
        final List<String> defendantIds = new ArrayList<>();

        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String userId = randomUUID().toString();
        caseIds.add(caseId1);
        defendantIds.add(defendantId1);

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId1, defendantId1, defendantId2);
        final String hearingId = pollProsecutionCasesProgressionAndReturnHearingId(caseId1, defendantId1,getProsecutionCaseMatchers(caseId1, defendantId1));

        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withName(PUBLIC_LISTING_HEARING_CONFIRMED)
                .withUserId(userId)
                .build();

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_PARTIAL_HEARING_CONFIRMED)
                        .replaceAll("CASE_ID_1", caseId1)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID_1", defendantId1));

        sendMessage(messageProducerClientPublic,
                PUBLIC_LISTING_HEARING_CONFIRMED, hearingConfirmedJson, metadata);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", is(defendantId1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants", hasSize(1))

        );

        ListingStub.verifyPostListCourtHearing(caseId1,defendantId2);
    }

}
