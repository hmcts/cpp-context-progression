package uk.gov.moj.cpp.application.event.listener;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static javax.json.Json.createReader;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import javax.json.JsonReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ApplicationOffencesUpdated;
import uk.gov.justice.core.courts.ApplicationReporderOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.events.ApplicationLaaReferenceUpdatedForHearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import static java.util.Objects.nonNull;
import static uk.gov.justice.core.courts.Offence.offence;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.application.event.listener.ApplicationHelper.getPersistedCourtApplication;
import static uk.gov.moj.cpp.application.event.listener.ApplicationHelper.getRelatedCaseIds;
import static uk.gov.moj.cpp.application.event.listener.ApplicationHelper.updateCase;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ApplicationOffencesUpdatedEventListener {
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtApplicationRepository repository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Inject
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Inject
    private HearingRepository hearingRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationOffencesUpdatedEventListener.class);


    @Handles("progression.event.application-offences-updated")
    public void processApplicationOffencesUpdated(final JsonEnvelope event) {
        final ApplicationOffencesUpdated applicationOffencesUpdated =
                jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationOffencesUpdated.class);
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(applicationOffencesUpdated.getApplicationId());
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findByApplicationId(applicationOffencesUpdated.getApplicationId());

        CourtApplication courtApplication = getPersistedCourtApplication(applicationEntity, jsonObjectToObjectConverter, stringToJsonObjectConverter);
        var caseIds = getRelatedCaseIds(applicationOffencesUpdated.getOffenceId(), courtApplication);

        updateCourtApplication(applicationEntity, applicationOffencesUpdated);

        updateInitiateCourtApplication(initiateCourtApplicationEntity, applicationOffencesUpdated);
        if (isNotEmpty(caseIds)) {
            updateCase(caseIds, courtApplication, applicationOffencesUpdated, prosecutionCaseRepository, jsonObjectToObjectConverter, stringToJsonObjectConverter, objectToJsonObjectConverter);
        }

    }

    @Handles("progression.event.application-laa-reference-updated-for-hearing")
    public  void updateApplicationLaaReferenceForHearing(final JsonEnvelope event){
        final ApplicationLaaReferenceUpdatedForHearing applicationLaaReferenceUpdatedForHearing =
                jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationLaaReferenceUpdatedForHearing.class);
        HearingEntity hearingEntity = hearingRepository.findBy(applicationLaaReferenceUpdatedForHearing.getHearingId());
        if (nonNull(hearingEntity)) {
            final JsonObject dbHearingJsonObject = jsonFromString(hearingEntity.getPayload());
            Hearing dbHearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);
            final Hearing updatedHearing = getUpdatedHearing(dbHearing, applicationLaaReferenceUpdatedForHearing);
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
            hearingRepository.save(hearingEntity);
        }
    }


    private Hearing getUpdatedHearing(Hearing hearing, ApplicationLaaReferenceUpdatedForHearing applicationLaaReferenceUpdatedForHearing) {
        List<CourtApplication> updatedCourtApplications = hearing.getCourtApplications().stream()
                .map(courtApplication -> {
                    if (courtApplication.getId().equals(applicationLaaReferenceUpdatedForHearing.getApplicationId()) &&
                            !isNull(courtApplication.getSubject()) &&
                            courtApplication.getSubject().getId().equals(applicationLaaReferenceUpdatedForHearing.getSubjectId())) {
                        List<CourtApplicationCase> updatedCases = getUpdatedCases(courtApplication, applicationLaaReferenceUpdatedForHearing.getOffenceId(), applicationLaaReferenceUpdatedForHearing.getLaaReference());
                        return CourtApplication.courtApplication()
                                .withValuesFrom(courtApplication)
                                .withCourtApplicationCases(updatedCases)
                                .build();
                    }
                    return courtApplication;
                })
                .toList();

        return Hearing.hearing()
                .withValuesFrom(hearing)
                .withCourtApplications(updatedCourtApplications)
                .build();
    }

    private static List<CourtApplicationCase> getUpdatedCases(CourtApplication courtApplication, UUID offenceId, LaaReference laaReference) {
        return courtApplication.getCourtApplicationCases().stream()
                .map(applicationCase -> {
                    List<Offence> updatedOffences = applicationCase.getOffences().stream()
                            .map(offence -> {
                                if (offence.getId().equals(offenceId)) {
                                    return Offence.offence()
                                            .withValuesFrom(offence)
                                            .withLaaApplnReference(laaReference)
                                            .build();
                                }
                                return offence;
                            })
                            .toList();
                    return CourtApplicationCase.courtApplicationCase()
                            .withValuesFrom(applicationCase)
                            .withOffences(updatedOffences)
                            .build();
                })
                .toList();
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private void updateInitiateCourtApplication(InitiateCourtApplicationEntity initiateCourtApplicationEntity, ApplicationOffencesUpdated applicationOffencesUpdated) {
        if(nonNull(initiateCourtApplicationEntity)){
            final JsonObject initiateCourtApplicationJson = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
            final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = jsonObjectToObjectConverter.convert(initiateCourtApplicationJson, InitiateCourtApplicationProceedings.class);
            final CourtApplication persistedInitiateCourtApplication = initiateCourtApplicationProceedings.getCourtApplication();
            if (nonNull(persistedInitiateCourtApplication.getSubject()) && persistedInitiateCourtApplication.getSubject().getId().equals(applicationOffencesUpdated.getSubjectId()) && isNotEmpty(persistedInitiateCourtApplication.getCourtApplicationCases())) {
                List<CourtApplicationCase> updatedCasesForInitiateCourtApplication = getUpdatedCases(persistedInitiateCourtApplication, applicationOffencesUpdated.getOffenceId(), applicationOffencesUpdated.getLaaReference());
                CourtApplication updatedCourtApplication = CourtApplication.courtApplication().withValuesFrom(persistedInitiateCourtApplication).withCourtApplicationCases(updatedCasesForInitiateCourtApplication).build();
                final InitiateCourtApplicationProceedings updatedPersistedInitiateCourtApplication = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings().withValuesFrom(initiateCourtApplicationProceedings)
                        .withCourtApplication(updatedCourtApplication).build();
                initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedPersistedInitiateCourtApplication).toString());
                initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);
            }
        }
    }

    private void updateCourtApplication(CourtApplicationEntity applicationEntity, ApplicationOffencesUpdated applicationOffencesUpdated) {
        if (nonNull(applicationEntity)) {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class);

            if (nonNull(persistedApplication.getSubject()) && persistedApplication.getSubject().getId().equals(applicationOffencesUpdated.getSubjectId()) && isNotEmpty(persistedApplication.getCourtApplicationCases())) {
                List<CourtApplicationCase> updatedCasesForCourtApplication = getUpdatedCases(persistedApplication, applicationOffencesUpdated.getOffenceId(), applicationOffencesUpdated.getLaaReference());
                CourtApplication updatedCourtApplication = CourtApplication.courtApplication().withValuesFrom(persistedApplication).withCourtApplicationCases(updatedCasesForCourtApplication).build();
                applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
                courtApplicationRepository.save(applicationEntity);
            }
        }
    }
}
