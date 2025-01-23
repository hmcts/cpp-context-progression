package uk.gov.moj.cpp.progression;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.helper.RestHelper.pollForResponse;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.stub.DocumentGeneratorStub;
import uk.gov.moj.cpp.progression.stub.HearingStub;
import uk.gov.moj.cpp.progression.stub.IdMapperStub;
import uk.gov.moj.cpp.progression.stub.ListingStub;

import java.io.IOException;

import javax.json.JsonObject;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PartialHearingConfirmIT extends AbstractIT {

    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    private static final String PUBLIC_LISTING_PARTIAL_HEARING_CONFIRMED = "public.listing.partial-hearing-confirmed.json";
    private static final String PROGRESSION_QUERY_HEARING_JSON = "application/vnd.progression.query.hearing+json";

    private static final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @BeforeEach
    public void setUp() {
        DocumentGeneratorStub.stubDocumentCreate(STRING.next());
        HearingStub.stubInitiateHearing();
        ListingStub.stubListCourtHearing();
        IdMapperStub.setUp();
    }

    @Test
    public void shouldPartialHearingConfirm() throws IOException, JSONException {
        final String caseId1 = randomUUID().toString();
        final String defendantId1 = randomUUID().toString();
        final String defendantId2 = randomUUID().toString();
        final String userId = randomUUID().toString();

        addProsecutionCaseToCrownCourtWithOneProsecutionCaseAndTwoDefendants(caseId1, defendantId1, defendantId2);
        final String hearingId = pollCaseAndGetHearingForDefendant(caseId1, defendantId1);

        final JsonObject hearingConfirmedJson = stringToJsonObjectConverter.convert(
                getPayload(PUBLIC_LISTING_PARTIAL_HEARING_CONFIRMED)
                        .replaceAll("CASE_ID_1", caseId1)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID_1", defendantId1));

        final JsonEnvelope publicEventEnvelope = envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollForResponse("/hearingSearch/" + hearingId, PROGRESSION_QUERY_HEARING_JSON,
                withJsonPath("$.hearing.id", is(hearingId)),
                withJsonPath("$.hearing.prosecutionCases[0].id", is(caseId1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants[0].id", is(defendantId1)),
                withJsonPath("$.hearing.prosecutionCases[0].defendants", hasSize(1))

        );

        ListingStub.verifyPostListCourtHearing(caseId1, defendantId2);
    }
}
