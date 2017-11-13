package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.NoMoreInformationRequiredEvent;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.repository.DefendantRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class NoMoreInformationRequiredEventListener {

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;


    @Inject
    DefendantRepository repository;

    @Transactional
    @Handles("progression.events.no-more-information-required")
    public void noMoreInformationRequiredEventListener(final JsonEnvelope event) {

        final NoMoreInformationRequiredEvent noMoreInformationRequiredEvent = jsonObjectConverter
                .convert(event.payloadAsJsonObject(), NoMoreInformationRequiredEvent.class);
        final Defendant defendant = repository.findByDefendantId(noMoreInformationRequiredEvent.getDefendantId());
        defendant.setIsNoMoreInformationRequired(Boolean.TRUE);
        defendant.setSentenceHearingReviewDecision(true);
        defendant.setSentenceHearingReviewDecisionDateTime(
                noMoreInformationRequiredEvent.getSentenceHearingReviewDecisionDateTime());
        repository.save(defendant);
    }
}
