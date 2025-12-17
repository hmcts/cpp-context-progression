package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ProsecutionCaseDefendantHearingResultUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingResultLineEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingResultLineEntityRepository;

import java.util.stream.Collectors;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantHearingResultUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingResultLineEntityRepository hearingResultLineEntityRepository;

    @Handles("progression.event.prosecutionCase-defendant-hearing-result-updated")
    public void process(final JsonEnvelope event) {
        final ProsecutionCaseDefendantHearingResultUpdated prosecutionCaseDefendantHearingResultUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantHearingResultUpdated.class);
        final HearingEntity hearingEntity = hearingRepository.findBy(prosecutionCaseDefendantHearingResultUpdated.getHearingId());
        hearingEntity.getResultLines().stream().forEach(hearingResultLineEntity -> hearingResultLineEntityRepository.remove(hearingResultLineEntity));
        hearingEntity.getResultLines().clear();

        hearingEntity.addResultLines(prosecutionCaseDefendantHearingResultUpdated.getSharedResultLines().stream()
                .map(sharedResultLine -> new HearingResultLineEntity(sharedResultLine.getId(), objectToJsonObjectConverter.convert(sharedResultLine).toString(), hearingEntity))
                .collect(Collectors.toList()));
        hearingRepository.save(hearingEntity);
    }
}
