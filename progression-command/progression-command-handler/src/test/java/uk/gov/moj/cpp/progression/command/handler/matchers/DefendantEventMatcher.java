package uk.gov.moj.cpp.progression.command.handler.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProsecutionCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;

public class DefendantEventMatcher extends TypeSafeDiagnosingMatcher<DefendantEvent> {

    private final DefendantCommand defendantCommand;

    public DefendantEventMatcher(DefendantCommand defendantCommand) {
        this.defendantCommand = defendantCommand;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("is same as").appendValue(defendantCommand);
    }

    @Override
    protected boolean matchesSafely(final DefendantEvent defendantEvent,
                    final Description mismatchDescription) {
        if (defendantEvent.getDefendantId() != defendantCommand.getDefendantId()) {
            mismatchDescription.appendText(" was ").appendValue(defendantEvent.getDefendantId());
            return false;
        }

        if (defendantEvent.getDefendantProgressionId() != defendantCommand
                        .getDefendantProgressionId()) {
            mismatchDescription.appendText(" was ")
                            .appendValue(defendantEvent.getDefendantProgressionId());
            return false;
        }

        AdditionalInformationEvent additionalInformationEvent =
                        defendantEvent.getAdditionalInformationEvent();
        AdditionalInformationCommand additionalInformationCommand =
                        defendantCommand.getAdditionalInformation();

        DefenceEvent defenceEvent = additionalInformationEvent.getDefenceEvent();

        if (defenceEvent != null) {
            DefenceCommand defenceCommand = additionalInformationCommand.getDefence();

        }

        ProbationEvent probationEvent = additionalInformationEvent.getProbationEvent();
        if (probationEvent != null) {
            ProbationCommand probationCommand = additionalInformationCommand.getProbation();
            if (probationEvent.getDangerousnessAssessment() != probationCommand
                            .getDangerousnessAssessment()) {
                mismatchDescription.appendText(" was ")
                                .appendValue(probationEvent.getDangerousnessAssessment());
                return false;
            }

        }

        ProsecutionEvent prosecutionEvent = additionalInformationEvent.getProsecutionEvent();
        if (prosecutionEvent != null) {
            ProsecutionCommand prosecutionCommand = additionalInformationCommand.getProsecution();

        }

        return true;
    }
}
