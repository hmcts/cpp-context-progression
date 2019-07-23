package uk.gov.moj.cpp.prosecutioncase.event.listener;

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
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantListingStatusChangedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CaseDefendantHearingRepository repository;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.prosecutionCase-defendant-listing-status-changed")
    public void process(final JsonEnvelope event) {
        final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChanged.class);
        final HearingEntity hearingEntity = transformHearing(prosecutionCaseDefendantListingStatusChanged.getHearing(), prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus());
        if (prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases() != null && !prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().isEmpty()) {
            prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().forEach(pc ->
                    pc.getDefendants().forEach(d ->
                            repository.save(transformCaseDefendantHearingEntity(d, pc, hearingEntity))

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
        if (hearingEntity == null) {
            hearingEntity = new HearingEntity();
            hearingEntity.setHearingId(hearing.getId());
        }
        hearingEntity.setListingStatus(hearingListingStatus);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        return hearingEntity;
    }
}
