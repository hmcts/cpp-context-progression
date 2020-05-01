package uk.gov.moj.cpp.progression.transformer;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.core.courts.BailStatus.bailStatus;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.ID;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.NATIONALITY;
import static uk.gov.moj.cpp.progression.service.ReferenceDataService.NATIONALITY_CODE;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AllocationDecision;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Plea;
import uk.gov.justice.core.courts.PleaValue;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.Source;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.constant.ProsecutingAuthority;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Defendant;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Hearing;
import uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.SendingSheetCompleted;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

import com.google.common.base.Strings;


@SuppressWarnings({"squid:S3655", "squid:S1213", "squid:S00116", "squid:CallToDeprecatedMethod", "pmd:NullAssignment"})
public class SendingSheetCompleteTransformer {

    //Constants for CPS cases
    protected static final String PROSECUTION_AUTHORITY_CODE = ProsecutingAuthority.CPS.getDescription();
    // id from ref-data for POLICE
    protected static final UUID PROSECUTION_AUTHORITY_ID = UUID.fromString("52b27284-0686-4894-b1c7-7d4b634cacdb");
    // SendingSheetComplete does not have hearingId as hearing is only created after public message is sent
    // to hearing context. So this is being set with a random hearingId. Approved by tech Arch.
    private final UUID ORIGINATING_HEARING_ID = randomUUID();
    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;
    @Inject
    private ReferenceDataOffenceService referenceDataOffenceService;
    @Inject
    private ReferenceDataService referenceDataService;

    private SendingSheetCompleteTransformer() {

    }

    private static String fetchValueFromKey(final JsonObject jsonObject, final String key) {
        return jsonObject.getString(key, null);
    }

    public ProsecutionCase transformToProsecutionCase(final SendingSheetCompleted sendingSheetCompleted, final JsonEnvelope jsonEnvelope) {
        final Hearing hearing = sendingSheetCompleted.getHearing();
        final String caseUrn = hearing.getCaseUrn();
        final List<Defendant> defendants = hearing.getDefendants();
        final UUID caseId = hearing.getCaseId();
        return ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(transformDefendants(defendants, caseId, hearing.getSendingCommittalDate(), jsonEnvelope))
                .withInitiationCode(InitiationCode.C)
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN(caseUrn)
                        .withProsecutionAuthorityCode(PROSECUTION_AUTHORITY_CODE)
                        .withProsecutionAuthorityId(PROSECUTION_AUTHORITY_ID)
                        .build())
                .withCaseStatus(CaseStatusEnum.READY_FOR_REVIEW.toString())
                .build();
    }

    private List<uk.gov.justice.core.courts.Defendant> transformDefendants(final List<Defendant> defendants, final UUID caseId,
                                                                           final String sendingCommitalDate, final JsonEnvelope jsonEnvelope) {
        return defendants.stream().map(d -> transformDefendant(d, caseId, sendingCommitalDate, jsonEnvelope)).collect(Collectors.toList());
    }

    private List<Offence> transformOffences(final List<uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence> offences, final String sendingCommitalDate, final JsonEnvelope
            jsonEnvelope) {
        return offences.stream().map(o -> transformOffence(o, sendingCommitalDate, jsonEnvelope)).collect(Collectors.toList());
    }

    private LocalDate sendingSheetAsDate(final String str) {
        return Strings.isNullOrEmpty(str)?null:LocalDate.parse(str);
    }

    private Offence transformOffence(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence offence, final String sendingCommitalDate, final JsonEnvelope
            jsonEnvelope) {
        final JsonObject offenceJson = referenceDataOffenceService.getOffenceByCjsCode(offence
                .getOffenceCode(), jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Offence", offence.getId().toString()));

        return Offence.offence()
                .withId(offence.getId())
                .withWording(offence.getWording())
                .withStartDate(sendingSheetAsDate(offence.getStartDate()))
                .withOffenceDefinitionId(UUID.fromString(fetchValueFromKey(offenceJson, "offenceId", offence.getId())))
                .withEndDate(sendingSheetAsDate(offence.getEndDate()))
                .withConvictionDate(offence.getConvictionDate())
                .withPlea(getPlea(offence))
                .withCount(0)
                .withOffenceLegislation(fetchValueFromKey(offenceJson, LEGISLATION, offence.getId()))
                .withOffenceLegislationWelsh(fetchValueFromKey(offenceJson, LEGISLATION_WELSH, offence.getId()))
                .withOffenceTitle(fetchValueFromKey(offenceJson, OFFENCE_TITLE, offence.getId()))
                .withOffenceTitleWelsh(fetchValueFromKey(offenceJson, WELSH_OFFENCE_TITLE, offence.getId()))
                .withModeOfTrial(fetchValueFromKey(offenceJson, "modeOfTrial", offence.getId()))
                .withOffenceCode(offence.getOffenceCode())
                .withOrderIndex(offence.getOrderIndex())
                .withIndicatedPlea(getIndicatedPlea(offence, sendingCommitalDate))
                .withAllocationDecision(getAllocationDecision(offence, sendingCommitalDate))
                .build();
    }

    private String fetchValueFromKey(final JsonObject jsonObject, final String key, final UUID offenceId) {
        if (!jsonObject.containsKey(key)) {
            throw new ReferenceDataNotFoundException(key, offenceId.toString());
        }
        return jsonObject.getString(key);
    }

    private IndicatedPlea getIndicatedPlea(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence offence, final String sendingCommitalDate) {
        return isNull(offence.getIndicatedPlea()) ? null : buildIndicatedPlea(offence.getIndicatedPlea(), offence.getId(), sendingCommitalDate);
    }

    private Plea getPlea(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence offence) {
        return offence.getPlea() == null ? null : Plea.plea()
                .withPleaValue(PleaValue.valueFor(offence.getPlea().getValue()).get())
                .withPleaDate(offence.getPlea().getPleaDate())
                .withOffenceId(offence.getId())
                .withOriginatingHearingId(ORIGINATING_HEARING_ID)
                .build();
    }

    private IndicatedPlea buildIndicatedPlea(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.IndicatedPlea indicatedPlea, final UUID offenceId, final String sendingCommitalDate) {
        return IndicatedPlea.indicatedPlea()
                .withOffenceId(offenceId)
                .withIndicatedPleaValue(IndicatedPleaValue.valueFor(indicatedPlea.getValue()).get())
                .withIndicatedPleaDate(sendingSheetAsDate(sendingCommitalDate))
                .withSource(Source.IN_COURT)
                .build();

    }

    private AllocationDecision getAllocationDecision(final uk.gov.moj.cpp.progression.domain.event.completedsendingsheet.Offence offence, final String sendingCommitalDate) {
        return isNull(offence.getIndicatedPlea()) ? null : buildAllocationDecision(offence.getIndicatedPlea().getAllocationDecision(), offence.getId(), sendingCommitalDate);
    }

    private AllocationDecision buildAllocationDecision(final String allocationDecision, final UUID offenceId, final String sendingCommitalDate) {
        if (allocationDecision == null || "ELECT_TRIAL".equalsIgnoreCase(allocationDecision) || "COURT_DECLINED".equalsIgnoreCase(allocationDecision)) {
            return AllocationDecision.allocationDecision()
                    .withMotReasonDescription("Defendant chooses trial by jury")
                    .withOffenceId(offenceId)
                    .withOriginatingHearingId(ORIGINATING_HEARING_ID)
                    .withAllocationDecisionDate(sendingSheetAsDate(sendingCommitalDate))
                    .withMotReasonId(fromString("f8eb278a-8bce-373e-b365-b45e939da38a"))
                    .withMotReasonCode("4")
                    .withSequenceNumber(40)
                    .build();
        } else {
            return AllocationDecision.allocationDecision()
                    .withOffenceId(offenceId)
                    .withOriginatingHearingId(ORIGINATING_HEARING_ID)
                    .withAllocationDecisionDate(sendingSheetAsDate(sendingCommitalDate))
                    .withMotReasonId(fromString("4ba29b9f-9e57-32ed-b376-1840f4ba6c53"))
                    .withMotReasonCode("2")
                    .withSequenceNumber(20)
                    .withMotReasonDescription("Indictable-only offence")
                    .build();
        }
    }

    private uk.gov.justice.core.courts.Defendant transformDefendant(final Defendant defendant, final UUID caseId, final String sendingCommitalDate, final JsonEnvelope
            jsonEnvelope) {
        return uk.gov.justice.core.courts.Defendant.defendant()
                .withDefenceOrganisation(defendant.getDefenceOrganisation() == null ? null : Organisation.organisation()
                        .withName(defendant.getDefenceOrganisation())
                        .build())
                .withId(defendant.getId())
                       .withMasterDefendantId(defendant.getId())
                .withProsecutionCaseId(caseId)
                .withOffences(transformOffences(defendant.getOffences(), sendingCommitalDate, jsonEnvelope))
                .withPersonDefendant(buildPersonDefendant(defendant, jsonEnvelope))
                .build();
    }

    private PersonDefendant buildPersonDefendant(final Defendant defendant, final JsonEnvelope jsonEnvelope) {
        return PersonDefendant.personDefendant()
                .withBailStatus(defendant.getBailStatus() == null ? null : bailStatus().withId(randomUUID()).withDescription(defendant.getBailStatus()).build())
                .withCustodyTimeLimit(sendingSheetAsDate(defendant.getCustodyTimeLimitDate()))
                .withPersonDetails(buildPerson(defendant, jsonEnvelope))
                .build();
    }

    private Person buildPerson(final Defendant defendant, final JsonEnvelope jsonEnvelope) {

        final JsonObject nationalityJson = getNationalityJson(defendant.getNationality(), jsonEnvelope);
        return Person.person()
                .withDateOfBirth(sendingSheetAsDate(defendant.getDateOfBirth()))
                .withFirstName(defendant.getFirstName())
                .withLastName(defendant.getLastName())
                .withNationalityId(UUID.fromString(fetchValueFromKey(nationalityJson, ID)))
                .withNationalityCode(fetchValueFromKey(nationalityJson, NATIONALITY_CODE))
                .withNationalityDescription(fetchValueFromKey(nationalityJson, NATIONALITY))
                .withGender(Gender.valueFor(defendant.getGender().toUpperCase()).get())
                .withAddress(
                        isNull(defendant.getAddress()) ? null :
                                Address.address()
                                        .withAddress1(defendant.getAddress().getAddress1())
                                        .withAddress2(defendant.getAddress().getAddress2())
                                        .withAddress3(defendant.getAddress().getAddress3())
                                        .withAddress4(defendant.getAddress().getAddress4())
                                        .withPostcode(defendant.getAddress().getPostcode())
                                        .build())
                .withInterpreterLanguageNeeds(getInterpreterLanguageNeeds(defendant))
                .build();

    }

    private JsonObject getNationalityJson(final String nationality, final JsonEnvelope jsonEnvelope) {
        if (nonNull(nationality)) {
            return referenceDataService
                    .getNationalityByNationality(jsonEnvelope, nationality, requester)
                    .orElseThrow(() -> new ReferenceDataNotFoundException("Country Nationality", nationality));
        }
        return Json.createObjectBuilder().build();
    }

    private String getInterpreterLanguageNeeds(final Defendant defendant) {
        return isNull(defendant.getInterpreter()) || isNull(defendant.getInterpreter().getLanguage()) ? null : defendant.getInterpreter().getLanguage();
    }
}
