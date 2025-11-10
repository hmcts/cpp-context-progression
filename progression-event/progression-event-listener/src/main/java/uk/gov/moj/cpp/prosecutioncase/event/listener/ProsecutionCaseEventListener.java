package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseEjectedViaBdf;
import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.CaseNoteAddedV2;
import uk.gov.justice.core.courts.CaseNoteEdited;
import uk.gov.justice.core.courts.CaseNoteEditedV2;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.progression.courts.CaseInsertedBdf;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseNoteRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseEventListener {

    private static final String CASE_STATUS_EJECTED = "EJECTED";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private CaseNoteRepository caseNoteRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private SearchProsecutionCase searchCase;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseEventListener.class);

    @Handles("progression.event.prosecution-case-created")
    public void processProsecutionCaseCreated(final JsonEnvelope event) {
        final ProsecutionCaseCreated prosecutionCaseCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseCreated.class);
        final ProsecutionCase prosecutionCase = dedupAllReportingRestrictions(prosecutionCaseCreated.getProsecutionCase());

        final List<Defendant> defendants = enrichDefendantsWithPoliceBailInformation(prosecutionCase);

        defendants.stream().forEach(d -> filterDuplicateOffencesById(d.getOffences()));

        repository.save(getProsecutionCaseEntity(ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase).withDefendants(defendants).build()));
        makeSearchable(prosecutionCase);
    }

    @Handles("progression.event.case-inserted-bdf")
    public void prosecutionCaseInserted(final JsonEnvelope event){
        final CaseInsertedBdf caseInsertedBdf = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseInsertedBdf.class);

        if(nonNull(repository.findOptionalByCaseId(caseInsertedBdf.getProsecutionCase().getId()))){
            return ;
        }

        repository.save(getProsecutionCaseEntity(caseInsertedBdf.getProsecutionCase()));

    }

    private void filterDuplicateOffencesById(List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
        LOGGER.info("Removing duplicate offence, offences count:{} and offences count after filtering:{} ", offences.size(), offenceIds.size());
    }

    @Handles("progression.event.case-ejected")
    public void processProsecutionCaseEjected(final JsonEnvelope event) {
        final CaseEjected caseEjected = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseEjected.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseEjected.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = updateProsecutionCase(persistentProsecutionCase, caseEjected.getRemovalReason());
        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));
        updateLinkedApplications(caseEjected.getProsecutionCaseId(),caseEjected.getRemovalReason());
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseEjected.getProsecutionCaseId());
        caseDefendantHearingEntities.forEach(caseDefendantHearingEntity -> {
            final HearingEntity hearingEntity = caseDefendantHearingEntity.getHearing();
            final UUID caseId = caseDefendantHearingEntity.getId().getCaseId();
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
            final Hearing updatedHearing = HearingEntityUtil.updateHearingWithCase(hearing, caseId);
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
            hearingRepository.save(hearingEntity);
        });
    }

    @Handles("progression.event.case-ejected-via-bdf")
    public void processProsecutionCaseEjectedViaBDF(final JsonEnvelope event) {
        final CaseEjectedViaBdf caseEjected = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseEjectedViaBdf.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseEjected.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = updateProsecutionCase(persistentProsecutionCase, caseEjected.getRemovalReason());
        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));
        updateLinkedApplications(caseEjected.getProsecutionCaseId(),caseEjected.getRemovalReason());
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseEjected.getProsecutionCaseId());
        caseDefendantHearingEntities.forEach(caseDefendantHearingEntity -> {
            final HearingEntity hearingEntity = caseDefendantHearingEntity.getHearing();
            final UUID caseId = caseDefendantHearingEntity.getId().getCaseId();
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
            final Hearing updatedHearing = HearingEntityUtil.updateHearingWithCase(hearing, caseId);
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
            hearingRepository.save(hearingEntity);
        });
    }

    private void updateLinkedApplications(UUID prosecutionCaseId, String removalReason) {
        final List<CourtApplicationCaseEntity> courtApplicationCaseEntities = courtApplicationCaseRepository.findByCaseId(prosecutionCaseId);
        courtApplicationCaseEntities.forEach(courtApplicationCaseEntity -> {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(courtApplicationCaseEntity.getCourtApplication().getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
            final CourtApplication updatedCourtApplication = updateCourtApplication(persistedApplication, removalReason);
            courtApplicationCaseEntity.getCourtApplication().setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
            courtApplicationCaseRepository.save(courtApplicationCaseEntity);

            final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(courtApplicationCaseEntity.getCourtApplication().getApplicationId());
            final JsonObject initiateCourtApplicationJson = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
            final InitiateCourtApplicationProceedings persistedInitiateCourtApplication = jsonObjectConverter.convert(initiateCourtApplicationJson, InitiateCourtApplicationProceedings.class);
            final InitiateCourtApplicationProceedings updatedpersistedInitiateCourtApplication = initiateCourtApplicationProceedingsBuilder(persistedInitiateCourtApplication)
                    .withCourtApplication(updatedCourtApplication).build();
            initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedpersistedInitiateCourtApplication).toString());
            initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);
        });
    }

    private InitiateCourtApplicationProceedings.Builder initiateCourtApplicationProceedingsBuilder(InitiateCourtApplicationProceedings initiateCourtApplicationProceedings) {
        return InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withValuesFrom(initiateCourtApplicationProceedings);
    }

    private CourtApplication updateCourtApplication(CourtApplication persistedApplication, String removalReason) {
        return CourtApplication.courtApplication()
                .withValuesFrom(persistedApplication)
                .withApplicationStatus(ApplicationStatus.EJECTED)
                .withRemovalReason(removalReason)
                .build();
    }

    private ProsecutionCase updateProsecutionCase(final ProsecutionCase persistentProsecutionCase, final String removalReason) {
        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(persistentProsecutionCase)
                .withCaseStatus(CASE_STATUS_EJECTED)
                .withRemovalReason(removalReason)
                .build();
    }

    private void makeSearchable(final ProsecutionCase prosecutionCase) {
        prosecutionCase.getDefendants().forEach(defendant ->
                searchCase.makeSearchable(prosecutionCase, defendant));
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

    private List<Defendant> enrichDefendantsWithPoliceBailInformation(final ProsecutionCase prosecutionCase) {
        final List<Defendant> defendants = new ArrayList<>();
        prosecutionCase.getDefendants()
                .forEach(defendant -> {
                    final PersonDefendant personDefendant = defendant.getPersonDefendant();

                    final Defendant.Builder defendantBuilder = Defendant.defendant().withValuesFrom(defendant);
                    if (nonNull(personDefendant)) {
                        defendantBuilder
                                .withPersonDefendant(PersonDefendant.personDefendant()
                                        .withValuesFrom(personDefendant)
                                        .withPoliceBailConditions(personDefendant.getBailConditions())
                                        .withPoliceBailStatus(personDefendant.getBailStatus()).build());
                    }
                    defendants.add(defendantBuilder.build());
                });
        return defendants;
    }

    @Handles("progression.event.case-note-added")
    public void caseNoteAdded(final JsonEnvelope event) {
        final CaseNoteAdded caseNoteAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseNoteAdded.class);
        final CaseNoteEntity caseNoteEntity = new CaseNoteEntity(randomUUID(), caseNoteAdded.getCaseId(),
                caseNoteAdded.getNote(), caseNoteAdded.getFirstName(), caseNoteAdded.getLastName(), caseNoteAdded.getCreatedDateTime(), toBoolean(caseNoteAdded.getIsPinned()));
        caseNoteRepository.save(caseNoteEntity);
    }

    @Handles("progression.event.case-note-added-v2")
    public void caseNoteAddedV2(final JsonEnvelope event) {
        final CaseNoteAddedV2 caseNoteAddedV2 = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseNoteAddedV2.class);
        final CaseNoteEntity caseNoteEntity = new CaseNoteEntity(caseNoteAddedV2.getCaseNoteId(), caseNoteAddedV2.getCaseId(),
                caseNoteAddedV2.getNote(), caseNoteAddedV2.getFirstName(), caseNoteAddedV2.getLastName(), caseNoteAddedV2.getCreatedDateTime(), toBoolean(caseNoteAddedV2.getIsPinned()));
        caseNoteRepository.save(caseNoteEntity);
    }

    @Handles("progression.event.case-note-edited")
    public void caseNoteEdited(final JsonEnvelope event) {
        final CaseNoteEdited caseNotePinned = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseNoteEdited.class);
        final CaseNoteEntity caseNoteEntity = caseNoteRepository.findBy(caseNotePinned.getCaseNoteId());
        if (nonNull(caseNoteEntity)) {
            //check for null to prevent errors during EventReplay (auto generated uuid when case note added)
            caseNoteEntity.setPinned(caseNotePinned.getIsPinned());
            caseNoteRepository.save(caseNoteEntity);
        }
    }

    @Handles("progression.event.case-note-edited-v2")
    public void caseNoteEditedV2(final JsonEnvelope event) {
        final CaseNoteEditedV2 caseNotePinned = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseNoteEditedV2.class);
        final CaseNoteEntity caseNoteEntity = caseNoteRepository.findBy(caseNotePinned.getCaseNoteId());
        caseNoteEntity.setPinned(caseNotePinned.getIsPinned());
        caseNoteRepository.save(caseNoteEntity);
    }
}
