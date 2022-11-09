package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.progression.courts.HearingDeleted;
import uk.gov.justice.progression.courts.HearingDeletedForProsecutionCase;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingDeletedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDeletedEventListener.class);

    private static final String PROGRESSION_EVENT_HEARING_DELETED = "progression.event.hearing-deleted";
    private static final String PROGRESSION_EVENT_HEARING_DELETED_FOR_PROSECUTION_CASE = "progression.event.hearing-deleted-for-prosecution-case";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Handles(PROGRESSION_EVENT_HEARING_DELETED)
    public void handleHearingDeletedEvent(final Envelope<HearingDeleted> event) {
        final UUID hearingId = event.payload().getHearingId();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received event '{}' hearingId: {}", PROGRESSION_EVENT_HEARING_DELETED, hearingId);
        }

        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);

        if (Objects.nonNull(hearingEntity)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Removing hearing with id: {} ", hearingId);
            }
            hearingRepository.remove(hearingEntity);
        }

        caseDefendantHearingRepository.removeByHearingId(hearingId);
        matchDefendantCaseHearingRepository.removeByHearingId(hearingId);
        hearingApplicationRepository.removeByHearingId(hearingId);
    }

    @Handles(PROGRESSION_EVENT_HEARING_DELETED_FOR_PROSECUTION_CASE)
    public void handleHearingDeletedForProsecutionCase(final Envelope<HearingDeletedForProsecutionCase> event) {
        final UUID hearingId = event.payload().getHearingId();
        final UUID prosecutionCaseId = event.payload().getProsecutionCaseId();
        final List<UUID> defendantIds = event.payload().getDefendantIds();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received event '{}' hearingId: {} prosecutionCaseId:{}", PROGRESSION_EVENT_HEARING_DELETED_FOR_PROSECUTION_CASE, hearingId, prosecutionCaseId);
        }

        defendantIds.stream().forEach(defendantId -> {
            caseDefendantHearingRepository.removeByHearingIdAndCaseIdAndDefendantId(hearingId, prosecutionCaseId, defendantId);
            matchDefendantCaseHearingRepository.removeByHearingIdAndCaseIdAndDefendantId(hearingId, prosecutionCaseId, defendantId);
        });
    }
}
