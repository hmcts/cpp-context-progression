package uk.gov.moj.cpp.progression;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.addProsecutionCaseToCrownCourtWithDefendantAsAdult;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.generateUrn;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollCaseAndGetLatestHearingForDefendant;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollHearingWithStatusInitialised;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.pollProsecutionCasesProgressionFor;
import static uk.gov.moj.cpp.progression.helper.PreAndPostConditionHelper.verifyHearingIsEmpty;
import static uk.gov.moj.cpp.progression.helper.QueueUtil.buildMetadata;
import static uk.gov.moj.cpp.progression.stub.ProbationCaseworkerStub.verifyProbationHearingDeletedCommandInvoked;
import static uk.gov.moj.cpp.progression.util.FileUtil.getPayload;

public class ACourtHearingMarkedAsDuplicateIT extends AbstractIT {

    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String PUBLIC_LISTING_HEARING_CONFIRMED = "public.listing.hearing-confirmed";

    private final JmsMessageProducerClient messageProducerClientPublic = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();

    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Test
    public void shouldHearingAsMarkedDuplicate() throws IOException, JSONException {
        final String userId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String defendantId = randomUUID().toString();
        final String duplicateUrn = generateUrn();
        final String urn = generateUrn();
        final String courtCentreId = UUID.randomUUID().toString();

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId, duplicateUrn);
        String duplicateHearingId = pollCaseAndGetHearingForDefendant(caseId, defendantId);

        addProsecutionCaseToCrownCourtWithDefendantAsAdult(caseId, defendantId, urn);
        final String hearingId = pollCaseAndGetLatestHearingForDefendant(caseId, defendantId, 2, List.of(duplicateHearingId));

        final JsonObject hearingConfirmedJson = getHearingJsonObject("public.listing.hearing-confirmed.json", caseId, duplicateHearingId, defendantId, courtCentreId, "Lavender Hill Magistrate's Court");

        final JsonEnvelope publicEventEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_LISTING_HEARING_CONFIRMED, userId), hearingConfirmedJson);
        messageProducerClientPublic.sendMessage(PUBLIC_LISTING_HEARING_CONFIRMED, publicEventEnvelope);

        pollHearingWithStatusInitialised(duplicateHearingId);

        final JsonObject hearingMarkedAsDuplicateJson = getHearingMarkedAsDuplicateObject(caseId, duplicateHearingId, defendantId);
        final JsonEnvelope publicEventDuplicateEnvelope = JsonEnvelope.envelopeFrom(buildMetadata(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, userId), hearingMarkedAsDuplicateJson);
        messageProducerClientPublic.sendMessage(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, publicEventDuplicateEnvelope);

        verifyHearingIsEmpty(duplicateHearingId);
        pollProsecutionCasesProgressionFor(caseId,
                withJsonPath("$.hearingsAtAGlance.defendantHearings[0].hearingIds[*]", hasSize(1)),
                withJsonPath("$.hearingsAtAGlance.defendantHearings[0].hearingIds[0]", equalTo(hearingId)));

        verifyProbationHearingDeletedCommandInvoked(newArrayList(duplicateHearingId));
    }

    private JsonObject getHearingMarkedAsDuplicateObject(final String caseId, final String hearingId,
                                                         final String defendantId) {
        return new StringToJsonObjectConverter().convert(
                getPayload("public.events.hearing.marked-as-duplicate.json")
                        .replaceAll("CASE_ID", caseId)
                        .replaceAll("HEARING_ID", hearingId)
                        .replaceAll("DEFENDANT_ID", defendantId)
                        .replaceAll("OFFENCE_ID", randomUUID().toString())
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
}
