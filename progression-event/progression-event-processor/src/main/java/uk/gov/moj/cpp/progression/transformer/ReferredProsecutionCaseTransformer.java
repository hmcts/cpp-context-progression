package uk.gov.moj.cpp.progression.transformer;


import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.CJS_OFFENCE_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.DVLA_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.ENDORSABLE_FLAG;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.LEGISLATION_WELSH;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.MODEOFTRIAL_CODE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService.WELSH_OFFENCE_TITLE;
import static uk.gov.moj.cpp.progression.service.RefDataService.ETHNICITY;
import static uk.gov.moj.cpp.progression.service.RefDataService.ETHNICITY_CODE;
import static uk.gov.moj.cpp.progression.service.RefDataService.NATIONALITY;
import static uk.gov.moj.cpp.progression.service.RefDataService.NATIONALITY_CODE;
import static uk.gov.moj.cpp.progression.service.RefDataService.SHORT_NAME;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.ReferredAssociatedPerson;
import uk.gov.justice.core.courts.ReferredDefendant;
import uk.gov.justice.core.courts.ReferredOffence;
import uk.gov.justice.core.courts.ReferredPerson;
import uk.gov.justice.core.courts.ReferredPersonDefendant;
import uk.gov.justice.core.courts.ReferredProsecutionCase;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.exception.DataValidationException;
import uk.gov.moj.cpp.progression.exception.ReferenceDataNotFoundException;
import uk.gov.moj.cpp.progression.service.ReferenceDataOffenceService;
import uk.gov.moj.cpp.progression.service.RefDataService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;

@SuppressWarnings({"squid:S3655", "squid:S2259", "squid:S1067", "squid:S1854", "squid:S1135", "squid:S1481"})
public class ReferredProsecutionCaseTransformer {


    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private ReferenceDataOffenceService referenceDataOffenceService;

    private static String fetchValueFromKey(final JsonObject jsonObject, final String key) {
        return jsonObject.getString(key, null);
    }

    private String fetchValueFromKey(final JsonObject jsonObject, final String key, final UUID offenceId) {
        if (!jsonObject.containsKey(key)) {
            throw new ReferenceDataNotFoundException(key, offenceId.toString());
        }
        return jsonObject.getString(key);
    }

    private Boolean fetchBooleanValueFromKey(final JsonObject jsonObject, final String key) {
        Boolean value = false;
        if (jsonObject.containsKey(key)) {
            value = jsonObject.getBoolean(key);
        }
        return value;
    }

    public ProsecutionCase transform(final ReferredProsecutionCase referredProsecutionCase, final HearingLanguage hearingLanguage, final JsonEnvelope
            jsonEnvelope) {


        return ProsecutionCase.prosecutionCase()
                .withProsecutionCaseIdentifier(transform(jsonEnvelope, referredProsecutionCase
                        .getProsecutionCaseIdentifier()))
                .withDefendants(referredProsecutionCase.getDefendants().stream()
                        .map(referredDefendant -> transform(referredDefendant, jsonEnvelope, referredProsecutionCase
                                .getInitiationCode(), hearingLanguage))
                        .collect(Collectors.toList()))
                .withId(referredProsecutionCase.getId())
                .withStatementOfFactsWelsh(referredProsecutionCase.getStatementOfFactsWelsh())
                .withStatementOfFacts(referredProsecutionCase.getStatementOfFacts())
                .withOriginatingOrganisation(referredProsecutionCase.getOriginatingOrganisation())
                .withInitiationCode(referredProsecutionCase.getInitiationCode())
                .withCaseStatus(CaseStatusEnum.SJP_REFERRAL.getDescription())
                .build();
    }

    private ProsecutionCaseIdentifier transform(final JsonEnvelope jsonEnvelope, final ProsecutionCaseIdentifier
            prosecutionCaseIdentifier) {

        if (!isNameInformationEmpty(prosecutionCaseIdentifier)) {
            return prosecutionCaseIdentifier;
        }

        final JsonObject prosecutorJson = referenceDataService.getProsecutor(jsonEnvelope, prosecutionCaseIdentifier
                .getProsecutionAuthorityId(), requester).orElseThrow(() ->
                new ReferenceDataNotFoundException("ProsecutionAuthorityCode", prosecutionCaseIdentifier
                        .getProsecutionAuthorityId().toString()));

        return ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                .withValuesFrom(prosecutionCaseIdentifier)
                .withProsecutionAuthorityCode(defaultString(fetchValueFromKey(prosecutorJson, SHORT_NAME)))
                .build();
    }

    private boolean isNameInformationEmpty(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
        return isBlank(prosecutionCaseIdentifier.getProsecutionAuthorityName());
    }

    public Defendant transform(final ReferredDefendant referredDefendant, final JsonEnvelope jsonEnvelope, final
    InitiationCode initiationCode, final HearingLanguage hearingLanguage) {
        String pnCid = null;
        if (nonNull(referredDefendant.getPersonDefendant())) {
            pnCid = referredDefendant.getPersonDefendant().getPncId();
        }
        return Defendant.defendant()
                .withOffences(referredDefendant
                        .getOffences().stream()
                        .map(referredOffence -> transform(referredOffence, jsonEnvelope, initiationCode))
                        .collect(Collectors.toList()))
                .withId(referredDefendant.getId())
                .withMasterDefendantId(referredDefendant.getId())
                .withCourtProceedingsInitiated(jsonEnvelope.metadata().createdAt().orElse(ZonedDateTime.now(ZoneId.of("UTC"))))
                .withPersonDefendant(transform(referredDefendant.getPersonDefendant(), hearingLanguage, jsonEnvelope))
                .withWitnessStatementWelsh(referredDefendant.getWitnessStatementWelsh())
                .withWitnessStatement(referredDefendant.getWitnessStatement())
                .withProsecutionCaseId(referredDefendant.getProsecutionCaseId())
                .withProsecutionAuthorityReference(referredDefendant.getProsecutionAuthorityReference())
                .withNumberOfPreviousConvictionsCited(referredDefendant.getNumberOfPreviousConvictionsCited())
                .withMitigationWelsh(referredDefendant.getMitigationWelsh())
                .withMitigation(referredDefendant.getMitigation())
                .withNumberOfPreviousConvictionsCited(referredDefendant.getNumberOfPreviousConvictionsCited())
                .withLegalEntityDefendant(referredDefendant.getLegalEntityDefendant())
                .withDefenceOrganisation(referredDefendant.getDefenceOrganisation())
                .withAssociatedPersons(referredDefendant.getAssociatedPersons() != null ? referredDefendant
                        .getAssociatedPersons().stream()
                        .map(referredAssociatedPerson -> transform(referredAssociatedPerson, jsonEnvelope))
                        .collect(Collectors.toList()) : null)
                .withAliases(referredDefendant.getAliases())
                .withPncId(pnCid)
                .build();
    }

    public PersonDefendant transform(final ReferredPersonDefendant personDefendant, final HearingLanguage hearingLanguage, final
    JsonEnvelope jsonEnvelope) {
        if (nonNull(personDefendant)) {
            final JsonObject selfDefinedEthnicityJson = getEthnicityJson(personDefendant.getSelfDefinedEthnicityId(),
                    jsonEnvelope);
            final JsonObject observedEthnicityJson = getEthnicityJson(personDefendant.getObservedEthnicityId(),
                    jsonEnvelope);

            final Ethnicity ethnicity = Ethnicity.ethnicity()
                    .withObservedEthnicityCode(fetchValueFromKey(observedEthnicityJson, ETHNICITY_CODE))
                    .withObservedEthnicityDescription(fetchValueFromKey(observedEthnicityJson, ETHNICITY))
                    .withObservedEthnicityId(personDefendant.getObservedEthnicityId())
                    .withSelfDefinedEthnicityCode(fetchValueFromKey(selfDefinedEthnicityJson, ETHNICITY_CODE))
                    .withSelfDefinedEthnicityDescription(fetchValueFromKey(selfDefinedEthnicityJson, ETHNICITY))
                    .withSelfDefinedEthnicityId(personDefendant.getSelfDefinedEthnicityId())
                    .build();

            return PersonDefendant.personDefendant()
                    .withPersonDetails(transform(personDefendant.getPersonDetails(), ethnicity, hearingLanguage, jsonEnvelope))
                    .withArrestSummonsNumber(personDefendant.getArrestSummonsNumber())
                    .withBailStatus(personDefendant.getBailStatus())
                    .withCustodyTimeLimit(personDefendant.getCustodyTimeLimit())
                    .withDriverNumber(personDefendant.getDriverNumber())
                    .withEmployerOrganisation(personDefendant.getEmployerOrganisation())
                    .withEmployerPayrollReference(personDefendant.getEmployerPayrollReference())
                    .withPerceivedBirthYear(personDefendant.getPerceivedBirthYear())
                    .build();
        }
        return null;
    }

    /**
     * Enrich  all expect IndicatedPlea , plea , verdict  and DateOfInformation
     *
     * @param referredOffence
     * @param jsonEnvelope
     * @return
     */

    public Offence transform(final ReferredOffence referredOffence, final JsonEnvelope jsonEnvelope, final
    InitiationCode initiationCode) {
        final JsonObject offenceJson = referenceDataOffenceService.getOffenceById(referredOffence
                .getOffenceDefinitionId(), jsonEnvelope, requester)
                .orElseThrow(() -> new ReferenceDataNotFoundException("Offence", referredOffence.getId().toString()));

        final Offence offence = Offence.offence()
                .withNotifiedPlea(referredOffence.getNotifiedPlea())
                .withId(referredOffence.getId())
                .withArrestDate(referredOffence.getArrestDate())
                .withChargeDate(referredOffence.getChargeDate())
                .withConvictionDate(referredOffence.getConvictionDate())
                .withConvictingCourt(referredOffence.getConvictingCourt())
                .withCount(nonNull(referredOffence.getCount()) ? referredOffence.getCount() : 0)
                .withEndDate(referredOffence.getEndDate())
                .withOffenceDefinitionId(referredOffence.getOffenceDefinitionId())
                .withOffenceFacts(referredOffence.getOffenceFacts())
                .withOffenceLegislation(fetchValueFromKey(offenceJson, LEGISLATION))
                .withOffenceLegislationWelsh(fetchValueFromKey(offenceJson, LEGISLATION_WELSH))
                .withOffenceTitle(fetchValueFromKey(offenceJson, (OFFENCE_TITLE), referredOffence.getId()))
                .withOffenceTitleWelsh(fetchValueFromKey(offenceJson, WELSH_OFFENCE_TITLE))
                .withModeOfTrial(fetchValueFromKey(offenceJson, MODEOFTRIAL_CODE))
                .withOffenceCode(fetchValueFromKey(offenceJson, CJS_OFFENCE_CODE, referredOffence.getId()))
                .withOrderIndex(referredOffence.getOrderIndex())
                .withStartDate(referredOffence.getStartDate())
                .withWording(referredOffence.getWording())
                .withWordingWelsh(referredOffence.getWordingWelsh())
                .withOffenceDateCode(referredOffence.getOffenceDateCode())
                .withDvlaOffenceCode(fetchValueFromKey(offenceJson, DVLA_CODE))
                .withReportingRestrictions(referredOffence.getReportingRestrictions())
                .withEndorsableFlag(fetchBooleanValueFromKey(offenceJson, ENDORSABLE_FLAG))
                .withMaxPenalty(referredOffence.getMaxPenalty())
                .withVerdict(referredOffence.getVerdict())
                .build();
        if ((initiationCode == InitiationCode.J || initiationCode == InitiationCode.Z)
                && nonNull(offence.getModeOfTrial())
                && "Indictable".equalsIgnoreCase(offence.getModeOfTrial()) || "EitherWay".equalsIgnoreCase
                (offence.getModeOfTrial())) {
            throw new DataValidationException("Incorrect initiation code: for J or Z, the mode of trial " +
                    "cannot be Indictable or EitherWay");
        }
        return offence;
    }

    public Person transform(final ReferredPerson referredPerson, final Ethnicity ethnicity, final HearingLanguage hearingLanguage, final JsonEnvelope jsonEnvelope) {
        final JsonObject nationalityJson = getNationalityJson(referredPerson.getNationalityId(), jsonEnvelope);
        final JsonObject additionalNationalityJson = getNationalityJson(referredPerson.getAdditionalNationalityId(),
                jsonEnvelope);

        return Person.person()
                .withAddress(referredPerson.getAddress())
                .withAdditionalNationalityCode(fetchValueFromKey(additionalNationalityJson, NATIONALITY_CODE))
                .withAdditionalNationalityDescription(fetchValueFromKey(additionalNationalityJson, NATIONALITY))
                .withAdditionalNationalityId(referredPerson.getAdditionalNationalityId())
                .withContact(referredPerson.getContact())
                .withDateOfBirth(referredPerson.getDateOfBirth())
                .withDisabilityStatus(referredPerson.getDisabilityStatus())
                .withDocumentationLanguageNeeds(referredPerson.getDocumentationLanguageNeeds())
                .withEthnicity(ethnicity)
                .withFirstName(referredPerson.getFirstName())
                .withGender(referredPerson.getGender())
                .withHearingLanguageNeeds(hearingLanguage)
                .withInterpreterLanguageNeeds(referredPerson.getInterpreterLanguageNeeds())
                .withLastName(referredPerson.getLastName())
                .withMiddleName(referredPerson.getMiddleName())
                .withNationalInsuranceNumber(referredPerson.getNationalInsuranceNumber())
                .withNationalityCode(fetchValueFromKey(nationalityJson, NATIONALITY_CODE))
                .withNationalityDescription(fetchValueFromKey(nationalityJson, NATIONALITY))
                .withNationalityId(referredPerson.getNationalityId())
                .withOccupation(referredPerson.getOccupation())
                .withOccupationCode(referredPerson.getOccupationCode())
                .withSpecificRequirements(referredPerson.getSpecificRequirements())
                .withTitle(referredPerson.getTitle())
                .build();
    }


    public AssociatedPerson transform(final ReferredAssociatedPerson referredAssociatedPerson, final JsonEnvelope
            jsonEnvelope) {
        return AssociatedPerson.associatedPerson()
                .withPerson(transform(referredAssociatedPerson.getPerson(), null,null, jsonEnvelope))
                .withRole(referredAssociatedPerson.getRole())
                .build();
    }

    private JsonObject getEthnicityJson(final UUID id, final JsonEnvelope jsonEnvelope) {
        if (nonNull(id)) {
            return referenceDataService
                    .getEthinicity(jsonEnvelope, id, requester)
                    .orElseThrow(() -> new ReferenceDataNotFoundException("Ethnicity", id.toString()));
        }
        return Json.createObjectBuilder().build();
    }

    private JsonObject getNationalityJson(final UUID id, final JsonEnvelope jsonEnvelope) {
        if (nonNull(id)) {
            return referenceDataService
                    .getNationality(jsonEnvelope, id, requester)
                    .orElseThrow(() -> new ReferenceDataNotFoundException("Country Nationality", id.toString()));
        }
        return Json.createObjectBuilder().build();
    }

}

