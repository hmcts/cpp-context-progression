package uk.gov.moj.cpp.application.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.application.event.listener.ApplicationHelper.getRelatedCaseIds;
import static uk.gov.moj.cpp.application.event.listener.ApplicationHelper.updateCase;

import uk.gov.justice.core.courts.ApplicationReporderOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.DefendantCase;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.event.ApplicationRepOrderUpdatedForApplication;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;


@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ApplicationRepOrderOffencesUpdatedEventListener {
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Handles("progression.event.application-reporder-offences-updated")
    public void processApplicationRepOrderOffencesUpdated(final JsonEnvelope event) {
        final ApplicationReporderOffencesUpdated applicationOffencesUpdated =
                jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationReporderOffencesUpdated.class);

        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(applicationOffencesUpdated.getApplicationId());
        final CourtApplicationEntity applicationEntity =
                courtApplicationRepository.findByApplicationId(applicationOffencesUpdated.getApplicationId());

        CourtApplication courtApplication = getPersistedCourtApplication(applicationEntity);
        var caseIds = getRelatedCaseIds(applicationOffencesUpdated.getOffenceId(), courtApplication);

        updateCourtApplication(applicationEntity, applicationOffencesUpdated);
        updateInitiateCourtApplication(initiateCourtApplicationEntity, applicationOffencesUpdated);
        if (isNotEmpty(caseIds)) {
            updateCase(caseIds, courtApplication, applicationOffencesUpdated, prosecutionCaseRepository, jsonObjectConverter, stringToJsonObjectConverter, objectToJsonObjectConverter);
        }
    }



    @Handles("progression.event.application-rep-order-updated-for-application")
    public void processApplicationRepOrderUpdatedForApplication(final JsonEnvelope event) {
        final ApplicationRepOrderUpdatedForApplication applicationRepOrderUpdatedForApplication = jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationRepOrderUpdatedForApplication.class);

        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(applicationRepOrderUpdatedForApplication.getApplicationId());
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findByApplicationId(applicationRepOrderUpdatedForApplication.getApplicationId());

        final CourtApplication courtApplication = getPersistedCourtApplication(applicationEntity);
        final List<UUID> caseIds = Optional.of(courtApplication.getSubject())
                .map(CourtApplicationParty::getMasterDefendant)
                .map(MasterDefendant::getDefendantCase)
                .orElse(Collections.emptyList())
                .stream()
                .map(DefendantCase::getCaseId)
                .toList();

        updateCourtApplication(applicationEntity, applicationRepOrderUpdatedForApplication);
        updateInitiateCourtApplication(initiateCourtApplicationEntity, applicationRepOrderUpdatedForApplication);
        if (isNotEmpty(caseIds)) {
            updateCase(caseIds, courtApplication, applicationRepOrderUpdatedForApplication.getLaaReference(), prosecutionCaseRepository, jsonObjectConverter, stringToJsonObjectConverter, objectToJsonObjectConverter);
        }
    }



    private void updateInitiateCourtApplication(InitiateCourtApplicationEntity initiateCourtApplicationEntity, ApplicationReporderOffencesUpdated applicationOffencesUpdated) {
        if (isNull(initiateCourtApplicationEntity)) {
            return;
        }

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = getPersistedInitiateCourtApplication(initiateCourtApplicationEntity);

        if (isNull(initiateCourtApplicationProceedings)) {
            return;
        }

        final CourtApplication persistedApplication = initiateCourtApplicationProceedings.getCourtApplication();
        if (!isValidSubject(persistedApplication, applicationOffencesUpdated.getSubjectId())) {
            return;
        }

        final List<CourtApplicationCase> updatedCases = updateOffences(persistedApplication, applicationOffencesUpdated);
        saveUpdatedInitiateCourtApplication(initiateCourtApplicationEntity, persistedApplication, initiateCourtApplicationProceedings, updatedCases);
    }

    private void updateInitiateCourtApplication(InitiateCourtApplicationEntity initiateCourtApplicationEntity, ApplicationRepOrderUpdatedForApplication applicationRepOrderUpdatedForApplication) {
        if (isNull(initiateCourtApplicationEntity)) {
            return;
        }

        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = getPersistedInitiateCourtApplication(initiateCourtApplicationEntity);

        if (isNull(initiateCourtApplicationProceedings)) {
            return;
        }

        final CourtApplication persistedApplication = initiateCourtApplicationProceedings.getCourtApplication();
        if (!isSubjectExistsOnApplication(persistedApplication, applicationRepOrderUpdatedForApplication.getSubjectId())) {
            return;
        }

        final CourtApplication updatedCourtApplication = CourtApplication.courtApplication()
                .withValuesFrom(persistedApplication)
                .withLaaApplnReference(applicationRepOrderUpdatedForApplication.getLaaReference())
                .build();


        final InitiateCourtApplicationProceedings updatedPersistedInitiateCourtApplication = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withValuesFrom(initiateCourtApplicationProceedings)
                .withCourtApplication(updatedCourtApplication)
                .build();
        initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedPersistedInitiateCourtApplication).toString());
        initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);

    }

    private void updateCourtApplication(final CourtApplicationEntity applicationEntity, final ApplicationReporderOffencesUpdated applicationOffencesUpdated) {
        if (isNull(applicationEntity)) {
            return;
        }

        final CourtApplication persistedApplication = getPersistedCourtApplication(applicationEntity);
        if (!isValidSubject(persistedApplication, applicationOffencesUpdated.getSubjectId())) {
            return;
        }

        final List<CourtApplicationCase> updatedCases = updateOffences(persistedApplication, applicationOffencesUpdated);
        saveUpdatedCourtApplication(applicationEntity, persistedApplication, updatedCases);
    }

    private void updateCourtApplication(final CourtApplicationEntity applicationEntity, final ApplicationRepOrderUpdatedForApplication applicationRepOrderUpdatedForApplication) {
        if (isNull(applicationEntity)) {
            return;
        }

        final CourtApplication persistedApplication = getPersistedCourtApplication(applicationEntity);
        if (!isSubjectExistsOnApplication(persistedApplication, applicationRepOrderUpdatedForApplication.getSubjectId())) {
            return;
        }

        final CourtApplication updatedCourtApplication = CourtApplication.courtApplication()
                .withValuesFrom(persistedApplication)
                .withLaaApplnReference(applicationRepOrderUpdatedForApplication.getLaaReference())
                .build();

        applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
        courtApplicationRepository.save(applicationEntity);
    }



    private CourtApplication getPersistedCourtApplication(final CourtApplicationEntity applicationEntity) {
        final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
        return jsonObjectConverter.convert(applicationJson, CourtApplication.class);
    }

    private InitiateCourtApplicationProceedings getPersistedInitiateCourtApplication(final InitiateCourtApplicationEntity initiateCourtApplicationEntity) {
        final JsonObject applicationJson = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
        return jsonObjectConverter.convert(applicationJson, InitiateCourtApplicationProceedings.class);
    }

    private boolean isValidSubject(final CourtApplication application, final UUID subjectId) {
        return isSubjectExistsOnApplication(application, subjectId) &&
                isNotEmpty(application.getCourtApplicationCases());
    }

    private boolean isSubjectExistsOnApplication(final CourtApplication application, final UUID subjectId) {
        return nonNull(application.getSubject()) &&
                application.getSubject().getId().equals(subjectId);
    }

    private List<CourtApplicationCase> updateOffences(final CourtApplication application, final ApplicationReporderOffencesUpdated updateData) {
        return application.getCourtApplicationCases().stream().filter(a -> isNotEmpty(a.getOffences()))
                .map(applicationCase -> updateCaseOffences(applicationCase, updateData))
                .toList();
    }

    private CourtApplicationCase updateCaseOffences(final CourtApplicationCase applicationCase, final ApplicationReporderOffencesUpdated updateData) {
        final List<Offence> updatedOffences = applicationCase.getOffences().stream()
                .map(offence -> offence.getId().equals(updateData.getOffenceId())
                        ? updateOffence(offence, updateData.getLaaReference())
                        : offence)
                .toList();
        return CourtApplicationCase.courtApplicationCase()
                .withValuesFrom(applicationCase)
                .withOffences(updatedOffences)
                .build();
    }

    private Offence updateOffence(final Offence offence, final LaaReference laaReference) {
        return offence()
                .withValuesFrom(offence)
                .withLaaApplnReference(laaReference)
                .build();
    }

    private void saveUpdatedCourtApplication(final CourtApplicationEntity entity, final CourtApplication application, final List<CourtApplicationCase> updatedCases) {
        entity.setPayload(objectToJsonObjectConverter.convert(getCourtApplication(application, updatedCases)).toString());
        courtApplicationRepository.save(entity);
    }

    private void saveUpdatedInitiateCourtApplication(final InitiateCourtApplicationEntity initiateCourtApplicationEntity, final CourtApplication application, final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings, final List<CourtApplicationCase> updatedCases) {
        final InitiateCourtApplicationProceedings updatedPersistedInitiateCourtApplication = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withValuesFrom(initiateCourtApplicationProceedings)
                .withCourtApplication(getCourtApplication(application, updatedCases)).build();
        initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedPersistedInitiateCourtApplication).toString());
        initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);
    }

    private CourtApplication getCourtApplication(final CourtApplication application, final List<CourtApplicationCase> updatedCases) {
        return courtApplication()
                .withValuesFrom(application)
                .withCourtApplicationCases(updatedCases)
                .build();
    }
}
