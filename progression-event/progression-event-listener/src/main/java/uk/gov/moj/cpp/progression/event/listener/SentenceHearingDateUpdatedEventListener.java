package uk.gov.moj.cpp.progression.event.listener;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateUpdated;
import uk.gov.moj.cpp.progression.event.service.CaseService;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class SentenceHearingDateUpdatedEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.sentence-hearing-date-updated")
    public void processEvent(final JsonEnvelope event) {
        SentenceHearingDateUpdated sentenceHearingDateUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(),
                SentenceHearingDateUpdated.class);
        caseService.updateSentenceHearingDate(sentenceHearingDateUpdated );
    }
}
