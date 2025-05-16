package uk.gov.moj.cpp.application.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtApplicationStatusUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class CourtApplicationsUpdatedEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;


    @Handles("progression.event.court-application-status-updated")
    public void processApplicationStatusUpdated(final JsonEnvelope event) {
        final CourtApplicationStatusUpdated payload = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationStatusUpdated.class);
        final CourtApplication courtApplication = payload.getCourtApplication();
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findByApplicationId(courtApplication.getId());

        final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
        final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
        final CourtApplication updatedApplication = CourtApplication.courtApplication()
                .withValuesFrom(persistedApplication)
                .withApplicationStatus(courtApplication.getApplicationStatus())
                .build();
        applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedApplication).toString());
        courtApplicationRepository.save(applicationEntity);

    }

}
