package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CaseEjected;
import uk.gov.justice.core.courts.CaseNoteAdded;
import uk.gov.justice.core.courts.CaseNoteEdited;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseCreated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseNoteEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseNoteRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;


@SuppressWarnings("squid:S3655")
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
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private CaseNoteRepository caseNoteRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private SearchProsecutionCase searchCase;


    @Handles("progression.event.prosecution-case-created")
    public void processProsecutionCaseCreated(final JsonEnvelope event) {
        final ProsecutionCaseCreated prosecutionCaseCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseCreated.class);
        final ProsecutionCase prosecutionCase = prosecutionCaseCreated.getProsecutionCase();
        repository.save(getProsecutionCaseEntity(prosecutionCase));
        makeSearchable(prosecutionCase);
    }
    @Handles("progression.event.case-ejected")
    public void processProsecutionCaseEjected(final JsonEnvelope event) {
        final CaseEjected caseEjected = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseEjected.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseEjected.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase persistentProsecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = updateProsecutionCase(persistentProsecutionCase, caseEjected.getRemovalReason());
        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));
        updateLinkedApplications(caseEjected);
        final List<CaseDefendantHearingEntity> caseDefendantHearingEntities = caseDefendantHearingRepository.findByCaseId(caseEjected.getProsecutionCaseId());
        caseDefendantHearingEntities.stream().forEach(caseDefendantHearingEntity -> {
            final HearingEntity hearingEntity = caseDefendantHearingEntity.getHearing();
            final UUID caseId = caseDefendantHearingEntity.getId().getCaseId();
            final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
            final Hearing updatedHearing = HearingEntityUtil.updateHearingWithCase(hearing, caseId);
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
            hearingRepository.save(hearingEntity);
        });
    }


    private void updateLinkedApplications(CaseEjected caseEjected) {
        final List<CourtApplicationEntity> courtApplicationEntities = courtApplicationRepository.findByLinkedCaseId(caseEjected.getProsecutionCaseId());
        courtApplicationEntities.forEach(applicationEntity -> {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
            final CourtApplication updatedCourtApplication = updateCourtApplication(persistedApplication, caseEjected.getRemovalReason());
            applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
            courtApplicationRepository.save(applicationEntity);
        });
    }

    private CourtApplication updateCourtApplication(CourtApplication persistedApplication, String removalReason) {

        return CourtApplication.courtApplication()
                .withId(persistedApplication.getId())
                .withType(persistedApplication.getType())
                .withApplicant(persistedApplication.getApplicant())
                .withApplicationDecisionSoughtByDate(persistedApplication.getApplicationDecisionSoughtByDate())
                .withApplicationOutcome(persistedApplication.getApplicationOutcome())
                .withApplicationParticulars(persistedApplication.getApplicationParticulars())
                .withApplicationReceivedDate(persistedApplication.getApplicationReceivedDate())
                .withApplicationReference(persistedApplication.getApplicationReference())
                .withCourtApplicationPayment(persistedApplication.getCourtApplicationPayment())
                .withJudicialResults(persistedApplication.getJudicialResults())
                .withParentApplicationId(persistedApplication.getParentApplicationId())
                .withLinkedCaseId(persistedApplication.getLinkedCaseId())
                .withOutOfTimeReasons(persistedApplication.getOutOfTimeReasons())
                .withRespondents(persistedApplication.getRespondents())
                .withRemovalReason(removalReason)
                .withApplicationStatus(ApplicationStatus.EJECTED)
                .withBreachedOrder(persistedApplication.getBreachedOrder())
                .withBreachedOrderDate(persistedApplication.getBreachedOrderDate())
                .withOrderingCourt(persistedApplication.getOrderingCourt())
                .build();
    }


    private ProsecutionCase updateProsecutionCase(final ProsecutionCase persistentProsecutionCase, final String removalReason) {
        return ProsecutionCase.prosecutionCase()
                .withId(persistentProsecutionCase.getId())
                .withProsecutionCaseIdentifier(persistentProsecutionCase.getProsecutionCaseIdentifier())
                .withInitiationCode(persistentProsecutionCase.getInitiationCode())
                .withDefendants(persistentProsecutionCase.getDefendants())
                .withAppealProceedingsPending(persistentProsecutionCase.getAppealProceedingsPending())
                .withBreachProceedingsPending(persistentProsecutionCase.getBreachProceedingsPending())
                .withCaseMarkers(persistentProsecutionCase.getCaseMarkers())
                .withCaseStatus(CASE_STATUS_EJECTED)
                .withOriginatingOrganisation(persistentProsecutionCase.getOriginatingOrganisation())
                .withPoliceOfficerInCase(persistentProsecutionCase.getPoliceOfficerInCase())
                .withRemovalReason(removalReason)
                .withStatementOfFacts(persistentProsecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(persistentProsecutionCase.getStatementOfFactsWelsh())
                .build();
    }

    private void makeSearchable(final ProsecutionCase prosecutionCase) {
        prosecutionCase.getDefendants().forEach(defendant ->
                searchCase.makeSearchable(prosecutionCase, defendant));
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    @Handles("progression.event.case-note-added")
    public void caseNoteAdded(final JsonEnvelope event) {
        final CaseNoteAdded caseNoteAdded = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseNoteAdded.class);
        final CaseNoteEntity caseNoteEntity = new CaseNoteEntity(caseNoteAdded.getCaseId(),
                caseNoteAdded.getNote(), caseNoteAdded.getFirstName(), caseNoteAdded.getLastName(), caseNoteAdded.getCreatedDateTime(), toBoolean(caseNoteAdded.getIsPinned()));
        caseNoteRepository.save(caseNoteEntity);
    }

    @Handles("progression.event.case-note-edited")
    public void caseNoteEdited(final JsonEnvelope event) {
        final CaseNoteEdited caseNotePinned = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseNoteEdited.class);
        final CaseNoteEntity caseNoteEntity = caseNoteRepository.findBy(caseNotePinned.getCaseNoteId());
        caseNoteEntity.setPinned(caseNotePinned.getIsPinned());
        caseNoteRepository.save(caseNoteEntity);

    }

}
