package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.progression.courts.HearingMarkedAsDuplicate;
import uk.gov.justice.progression.courts.HearingMarkedAsDuplicateForCase;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingMarkedAsDuplicateEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingMarkedAsDuplicateEventListener.class);

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;


    @Handles("progression.event.hearing-marked-as-duplicate")
    public void hearingMarkedAsDuplicate(final Envelope<HearingMarkedAsDuplicate> event) {
        final UUID hearingId = event.payload().getHearingId();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-marked-as-duplicate hearingId: {} ", hearingId);
        }

        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);
        if (Objects.nonNull(hearingEntity)) {
            hearingRepository.remove(hearingEntity);
        }
    }

    @Handles("progression.event.hearing-marked-as-duplicate-for-case")
    public void hearingMarkedAsDuplicateForCase(final Envelope<HearingMarkedAsDuplicateForCase> event) {
        final UUID hearingId = event.payload().getHearingId();
        final UUID caseId = event.payload().getCaseId();
        final List<UUID> defendantIds = event.payload().getDefendantIds();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-marked-as-duplicate-for-case hearingId: {} caseId:{}", hearingId, caseId);
        }

        defendantIds.forEach(defendantId ->
                caseDefendantHearingRepository.removeByHearingIdAndCaseIdAndDefendantId(hearingId, caseId, defendantId));
    }


}
