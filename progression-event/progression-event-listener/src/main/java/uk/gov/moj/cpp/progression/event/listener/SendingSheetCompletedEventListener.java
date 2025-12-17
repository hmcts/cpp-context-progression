package uk.gov.moj.cpp.progression.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.event.service.CaseService;

/**
 *
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@ServiceComponent(EVENT_LISTENER)
public class SendingSheetCompletedEventListener {

    @Inject
    private CaseService caseService;

    @Inject
    JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.events.sending-sheet-completed")
    public void processEvent(final JsonEnvelope event) {
        final SendingSheetCompleted sendingSheetCompleted = jsonObjectConverter
                .convert(event.payloadAsJsonObject(), SendingSheetCompleted.class);
        caseService.caseAssignedForReview(sendingSheetCompleted.getHearing().getCaseId(), CaseStatusEnum.READY_FOR_REVIEW);
    }

}
