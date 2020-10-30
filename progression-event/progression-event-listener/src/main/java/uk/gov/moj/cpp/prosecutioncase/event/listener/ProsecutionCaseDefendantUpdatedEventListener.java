package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantUpdatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantUpdatedEventListener.class);
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Inject
    private ProsecutionCaseRepository repository;
    @Inject
    private SearchProsecutionCase searchCase;
    @Inject
    private CourtApplicationRepository courtApplicationRepository;
    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    @Handles("progression.event.prosecution-case-defendant-updated")
    public void processProsecutionCaseDefendantUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event  progression.event.prosecution-case-defendant-updated {} ", event.toObfuscatedDebugString());
        }
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantUpdated.class);
        final DefendantUpdate defendantUpdate = prosecutionCaseDefendantUpdated.getDefendant();
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendantUpdate.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final Optional<Defendant> originDefendant = prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantUpdate.getId())).findFirst();

        final List<CourtApplicationEntity> applicationEntities = courtApplicationRepository.findByLinkedCaseId(defendantUpdate.getProsecutionCaseId());

        if (originDefendant.isPresent()) {
            final Defendant updatedDefendant = updateDefendant(originDefendant.get(), defendantUpdate);
            prosecutionCase.getDefendants().remove(originDefendant.get());
            prosecutionCase.getDefendants().add(updatedDefendant);
            if (nonNull(applicationEntities) && !applicationEntities.isEmpty()) {
                updateDefendantForCourtApplication(applicationEntities, updatedDefendant);
            }
        }
        repository.save(getProsecutionCaseEntity(prosecutionCase));

        updateSearchable(prosecutionCase);
    }

    @Handles("progression.event.hearing-resulted-case-updated")
    public void processProsecutionCaseUpdated(final JsonEnvelope event) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.hearing-resulted-case-updated {} ", event.toObfuscatedDebugString());
        }

        final HearingResultedCaseUpdated hearingResultedCaseUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingResultedCaseUpdated.class);
        final List<Defendant> defendants = hearingResultedCaseUpdated.getProsecutionCase().getDefendants();

        if (CollectionUtils.isNotEmpty(defendants)) {
            final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendants.get(0).getProsecutionCaseId());
            final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());

            final ProsecutionCase prosecutionCaseInRepository = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
            for (final Defendant defendant : defendants) {
                final Optional<Defendant> defendantFromRespository = prosecutionCaseInRepository.getDefendants().stream().filter(def -> def.getId().equals(defendant.getId())).findFirst();

                if (defendantFromRespository.isPresent()) {
                    final Defendant originalDefendant = defendantFromRespository.get();
                    prosecutionCaseInRepository.getDefendants().remove(defendantFromRespository.get());
                    // GPE-12381 . This has been done explicitly  not to loose  progression flag associationLockedByRepOrder when we receive result from hearing
                    final Defendant updatedDefendant = getUpdatedDefendant(originalDefendant, defendant);
                    prosecutionCaseInRepository.getDefendants().add(updatedDefendant);
                }
            }
            final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                    .withPoliceOfficerInCase(prosecutionCaseInRepository.getPoliceOfficerInCase())
                    .withProsecutionCaseIdentifier(prosecutionCaseInRepository.getProsecutionCaseIdentifier())
                    .withId(prosecutionCaseInRepository.getId())
                    .withDefendants(prosecutionCaseInRepository.getDefendants())
                    .withInitiationCode(prosecutionCaseInRepository.getInitiationCode())
                    .withOriginatingOrganisation(prosecutionCaseInRepository.getOriginatingOrganisation())
                    .withCpsOrganisation(prosecutionCaseInRepository.getCpsOrganisation())
                    .withIsCpsOrgVerifyError(prosecutionCaseInRepository.getIsCpsOrgVerifyError())
                    .withStatementOfFacts(prosecutionCaseInRepository.getStatementOfFacts())
                    .withStatementOfFactsWelsh(prosecutionCaseInRepository.getStatementOfFactsWelsh())
                    .withCaseMarkers(prosecutionCaseInRepository.getCaseMarkers())
                    .withAppealProceedingsPending(prosecutionCaseInRepository.getAppealProceedingsPending())
                    .withBreachProceedingsPending(prosecutionCaseInRepository.getBreachProceedingsPending())
                    .withRemovalReason(prosecutionCaseInRepository.getRemovalReason())
                    .withCaseStatus(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus())
                    .build();
            repository.save(getProsecutionCaseEntity(updatedProsecutionCase));
            updateSearchable(updatedProsecutionCase);
        }
    }

    private Defendant getUpdatedDefendant(final Defendant originalDefendant, final Defendant defendant) {
        final List<Offence> offences = nonNull(originalDefendant.getOffences()) ? new ArrayList<>(originalDefendant.getOffences()): new ArrayList<>();

        if (nonNull(defendant.getOffences())) {
            final List<Offence> updatedOffences = getUpdatedOffencesWithNonNowsJudicialResults(defendant.getOffences());

            updatedOffences.forEach(updatedOffence -> {
                if (offences.removeIf(offence -> offence.getId().equals(updatedOffence.getId()))) {
                    offences.add(updatedOffence);
                }
            });
        }

        return Defendant.defendant()
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withOffences(offences)
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalAidStatus(defendant.getLegalAidStatus())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withDefendantCaseJudicialResults(getNonNowsResults(defendant.getDefendantCaseJudicialResults()))
                .withWitnessStatement(defendant.getWitnessStatement())
                .withWitnessStatementWelsh(defendant.getWitnessStatementWelsh())
                .withLegalEntityDefendant(originalDefendant.getLegalEntityDefendant())
                .withAssociatedPersons(originalDefendant.getAssociatedPersons())
                .withMitigation(originalDefendant.getMitigation())
                .withMitigationWelsh(originalDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(originalDefendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(originalDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(originalDefendant.getProsecutionCaseId())
                .withDefenceOrganisation(originalDefendant.getDefenceOrganisation())
                .withPncId(originalDefendant.getPncId())
                .withAliases(originalDefendant.getAliases())
                .withIsYouth(originalDefendant.getIsYouth())
                .withAssociatedDefenceOrganisation(originalDefendant.getAssociatedDefenceOrganisation())
                .withCroNumber(originalDefendant.getCroNumber())
                .withAssociationLockedByRepOrder(originalDefendant.getAssociationLockedByRepOrder())
                .build();
    }

    private List<Offence> getUpdatedOffencesWithNonNowsJudicialResults(final List<Offence> offences) {
        Optional.ofNullable(offences).ifPresent(
                offenceList -> offenceList.stream().filter(Objects::nonNull).forEach(offence -> {
                    final List<JudicialResult> judicialResults = getNonNowsResults(offence.getJudicialResults());
                    if (nonNull(judicialResults) && !judicialResults.isEmpty()) {
                        offence.getJudicialResults().clear();
                        offence.getJudicialResults().addAll(judicialResults);
                    }
                }));

        return offences;
    }

    private List<JudicialResult> getNonNowsResults(final List<JudicialResult> judicialResultList) {
        if (isNull(judicialResultList) || judicialResultList.isEmpty()) {
            return judicialResultList;
        }

        return judicialResultList.stream()
                .filter(Objects::nonNull)
                .filter(jr -> !Boolean.TRUE.equals(jr.getPublishedForNows()))
                .collect(Collectors.toList());
    }

    private void updateSearchable(final ProsecutionCase prosecutionCase) {
        prosecutionCase.getDefendants().forEach(caseDefendant ->
                searchCase.makeSearchable(prosecutionCase, caseDefendant));
    }

    private void updateDefendantForCourtApplication(final List<CourtApplicationEntity> applicationEntities, final Defendant updatedDefendant) {
        applicationEntities.forEach(applicationEntity -> {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
            if (persistedApplication.getApplicant() != null && persistedApplication.getApplicant().getDefendant() != null) {
                updateApplicant(persistedApplication, updatedDefendant, applicationEntity);
            }
            if (persistedApplication.getRespondents() != null) {
                updateRespondents(persistedApplication, updatedDefendant, applicationEntity);
            }
        });

    }

    private void updateApplicant(final CourtApplication persistedApplication, final Defendant updatedDefendant, final CourtApplicationEntity applicationEntity) {
        if (persistedApplication.getApplicant().getDefendant().getId().equals(updatedDefendant.getId())) {
            final CourtApplication updatedApplication = CourtApplication.courtApplication()
                    .withId(persistedApplication.getId())
                    .withType(persistedApplication.getType())
                    .withApplicant(CourtApplicationParty.courtApplicationParty()
                            .withDefendant(updatedDefendant)
                            .withId(persistedApplication.getApplicant().getId())
                            .withSynonym(persistedApplication.getApplicant().getSynonym())
                            .withRepresentationOrganisation(persistedApplication.getApplicant().getRepresentationOrganisation())
                            .withOrganisation(persistedApplication.getApplicant().getOrganisation())
                            .withOrganisationPersons(persistedApplication.getApplicant().getOrganisationPersons())
                            .withProsecutingAuthority(persistedApplication.getApplicant().getProsecutingAuthority())
                            .withPersonDetails(persistedApplication.getApplicant().getPersonDetails())
                            .build())
                    .withApplicationDecisionSoughtByDate(persistedApplication.getApplicationDecisionSoughtByDate())
                    .withApplicationOutcome(persistedApplication.getApplicationOutcome())
                    .withApplicationParticulars(persistedApplication.getApplicationParticulars())
                    .withApplicationReceivedDate(persistedApplication.getApplicationReceivedDate())
                    .withApplicationReference(persistedApplication.getApplicationReference())
                    .withApplicationStatus(persistedApplication.getApplicationStatus())
                    .withCourtApplicationPayment(persistedApplication.getCourtApplicationPayment())
                    .withJudicialResults(persistedApplication.getJudicialResults())
                    .withParentApplicationId(persistedApplication.getParentApplicationId())
                    .withLinkedCaseId(persistedApplication.getLinkedCaseId())
                    .withOutOfTimeReasons(persistedApplication.getOutOfTimeReasons())
                    .withRespondents(persistedApplication.getRespondents())
                    .withOutOfTimeReasons(persistedApplication.getOutOfTimeReasons())
                    .build();


            applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedApplication).toString());
            courtApplicationRepository.save(applicationEntity);
        }
    }

    private void updateRespondents(final CourtApplication persistedApplication, final Defendant updatedDefendant, final CourtApplicationEntity applicationEntity) {
        if (getAllDefendantsUUID(persistedApplication).contains(updatedDefendant.getId())) {
            final CourtApplication updatedApplication = CourtApplication.courtApplication()
                    .withId(persistedApplication.getId())
                    .withType(persistedApplication.getType())
                    .withApplicant(persistedApplication.getApplicant())
                    .withApplicationDecisionSoughtByDate(persistedApplication.getApplicationDecisionSoughtByDate())
                    .withApplicationOutcome(persistedApplication.getApplicationOutcome())
                    .withApplicationParticulars(persistedApplication.getApplicationParticulars())
                    .withApplicationReceivedDate(persistedApplication.getApplicationReceivedDate())
                    .withApplicationReference(persistedApplication.getApplicationReference())
                    .withApplicationStatus(persistedApplication.getApplicationStatus())
                    .withCourtApplicationPayment(persistedApplication.getCourtApplicationPayment())
                    .withJudicialResults(persistedApplication.getJudicialResults())
                    .withParentApplicationId(persistedApplication.getParentApplicationId())
                    .withLinkedCaseId(persistedApplication.getLinkedCaseId())
                    .withOutOfTimeReasons(persistedApplication.getOutOfTimeReasons())
                    .withRespondents(persistedApplication.getRespondents().stream()
                            .map(r -> r.getPartyDetails().getDefendant() != null && r.getPartyDetails().getDefendant().getId().equals(updatedDefendant.getId()) ? updateRespondent(r, updatedDefendant) : r)
                            .collect(Collectors.toList()))
                    .withOutOfTimeReasons(persistedApplication.getOutOfTimeReasons())
                    .build();


            applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedApplication).toString());
            courtApplicationRepository.save(applicationEntity);
        }
    }

    private CourtApplicationRespondent updateRespondent(final CourtApplicationRespondent respondent, final Defendant updatedDefendant) {
        return CourtApplicationRespondent.courtApplicationRespondent()
                .withApplicationResponse(respondent.getApplicationResponse())
                .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(respondent.getPartyDetails().getPersonDetails())
                        .withRepresentationOrganisation(respondent.getPartyDetails().getRepresentationOrganisation())
                        .withProsecutingAuthority(respondent.getPartyDetails().getProsecutingAuthority())
                        .withOrganisationPersons(respondent.getPartyDetails().getOrganisationPersons())
                        .withDefendant(updatedDefendant)
                        .withId(respondent.getPartyDetails().getId())
                        .withSynonym(respondent.getPartyDetails().getSynonym())
                        .withOrganisation(respondent.getPartyDetails().getOrganisation())
                        .build())
                .build();
    }

    private Defendant updateDefendant(final Defendant originDefendant, final DefendantUpdate defendant) {

        // Due to the aggregate not containing all defendant information we have to do a manual merge
        // with the original defendant to avoid losing data. See Jira GPE-14200 for details.
        // Please read full description of method signature for more details.
        final PersonDefendant updatedPersonDefendant = getUpdatedPersonDefendant(originDefendant, defendant);
        final List<AssociatedPerson> updatedAssociatedPeople =
                defendant.getAssociatedPersons() == null
                        ? originDefendant.getAssociatedPersons()
                        : getUpdatedAssociatedPeople(originDefendant.getAssociatedPersons(), defendant.getAssociatedPersons());

        return Defendant.defendant()
                .withOffences(getUpdatedOffencesWithNonNowsJudicialResults(originDefendant.getOffences()))
                .withPersonDefendant(updatedPersonDefendant)
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withAssociatedPersons(updatedAssociatedPeople)
                .withId(defendant.getId())
                .withMasterDefendantId(originDefendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(originDefendant.getCourtProceedingsInitiated())
                .withMitigation(originDefendant.getMitigation())
                .withMitigationWelsh(originDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(originDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(originDefendant.getProsecutionCaseId())
                .withWitnessStatement(originDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(originDefendant.getWitnessStatementWelsh())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withPncId(defendant.getPncId())
                .withDefendantCaseJudicialResults(originDefendant.getDefendantCaseJudicialResults())
                .withAliases(defendant.getAliases())
                .withIsYouth(defendant.getIsYouth())
                .withCroNumber(originDefendant.getCroNumber())
                .withLegalAidStatus(originDefendant.getLegalAidStatus())
                .withAssociatedDefenceOrganisation(originDefendant.getAssociatedDefenceOrganisation())
                .withProceedingsConcluded(originDefendant.getProceedingsConcluded())
                .withAssociationLockedByRepOrder(originDefendant.getAssociationLockedByRepOrder())
                .build();

    }

    /**
     * Get the updated {@link PersonDefendant}. Takes all data from the new event (the updated
     * defendant), except the occupation field. The occupation field is not visible on the UI so the
     * JSON payload for the update does not include this field, therefore if will be null and
     * override any existing value without this check.
     *
     * @param originDefendant - the original defendant from the viewstore
     * @param defendant       - the updated defendant from the new defendant updated event
     * @return the latest version of the defendant (i.e. the event version), but with the occupation
     * field not overwritten.
     */
    private PersonDefendant getUpdatedPersonDefendant(final Defendant originDefendant, final DefendantUpdate defendant) {
        final PersonDefendant updatedPersonDefendant;

        if (defendant.getPersonDefendant() != null) {
            final PersonDefendant personDefendant = defendant.getPersonDefendant();
            final Person person = personDefendant.getPersonDetails();

            final PersonDefendant originalPersonDefendant = originDefendant.getPersonDefendant();
            final Person originalPerson = originalPersonDefendant.getPersonDetails();

            final Person updatedPerson = getUpdatedPerson(originalPerson, person);

            updatedPersonDefendant = PersonDefendant.personDefendant()
                    .withPersonDetails(updatedPerson)
                    .withArrestSummonsNumber(personDefendant.getArrestSummonsNumber())
                    .withCustodyTimeLimit(personDefendant.getCustodyTimeLimit())
                    .withDriverNumber(personDefendant.getDriverNumber())
                    .withDriverLicenseIssue(personDefendant.getDriverLicenseIssue())
                    .withEmployerPayrollReference(personDefendant.getEmployerPayrollReference())
                    .withPerceivedBirthYear(personDefendant.getPerceivedBirthYear())
                    .withBailConditions(originalPersonDefendant.getBailConditions())
                    .withBailReasons(personDefendant.getBailReasons())
                    .withVehicleOperatorLicenceNumber(personDefendant.getVehicleOperatorLicenceNumber())
                    .withDriverLicenceCode(personDefendant.getDriverLicenceCode())
                    .withEmployerOrganisation(personDefendant.getEmployerOrganisation())
                    .withCustodialEstablishment(personDefendant.getCustodialEstablishment())
                    .withBailStatus(personDefendant.getBailStatus())
                    .build();
        } else {
            updatedPersonDefendant = originDefendant.getPersonDefendant();
        }
        return updatedPersonDefendant;
    }

    /**
     * Get the Updated {@link Person} with the original occupation field in-tact.
     *
     * @param originalPerson - the original person from the viewstore
     * @param person         - the updated person from the new event (missing occupation)
     * @return the latest version of the defendant with the original occupation field value.
     */
    private Person getUpdatedPerson(final Person originalPerson, final Person person) {

        final Ethnicity updatedEthnicity = getUpdatedEthnicity(originalPerson.getEthnicity(), person.getEthnicity());

        return Person.person()
                .withLastName(person.getLastName())
                .withFirstName(person.getFirstName())
                .withMiddleName(person.getMiddleName())
                .withTitle(person.getTitle())
                .withOccupation(getUpdatedValue(originalPerson.getOccupation(), person.getOccupation()))
                .withOccupationCode(getUpdatedValue(originalPerson.getOccupationCode(), person.getOccupationCode()))
                .withDateOfBirth(person.getDateOfBirth())
                .withEthnicity(updatedEthnicity)
                .withGender(person.getGender())
                .withAdditionalNationalityCode(person.getAdditionalNationalityCode())
                .withAdditionalNationalityId(person.getAdditionalNationalityId())
                .withAdditionalNationalityDescription(person.getAdditionalNationalityDescription())
                .withDocumentationLanguageNeeds(person.getDocumentationLanguageNeeds())
                .withInterpreterLanguageNeeds(person.getInterpreterLanguageNeeds())
                .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                .withNationalityCode(person.getNationalityCode())
                .withNationalityDescription(person.getNationalityDescription())
                .withNationalityId(person.getNationalityId())
                .withSpecificRequirements(person.getSpecificRequirements())
                .withAddress(person.getAddress())
                .withContact(person.getContact())
                .withDisabilityStatus(person.getDisabilityStatus())
                .withPersonMarkers(person.getPersonMarkers())
                .build();
    }

    private Ethnicity getUpdatedEthnicity(final Ethnicity originalEthnicity, final Ethnicity ethnicity) {
        return ethnicity == null ? originalEthnicity : Ethnicity.ethnicity()
                .withSelfDefinedEthnicityId(ethnicity.getSelfDefinedEthnicityId())
                .withSelfDefinedEthnicityCode(ethnicity.getSelfDefinedEthnicityCode())
                .withSelfDefinedEthnicityDescription(ethnicity.getSelfDefinedEthnicityDescription())
                .withObservedEthnicityId(getUpdatedValue(originalEthnicity.getObservedEthnicityId(), ethnicity.getObservedEthnicityId()))
                .withObservedEthnicityCode(getUpdatedValue(originalEthnicity.getObservedEthnicityCode(), ethnicity.getObservedEthnicityCode()))
                .withObservedEthnicityDescription(getUpdatedValue(originalEthnicity.getObservedEthnicityDescription(), ethnicity.getObservedEthnicityCode()))
                .build();
    }

    private List<AssociatedPerson> getUpdatedAssociatedPeople(final List<AssociatedPerson> originalAssociatedPeople, final List<AssociatedPerson> associatedPeople) {

        final List<AssociatedPerson> updatedAssociatedPeople = new ArrayList<>();

        for (final AssociatedPerson associatedPerson : associatedPeople) {

            final Person person = associatedPerson.getPerson();
            final Optional<Person> originPerson = getMatchingOriginAssociatedPerson(originalAssociatedPeople, person);

            updatedAssociatedPeople.add(AssociatedPerson.associatedPerson()
                    .withPerson(originPerson.isPresent() ? Person.person()
                            .withLastName(person.getLastName())
                            .withFirstName(person.getFirstName())
                            .withPersonMarkers(person.getPersonMarkers())
                            .withSpecificRequirements(person.getSpecificRequirements())
                            .withNationalityId(person.getNationalityId())
                            .withNationalityCode(person.getNationalityCode())
                            .withNationalInsuranceNumber(person.getNationalInsuranceNumber())
                            .withInterpreterLanguageNeeds(person.getInterpreterLanguageNeeds())
                            .withDocumentationLanguageNeeds(person.getDocumentationLanguageNeeds())
                            .withDisabilityStatus(person.getDisabilityStatus())
                            .withAddress(person.getAddress())
                            .withGender(person.getGender())
                            .withAdditionalNationalityId(person.getAdditionalNationalityId())
                            .withAdditionalNationalityCode(person.getAdditionalNationalityCode())
                            .withAdditionalNationalityDescription(person.getAdditionalNationalityDescription())
                            .withOccupation(person.getOccupation())
                            .withOccupationCode(person.getOccupationCode())
                            .withMiddleName(person.getMiddleName())
                            .withContact(getUpdatedValue(originPerson.get().getContact(), person.getContact()))
                            .withEthnicity(getUpdatedValue(originPerson.get().getEthnicity(), person.getEthnicity()))
                            .withDateOfBirth(getUpdatedValue(originPerson.get().getDateOfBirth(), person.getDateOfBirth()))
                            .withTitle(getUpdatedValue(originPerson.get().getTitle(), person.getTitle()))
                            .build()
                            : person)
                    .withRole(associatedPerson.getRole())
                    .build());
        }

        return updatedAssociatedPeople;
    }

    private Optional<Person> getMatchingOriginAssociatedPerson(final List<AssociatedPerson> originalAssociatedPeople, final Person person) {

        if (CollectionUtils.isNotEmpty(originalAssociatedPeople)) {
            return originalAssociatedPeople.stream().map(AssociatedPerson::getPerson)
                    .filter(p -> StringUtils.equals(person.getLastName(), p.getLastName())
                            && StringUtils.equals(person.getFirstName(), p.getFirstName())
                            && (person.getDateOfBirth() == null || person.getDateOfBirth().equals(p.getDateOfBirth())))
                    .findFirst();
        }
        return Optional.empty();
    }

    private <T extends Object> T getUpdatedValue(T originalValue, T newValue) {
        return newValue != null ? newValue : originalValue;
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private List<UUID> getAllDefendantsUUID(CourtApplication courtApplication) {
        return courtApplication.getRespondents()
                .stream()
                .filter(respondents -> respondents.getPartyDetails() != null && respondents.getPartyDetails().getDefendant() != null)
                .map(filteredRespondents -> filteredRespondents.getPartyDetails().getDefendant().getId()).collect(Collectors.toList());
    }
}