package uk.gov.moj.cpp.prosecutioncase.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantUpdate;
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

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantUpdatedEventListener {

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

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseDefendantUpdatedEventListener.class);

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
            Defendant updatedDefendant = updateDefendant(originDefendant.get(), defendantUpdate);
            prosecutionCase.getDefendants().remove(originDefendant.get());
            prosecutionCase.getDefendants().add(updatedDefendant);
            if (nonNull(applicationEntities) && !applicationEntities.isEmpty()) {
                updateDefendantForCourtApplication(applicationEntities, updatedDefendant);
            }
        }
        repository.save(getProsecutionCaseEntity(prosecutionCase));

        updateSearchable(prosecutionCase);
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
      if(getAllDefendantsUUID(persistedApplication).contains(updatedDefendant.getId())){
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
                    .withRespondents( persistedApplication.getRespondents().stream()
                            .map(r->r.getPartyDetails().getDefendant()!= null && r.getPartyDetails().getDefendant().getId().equals(updatedDefendant.getId()) ? updateRespondent(r, updatedDefendant) : r  )
                            .collect(Collectors.toList()))
                    .withOutOfTimeReasons(persistedApplication.getOutOfTimeReasons())
                    .build();


            applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedApplication).toString());
            courtApplicationRepository.save(applicationEntity);
        }
    }

    private  CourtApplicationRespondent updateRespondent(final CourtApplicationRespondent respondent, final Defendant updatedDefendant) {
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

        return Defendant.defendant().withOffences(originDefendant.getOffences())
                .withPersonDefendant(defendant.getPersonDefendant())
                .withLegalEntityDefendant(originDefendant.getLegalEntityDefendant())
                .withAssociatedPersons(defendant.getAssociatedPersons())
                .withId(defendant.getId())
                .withMitigation(originDefendant.getMitigation())
                .withMitigationWelsh(originDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(defendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(originDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(defendant.getProsecutionCaseId())
                .withWitnessStatement(originDefendant.getWitnessStatement())
                .withDefenceOrganisation(defendant.getDefenceOrganisation())
                .withPncId(defendant.getPncId())
                .withAliases(defendant.getAliases())
                .build();
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

    private List<UUID> getAllDefendantsUUID(CourtApplication courtApplication){
        return courtApplication.getRespondents()
                .stream()
                .filter(respondents -> respondents.getPartyDetails() != null && respondents.getPartyDetails().getDefendant() != null)
                .map(filteredRespondents -> filteredRespondents.getPartyDetails().getDefendant().getId()).collect(Collectors.toList());
    }

}
