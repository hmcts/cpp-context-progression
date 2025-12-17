package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonReader;

import uk.gov.justice.core.courts.CaseCpsProsecutorUpdated;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.Prosecutor;
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
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_LISTENER)
public class UpdateProsecutionCaseCpsProsecutorEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateProsecutionCaseCpsProsecutorEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private SearchProsecutionCase searchCase;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;


    @Handles("progression.event.case-cps-prosecutor-updated")
    public void handleUpdateCaseCpsProsecutor(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.case-cps-prosecutor-updated {} ", event.toObfuscatedDebugString());
        }

        final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseCpsProsecutorUpdated.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseCpsProsecutorUpdated.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = updateProsecutionCase(persistentProsecutionCase, caseCpsProsecutorUpdated);
        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(updatedProsecutionCase.getId());
        caseDefendantHearingEntities.stream().filter(entity ->
                Objects.nonNull(entity.getHearing()) && entity.getHearing().getListingStatus() != HearingListingStatus.HEARING_RESULTED)
                .forEach(caseDefendantHearingEntity -> {
                    final HearingEntity hearingEntity = caseDefendantHearingEntity.getHearing();
                    final JsonObject hearingJson = jsonFromString(hearingEntity.getPayload());
                    final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
                    if (Objects.nonNull(hearing.getProsecutionCases())) {
                        final Hearing updatedHearing = Hearing.hearing().withValuesFrom(hearing)
                                .withProsecutionCases(hearing.getProsecutionCases().stream()
                                        .map(prosecutionCase -> prosecutionCase.getId().equals(caseCpsProsecutorUpdated.getProsecutionCaseId()) ? updateProsecutionCase(prosecutionCase, caseCpsProsecutorUpdated) : prosecutionCase)
                                        .collect(Collectors.toList())).build();
                        hearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
                        hearingRepository.save(hearingEntity);
                    }

                });
        searchCase.updateSearchable(updatedProsecutionCase);
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private ProsecutionCase updateProsecutionCase(final ProsecutionCase prosecutionCase, final CaseCpsProsecutorUpdated caseCpsProsecutorUpdated) {
        Prosecutor prosecutor = null;
        if (isNull(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError()) || !caseCpsProsecutorUpdated.getIsCpsOrgVerifyError()) {
            prosecutor = Prosecutor.prosecutor()
                    .withAddress(caseCpsProsecutorUpdated.getAddress())
                    .withProsecutorCode(caseCpsProsecutorUpdated.getProsecutionAuthorityCode())
                    .withProsecutorId(caseCpsProsecutorUpdated.getProsecutionAuthorityId())
                    .withProsecutorName(caseCpsProsecutorUpdated.getProsecutionAuthorityName())
                    .withIsCps(true)
                    .build();
        }

        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withProsecutor(prosecutor)
                .withIsCpsOrgVerifyError(caseCpsProsecutorUpdated.getIsCpsOrgVerifyError())
                .build();
    }


    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            pCaseEntity.setGroupId(prosecutionCase.getGroupId());
        }
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }
}
