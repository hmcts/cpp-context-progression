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
import javax.persistence.NoResultException;

import java.io.StringReader;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.event.util.DuplicateOffencesHelper.filterDuplicateOffencesByIdForCases;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictionsForCases;

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
        final List<ProsecutionCase> prosecutionCasesToAdd = dedupAllReportingRestrictionsForCases(hearingListingNeeds.getProsecutionCases());
        filterDuplicateOffencesByIdForCases(prosecutionCasesToAdd);

        if (isNotEmpty(hearingListingNeeds.getProsecutionCases())) {
            // unAllocated Hearing
            final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingListingNeeds.getId());

            final JsonObject dbHearingJsonObject = jsonFromString(dbHearingEntity.getPayload());

            final Hearing dbHearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);
            dbHearing.getProsecutionCases().addAll(prosecutionCasesToAdd);
            final JsonObject updatedJsonObject = objectToJsonObjectConverter.convert(dbHearing);
            dbHearingEntity.setPayload(updatedJsonObject.toString());
            // save in updated hearing in hearing table
            hearingRepository.save(dbHearingEntity);
            LOGGER.info("Hearing : {} has been updated with new cases ", dbHearingEntity.getHearingId());

            removeUnallocatedHearing(hearingExtended, prosecutionCasesToAdd);

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

    private void removeUnallocatedHearing(HearingExtended hearingExtended, List<ProsecutionCase> prosecutionCasesToAdd) {
        if (FALSE.equals(hearingExtended.getIsAdjourned()) && FALSE.equals(hearingExtended.getIsPartiallyAllocated())) {
            prosecutionCasesToAdd.forEach(prosecutionCase -> prosecutionCase.getDefendants().forEach(defendant -> {
                LOGGER.info("Remove entries from link table 'case_defendant_hearing' table for hearing id :{}, case id :{}, defendant id :{}",
                        hearingExtended.getIsPartiallyAllocated(), prosecutionCase.getId(), defendant.getId());
                try {
                    final CaseDefendantHearingEntity entity = caseDefendantHearingRepository.findByHearingIdAndCaseIdAndDefendantId(hearingExtended.getExtendedHearingFrom(), prosecutionCase.getId(), defendant.getId());
                    caseDefendantHearingRepository.remove(entity);
                } catch (NoResultException ex) {
                    //Handling NoResultException to prevent errors during EventReplay (dublicate call)
                    LOGGER.error(String.format("CaseDefendantHearingEntity not found CaseId: %s DefendantId: %s HearingId: %s",
                            hearingExtended.getExtendedHearingFrom(), prosecutionCase.getId(), defendant.getId()), ex);
                }
            }));
        }
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}

