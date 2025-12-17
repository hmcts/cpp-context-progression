package uk.gov.moj.cpp.prosecutioncase.event.listener;


import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CaseGroupInfoUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class CaseGroupInfoUpdatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseGroupInfoUpdatedEventListener.class);

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    HearingRepository hearingRepository;

    @Handles("progression.event.case-group-info-updated")
    public void processEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event {} {} ", "progression.event.case-group-info-updated", event.toObfuscatedDebugString());
        }

        final CaseGroupInfoUpdated caseGroupInfoUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseGroupInfoUpdated.class);

        prosecutionCaseRepository.save(getProsecutionCaseEntity(caseGroupInfoUpdated.getProsecutionCase()));

        final AtomicReference<HearingEntity> hearingEntity = new AtomicReference<>();
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities =
                caseDefendantHearingRepository.findByCaseId(caseGroupInfoUpdated.getProsecutionCase().getId());
        caseDefendantHearingEntities.stream().forEach(caseDefendantHearingEntity ->
                hearingEntity.set(caseDefendantHearingEntity.getHearing())
        );

        updateHearingWithCaseGroupInfo(hearingEntity.get(), caseGroupInfoUpdated);
    }

    private void updateHearingWithCaseGroupInfo(final HearingEntity hearingEntity, CaseGroupInfoUpdated caseGroupInfoUpdated) {
        if (nonNull(hearingEntity)) {
            final UUID hearingId = hearingEntity.getHearingId();

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Updating hearing with id: {} ", hearingId);
            }

            final JsonObject dbHearingJsonObject = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectConverter.convert(dbHearingJsonObject, Hearing.class);

            final HearingEntity updatedHearingEntity = new HearingEntity();
            updatedHearingEntity.setHearingId(hearingId);
            updatedHearingEntity.setListingStatus(hearingEntity.getListingStatus());
            updatedHearingEntity.setResultLines(hearingEntity.getResultLines());

            final List<ProsecutionCase> updatedProsecutionCases = new ArrayList<>();

            final List<ProsecutionCase> prosecutionCases = hearing.getProsecutionCases();
            prosecutionCases.forEach(pCase -> {
                if (pCase.getId().equals(caseGroupInfoUpdated.getProsecutionCase().getId())) {
                    updatedProsecutionCases.add(caseGroupInfoUpdated.getProsecutionCase());
                } else {
                    updatedProsecutionCases.add(pCase);
                }
            });

            final Hearing updatedHearing = Hearing.hearing()
                    .withValuesFrom(hearing)
                    .withProsecutionCases(updatedProsecutionCases)
                    .build();
            updatedHearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());

            hearingRepository.save(updatedHearingEntity);
        }
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity caseEntity = new ProsecutionCaseEntity();
        caseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            caseEntity.setGroupId(prosecutionCase.getGroupId());
        }
        caseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return caseEntity;
    }
}
