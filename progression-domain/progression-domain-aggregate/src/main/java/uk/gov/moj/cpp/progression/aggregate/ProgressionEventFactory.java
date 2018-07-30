package uk.gov.moj.cpp.progression.aggregate;


import static uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent.AdditionalInformationEventBuilder.anAdditionalInformationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.AncillaryOrdersEvent.AncillaryOrdersEventBuilder.anAncillaryOrdersEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent.DefenceEventBuilder.aDefenceEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded.DefendantEventBuilder.aDefendantEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.MedicalDocumentationEvent.MedicalDocumentationBuilder.aMedicalDocumentationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.PreSentenceReportEvent.PreSentenceReportEventBuilder.aPreSentenceReportEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent.ProbationEventBuilder.aProbationEvent;
import static uk.gov.moj.cpp.progression.domain.event.defendant.StatementOfMeansEvent.StatementOfMeansEventBuilder.aStatementOfMeansEvent;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseToBeAssignedUpdated;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Address;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.CrownCourtHearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.IndicatedPlea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Plea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.AdditionalInformationEvent.AdditionalInformationEventBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefenceEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProbationEvent;
import uk.gov.moj.cpp.progression.domain.event.defendant.ProsecutionEvent;

public class ProgressionEventFactory {
    private static final String FIELD_HEARING = "hearing";
    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_FROM_COURT_CENTRE = "fromCourtCentre";
    private static final String FIELD_SENDING_COMMITTAL_DATE = "sendingCommittalDate";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_PSR_IS_REQUESTED = "psrIsRequested";
    private static final String FIELD_DEFENDANTS = "defendants";
    static Function<DefendantCommand, DefendantAdditionalInformationAdded> defendantToDefendantAdded =
            defendant -> {
                final DefendantAdditionalInformationAdded.DefendantEventBuilder defendantEventBuilder =
                        aDefendantEvent()
                                .setDefendantId(defendant.getDefendantId())
                                .setCaseId(
                                        defendant.getCaseId())
                                .setSentenceHearingReviewDecision(true)
                                .setSentenceHearingReviewDecisionDateTime(ZonedDateTime.now());
                buildAdditionalInformationEvent(defendant, defendantEventBuilder);
                return defendantEventBuilder.build();
            };

    private ProgressionEventFactory(){

    }


    public static CaseAddedToCrownCourt createCaseAddedToCrownCourt(final JsonEnvelope envelope) {
        final UUID caseId =
                UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String courtCentreId =
                envelope.payloadAsJsonObject().getString(FIELD_COURT_CENTRE_ID);
        return new CaseAddedToCrownCourt(caseId, courtCentreId);
    }

    public static SendingSheetCompleted completedSendingSheet(final JsonEnvelope envelope) {
        final Hearing hearing = createHearingObj(envelope);
        final CrownCourtHearing crownCourtHearing = createCrownCourtHearingObj(envelope);
        final SendingSheetCompleted sendingSheetCompleted = new SendingSheetCompleted();
        sendingSheetCompleted.setHearing(hearing);
        sendingSheetCompleted.setCrownCourtHearing(crownCourtHearing);
        return sendingSheetCompleted;
    }

    private static Hearing createHearingObj(final JsonEnvelope envelope) {
        final JsonObject hearingJsonObject = envelope.payloadAsJsonObject().getJsonObject(FIELD_HEARING);
        final String courtCentreName = hearingJsonObject.getString("courtCentreName");
        final String courtCentreId = hearingJsonObject.getString(FIELD_COURT_CENTRE_ID);
        final String type = hearingJsonObject.getString("type");
        final String sendingCommittalDate = hearingJsonObject.getString(FIELD_SENDING_COMMITTAL_DATE);
        final UUID caseId = UUID.fromString(hearingJsonObject.getString(FIELD_CASE_ID));
        final String caseUrn = hearingJsonObject.getString("caseUrn");
        final JsonArray defendantsJsonObjects = hearingJsonObject.getJsonArray(FIELD_DEFENDANTS);
        final List<uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant> defendants = defendantsJsonObjects.stream()
                .map(tempDefendantJsonObj -> createDefendantObj((JsonObject) tempDefendantJsonObj))
                .collect(Collectors.toList());
        final Hearing hearing = new Hearing();
        hearing.setCourtCentreName(courtCentreName);
        hearing.setCourtCentreId(courtCentreId);
        hearing.setType(type);
        hearing.setSendingCommittalDate(sendingCommittalDate);
        hearing.setCaseId(caseId);
        hearing.setCaseUrn(caseUrn);
        hearing.setDefendants(defendants);
        return hearing;
    }

    private static uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant createDefendantObj(final JsonObject tempDefendantJsonObj) {
        final UUID id = UUID.fromString(tempDefendantJsonObj.getString("id"));
        final UUID personId = UUID.fromString(tempDefendantJsonObj.getString("personId"));
        final String firstName = tempDefendantJsonObj.getString("firstName");
        final String lastName = tempDefendantJsonObj.getString("lastName");
        final String nationality = tempDefendantJsonObj.getString("nationality");
        final String gender = tempDefendantJsonObj.getString("gender");
        final Address address = createAddressObj(tempDefendantJsonObj);
        final String dateOfBirth = tempDefendantJsonObj.getString("dateOfBirth");
        final String bailStatus = tempDefendantJsonObj.getString("bailStatus", null);
        final String custodyTimeLimitDate = tempDefendantJsonObj.getString("custodyTimeLimitDate", null);
        final String defenceOrganisation = tempDefendantJsonObj.getString("defenceOrganisation", null);
        final Interpreter interpreter = createInterpreterObj(tempDefendantJsonObj);

        final JsonArray offenceJsonObjects = tempDefendantJsonObj.getJsonArray("offences");
        final List<Offence> offenceList = offenceJsonObjects
                .stream()
                .map(tempOffenceJsonObj -> createOffenceObj((JsonObject) tempOffenceJsonObj))
                .collect(Collectors.toList());

        final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant defendant =
                new uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant();

        defendant.setId(id);
        defendant.setPersonId(personId);
        defendant.setFirstName(firstName);
        defendant.setLastName(lastName);
        defendant.setNationality(nationality);
        defendant.setGender(gender);
        defendant.setAddress(address);
        defendant.setDateOfBirth(dateOfBirth);
        defendant.setBailStatus(bailStatus);
        defendant.setCustodyTimeLimitDate(custodyTimeLimitDate);
        defendant.setDefenceOrganisation(defenceOrganisation);
        defendant.setInterpreter(interpreter);
        defendant.setOffences(offenceList);


        return defendant;
    }

    private static Interpreter createInterpreterObj(final JsonObject tempDefendantJsonObj) {
        final JsonObject interpreterJsonObj = tempDefendantJsonObj.getJsonObject("interpreter");
        final boolean needed = interpreterJsonObj.getBoolean("needed");
        final String language = interpreterJsonObj.getString("language", null);
        final Interpreter interpreter = new Interpreter();
        interpreter.setNeeded(needed);
        interpreter.setLanguage(language);
        return interpreter;
    }

    private static Address createAddressObj(final JsonObject tempDefendantJsonObj) {
        final JsonObject addressJsonObj = tempDefendantJsonObj.getJsonObject("address");
        final String address1 = addressJsonObj.getString("address1");
        final String address2 = addressJsonObj.getString("address2", null);
        final String address3 = addressJsonObj.getString("address3", null);
        final String address4 = addressJsonObj.getString("address4", null);
        final String postcode = addressJsonObj.getString("postCode", null);
        final Address address = new Address();
        address.setAddress1(address1);
        address.setAddress2(address2);
        address.setAddress3(address3);
        address.setAddress4(address4);
        address.setPostcode(postcode);
        return address;
    }

    private static Offence createOffenceObj(final JsonObject tempOffenceJsonObj) {
        final UUID id = UUID.fromString(tempOffenceJsonObj.getString("id"));
        final String offenceCode = tempOffenceJsonObj.getString("offenceCode");
        final Plea plea =  createPleaObj(tempOffenceJsonObj.getJsonObject("plea"));
        final IndicatedPlea indicatedPlea = createIndicatedPleaObj(tempOffenceJsonObj.getJsonObject("indicatedPlea"));
        final String section = tempOffenceJsonObj.getString("section");
        final String wording = tempOffenceJsonObj.getString("wording");
        final String reason = tempOffenceJsonObj.getString("reason", null);
        final String description = tempOffenceJsonObj.getString("description", null);
        final String category = tempOffenceJsonObj.getString("category", null);
        final String startDate = tempOffenceJsonObj.getString("startDate");
        final String endDate = tempOffenceJsonObj.getString("endDate", null);
        final Offence offence = new Offence();
        offence.setCategory(category);
        offence.setDescription(description);
        offence.setStartDate(startDate);
        offence.setEndDate(endDate);
        offence.setIndicatedPlea(indicatedPlea);
        offence.setId(id);
        offence.setPlea(plea);
        offence.setOffenceCode(offenceCode);
        offence.setReason(reason);
        offence.setSection(section);
        offence.setWording(wording);
        offence.setConvictionDate(tempOffenceJsonObj.getString("convictionDate", null) == null ? null : LocalDate.parse(tempOffenceJsonObj.getString("convictionDate")));
        return offence;
    }

    private static IndicatedPlea createIndicatedPleaObj(final JsonObject indicatedPleaJsonObj) {

        IndicatedPlea indicatedPlea = null;
        if (indicatedPleaJsonObj != null) {
            indicatedPlea = new IndicatedPlea(UUID.fromString(indicatedPleaJsonObj.getString("id")),
                    indicatedPleaJsonObj.getString("value"), indicatedPleaJsonObj.getString("allocationDecision", null));
        }
        return indicatedPlea;
    }

    private static Plea createPleaObj(final JsonObject pleaJsonObj) {
        Plea plea = null;
        if (pleaJsonObj != null) {
            plea = new Plea(UUID.fromString(pleaJsonObj.getString("id")), pleaJsonObj.getString("value"), LocalDate.parse(pleaJsonObj.getString("pleaDate")));
        }
        return plea;
    }
    private static CrownCourtHearing createCrownCourtHearingObj(final JsonEnvelope envelope) {
        final JsonObject crownCourtHearingJsonObject =
                        envelope.payloadAsJsonObject().getJsonObject("crownCourtHearing");
        final String ccHearingDate = crownCourtHearingJsonObject.getString("ccHearingDate");
        final String courtCentreNameForListringReq = crownCourtHearingJsonObject.getString("courtCentreName");
        final UUID courtCentreIdForListingReq =
                UUID.fromString(crownCourtHearingJsonObject.getString(FIELD_COURT_CENTRE_ID));
        final CrownCourtHearing crownCourtHearing = new CrownCourtHearing();
        crownCourtHearing.setCcHearingDate(ccHearingDate);
        crownCourtHearing.setCourtCentreName(courtCentreNameForListringReq);
        crownCourtHearing.setCourtCentreId(courtCentreIdForListingReq);
        return crownCourtHearing;
    }

    public static SendingCommittalHearingInformationAdded createSendingCommittalHearingInformationAdded(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String fromCourtCentre =
                envelope.payloadAsJsonObject().getString(FIELD_FROM_COURT_CENTRE);
        final LocalDate sendingCommittalDate = LocalDate.parse(
                envelope.payloadAsJsonObject().getString(FIELD_SENDING_COMMITTAL_DATE));
        return new SendingCommittalHearingInformationAdded(caseId, fromCourtCentre,
                sendingCommittalDate);
    }

    public static CaseToBeAssignedUpdated createCaseToBeAssignedUpdated(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        return new CaseToBeAssignedUpdated(caseId, CaseStatusEnum.READY_FOR_REVIEW);
    }


    public static CaseReadyForSentenceHearing createCaseReadyForSentenceHearing(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(
                envelope.payloadAsJsonObject().getJsonObject(FIELD_HEARING).getString(FIELD_CASE_ID));
        return new CaseReadyForSentenceHearing(caseId,
                CaseStatusEnum.READY_FOR_SENTENCING_HEARING, ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static PreSentenceReportForDefendantsRequested createPsrForDefendantsRequested(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final JsonArray defendants = envelope.payloadAsJsonObject().getJsonArray(FIELD_DEFENDANTS);
        final List<DefendantPSR> defendantPsrsRequested =
                defendants.stream()
                        .map(obj -> new DefendantPSR(UUID.fromString(
                                ((JsonObject) obj).getString(FIELD_DEFENDANT_ID)),
                                ((JsonObject) obj).getBoolean(FIELD_PSR_IS_REQUESTED)))
                        .collect(Collectors.toList());
        return new PreSentenceReportForDefendantsRequested(caseId, defendantPsrsRequested);
    }

    private static void buildAdditionalInformationEvent(final DefendantCommand defendant,
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

    private static void buildProsecutionEvent(final AdditionalInformationCommand additionalInformation,
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

    private static void buildDefenceEvent(final AdditionalInformationCommand additionalInformation,
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

    private static void buildProbationEvent(final AdditionalInformationCommand additionalInformation,
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

    public static NewCaseDocumentReceivedEvent newCaseDocumentReceivedEvent(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID cppCaseId = UUID.fromString(payload.getString("cppCaseId"));
        final String fileId = payload.getString("fileId");
        final String fileMimeType = payload.getString("fileMimeType");
        final String fileName = payload.getString("fileName");
        return new NewCaseDocumentReceivedEvent(cppCaseId, fileId, fileMimeType, fileName);
    }

    public static Object addDefendantEvent(final DefendantCommand defendant) {
        return defendantToDefendantAdded.apply(defendant);
    }
}
