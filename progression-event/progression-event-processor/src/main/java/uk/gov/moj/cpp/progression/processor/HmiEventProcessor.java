package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;


import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingMovedToUnallocated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(EVENT_PROCESSOR)
public class HmiEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(HmiEventProcessor.class);
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String PROGRESSION_COMMAND_DECREASE_LISTING_NUMBER_FOR_PROSECUTION_CASE = "progression.command.decrease-listing-number-for-prosecution-case";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("progression.event.hearing-moved-to-unallocated")
    public void handleHearingMovedToUnallocated(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final HearingMovedToUnallocated hearingMovedToUnallocated = jsonObjectToObjectConverter.convert(payload, HearingMovedToUnallocated.class);

        if (Objects.isNull(hearingMovedToUnallocated.getHearing().getProsecutionCases())) {
            return;
        }

        final List<UUID> offenceIds = hearingMovedToUnallocated.getHearing().getProsecutionCases().stream()
                .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .map(Offence::getId)
                .collect(Collectors.toList());

        hearingMovedToUnallocated.getHearing().getProsecutionCases().stream().map(ProsecutionCase::getId).forEach(prosecutionCaseId ->
                sendCommandDecreaseListingNumberForProsecutionCase(event, prosecutionCaseId, offenceIds)
        );

    }

    private void sendCommandDecreaseListingNumberForProsecutionCase(final JsonEnvelope jsonEnvelope, final UUID prosecutionCaseId, final List<UUID> offenceIds) {
        final JsonArrayBuilder offenceIdsBuilder = createArrayBuilder();
        offenceIds.forEach(id -> offenceIdsBuilder.add(id.toString()));

        final JsonObjectBuilder decreaseListingNumberCommandBuilder = createObjectBuilder()
                .add(PROSECUTION_CASE_ID, prosecutionCaseId.toString())
                .add("offenceIds", offenceIdsBuilder.build());

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PROGRESSION_COMMAND_DECREASE_LISTING_NUMBER_FOR_PROSECUTION_CASE),
                decreaseListingNumberCommandBuilder.build()));
    }

}
