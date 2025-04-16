package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictionsForDefendants;

import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Ethnicity;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDefendantUpdated;
import uk.gov.justice.core.courts.HearingResultedCaseUpdated;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantUpdated;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberDecreased;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberIncreased;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberUpdated;
import uk.gov.justice.core.courts.ReportingRestriction;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

@SuppressWarnings({"squid:S3655", "squid:S1135", "squid:CommentedOutCodeLine", "squid:UnusedPrivateMethod"})
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
    private HearingRepository hearingRepository;
    @Inject
    private SearchProsecutionCase searchProsecutionCase;

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    @Handles("progression.event.prosecution-case-defendant-updated")
    public void  processProsecutionCaseDefendantUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event  progression.event.prosecution-case-defendant-updated {} ", event.toObfuscatedDebugString());
        }
        final ProsecutionCaseDefendantUpdated prosecutionCaseDefendantUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantUpdated.class);
        final DefendantUpdate defendantUpdate = prosecutionCaseDefendantUpdated.getDefendant();
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendantUpdate.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final Optional<Defendant> originDefendant = prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantUpdate.getId())).findFirst();

        if (originDefendant.isPresent()) {
            final Defendant updatedDefendant = updateDefendant(originDefendant.get(), defendantUpdate);
            filterDuplicateOffencesById(updatedDefendant.getOffences());
            prosecutionCase.getDefendants().remove(originDefendant.get());
            prosecutionCase.getDefendants().add(updatedDefendant);
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

        if (isNotEmpty(defendants)) {
            final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(defendants.get(0).getProsecutionCaseId());
            final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
            final ProsecutionCase prosecutionCaseInRepository = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);

            for (final Defendant defendant : defendants) {
                final Optional<Defendant> defendantFromRepository = prosecutionCaseInRepository.getDefendants().stream().filter(def -> def.getId().equals(defendant.getId())).findFirst();
                if (defendantFromRepository.isPresent()) {
                    final Defendant originalDefendant = defendantFromRepository.get();
                    prosecutionCaseInRepository.getDefendants().remove(defendantFromRepository.get());
                    // GPE-12381 . This has been done explicitly  not to lose  progression flag associationLockedByRepOrder when we receive result from hearing
                    final Defendant updatedDefendant = getUpdatedDefendant(originalDefendant, defendant);
                    filterDuplicateOffencesById(updatedDefendant.getOffences());
                    prosecutionCaseInRepository.getDefendants().add(updatedDefendant);
                }
            }

            final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase()
                    .withValuesFrom(prosecutionCaseInRepository)
                    .withDefendants(dedupAllReportingRestrictionsForDefendants(prosecutionCaseInRepository.getDefendants()))
                    .withCaseStatus(hearingResultedCaseUpdated.getProsecutionCase().getCaseStatus())
                    .withMigrationSourceSystem(prosecutionCaseInRepository.getMigrationSourceSystem())
                    .build();
            repository.save(getProsecutionCaseEntity(updatedProsecutionCase));
            updateSearchable(updatedProsecutionCase);
        }
    }

    @Handles("progression.event.hearing-defendant-updated")
    public void processHearingDefendantUpdated(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event  progression.event.hearing-defendant-updated {} ", event.toObfuscatedDebugString());
        }
        final HearingDefendantUpdated hearingDefendantUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingDefendantUpdated.class);
        final DefendantUpdate defendantUpdate = hearingDefendantUpdated.getDefendant();
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingDefendantUpdated.getHearingId());
        if(isNull(hearingEntity)){
            return;
        }
        final JsonObject hearingJson = jsonFromString(hearingEntity.getPayload());
        final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);

        if (nonNull(hearing.getProsecutionCases())) {
            hearing.getProsecutionCases().forEach(prosecutionCase -> {
                final Optional<Defendant> oldDefendant = prosecutionCase.getDefendants().stream()
                        .filter(d -> d.getId().equals(defendantUpdate.getId()))
                        .findFirst();

                if (oldDefendant.isPresent()) {
                    final Defendant updatedDefendant = updateDefendant(oldDefendant.get(), defendantUpdate);
                    prosecutionCase.getDefendants().remove(oldDefendant.get());
                    filterDuplicateOffencesById(updatedDefendant.getOffences());
                    prosecutionCase.getDefendants().add(dedupAllReportingRestrictions(updatedDefendant));
                }
            });

            hearingRepository.save(prepareHearingEntity(hearing, hearingEntity));
        }
    }

    private void filterDuplicateOffencesById(final List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
        LOGGER.info("Removing duplicate offence, offences count:{} and offences count after filtering:{} ", offences.size(), offenceIds.size());
    }

    @Handles("progression.event.prosecution-case-listing-number-updated")
    public void processProsecutionCaseUpdatedWithListingNumber(final JsonEnvelope event) {
        final ProsecutionCaseListingNumberUpdated prosecutionCaseListingNumberUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseListingNumberUpdated.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(prosecutionCaseListingNumberUpdated.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);


        final Map<UUID, Integer> listingMap = prosecutionCaseListingNumberUpdated.getOffenceListingNumbers().stream().collect(Collectors.toMap(OffenceListingNumbers::getOffenceId, OffenceListingNumbers::getListingNumber, Math::max));
        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(persistentProsecutionCase)
                .withDefendants(persistentProsecutionCase.getDefendants().stream()
                        .map(defendant -> uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant)
                                .withOffences(defendant.getOffences().stream()
                                        .map(offence -> uk.gov.justice.core.courts.Offence.offence().withValuesFrom(offence)
                                                .withListingNumber(ofNullable(listingMap.get(offence.getId())).orElse(offence.getListingNumber()))
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .build();
        LOGGER.info("progression.event.prosecution-case-listing-number-updated {} ", objectToJsonObjectConverter.convert(updatedProsecutionCase));
        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));

    }

    @Handles("progression.event.prosecution-case-listing-number-decreased")
    public void processProsecutionCaseListingNumberDecreased(final JsonEnvelope event) {
        final ProsecutionCaseListingNumberDecreased prosecutionCaseListingNumberDecreased = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseListingNumberDecreased.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(prosecutionCaseListingNumberDecreased.getProsecutionCaseId());
        final Set<UUID> listingSet = prosecutionCaseListingNumberDecreased.getOffenceIds().stream().collect(Collectors.toSet());

        saveListingNumber(prosecutionCaseEntity, listingSet, -1);
    }


    @Handles("progression.event.prosecution-case-listing-number-increased")
    public void processProsecutionCaseListingNumberIncreased(final JsonEnvelope event) {
        final ProsecutionCaseListingNumberIncreased prosecutionCaseListingNumberIncreased = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseListingNumberIncreased.class);
        if (CollectionUtils.isNotEmpty(prosecutionCaseListingNumberIncreased.getOffenceListingNumbers())) {
            final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(prosecutionCaseListingNumberIncreased.getProsecutionCaseId());
            final Set<UUID> listingSet = prosecutionCaseListingNumberIncreased.getOffenceListingNumbers().stream().map(OffenceListingNumbers::getOffenceId).collect(Collectors.toSet());

            saveListingNumber(prosecutionCaseEntity, listingSet, 1);
        }

    }

    private void saveListingNumber(final ProsecutionCaseEntity prosecutionCaseEntity, final Set<UUID> listingSet, int accumulator) {
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = ProsecutionCase.prosecutionCase().withValuesFrom(persistentProsecutionCase)
                .withDefendants(persistentProsecutionCase.getDefendants().stream()
                        .map(defendant -> uk.gov.justice.core.courts.Defendant.defendant().withValuesFrom(defendant)
                                .withOffences(defendant.getOffences().stream()
                                        .map(offence -> uk.gov.justice.core.courts.Offence.offence().withValuesFrom(offence)
                                                .withListingNumber((listingSet.contains(offence.getId()) ? Integer.valueOf(ofNullable(offence.getListingNumber()).orElse(0) + accumulator) : offence.getListingNumber()))
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(toList()))
                .build();
        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));

    }

    private Defendant getUpdatedDefendant(final Defendant originalDefendant, final Defendant defendant) {
        final List<Offence> offences = nonNull(originalDefendant.getOffences()) ? new ArrayList<>(originalDefendant.getOffences()) : new ArrayList<>();

        if (nonNull(defendant.getOffences())) {
            final List<Offence> updatedOffences = getUpdatedOffencesWithNonNowsJudicialResults(defendant.getOffences());

            updatedOffences.forEach(updatedOffence -> {
                if (isNotEmpty(updatedOffence.getReportingRestrictions())) {
                    updateReportingRestriction(offences, updatedOffence);
                }
                final Offence offence = maintainLaaReferenceFromPersistedOffence(offences, updatedOffence);
                if (offences.removeIf(anOffence -> anOffence.getId().equals(offence.getId()))) {
                    offences.add(offence);
                }
            });
        }

        final PersonDefendant updatedPersonDefendant = getUpdatedPersonDefendant(originalDefendant, defendant);

        return Defendant.defendant()
                .withId(defendant.getId())
                .withMasterDefendantId(defendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(defendant.getCourtProceedingsInitiated())
                .withOffences(offences)
                .withCpsDefendantId(originalDefendant.getCpsDefendantId())
                .withPersonDefendant(updatedPersonDefendant)
                .withLegalAidStatus(originalDefendant.getLegalAidStatus())
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

    private Offence maintainLaaReferenceFromPersistedOffence(final List<Offence> persistedOffences, final Offence updatedOffence) {
        final Optional<Offence> persistedOffence = persistedOffences.stream().filter(offence -> offence.getId().equals(updatedOffence.getId())).findFirst();
        return persistedOffence.isPresent() && nonNull(persistedOffence.get().getLaaApplnReference()) ?
                Offence.offence().withValuesFrom(updatedOffence).withLaaApplnReference(persistedOffence.get().getLaaApplnReference()).build() : updatedOffence;
    }

    private void updateReportingRestriction(final List<Offence> offences, final Offence updatedOffence) {
        final List<ReportingRestriction> reportingRestrictions = updatedOffence.getReportingRestrictions();
        final Optional<Offence> originalOffence = offences.stream().filter(offence -> offence.getId().equals(updatedOffence.getId())).findFirst();
        if (originalOffence.isPresent() && isNotEmpty(originalOffence.get().getReportingRestrictions())) {
            originalOffence.get().getReportingRestrictions().forEach(reportingRestriction -> {
                //if  reportingRestriction not exist then add it
                final Optional<ReportingRestriction> originalReportingRestriction = reportingRestrictions.stream().filter(rr -> rr.getLabel().equals(reportingRestriction.getLabel())).findFirst();
                if (!originalReportingRestriction.isPresent()) {
                    reportingRestrictions.add(reportingRestriction);
                }
            });
        }
    }

    private PersonDefendant getUpdatedPersonDefendant(final Defendant originalDefendant, final Defendant defendant) {
        PersonDefendant updatedPersonDefendant = null;
        if (nonNull(defendant.getPersonDefendant())) {
            updatedPersonDefendant = PersonDefendant.personDefendant()
                    .withValuesFrom(defendant.getPersonDefendant())
                    .withPoliceBailStatus(getUpdatedValueIfNotPresent(originalDefendant.getPersonDefendant().getPoliceBailStatus(),
                            defendant.getPersonDefendant().getPoliceBailStatus()))
                    .withPoliceBailConditions(getUpdatedValueIfNotPresent(originalDefendant.getPersonDefendant().getPoliceBailConditions(),
                            defendant.getPersonDefendant().getPoliceBailConditions()))
                    .build();
        }
        return updatedPersonDefendant;
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
                searchProsecutionCase.makeSearchable(prosecutionCase, caseDefendant));
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
                .withValuesFrom(originDefendant)
                .withPersonDefendant(updatedPersonDefendant)
                .withLegalEntityDefendant(defendant.getLegalEntityDefendant())
                .withAssociatedPersons(updatedAssociatedPeople)
                .withId(defendant.getId())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withPncId(defendant.getPncId())
                .withAliases(defendant.getAliases())
                .withIsYouth(defendant.getIsYouth())
                .withMasterDefendantId(nonNull(defendant.getMasterDefendantId()) ? defendant.getMasterDefendantId() : originDefendant.getMasterDefendantId())
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
                    .withPoliceBailConditions(getUpdatedValueIfNotPresent(originalPersonDefendant.getPoliceBailConditions(), personDefendant.getPoliceBailConditions()))
                    .withPoliceBailStatus(getUpdatedValueIfNotPresent(originalPersonDefendant.getPoliceBailStatus(), personDefendant.getPoliceBailStatus()))
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
                .withHearingLanguageNeeds(person.getHearingLanguageNeeds())
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
        if (ethnicity == null) {
            return originalEthnicity;
        }
        if (originalEthnicity == null) {
            return ethnicity;
        }
        return Ethnicity.ethnicity()
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
                            .withHearingLanguageNeeds(person.getHearingLanguageNeeds())
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

        if (isNotEmpty(originalAssociatedPeople)) {
            return originalAssociatedPeople.stream().map(AssociatedPerson::getPerson)
                    .filter(p -> StringUtils.equals(person.getLastName(), p.getLastName())
                            && StringUtils.equals(person.getFirstName(), p.getFirstName())
                            && (person.getDateOfBirth() == null || person.getDateOfBirth().equals(p.getDateOfBirth())))
                    .findFirst();
        }
        return Optional.empty();
    }

    private <T> T getUpdatedValueIfNotPresent(T originalValue, T newValue) {
        return nonNull(originalValue) ? originalValue : newValue;
    }

    private <T> T getUpdatedValue(T originalValue, T newValue) {
        return nonNull(newValue) ? newValue : originalValue;
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            pCaseEntity.setGroupId(prosecutionCase.getGroupId());
        }
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private HearingEntity prepareHearingEntity(final Hearing hearing, final HearingEntity hearingEntity) {
        final HearingEntity entity = new HearingEntity();
        entity.setHearingId(hearing.getId());
        entity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        entity.setListingStatus(hearingEntity.getListingStatus());
        entity.setResultLines(hearingEntity.getResultLines());
        entity.setConfirmedDate(hearingEntity.getConfirmedDate());
        return entity;
    }
}
