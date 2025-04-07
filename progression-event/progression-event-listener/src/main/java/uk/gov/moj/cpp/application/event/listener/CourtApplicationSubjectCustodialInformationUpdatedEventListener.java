package uk.gov.moj.cpp.application.event.listener;


import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationSubjectCustodialInformationUpdated;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.MasterDefendant;
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
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class CourtApplicationSubjectCustodialInformationUpdatedEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    @Inject
    private CourtApplicationRepository courtApplicationRepository;


    @Handles("progression.event.court-application-subject-custodial-information-updated")
    public void processCourtApplicationSubjectCustodialInformationUpdated(final JsonEnvelope event) {
        final CourtApplicationSubjectCustodialInformationUpdated courtApplicationSubjectCustodialInformationUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationSubjectCustodialInformationUpdated.class);
        final UUID applicationId = courtApplicationSubjectCustodialInformationUpdated.getApplicationId();
        final DefendantUpdate defendantUpdate = courtApplicationSubjectCustodialInformationUpdated.getDefendant();
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findBy(applicationId);
        if (nonNull(applicationEntity)) {

            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
            if (nonNull(persistedApplication.getSubject())) {
                final CourtApplication courtApplicationWithUpdatedCustodialInformation = buildCourtApplicationWithDefendantUpdate(persistedApplication, defendantUpdate);
                applicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplicationWithUpdatedCustodialInformation).toString());
                courtApplicationRepository.save(applicationEntity);
            }
        }

    }


    private CourtApplication buildCourtApplicationWithDefendantUpdate(final CourtApplication persistedApplication, final DefendantUpdate defendantUpdate) {

        return CourtApplication.courtApplication()
                .withValuesFrom(persistedApplication)
                .withSubject(updateApplicationSubject(persistedApplication.getSubject(),defendantUpdate))
                .build();

    }

    private CourtApplicationParty updateApplicationSubject(final CourtApplicationParty subject, final DefendantUpdate defendantUpdate) {

       return CourtApplicationParty.courtApplicationParty()
                .withValuesFrom(subject)
                .withMasterDefendant(updateMasterDefendant(subject.getMasterDefendant(), defendantUpdate))
                .build();

    }

    private MasterDefendant updateMasterDefendant(final MasterDefendant masterDefendant, final DefendantUpdate defendantUpdate) {
        return MasterDefendant.masterDefendant()
                .withValuesFrom(masterDefendant)
                .withPersonDefendant(defendantUpdate.getPersonDefendant())
                .build();

    }

}
