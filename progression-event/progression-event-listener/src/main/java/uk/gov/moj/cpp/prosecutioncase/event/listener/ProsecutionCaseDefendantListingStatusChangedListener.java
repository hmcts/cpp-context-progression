package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantListingStatusChangedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.prosecutionCase-defendant-listing-status-changed")
    public void process(final JsonEnvelope event) {
        final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChanged.class);
        final HearingEntity hearingEntity = transformHearing(prosecutionCaseDefendantListingStatusChanged.getHearing(), prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus());
        if (isNotEmpty(prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases())) {
            prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().forEach(pc ->
                    pc.getDefendants().forEach(d ->
                            caseDefendantHearingRepository.save(transformCaseDefendantHearingEntity(d, pc, hearingEntity))
                    )
            );
        }

        updateHearingForMatchedDefendants(prosecutionCaseDefendantListingStatusChanged);
    }

    private void updateHearingForMatchedDefendants(final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged) {
        final UUID hearingId = prosecutionCaseDefendantListingStatusChanged.getHearing().getId();
        final List<ProsecutionCase> prosecutionCases = prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases();
        if (Objects.nonNull(prosecutionCases) && prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus() == HearingListingStatus.HEARING_INITIALISED) {
            prosecutionCases.forEach(
                    prosecutionCase -> prosecutionCase.getDefendants().forEach(
                            defendant -> {
                                final List<MatchDefendantCaseHearingEntity> entities = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(prosecutionCase.getId(), defendant.getId());
                                entities.forEach(entity -> {
                                    entity.setHearingId(hearingId);
                                    entity.setHearing(hearingRepository.findBy(hearingId));
                                    matchDefendantCaseHearingRepository.save(entity);
                                });
                            }
                    )
            );
        }
    }

    private CaseDefendantHearingEntity transformCaseDefendantHearingEntity(final Defendant defendant, final ProsecutionCase prosecutionCase, final HearingEntity hearingEntity) {
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCase.getId(), defendant.getId(), hearingEntity.getHearingId()));
        caseDefendantHearingEntity.setHearing(hearingEntity);
        return caseDefendantHearingEntity;
    }

    private HearingEntity transformHearing(final Hearing hearing, final HearingListingStatus hearingListingStatus) {
        HearingEntity hearingEntity = hearingRepository.findBy(hearing.getId());

        // no updates should be allowed to hearing payload once its been resulted unless results are shared again
        if (hearingEntity != null && hearingEntity.getListingStatus() == HearingListingStatus.HEARING_RESULTED) {
            return hearingEntity;
        }

        if (hearingEntity == null) {
            hearingEntity = new HearingEntity();
            hearingEntity.setHearingId(hearing.getId());
        }
        hearingEntity.setListingStatus(hearingListingStatus);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        return hearingEntity;
    }
}