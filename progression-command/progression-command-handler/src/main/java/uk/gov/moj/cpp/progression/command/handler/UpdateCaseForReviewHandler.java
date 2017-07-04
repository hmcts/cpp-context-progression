package uk.gov.moj.cpp.progression.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCaseForReviewHandler extends CaseProgressionCommandHandler {

    @Handles("progression.command.case-assigned-for-review")
    public void updateCaseAssignedForReview(final JsonEnvelope command) throws EventStreamException {
        applyToCaseProgressionAggregate(command,
                aCase -> aCase.caseAssignedForReview(command));
    }


}
