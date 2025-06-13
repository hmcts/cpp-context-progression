package uk.gov.moj.cpp.application.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ApplicationDefenceOrganisationChanged;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class ApplicationDefenceOrganisationChangedEventListener {
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDefenceOrganisationChangedEventListener.class);

    @Handles("progression.event.application-defence-organisation-changed")
    public void processApplicationDefenceOrganisationChanged(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.application-defence-organisation-changed {} ", event.toObfuscatedDebugString());
        }
        final ApplicationDefenceOrganisationChanged defenceOrganisationChanged = jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationDefenceOrganisationChanged.class);
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(defenceOrganisationChanged.getApplicationId());
        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(defenceOrganisationChanged.getApplicationId());

        updateCourtApplication(courtApplicationEntity, defenceOrganisationChanged);
        updateInitiateCourtApplication(initiateCourtApplicationEntity, defenceOrganisationChanged);
    }

    private void updateInitiateCourtApplication(final InitiateCourtApplicationEntity initiateCourtApplicationEntity, final ApplicationDefenceOrganisationChanged defenceOrganisationChanged) {
        if (isNull(initiateCourtApplicationEntity)) {
            return;
        }
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = getPersistedInitiateCourtApplication(initiateCourtApplicationEntity);

        if (isNull(initiateCourtApplicationProceedings)) {
            return;
        }
        final CourtApplication updatedCourtApplication = updateSubjectForApplication(defenceOrganisationChanged.getSubjectId(), defenceOrganisationChanged.getAssociatedDefenceOrganisation(), initiateCourtApplicationProceedings.getCourtApplication());
        final InitiateCourtApplicationProceedings updatedPersistedInitiateCourtApplication = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withValuesFrom(initiateCourtApplicationProceedings)
                .withCourtApplication(updatedCourtApplication).build();
        initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedPersistedInitiateCourtApplication).toString());
        initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);
    }

    private void updateCourtApplication(final CourtApplicationEntity courtApplicationEntity, final ApplicationDefenceOrganisationChanged defenceOrganisationChanged) {
        if (isNull(courtApplicationEntity)) {
            return;
        }
        final JsonObject courtApplicationJson = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
        final CourtApplication courtApplication = jsonObjectConverter.convert(courtApplicationJson, CourtApplication.class);
        final CourtApplication updatedCourtApplication = updateSubjectForApplication(defenceOrganisationChanged.getSubjectId(), defenceOrganisationChanged.getAssociatedDefenceOrganisation(), courtApplication);
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
        courtApplicationRepository.save(courtApplicationEntity);
    }

    private InitiateCourtApplicationProceedings getPersistedInitiateCourtApplication(final InitiateCourtApplicationEntity initiateCourtApplicationEntity) {
        final JsonObject applicationJson = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
        return jsonObjectConverter.convert(applicationJson, InitiateCourtApplicationProceedings.class);
    }

    private CourtApplication updateSubjectForApplication(UUID subjectId, AssociatedDefenceOrganisation associatedDefenceOrganisation, CourtApplication courtApplication) {
        if (subjectId.equals(courtApplication.getSubject().getId())) {
            final CourtApplicationParty courtApplicationParty = updateCourtApplicationParty(courtApplication.getSubject(), associatedDefenceOrganisation);
            return CourtApplication.courtApplication()
                    .withValuesFrom(courtApplication)
                    .withSubject(courtApplicationParty)
                    .build();
        }
        return courtApplication;
    }

    private CourtApplicationParty updateCourtApplicationParty(final CourtApplicationParty courtApplicationParty, final AssociatedDefenceOrganisation associatedDefenceOrganisation) {
        return CourtApplicationParty.courtApplicationParty()
                .withValuesFrom(courtApplicationParty)
                .withAssociatedDefenceOrganisation(associatedDefenceOrganisation)
                .build();

    }
}
