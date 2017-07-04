package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.event.service.CaseService;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class SentenceHearingAddedEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.sentence-hearing-added")
    public void processEvent(final JsonEnvelope event) {
        SentenceHearingAdded sentenceHearingAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(),
                SentenceHearingAdded.class);
        caseService.addSentenceHearing(sentenceHearingAdded);
    }
}
