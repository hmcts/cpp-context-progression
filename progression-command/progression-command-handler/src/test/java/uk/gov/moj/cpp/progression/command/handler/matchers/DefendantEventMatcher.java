package uk.gov.moj.cpp.progression.command.handler.matchers;

import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.AncillaryOrdersCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.MedicalDocumentationCommand;
import uk.gov.moj.cpp.progression.command.defendant.OthersCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProsecutionCommand;
import uk.gov.moj.cpp.progression.command.defendant.StatementOfMeansCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.AncillaryOrdersEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.MedicalDocumentationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.OtherEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.PreSentenceReportEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.StatementOfMeansEvent;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

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
    protected boolean matchesSafely(final DefendantEvent defendantEvent, final Description mismatchDescription) {
        if (defendantEvent.getDefendantId() != defendantCommand.getDefendantId()) {
            mismatchDescription.appendText(" was ").appendValue(defendantEvent.getDefendantId());
            return false;
        }

        if (defendantEvent.getDefendantProgressionId() != defendantCommand.getDefendantProgressionId()) {
            mismatchDescription.appendText(" was ").appendValue(defendantEvent.getDefendantProgressionId());
            return false;
        }

        AdditionalInformationEvent additionalInformationEvent = defendantEvent.getAdditionalInformationEvent();
        AdditionalInformationCommand additionalInformationCommand = defendantCommand.getAdditionalInformationCommand();

        DefenceEvent defenceEvent = additionalInformationEvent.getDefenceEvent();

        if (defenceEvent != null) {
            DefenceCommand defenceCommand = additionalInformationCommand.getDefenceEvent();
            MedicalDocumentationEvent medicalDocumentationEvent = defenceEvent.getMedicalDocumentationEvent();
            if (medicalDocumentationEvent != null) {
                MedicalDocumentationCommand medicalDocumentationCommand = defenceCommand.getMedicalDocumentationCommand();
                if (!medicalDocumentationEvent.getDetails().equals(medicalDocumentationCommand.getDetails())) {
                    mismatchDescription.appendText(" was ").appendValue(medicalDocumentationEvent.getDetails());
                    return false;
                }
            }

            StatementOfMeansEvent statementOfMeansEvent = defenceEvent.getStatementOfMeansEvent();
            if (statementOfMeansEvent != null) {
                StatementOfMeansCommand statementOfMeansCommand = defenceCommand.getStatementOfMeansCommand();
                if (!statementOfMeansEvent.getDetails().equals(statementOfMeansCommand.getDetails())) {
                    mismatchDescription.appendText(" was ").appendValue(statementOfMeansEvent.getDetails());
                    return false;
                }
            }

            OtherEvent othersEvent = defenceEvent.getOthersEvent();
            if (othersEvent != null) {
                OthersCommand othersCommand = defenceCommand.getOthers();
                if (!othersEvent.getDetails().equals(othersCommand.getDetails())) {
                    mismatchDescription.appendText(" was ").appendValue(othersEvent.getDetails());
                    return false;
                }
            }
        }

        ProbationEvent probationEvent = additionalInformationEvent.getProbationEvent();
        if (probationEvent != null) {
            ProbationCommand probationCommand = additionalInformationCommand.getProbationCommand();
            if (probationEvent.getDangerousnessAssessment() != probationCommand.getDangerousnessAssessment()) {
                mismatchDescription.appendText(" was ").appendValue(probationEvent.getDangerousnessAssessment());
                return false;
            }
            PreSentenceReportEvent preSentenceReportEvent = probationEvent.getPreSentenceReportEvent();
            if (preSentenceReportEvent != null) {
                PreSentenceReportCommand preSentenceReportCommand = probationCommand.getPreSentenceReportCommand();
                if (preSentenceReportEvent.getDrugAssessment() != preSentenceReportCommand.getDrugAssessment()) {
                    mismatchDescription.appendText(" was ").appendValue(probationEvent.getDangerousnessAssessment());
                    return false;
                }

                if (!preSentenceReportEvent.getProvideGuidance().equals(preSentenceReportCommand.getProvideGuidance())) {
                    mismatchDescription.appendText(" was ").appendValue(preSentenceReportEvent.getProvideGuidance());
                    return false;
                }
            }


        }

        ProsecutionEvent prosecutionEvent = additionalInformationEvent.getProsecutionEvent();
        if (prosecutionEvent != null) {
            ProsecutionCommand prosecutionCommand = additionalInformationCommand.getProsecutionCommand();
            AncillaryOrdersEvent ancillaryOrdersEvent = prosecutionEvent.getAncillaryOrdersEvent();
            if (ancillaryOrdersEvent != null) {
                AncillaryOrdersCommand ancillaryOrdersCommand = prosecutionCommand.getAncillaryOrdersCommand();
                if (!ancillaryOrdersEvent.getDetails().equals(ancillaryOrdersCommand.getDetails())) {
                    mismatchDescription.appendText(" was ").appendValue(ancillaryOrdersEvent.getDetails());
                    return false;
                }
            }

            OtherEvent othersEvent = prosecutionEvent.getOthersEvent();
            if (othersEvent != null) {
                OthersCommand othersCommand = prosecutionCommand.getOthersCommand();
                if (!othersEvent.getDetails().equals(othersCommand.getDetails())) {
                    mismatchDescription.appendText(" was ").appendValue(othersEvent.getDetails());
                    return false;
                }
            }
        }

        return true;
    }
}
