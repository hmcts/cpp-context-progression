package uk.gov.moj.cpp.progression.command.controller;

import javax.inject.Inject;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.COMMAND_CONTROLLER)
public class ProgressionCommandController {
    @Inject
    private Sender sender;

    @Handles("progression.command.send-to-crown-court")
    public void sendToCrownCourt(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.add-case-to-crown-court")
    public void addCaseToCrownCourt(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.add-defence-issues")
    public void addDefenceIssues(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.addsfrissues")
    public void addSfrIssues(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.sending-committal-hearing-information")
    public void sendCommittalHearingInformation(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.defence-trial-estimate")
    public void addDefenceTrialEstimate(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.prosecution-trial-estimate")
    public void addProsecutionTrialEstimate(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.issue-direction")
    public void issueDirection(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.pre-sentence-report")
    public void preSentenceReport(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.indicate-statement")
    public void indicateStatement(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.indicate-all-statements-identified")
    public void indicateAllStatementsIdentified(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.indicate-all-statements-served")
    public void indicateAllStatementsServed(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.vacate-ptp-hearing")
    public void vacatePTPHearing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.sentence-hearing-date")
    public void addSentenceHearingDate(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.case-to-be-assigned")
    public void updateCaseToBeAssigned(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("progression.command.case-assigned-for-review")
    public void updateCaseAssignedForReview(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
