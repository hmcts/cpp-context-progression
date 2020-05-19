package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtended;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class HearingExtendedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingExtendedEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Handles("progression.event.hearing-extended")
    public void hearingExtendedForCase(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-extended {} ", event.toObfuscatedDebugString());
        }

        final JsonObject payload = event.payloadAsJsonObject();
        final HearingExtended hearingExtended = jsonObjectToObjectConverter.convert(payload, HearingExtended.class);
        final HearingListingNeeds hearingListingNeeds = hearingExtended.getHearingRequest();
        final List<ProsecutionCase> prosecutionCasesToAdd = hearingListingNeeds.getProsecutionCases();

        if (isNotEmpty(hearingListingNeeds.getProsecutionCases())) {
            final UUID hearingId = hearingListingNeeds.getId();
            final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingId);

            final JsonObject dbHearingJsonObject = jsonFromString(dbHearingEntity.getPayload());

            final Hearing dbHearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);
            dbHearing.getProsecutionCases().addAll(prosecutionCasesToAdd);
            final JsonObject updatedJsonObject = objectToJsonObjectConverter.convert(dbHearing);
            dbHearingEntity.setPayload(updatedJsonObject.toString());
            // save in updated hearing in hearing table
            hearingRepository.save(dbHearingEntity);
            LOGGER.info("Hearing : {} has been updated with new cases ", dbHearingEntity.getHearingId());

            removeUnallocatedHearing(hearingExtended, prosecutionCasesToAdd, dbHearingEntity);

            // associate new cases and defendant with existing allocated hearing and save in case_defendant_hearing joint table
            prosecutionCasesToAdd.forEach(prosecutionCase -> prosecutionCase.getDefendants().forEach(defendant -> {
                final CaseDefendantHearingKey caseDefendantHearingKey = new CaseDefendantHearingKey();
                caseDefendantHearingKey.setCaseId(prosecutionCase.getId());
                caseDefendantHearingKey.setDefendantId(defendant.getId());
                caseDefendantHearingKey.setHearingId(dbHearing.getId());
                final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
                caseDefendantHearingEntity.setId(caseDefendantHearingKey);
                caseDefendantHearingEntity.setHearing(dbHearingEntity);
                caseDefendantHearingRepository.save(caseDefendantHearingEntity);
            }));
        }
    }

    private void removeUnallocatedHearing(HearingExtended hearingExtended, List<ProsecutionCase> prosecutionCasesToAdd, HearingEntity dbHearingEntity) {
        if (nonNull(hearingExtended.getIsAdjourned()) && !hearingExtended.getIsAdjourned()) {
            //remove unallocated hearing id from case_defendant_hearing table
            prosecutionCasesToAdd.forEach(prosecutionCase -> prosecutionCase.getDefendants().forEach(defendant -> {
                final List<CaseDefendantHearingEntity> entitiesToRemove = caseDefendantHearingRepository.findByCaseIdAndDefendantId(prosecutionCase.getId(), defendant.getId());
                if (isNotEmpty(entitiesToRemove)) {
                    entitiesToRemove.forEach(entity -> {
                        if(!entity.getHearing().getHearingId().equals(dbHearingEntity.getHearingId())){
                            caseDefendantHearingRepository.remove(entity);
                            LOGGER.info("Hearing : {} has been deleted ", entity.getHearing().getHearingId());
                        }
                    });
                }
            }));
        }
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}

