package uk.gov.moj.cpp.progression.command.handler;


import static uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent.AdditionalInformationEventBuilder.anAdditionalInformationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent.DefenceEventBuilder.aDefenceEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent.DefendantEventBuilder.aDefendantEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.MedicalDocumentationEvent.MedicalDocumentationBuilder.aMedicalDocumentationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.OtherEvent.OthersEventBuilder.anOthersEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.PreSentenceReportEvent.PreSentenceReportEventBuilder.aPreSentenceReportEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent.ProbationEventBuilder.aProbationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.StatementOfMeansEvent.StatementOfMeansEventBuilder.aStatementOfMeansEvent;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProsecutionCommand;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsIdentified;
import uk.gov.moj.cpp.progression.domain.event.AllStatementsServed;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseSentToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.DefenceIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.DefenceTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.IndicateEvidenceServed;
import uk.gov.moj.cpp.progression.domain.event.PTPHearingVacated;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportOrdered;
import uk.gov.moj.cpp.progression.domain.event.ProsecutionTrialEstimateAdded;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.SfrIssuesAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.AncillaryOrdersEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.StatementOfMeansEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Function;

public class ProgressionEventFactory {
    public static final String FIELD_CASE_PROGRESSION_ID = "caseProgressionId";
    public static final String FIELD_CASE_ID = "caseId";
    public static final String FIELD_DEFENCE_ISSUES = "defenceIssues";
    public static final String FIELD_SFR_ISSUES = "sfrIssues";
    public static final String FIELD_COURT_CENTER_ID_ = "courtCentreId";
    public static final String FIELD_FROM_COURT_CENTRE = "fromCourtCentre";
    public static final String FIELD_SENDING_COMMITTAL_DATE = "sendingCommittalDate";
    public static final String FIELD_DEFENCE_TRIAL_ESTIMATE = "defenceTrialEstimate";
    public static final String FIELD_PROSECUTION_TRIAL_ESTIMATE = "prosecutionTrialEstimate";
    public static final String FIELD_IS_PSR_ORDERED = "isPSROrdered";
    public static final String FIELD_INDICATE_STATEMENT_ID = "indicateStatementId";
    public static final String FIELD_IS_KEY_EVIDENCE = "isKeyEvidence";
    public static final String FIELD_EVIDENCE_NAME = "evidenceName";
    public static final String FIELD_PLAN_DATE = "planDate";
    public static final String FIELD_SENTENCE_HEARING_DATE = "sentenceHearingDate";
    public static final String FIELD_DEFENDANT_PROGRESSION_ID = "defendantProgressionId";

    public Object createCaseSentToCrownCourt(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final UUID caseId =
                UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        return new CaseSentToCrownCourt(caseProgressionId, caseId, LocalDate.now());
    }

    public Object createCaseAddedToCrownCourt(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final UUID caseId =
                UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String courtCentreId =
                envelope.payloadAsJsonObject().getString(FIELD_COURT_CENTER_ID_);
        return new CaseAddedToCrownCourt(caseProgressionId, caseId, courtCentreId);
    }

    public Object createDefenceIssuesAdded(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final String defenceIssues = envelope.payloadAsJsonObject().getString(FIELD_DEFENCE_ISSUES);
        return new DefenceIssuesAdded(caseProgressionId, defenceIssues);

    }

    public Object createSfrIssuesAdded(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final String sfrIssues = envelope.payloadAsJsonObject().getString(FIELD_SFR_ISSUES);
        return new SfrIssuesAdded(caseProgressionId, sfrIssues);

    }

    public Object createSendingCommittalHearingInformationAdded(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final String fromCourtCentre =
                envelope.payloadAsJsonObject().getString(FIELD_FROM_COURT_CENTRE);
        final LocalDate sendingCommittalDate = LocalDate.parse(
                envelope.payloadAsJsonObject().getString(FIELD_SENDING_COMMITTAL_DATE));
        return new SendingCommittalHearingInformationAdded(caseProgressionId, fromCourtCentre,
                sendingCommittalDate);
    }

    public Object createDefenceTrialEstimateAdded(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final int defenceTrialEstimate =
                envelope.payloadAsJsonObject().getInt(FIELD_DEFENCE_TRIAL_ESTIMATE);
        return new DefenceTrialEstimateAdded(caseProgressionId, defenceTrialEstimate);
    }

    public Object createProsecutionTrialEstimateAdded(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final int prosecutionTrialEstimate =
                envelope.payloadAsJsonObject().getInt(FIELD_PROSECUTION_TRIAL_ESTIMATE);
        return new ProsecutionTrialEstimateAdded(caseProgressionId, prosecutionTrialEstimate);
    }

    public Object createDirectionIssued(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new DirectionIssued(caseProgressionId, LocalDate.now());
    }

    public Object createPreSentenceReportOrdered(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final Boolean isPSROrdered =
                envelope.payloadAsJsonObject().getBoolean(FIELD_IS_PSR_ORDERED);
        return new PreSentenceReportOrdered(caseProgressionId, isPSROrdered);
    }

    public Object createIndicateEvidenceServed(final JsonEnvelope envelope) {
        final UUID indicateStatementId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_INDICATE_STATEMENT_ID));
        final UUID caseId =
                UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String evidenceName = envelope.payloadAsJsonObject().getString(FIELD_EVIDENCE_NAME);
        final Boolean isKeyEvidence =
                envelope.payloadAsJsonObject().getBoolean(FIELD_IS_KEY_EVIDENCE);
        final LocalDate planDate =
                LocalDate.parse(envelope.payloadAsJsonObject().getString(FIELD_PLAN_DATE));
        return new IndicateEvidenceServed(indicateStatementId, caseId, planDate, evidenceName,
                isKeyEvidence);

    }

    public Object createAllStatementsIdentified(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new AllStatementsIdentified(caseProgressionId);
    }

    public Object createAllStatementsServed(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new AllStatementsServed(caseProgressionId);
    }

    public Object createPTPHearingVacated(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new PTPHearingVacated(caseProgressionId, LocalDate.now());
    }

    public Object createSentenceHearingDateAdded(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final LocalDate hearingDate = LocalDate.parse(
                envelope.payloadAsJsonObject().getString(FIELD_SENTENCE_HEARING_DATE));
        return new SentenceHearingDateAdded(caseProgressionId, hearingDate);
    }

    public Object createCaseToBeAssignedUpdated(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new CaseToBeAssignedUpdated(caseProgressionId, CaseStatusEnum.READY_FOR_REVIEW);
    }

    public Object createCaseAssignedForReviewUpdated(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new CaseAssignedForReviewUpdated(caseProgressionId,
                CaseStatusEnum.ASSIGNED_FOR_REVIEW);
    }

    public Object createCaseReadyForSentenceHearing(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new CaseReadyForSentenceHearing(caseProgressionId,
                CaseStatusEnum.READY_FOR_SENTENCING_HEARING, LocalDateTime.now());
    }

    Function<DefendantCommand, DefendantEvent> defendantToDefendantAdded = new Function<DefendantCommand, DefendantEvent>() {
        public DefendantEvent apply(DefendantCommand defendant) {
            DefendantEvent.DefendantEventBuilder defendantEventBuilder = aDefendantEvent()
                    .defendantProgressionId(defendant.getDefendantProgressionId())
                    .defendantId(defendant.getDefendantId());
            buildAdditionalInformationEvent(defendant, defendantEventBuilder);
            return defendantEventBuilder.build();
        }
    };

    private void buildAdditionalInformationEvent(DefendantCommand defendant, DefendantEvent.DefendantEventBuilder defendantEventBuilder) {
        AdditionalInformationCommand additionalInformation = defendant.getAdditionalInformationCommand();

        if (additionalInformation != null) {

            AdditionalInformationEvent.AdditionalInformationEventBuilder additionalInformationEventBuilder = anAdditionalInformationEvent();
            buildProbationEvent(additionalInformation, additionalInformationEventBuilder);
            buildDefenceEvent(additionalInformation, additionalInformationEventBuilder);
            buildProsecutionEvent(additionalInformation, additionalInformationEventBuilder);

            AdditionalInformationEvent additionalInformationEvent = additionalInformationEventBuilder.build();
            defendantEventBuilder.additionalInformation(additionalInformationEvent);
        }
    }

    private void buildProsecutionEvent(AdditionalInformationCommand additionalInformation, AdditionalInformationEvent.AdditionalInformationEventBuilder additionalInformationEventBuilder) {
        ProsecutionCommand prosecution = additionalInformation.getProsecutionCommand();
        if (prosecution != null) {
            additionalInformationEventBuilder.prosecution(ProsecutionEvent.ProsecutionEventBuilder.aProsecutionEvent()
                    .ancillaryOrders(AncillaryOrdersEvent.AncillaryOrdersEventBuilder.anAncillaryOrdersEvent()
                            .details(prosecution.getAncillaryOrdersCommand() == null ? null : prosecution.getAncillaryOrdersCommand().getDetails())
                            .build())
                    .others(anOthersEvent()
                            .details(prosecution.getOthersCommand() == null ? null : prosecution.getOthersCommand().getDetails())
                            .build())
                    .build());

        }
    }

    private void buildDefenceEvent(AdditionalInformationCommand additionalInformation, AdditionalInformationEvent.AdditionalInformationEventBuilder additionalInformationEventBuilder) {
        DefenceCommand defence = additionalInformation.getDefenceEvent();
        if (defence != null) {
            StatementOfMeansEvent statementOfMeansEvent = aStatementOfMeansEvent()
                    .details(defence.getStatementOfMeansCommand() == null ? null : defence.getStatementOfMeansCommand().getDetails())
                    .build();
            DefenceEvent defenceEvent = aDefenceEvent()
                    .statementOfMeans(statementOfMeansEvent)
                    .medicalDocumentation(aMedicalDocumentationEvent()
                            .details(defence.getMedicalDocumentationCommand() == null ? null : defence.getMedicalDocumentationCommand().getDetails())
                            .build())
                    .others(anOthersEvent()
                            .details(defence.getOthers() == null ? null : defence.getOthers().getDetails())
                            .build())
                    .build();
            additionalInformationEventBuilder.defence(defenceEvent);
        }
    }

    private void buildProbationEvent(AdditionalInformationCommand additionalInformation, AdditionalInformationEvent.AdditionalInformationEventBuilder additionalInformationEventBuilder) {
        ProbationCommand probation = additionalInformation.getProbationCommand();

        if (probation != null) {
            ProbationEvent.ProbationEventBuilder probationEventBuilder = aProbationEvent();
            PreSentenceReportCommand preSentenceReportCommand = probation.getPreSentenceReportCommand();

            if (preSentenceReportCommand != null) {
                probationEventBuilder
                        .preSentenceReport(aPreSentenceReportEvent()
                                .drugAssessment(preSentenceReportCommand.getDrugAssessment())
                                .provideGuidance(preSentenceReportCommand.getProvideGuidance())
                                .build());
            }

            probationEventBuilder.dangerousnessAssessment(probation.getDangerousnessAssessment());
            additionalInformationEventBuilder.probation(probationEventBuilder.build());
        }
    }

    public Object addDefendantEvent(final DefendantCommand defendant) {
        return defendantToDefendantAdded.apply(defendant);
    }
}
