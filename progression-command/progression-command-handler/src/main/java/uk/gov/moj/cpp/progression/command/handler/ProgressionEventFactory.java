package uk.gov.moj.cpp.progression.command.handler;


import static uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent.AdditionalInformationEventBuilder.anAdditionalInformationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.AncillaryOrdersEvent.AncillaryOrdersEventBuilder.anAncillaryOrdersEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent.DefenceEventBuilder.aDefenceEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded.DefendantEventBuilder.aDefendantEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.MedicalDocumentationEvent.MedicalDocumentationBuilder.aMedicalDocumentationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.PreSentenceReportEvent.PreSentenceReportEventBuilder.aPreSentenceReportEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent.ProbationEventBuilder.aProbationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.StatementOfMeansEvent.StatementOfMeansEventBuilder.aStatementOfMeansEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.defendant.AdditionalInformationCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefenceCommand;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.defendant.PreSentenceReportCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProbationCommand;
import uk.gov.moj.cpp.progression.command.defendant.ProsecutionCommand;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.Defendant;
import uk.gov.moj.cpp.progression.domain.event.DirectionIssued;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;

import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.SentenceHearingDateAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent.AdditionalInformationEventBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsUpdated;

public class ProgressionEventFactory {
    public static final String FIELD_CASE_PROGRESSION_ID = "caseProgressionId";
    public static final String FIELD_CASE_ID = "caseId";
    public static final String FIELD_COURT_CENTER_ID_ = "courtCentreId";
    public static final String FIELD_FROM_COURT_CENTRE = "fromCourtCentre";
    public static final String FIELD_SENDING_COMMITTAL_DATE = "sendingCommittalDate";
    public static final String FIELD_IS_PSR_ORDERED = "isPSROrdered";
    public static final String FIELD_SENTENCE_HEARING_DATE = "sentenceHearingDate";
    public static final String FIELD_DEFENDANT_PROGRESSION_ID = "defendantProgressionId";
    public static final String FIELD_DEFENDANT_ID = "defendantId";
    public static final String FIELD_PSR_IS_REQUESTED = "psrIsRequested";

    public Object createCaseAddedToCrownCourt(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                        envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final UUID caseId =
                        UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String courtCentreId =
                        envelope.payloadAsJsonObject().getString(FIELD_COURT_CENTER_ID_);
        final JsonArray defendants = envelope.payloadAsJsonObject().getJsonArray("defendants");
        final List<Defendant> defendantIds = defendants.stream()
                        .map(s -> new Defendant(UUID.fromString(((JsonObject) s).getString("id"))))
                        .collect(Collectors.toList());
        return new CaseAddedToCrownCourt(caseProgressionId, caseId, courtCentreId, defendantIds);
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

    public Object createDirectionIssued(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                        envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        return new DirectionIssued(caseProgressionId, LocalDate.now());
    }

    public Object createSentenceHearingDateAdded(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                        envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final UUID caseId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final LocalDate hearingDate = LocalDate.parse(
                        envelope.payloadAsJsonObject().getString(FIELD_SENTENCE_HEARING_DATE));
        return new SentenceHearingDateAdded(caseProgressionId, hearingDate,caseId );
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

    public Object createPsrForDefendantsUpdated(final JsonEnvelope envelope) {
        final UUID caseProgressionId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_PROGRESSION_ID));
        final JsonArray defendants = envelope.payloadAsJsonObject().getJsonArray("defendants");
        final List<DefendantPSR> defendantPsrsRequested =
                defendants.stream()
                .map(obj -> new DefendantPSR(UUID.fromString(
                                         ((JsonObject) obj).getString(FIELD_DEFENDANT_ID)),
                                         ((JsonObject) obj).getBoolean(FIELD_PSR_IS_REQUESTED)))
                .collect(Collectors.toList());
        return new PreSentenceReportForDefendantsUpdated(caseProgressionId, defendantPsrsRequested);
    }

    Function<DefendantCommand, DefendantAdditionalInformationAdded> defendantToDefendantAdded =
                    defendant -> {
                        final DefendantAdditionalInformationAdded.DefendantEventBuilder defendantEventBuilder =
                                        aDefendantEvent()
                                                        .setDefendantProgressionId(
                                                                        defendant.getDefendantProgressionId())
                                                        .setDefendantId(defendant.getDefendantId())
                                                        .setCaseProgressionId(
                                                                        defendant.getCaseProgressionId())
                                                        .setSentenceHearingReviewDecision(true)
                                                        .setSentenceHearingReviewDecisionDateTime(
                                                                        LocalDateTime.now());
                        buildAdditionalInformationEvent(defendant, defendantEventBuilder);
                        return defendantEventBuilder.build();
                    };

    private void buildAdditionalInformationEvent(final DefendantCommand defendant,
                    final DefendantAdditionalInformationAdded.DefendantEventBuilder defendantEventBuilder) {
        final AdditionalInformationCommand additionalInformation =
                        defendant.getAdditionalInformation();

        if (additionalInformation != null) {

            final AdditionalInformationEventBuilder additionalInformationEventBuilder =
                            anAdditionalInformationEvent();
            buildProbationEvent(additionalInformation, additionalInformationEventBuilder);
            buildDefenceEvent(additionalInformation, additionalInformationEventBuilder);
            buildProsecutionEvent(additionalInformation, additionalInformationEventBuilder);

            final AdditionalInformationEvent additionalInformationEvent =
                            additionalInformationEventBuilder.build();
            defendantEventBuilder.setAdditionalInformation(additionalInformationEvent);
        }
    }

    private void buildProsecutionEvent(final AdditionalInformationCommand additionalInformation,
                    final AdditionalInformationEventBuilder additionalInformationEventBuilder) {
        final ProsecutionCommand prosecution = additionalInformation.getProsecution();
        if (prosecution != null) {
            final ProsecutionEvent.ProsecutionEventBuilder prosecutionEventBuilder =
                            ProsecutionEvent.ProsecutionEventBuilder.aProsecutionEvent();

            if (prosecution.getAncillaryOrders() != null) {
                prosecutionEventBuilder.setAncillaryOrders(anAncillaryOrdersEvent()
                                .setIsAncillaryOrders(prosecution.getAncillaryOrders()
                                                .getIsAncillaryOrders())
                                .setDetails(prosecution.getAncillaryOrders().getDetails()).build());
            }

            if (prosecution.getOtherDetails() != null) {
                prosecutionEventBuilder.setOtherDetails(prosecution.getOtherDetails()).build();
            }

            additionalInformationEventBuilder.prosecution(prosecutionEventBuilder.build());
        }
    }

    private void buildDefenceEvent(final AdditionalInformationCommand additionalInformation,
                    final AdditionalInformationEventBuilder additionalInformationEventBuilder) {

        final DefenceCommand defence = additionalInformation.getDefence();
        if (defence != null) {
            final DefenceEvent.DefenceEventBuilder defenceEventBuilder = aDefenceEvent();

            if (defence.getStatementOfMeans() != null) {
                defenceEventBuilder.statementOfMeans(aStatementOfMeansEvent()
                                .setIsStatementOfMeans(defence.getStatementOfMeans()
                                                .getIsStatementOfMeans())
                                .setDetails(defence.getStatementOfMeans().getDetails()).build());
            }

            if (defence.getMedicalDocumentation() != null) {
                defenceEventBuilder.medicalDocumentation(aMedicalDocumentationEvent()
                                .setIsMedicalDocumentation(defence.getMedicalDocumentation()
                                                .getIsMedicalDocumentation())
                                .setDetails(defence.getMedicalDocumentation().getDetails())
                                .build());
            }

            if (defence.getOtherDetails() != null) {
                defenceEventBuilder.setOtherDetails(defence.getOtherDetails()).build();
            }

            additionalInformationEventBuilder.defence(defenceEventBuilder.build());
        }
    }

    private void buildProbationEvent(final AdditionalInformationCommand additionalInformation,
                    final AdditionalInformationEventBuilder additionalInformationEventBuilder) {
        final ProbationCommand probation = additionalInformation.getProbation();

        if (probation != null) {
            final ProbationEvent.ProbationEventBuilder probationEventBuilder = aProbationEvent();
            final PreSentenceReportCommand preSentenceReportCommand =
                            probation.getPreSentenceReport();

            if (preSentenceReportCommand != null) {
                probationEventBuilder.setPreSentenceReport(aPreSentenceReportEvent()
                                .setPsrIsRequested(preSentenceReportCommand.getPsrIsRequested())
                                .setDrugAssessment(preSentenceReportCommand.getDrugAssessment())
                                .setProvideGuidance(preSentenceReportCommand.getProvideGuidance())
                                .build());
            }

            probationEventBuilder
                            .setDangerousnessAssessment(probation.getDangerousnessAssessment());
            additionalInformationEventBuilder.probation(probationEventBuilder.build());
        }
    }
    
    public Object newCaseDocumentReceivedEvent(UUID id, JsonEnvelope command) {
        JsonObject payload = command.payloadAsJsonObject();
        return new NewCaseDocumentReceivedEvent(id, payload);
    }

    public Object addDefendantEvent(final DefendantCommand defendant) {
        return defendantToDefendantAdded.apply(defendant);
    }
}
