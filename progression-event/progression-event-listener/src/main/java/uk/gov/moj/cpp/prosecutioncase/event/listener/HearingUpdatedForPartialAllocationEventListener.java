package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.DefendantsToRemove;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingUpdatedForPartialAllocation;
import uk.gov.justice.core.courts.OffencesToRemove;
import uk.gov.justice.core.courts.ProsecutionCasesToRemove;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.NoResultException;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class HearingUpdatedForPartialAllocationEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingUpdatedForPartialAllocationEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Handles("progression.event.hearing-updated-for-partial-allocation")
    public void hearingUpdatedForPartialAllocation(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-updated-for-partial-allocation {} ", event.toObfuscatedDebugString());
        }

        final JsonObject payload = event.payloadAsJsonObject();
        final HearingUpdatedForPartialAllocation hearingUpdatedForPartialAllocation = jsonObjectToObjectConverter.convert(payload, HearingUpdatedForPartialAllocation.class);

        final UUID hearingId = hearingUpdatedForPartialAllocation.getHearingId();
        final Map<UUID, Map<UUID, Set<UUID>>> caseDefendantOffenceMap =
                hearingUpdatedForPartialAllocation.getProsecutionCasesToRemove().stream()
                        .collect(toMap(ProsecutionCasesToRemove::getCaseId, defendantsToRemove -> defendantsToRemove.getDefendantsToRemove().stream()
                                .collect(toMap(DefendantsToRemove::getDefendantId, offencesToRemove -> offencesToRemove.getOffencesToRemove().stream()
                                        .map(OffencesToRemove::getOffenceId).collect(toSet()),// Merge function for defendantId level (combine lists for duplicate keys)
                                        (list1,list2) -> {
                                            Set<UUID> mergedSet = new HashSet<>(list1);
                                            mergedSet.addAll(list2);
                                            return mergedSet;
                                        })),                                // Merge function for caseId level (combine maps for duplicate keys)
                                (map1, map2) -> {
                                    map1.putAll(map2);
                                    return map1;
                                }

                        ));

        final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingId);
        if(isNull(dbHearingEntity)){
            return;
        }
        final JsonObject dbHearingJsonObject = jsonFromString(dbHearingEntity.getPayload());
        final Hearing dbHearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);

        final Set<UUID> removedDefendantIdsFromHearing = new HashSet<>();
        removeProsecutionCasesFromHearing(dbHearing, caseDefendantOffenceMap, removedDefendantIdsFromHearing);
        removeDefenceCounselsFromHearing(dbHearing, removedDefendantIdsFromHearing);

        final JsonObject updatedJsonObject = objectToJsonObjectConverter.convert(dbHearing);
        dbHearingEntity.setPayload(updatedJsonObject.toString());
        // save updated hearing in hearing table
        hearingRepository.save(dbHearingEntity);
        LOGGER.info("Hearing : {} has been updated after partial allocation ", dbHearingEntity.getHearingId());

    }

    private void removeFromCaseDefendantHearingMappingTable(final UUID hearingId, final UUID caseId, final UUID defendantId) {
        CaseDefendantHearingEntity entityToRemove = null;

        try {
            entityToRemove = caseDefendantHearingRepository.findByHearingIdAndCaseIdAndDefendantId(hearingId, caseId, defendantId);
        } catch (NoResultException ex) {
            LOGGER.warn("Entity not found: ", ex);
        }
        if (nonNull(entityToRemove)) {

            caseDefendantHearingRepository.remove(entityToRemove);
            LOGGER.info("Entity with hearingId: {} / caseId: {} / defendantId: {} has been deleted", hearingId, caseId, defendantId);

        }
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private void removeProsecutionCasesFromHearing(final Hearing hearing, final Map<UUID, Map<UUID, Set<UUID>>> caseDefendantOffenceMap, final Set<UUID> removedDefendantIdsFromHearing) {
        hearing.getProsecutionCases().forEach(prosecutionCase -> {

            prosecutionCase.getDefendants().forEach(defendant -> {

                //remove the offence from defendant if it exists in the request payload
                defendant.getOffences().removeIf(offence ->
                        caseDefendantOffenceMap.containsKey(prosecutionCase.getId()) &&
                                caseDefendantOffenceMap.get(prosecutionCase.getId()).containsKey(defendant.getId()) &&
                                caseDefendantOffenceMap.get(prosecutionCase.getId()).get(defendant.getId()).contains(offence.getId())
                );

                if (defendant.getOffences().isEmpty()) {
                    removedDefendantIdsFromHearing.add(defendant.getId());
                    removeFromCaseDefendantHearingMappingTable(hearing.getId(), prosecutionCase.getId(), defendant.getId()); //remove defendant from mapping table since it will be deleted in the next step
                }
            });
            prosecutionCase.getDefendants().removeIf(defendant -> defendant.getOffences().isEmpty()); //remove defendant if all offences already removed

        });
        hearing.getProsecutionCases().removeIf(prosecutionCase -> prosecutionCase.getDefendants().isEmpty()); //remove case if all defendants are already removed
    }

    private void removeDefenceCounselsFromHearing(final Hearing hearing, final Set<UUID> removedDefendantIdsFromHearing) {
        if(nonNull(hearing.getDefenceCounsels()) && !hearing.getDefenceCounsels().isEmpty()) {
            hearing.getDefenceCounsels().forEach(defenceCounsel -> defenceCounsel.getDefendants().removeIf(removedDefendantIdsFromHearing::contains));
            hearing.getDefenceCounsels().removeIf(defenceCounsel -> defenceCounsel.getDefendants().isEmpty());
        }
    }
}
