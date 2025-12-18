package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.cpp.progression.events.DefendantDefenceOrganisationSaved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class DefendantDefenceOrganisationSavedListener {
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private HearingRepository hearingRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantDefenceOrganisationSavedListener.class);

    @Handles("progression.event.defendant-defence-organisation-saved")
    public void processDefendantDefenceOrganisationSaved(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.defendant-defence-organisation-saved {} ", event.toObfuscatedDebugString());
        }
        final DefendantDefenceOrganisationSaved defendantDefenceOrganisationSaved = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantDefenceOrganisationSaved.class);
        final UUID prosecutionCaseId = defendantDefenceOrganisationSaved.getProsecutionCaseId();
        final UUID defendantId = defendantDefenceOrganisationSaved.getDefendantId();
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation()
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withLaaContractNumber(defendantDefenceOrganisationSaved.getLaaContractNumber())
                        .withOrganisation(Organisation
                                .organisation()
                                .withName(defendantDefenceOrganisationSaved.getOrganisationName())
                                .withContact(ContactNumber.contactNumber()
                                        .withPrimaryEmail(defendantDefenceOrganisationSaved.getEmail())
                                        .withWork(defendantDefenceOrganisationSaved.getPhoneNumber())
                                        .build())
                                .withAddress(Address.
                                        address()
                                        .withAddress1(defendantDefenceOrganisationSaved.getAddressLine1())
                                        .withAddress2(defendantDefenceOrganisationSaved.getAddressLine2())
                                        .withAddress3(defendantDefenceOrganisationSaved.getAddressLine3())
                                        .withAddress4(defendantDefenceOrganisationSaved.getAddressLine4())
                                        .withPostcode(defendantDefenceOrganisationSaved.getPostCode())
                                        .build())
                                .build())
                        .build())
                .build();
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(prosecutionCaseId);
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        updateDefendantForCase(defendantId, associatedDefenceOrganisation, prosecutionCase);
        repository.save(getProsecutionCaseEntity(prosecutionCase));
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByDefendantId(defendantId);
        caseDefendantHearingEntities.stream().forEach(caseDefendantHearingEntity -> {
            final HearingEntity hearingEntity = caseDefendantHearingEntity.getHearing();
            final JsonObject hearingJson = jsonFromString(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
            hearing.getProsecutionCases().stream().forEach(hearingProsecutionCase ->
                    updateDefendantForCase(defendantId, associatedDefenceOrganisation, hearingProsecutionCase)
            );
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
            hearingRepository.save(hearingEntity);
        });

    }

    private void updateDefendantForCase(UUID defendantId, AssociatedDefenceOrganisation associatedDefenceOrganisation, ProsecutionCase prosecutionCase) {
        final Optional<Defendant> originalDefendant = prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst();
        if (originalDefendant.isPresent()) {
            final Defendant updatedDefendant = updateDefendant(originalDefendant.get(), associatedDefenceOrganisation);
            prosecutionCase.getDefendants().remove(originalDefendant.get());
            prosecutionCase.getDefendants().add(updatedDefendant);
        }
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private Defendant updateDefendant(final Defendant originDefendant, final AssociatedDefenceOrganisation associatedDefenceOrganisation) {

        return Defendant.defendant()
                .withOffences(originDefendant.getOffences())
                .withCpsDefendantId(originDefendant.getCpsDefendantId())
                .withPersonDefendant(originDefendant.getPersonDefendant())
                .withLegalEntityDefendant(originDefendant.getLegalEntityDefendant())
                .withAssociatedPersons(originDefendant.getAssociatedPersons())
                .withId(originDefendant.getId())
                .withMitigation(originDefendant.getMitigation())
                .withMitigationWelsh(originDefendant.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(originDefendant.getNumberOfPreviousConvictionsCited())
                .withProsecutionAuthorityReference(originDefendant.getProsecutionAuthorityReference())
                .withProsecutionCaseId(originDefendant.getProsecutionCaseId())
                .withWitnessStatement(originDefendant.getWitnessStatement())
                .withWitnessStatementWelsh(originDefendant.getWitnessStatementWelsh())
                .withDefenceOrganisation(originDefendant.getDefenceOrganisation())
                .withPncId(originDefendant.getPncId())
                .withAliases(originDefendant.getAliases())
                .withIsYouth(originDefendant.getIsYouth())
                .withLegalAidStatus(originDefendant.getLegalAidStatus())
                .withDefendantCaseJudicialResults(originDefendant.getDefendantCaseJudicialResults())
                .withCroNumber(originDefendant.getCroNumber())
                .withProceedingsConcluded(originDefendant.getProceedingsConcluded())
                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                .withAssociationLockedByRepOrder(originDefendant.getAssociationLockedByRepOrder())
                .build();

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
}
