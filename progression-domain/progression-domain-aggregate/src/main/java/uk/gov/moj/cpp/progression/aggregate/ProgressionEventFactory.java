package uk.gov.moj.cpp.progression.aggregate;

import uk.gov.justice.services.messaging.JsonObjects;


import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt;
import uk.gov.moj.cpp.progression.domain.event.NewCaseDocumentReceivedEvent;
import uk.gov.moj.cpp.progression.domain.event.PreSentenceReportForDefendantsRequested;
import uk.gov.moj.cpp.progression.domain.event.SendingCommittalHearingInformationAdded;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Address;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.IndicatedPlea;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Plea;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantPSR;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonObject;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
public class ProgressionEventFactory {
    private static final String FIELD_HEARING = "hearing";
    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_FROM_COURT_CENTRE = "fromCourtCentre";
    private static final String FIELD_SENDING_COMMITTAL_DATE = "sendingCommittalDate";
    private static final String FIELD_DEFENDANT_ID = "defendantId";
    private static final String FIELD_PSR_IS_REQUESTED = "psrIsRequested";
    private static final String FIELD_DEFENDANTS = "defendants";


    private ProgressionEventFactory() {

    }


    public static CaseAddedToCrownCourt createCaseAddedToCrownCourt(final JsonEnvelope envelope) {
        final UUID caseId =
                UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String courtCentreId =
                envelope.payloadAsJsonObject().getString(FIELD_COURT_CENTRE_ID);
        return new CaseAddedToCrownCourt(caseId, courtCentreId);
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
        final String postcode = addressJsonObj.getString("postcode", null);
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
        final Plea plea = createPleaObj(tempOffenceJsonObj.getJsonObject("plea"));
        final IndicatedPlea indicatedPlea = createIndicatedPleaObj(tempOffenceJsonObj.getJsonObject("indicatedPlea"));
        final String section = tempOffenceJsonObj.getString("section");
        final String wording = tempOffenceJsonObj.getString("wording");
        final String reason = tempOffenceJsonObj.getString("reason", null);
        final String description = tempOffenceJsonObj.getString("description", null);
        final String category = tempOffenceJsonObj.getString("category", null);
        final String startDate = tempOffenceJsonObj.getString("startDate");
        final String endDate = tempOffenceJsonObj.getString("endDate", null);
        final int orderIndex = tempOffenceJsonObj.getInt("orderIndex", -1);
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
        offence.setTitle(tempOffenceJsonObj.getString("title", null));
        offence.setLegislation(tempOffenceJsonObj.getString("legislation", null));
        offence.setOrderIndex(orderIndex == -1 ? null : orderIndex);
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

    public static SendingCommittalHearingInformationAdded createSendingCommittalHearingInformationAdded(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(
                envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        final String fromCourtCentre =
                envelope.payloadAsJsonObject().getString(FIELD_FROM_COURT_CENTRE);
        final LocalDate sendingCommittalDate = LocalDate.parse(
                envelope.payloadAsJsonObject().getString(FIELD_SENDING_COMMITTAL_DATE));
        final String courtCenterId = envelope.payloadAsJsonObject().getString(FIELD_COURT_CENTRE_ID, null);
        return new SendingCommittalHearingInformationAdded(caseId, fromCourtCentre,
                sendingCommittalDate, courtCenterId);
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


    public static NewCaseDocumentReceivedEvent newCaseDocumentReceivedEvent(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID cppCaseId = UUID.fromString(payload.getString("cppCaseId"));
        final String fileId = payload.getString("fileId");
        final String fileMimeType = payload.getString("fileMimeType");
        final String fileName = payload.getString("fileName");
        return new NewCaseDocumentReceivedEvent(cppCaseId, fileId, fileMimeType, fileName);
    }


}
