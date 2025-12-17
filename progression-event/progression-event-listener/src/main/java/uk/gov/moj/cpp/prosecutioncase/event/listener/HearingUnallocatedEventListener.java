package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.progression.courts.OffencesRemovedFromHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingUnallocatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUnallocatedEventListener.class);

    private static final String PROGRESSION_EVENTS_OFFENCES_REMOVED_FROM_HEARING = "progression.events.offences-removed-from-hearing";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Handles(PROGRESSION_EVENTS_OFFENCES_REMOVED_FROM_HEARING)
    public void handleOffencesRemovedFromHearingEvent(final Envelope<OffencesRemovedFromHearing> event) {
        final UUID hearingId = event.payload().getHearingId();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received event '{}' hearingId: {}", PROGRESSION_EVENTS_OFFENCES_REMOVED_FROM_HEARING, hearingId);
        }

        final HearingEntity hearingEntityToUpdate = hearingRepository.findBy(hearingId);

        if (nonNull(hearingEntityToUpdate)) {

            final JsonObject dbHearingJsonObject = stringToJsonObjectConverter.convert(hearingEntityToUpdate.getPayload());
            final Hearing hearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);

            final List<UUID> prosecutionCasesToRemove = event.payload().getProsecutionCaseIds();
            final List<UUID> defendantsToRemove = event.payload().getDefendantIds();
            final List<UUID> offencesToRemove = event.payload().getOffenceIds();

            removeProsecutionCases(prosecutionCasesToRemove, hearing);
            removeDefendants(defendantsToRemove, hearing, hearingId);
            removeOffences(offencesToRemove, hearing);

            final JsonObject updatedJsonObject = objectToJsonObjectConverter.convert(hearing);
            hearingEntityToUpdate.setPayload(updatedJsonObject.toString());
            hearingRepository.save(hearingEntityToUpdate);
        }
    }

    private void removeOffences(final List<UUID> offencesToRemove, final Hearing hearing) {
        hearing.getProsecutionCases()
                .forEach(prosecutionCase -> prosecutionCase.getDefendants().forEach(defendant ->
                        defendant.getOffences().removeIf(offence -> offencesToRemove.contains(offence.getId()))
                ));
    }

    private void removeDefendants(final List<UUID> defendantsToRemove, final Hearing hearing, final UUID hearingId) {
        hearing.getProsecutionCases()
                .forEach(prosecutionCase -> prosecutionCase.getDefendants()
                        .removeIf(defendant -> defendantsToRemove.contains(defendant.getId())));
        defendantsToRemove.forEach(
                defendantId -> caseDefendantHearingRepository.removeByHearingIdAndDefendantId(hearingId, defendantId)
        );
    }

    private void removeProsecutionCases(final List<UUID> prosecutionCasesToRemove, final Hearing hearing) {
        hearing.getProsecutionCases()
                .removeIf(prosecutionCase -> prosecutionCasesToRemove.contains(prosecutionCase.getId()));
    }
}
