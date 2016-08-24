package uk.gov.moj.cpp.progression.command.handler.matchers;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.AncillaryOrdersCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.MedicalDocumentationCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProsecutionCommand;
import uk.gov.moj.cpp.progression.command.defendant.StatementOfMeansCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.AncillaryOrdersEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.MedicalDocumentationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.PreSentenceReportEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.StatementOfMeansEvent;

public class DefendantEventMatcher extends TypeSafeDiagnosingMatcher<DefendantAdditionalInformationAdded> {

    private final DefendantCommand defendantCommand;

    public DefendantEventMatcher(DefendantCommand defendantCommand) {
        this.defendantCommand = defendantCommand;
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("is same as").appendValue(defendantCommand);
    }

    @Override
    protected boolean matchesSafely(final DefendantAdditionalInformationAdded defendantEvent,
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
            MedicalDocumentationEvent medicalDocumentationEvent =
                            defenceEvent.getMedicalDocumentationEvent();
            if (medicalDocumentationEvent != null) {
                MedicalDocumentationCommand medicalDocumentationCommand =
                                defenceCommand.getMedicalDocumentation();
                if (!medicalDocumentationEvent.getDetails()
                                .equals(medicalDocumentationCommand.getDetails())) {
                    mismatchDescription.appendText(" was ")
                                    .appendValue(medicalDocumentationEvent.getDetails());
                    return false;
                }
            }

            StatementOfMeansEvent statementOfMeansEvent = defenceEvent.getStatementOfMeansEvent();
            if (statementOfMeansEvent != null) {
                StatementOfMeansCommand statementOfMeansCommand =
                                defenceCommand.getStatementOfMeans();
                if (!statementOfMeansEvent.getDetails()
                                .equals(statementOfMeansCommand.getDetails())) {
                    mismatchDescription.appendText(" was ")
                                    .appendValue(statementOfMeansEvent.getDetails());
                    return false;
                }
            }


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
            PreSentenceReportEvent preSentenceReportEvent =
                            probationEvent.getPreSentenceReportEvent();
            if (preSentenceReportEvent != null) {
                PreSentenceReportCommand preSentenceReportCommand =
                                probationCommand.getPreSentenceReport();
                if (preSentenceReportEvent.getDrugAssessment() != preSentenceReportCommand
                                .getDrugAssessment()) {
                    mismatchDescription.appendText(" was ")
                                    .appendValue(probationEvent.getDangerousnessAssessment());
                    return false;
                }

                if (!preSentenceReportEvent.getProvideGuidance()
                                .equals(preSentenceReportCommand.getProvideGuidance())) {
                    mismatchDescription.appendText(" was ")
                                    .appendValue(preSentenceReportEvent.getProvideGuidance());
                    return false;
                }
            }


        }

        ProsecutionEvent prosecutionEvent = additionalInformationEvent.getProsecutionEvent();
        if (prosecutionEvent != null) {
            ProsecutionCommand prosecutionCommand = additionalInformationCommand.getProsecution();
            AncillaryOrdersEvent ancillaryOrdersEvent = prosecutionEvent.getAncillaryOrdersEvent();
            if (ancillaryOrdersEvent != null) {
                AncillaryOrdersCommand ancillaryOrdersCommand =
                                prosecutionCommand.getAncillaryOrders();
                if (!ancillaryOrdersEvent.getDetails()
                                .equals(ancillaryOrdersCommand.getDetails())) {
                    mismatchDescription.appendText(" was ")
                                    .appendValue(ancillaryOrdersEvent.getDetails());
                    return false;
                }
            }


        }

        return true;
    }
}
