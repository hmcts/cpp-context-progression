package uk.gov.moj.cpp.application.event.listener;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
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
import javax.json.JsonObject;
import java.util.Optional;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class CourtApplicationEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CourtApplicationRepository repository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private SearchProsecutionCase searchCase;

    @Inject
    private SearchProsecutionCase searchApplication;

    @Handles("progression.event.court-application-created")
    public void processCourtApplicationCreated(final JsonEnvelope event) {
        final CourtApplicationCreated courtApplicationCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationCreated.class);
        final CourtApplication courtApplication = courtApplicationCreated.getCourtApplication();
        final CourtApplication applicationToBeSaved = CourtApplication.courtApplication()
                .withRespondents(courtApplication.getRespondents())
                .withApplicant(courtApplication.getApplicant())
                .withLinkedCaseId(courtApplication.getLinkedCaseId())
                .withId(courtApplication.getId())
                .withApplicationDecisionSoughtByDate(courtApplication.getApplicationDecisionSoughtByDate())
                .withApplicationOutcome(courtApplication.getApplicationOutcome())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withApplicationReceivedDate(courtApplication.getApplicationReceivedDate())
                .withApplicationReference(courtApplicationCreated.getArn())
                .withApplicationStatus(ApplicationStatus.DRAFT)
                .withCourtApplicationPayment(courtApplication.getCourtApplicationPayment())
                .withJudicialResults(courtApplication.getJudicialResults())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withOutOfTimeReasons(courtApplication.getOutOfTimeReasons())
                .withBreachedOrder(courtApplication.getBreachedOrder())
                .withBreachedOrderDate(courtApplication.getBreachedOrderDate())
                .withOrderingCourt(courtApplication.getOrderingCourt())
                .withType(courtApplication.getType())
                .build();
        repository.save(getCourtApplicationEntity(applicationToBeSaved));

        makeStandaloneApplicationSearchable(applicationToBeSaved);
    }

    @Handles("progression.event.court-application-status-changed")
    public void processCourtApplicationStatusChanged(final JsonEnvelope event) {
        final CourtApplicationStatusChanged courtApplicationStatusChanged = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationStatusChanged.class);

        CourtApplicationEntity applicationEntity = repository.findByApplicationId(courtApplicationStatusChanged.getId());

        if (applicationEntity != null) {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);

            final ApplicationStatus applicationStatus = persistedApplication.getApplicationStatus() == ApplicationStatus.FINALISED ? ApplicationStatus.FINALISED : courtApplicationStatusChanged.getApplicationStatus();

            final CourtApplication updatedApplication = CourtApplication.courtApplication()
                    .withId(persistedApplication.getId())
                    .withType(persistedApplication.getType())
                    .withApplicant(persistedApplication.getApplicant())
                    .withApplicationDecisionSoughtByDate(persistedApplication.getApplicationDecisionSoughtByDate())
                    .withOrderingCourt(persistedApplication.getOrderingCourt())
                    .withApplicationOutcome(persistedApplication.getApplicationOutcome())
                    .withApplicationParticulars(persistedApplication.getApplicationParticulars())
                    .withApplicationReceivedDate(persistedApplication.getApplicationReceivedDate())
                    .withApplicationReference(persistedApplication.getApplicationReference())
                    .withApplicationStatus(applicationStatus)
                    .withCourtApplicationPayment(persistedApplication.getCourtApplicationPayment())
                    .withJudicialResults(persistedApplication.getJudicialResults())
                    .withParentApplicationId(persistedApplication.getParentApplicationId())
                    .withLinkedCaseId(persistedApplication.getLinkedCaseId())
                    .withBreachedOrder(persistedApplication.getBreachedOrder())
                    .withBreachedOrderDate(persistedApplication.getBreachedOrderDate())
                    .withRespondents(persistedApplication.getRespondents())
                    .withOutOfTimeReasons(persistedApplication.getOutOfTimeReasons())
                    .build();

            applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedApplication).toString());
            repository.save(applicationEntity);
        }
    }

    @Handles("progression.event.court-application-updated")
    public void processCourtApplicationUpdated(final JsonEnvelope event) {
        final CourtApplicationUpdated courtApplicationUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationUpdated.class);
        final CourtApplication updatedCourtApplication = courtApplicationUpdated.getCourtApplication();

        final CourtApplicationEntity courtApplicationEntity = repository.findByApplicationId(courtApplicationUpdated.getCourtApplication().getId());
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
        repository.save(courtApplicationEntity);
        if (updatedCourtApplication.getLinkedCaseId() != null) {
            updateDefendantForProsecutionCase(updatedCourtApplication);
        }
        makeStandaloneApplicationSearchable(updatedCourtApplication);
    }

    private void updateDefendantForProsecutionCase(final CourtApplication updatedCourtApplication) {
        final ProsecutionCaseEntity prosecutionCaseEntity = prosecutionCaseRepository.findByCaseId(updatedCourtApplication.getLinkedCaseId());
        final JsonObject prosecutionCaseJson = stringToJsonObjectConverter.convert(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        if (updatedCourtApplication.getApplicant() != null) {
            updateProsecutionCaseDefendant(prosecutionCase, updatedCourtApplication.getApplicant().getDefendant());
        }
        if (updatedCourtApplication.getRespondents() != null) {
            updatedCourtApplication.getRespondents().forEach(respondent ->
                    updateProsecutionCaseDefendant(prosecutionCase, respondent.getPartyDetails().getDefendant())
            );
        }
    }

    private void updateProsecutionCaseDefendant(final ProsecutionCase prosecutionCase, final Defendant defendantUpdate) {
        if (defendantUpdate != null) {
            final Optional<Defendant> originDefendant = prosecutionCase.getDefendants().stream().filter(d -> d.getId().equals(defendantUpdate.getId())).findFirst();
            if (originDefendant.isPresent()) {
                prosecutionCase.getDefendants().remove(originDefendant.get());
                prosecutionCase.getDefendants().add(defendantUpdate);
            }
            prosecutionCaseRepository.save(getProsecutionCaseEntity(prosecutionCase));
            updateSearchable(prosecutionCase);
        }
    }

    private CourtApplicationEntity getCourtApplicationEntity(final CourtApplication courtApplication) {
        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(courtApplication.getId());
        applicationEntity.setLinkedCaseId(courtApplication.getLinkedCaseId());
        applicationEntity.setParentApplicationId(courtApplication.getParentApplicationId());
        applicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());
        return applicationEntity;
    }

    private void makeStandaloneApplicationSearchable(final CourtApplication courtApplication) {
        if (courtApplication.getLinkedCaseId() == null) {
            searchApplication.makeApplicationSearchable(courtApplication);
        }
    }

    private void updateSearchable(final ProsecutionCase prosecutionCase) {
        prosecutionCase.getDefendants().forEach(caseDefendant ->
                searchCase.makeSearchable(prosecutionCase, caseDefendant));
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

}
