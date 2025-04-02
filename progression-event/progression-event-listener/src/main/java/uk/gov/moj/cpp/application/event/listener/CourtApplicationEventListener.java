package uk.gov.moj.cpp.application.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupReportingRestriction;

import uk.gov.justice.core.courts.ApplicationEjected;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationAddedToCase;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationCreated;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationProceedingsEdited;
import uk.gov.justice.core.courts.CourtApplicationProceedingsInitiated;
import uk.gov.justice.core.courts.CourtApplicationStatusChanged;
import uk.gov.justice.core.courts.CourtApplicationUpdated;
import uk.gov.justice.core.courts.CourtOrder;
import uk.gov.justice.core.courts.CourtOrderOffence;
import uk.gov.justice.core.courts.DefendantAddressOnApplicationUpdated;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingResultedApplicationUpdated;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.event.listener.HearingEntityUtil;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationCaseKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class CourtApplicationEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private SearchProsecutionCase searchApplication;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CourtApplicationCaseRepository courtApplicationCaseRepository;

    @Inject
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationEventListener.class);



    @Handles("progression.event.court-application-created")
    public void processCourtApplicationCreated(final JsonEnvelope event) {
        final CourtApplicationCreated courtApplicationCreated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationCreated.class);
        final CourtApplication courtApplication = courtApplicationCreated.getCourtApplication();
        if(nonNull(courtApplicationRepository.findBy(courtApplication.getId()))){
            return;
        }
        deDupAllOffencesForCourtApplication(courtApplication);
        final CourtApplication applicationToBeSaved = dedupAllReportingRestrictions(buildCourtApplication(courtApplication));
        courtApplicationRepository.save(getCourtApplicationEntity(applicationToBeSaved));
        makeStandaloneApplicationSearchable(applicationToBeSaved);
    }

    @Handles("progression.event.court-application-added-to-case")
    public void processCourtApplicationAddedToCase(final JsonEnvelope event) {
        final CourtApplicationAddedToCase courtApplicationAddedToCase = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationAddedToCase.class);
        final CourtApplication courtApplication = dedupAllReportingRestrictions(courtApplicationAddedToCase.getCourtApplication());
        deDupAllOffencesForCourtApplication(courtApplication);
        final Set<UUID> caseIds = new HashSet<>();
        if (nonNull(courtApplication.getCourtApplicationCases())) {
            courtApplication.getCourtApplicationCases().forEach(courtApplicationCase -> {
                final String caseReference = isNotBlank(courtApplicationCase.getProsecutionCaseIdentifier().getCaseURN()) ?
                        courtApplicationCase.getProsecutionCaseIdentifier().getCaseURN() : courtApplicationCase.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
                if (!caseIds.contains(courtApplicationCase.getProsecutionCaseId())) {
                    addCourtApplicationToCase(courtApplication, courtApplicationCase.getProsecutionCaseId(), caseReference);
                    caseIds.add(courtApplicationCase.getProsecutionCaseId());
                }
            });
        }

        if (nonNull(courtApplication.getCourtOrder())) {
            courtApplication.getCourtOrder().getCourtOrderOffences().forEach(courtOrderOffence -> {
                final String courtReference = isNotBlank(courtOrderOffence.getProsecutionCaseIdentifier().getCaseURN()) ?
                        courtOrderOffence.getProsecutionCaseIdentifier().getCaseURN() : courtOrderOffence.getProsecutionCaseIdentifier().getProsecutionAuthorityReference();
                if (!caseIds.contains(courtOrderOffence.getProsecutionCaseId())) {
                    addCourtApplicationToCase(courtApplication, courtOrderOffence.getProsecutionCaseId(), courtReference);
                    caseIds.add(courtOrderOffence.getProsecutionCaseId());
                }
            });
        }
    }

    @Handles("progression.event.court-application-status-changed")
    public void processCourtApplicationStatusChanged(final JsonEnvelope event) {
        final CourtApplicationStatusChanged courtApplicationStatusChanged = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationStatusChanged.class);
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findBy(courtApplicationStatusChanged.getId());
        if (nonNull(applicationEntity)) {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
            final ApplicationStatus applicationStatus = persistedApplication.getApplicationStatus() == ApplicationStatus.FINALISED ? ApplicationStatus.FINALISED : courtApplicationStatusChanged.getApplicationStatus();
            final CourtApplication updatedApplication = buildCourtApplicationWithStatus(persistedApplication, applicationStatus);
            applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedApplication).toString());
            courtApplicationRepository.save(applicationEntity);
        }
    }

    @Handles("progression.event.court-application-updated")
    public void processCourtApplicationUpdated(final JsonEnvelope event) {
        final CourtApplicationUpdated courtApplicationUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationUpdated.class);
        final CourtApplication updatedCourtApplication = dedupAllReportingRestrictions(courtApplicationUpdated.getCourtApplication());
        deDupAllOffencesForCourtApplication(updatedCourtApplication);
        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(courtApplicationUpdated.getCourtApplication().getId());
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedCourtApplication).toString());
        courtApplicationRepository.save(courtApplicationEntity);
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(courtApplicationUpdated.getCourtApplication().getId());
        if (nonNull(initiateCourtApplicationEntity)) {
            final JsonObject initiateCourtApplicationJson = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
            final InitiateCourtApplicationProceedings persistedInitiateCourtApplication = jsonObjectConverter.convert(initiateCourtApplicationJson, InitiateCourtApplicationProceedings.class);
            final InitiateCourtApplicationProceedings updatedPersistedInitiateCourtApplication = initiateCourtApplicationProceedingsBuilder(persistedInitiateCourtApplication)
                    .withCourtApplication(updatedCourtApplication).build();
            initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedPersistedInitiateCourtApplication).toString());
            initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);
        }
        makeStandaloneApplicationSearchable(updatedCourtApplication);
    }

    @Handles("progression.event.application-ejected")
    public void processCourtApplicationEjected(final JsonEnvelope event) {
        final ApplicationEjected applicationEjected = jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationEjected.class);
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findByApplicationId(applicationEjected.getApplicationId());
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(applicationEjected.getApplicationId());
        saveEjectedApplication(applicationEjected, applicationEntity,initiateCourtApplicationEntity);
        final List<CourtApplicationEntity> childApplications = courtApplicationRepository.findByParentApplicationId(applicationEjected.getApplicationId());
        childApplications.stream().filter(Objects::nonNull).forEach(childApplicationEntity -> saveEjectedApplication(applicationEjected, childApplicationEntity,initiateCourtApplicationEntity));
    }

    @Handles("progression.event.court-application-proceedings-initiated")
    public void processCourtApplicationProceedingsInitiated(final JsonEnvelope event) {
        final CourtApplicationProceedingsInitiated initiateCourtApplicationProceedings = dedupReportingRestriction(jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsInitiated.class));
        if(nonNull(initiateCourtApplicationRepository.findBy(initiateCourtApplicationProceedings.getCourtApplication().getId()))){
            return;
        }
        deDupAllOffencesForCourtApplication(initiateCourtApplicationProceedings.getCourtApplication());
        initiateCourtApplicationRepository.save(getInitiateCourtApplication(initiateCourtApplicationProceedings));
    }

    @Handles("progression.event.court-application-proceedings-edited")
    public void processCourtApplicationProceedingsEdited(final JsonEnvelope event) {
        final CourtApplicationProceedingsEdited initiateCourtApplicationProceedings = dedupReportingRestriction(jsonObjectConverter.convert(event.payloadAsJsonObject(), CourtApplicationProceedingsEdited.class));
        deDupAllOffencesForCourtApplication(initiateCourtApplicationProceedings.getCourtApplication());
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(initiateCourtApplicationProceedings.getCourtApplication().getId());
        initiateCourtApplicationRepository.save(updateInitiateCourtApplication(initiateCourtApplicationProceedings, initiateCourtApplicationEntity));

        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(initiateCourtApplicationProceedings.getCourtApplication().getId());
        courtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(initiateCourtApplicationProceedings.getCourtApplication()).toString());
        courtApplicationRepository.save(courtApplicationEntity);
        makeStandaloneApplicationSearchable(initiateCourtApplicationProceedings.getCourtApplication());
    }

    @Handles("progression.event.hearing-resulted-application-updated")
    public void processHearingResultedApplicationUpdated(final JsonEnvelope event) {
        final HearingResultedApplicationUpdated hearingResultedApplicationUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingResultedApplicationUpdated.class);
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findBy(hearingResultedApplicationUpdated.getCourtApplication().getId());
        if (nonNull(applicationEntity)) {
            final CourtApplication updatedCourtApplication = dedupAllReportingRestrictions(hearingResultedApplicationUpdated.getCourtApplication());
            deDupAllOffencesForCourtApplication(updatedCourtApplication);
            final CourtApplication courtApplicationWithoutNows = getCourtApplicationCourtApplicationWithOutNowResults(updatedCourtApplication);
            applicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplicationWithoutNows).toString());
            courtApplicationRepository.save(applicationEntity);
        }
    }


    @Handles("progression.event.defendant-address-on-application-updated")
    public void processDefendantAddressOnApplicationUpdated(final JsonEnvelope event) {
        final DefendantAddressOnApplicationUpdated defendantAddressOnApplicationUpdated = jsonObjectConverter.convert(event.payloadAsJsonObject(), DefendantAddressOnApplicationUpdated.class);
        final UUID applicationId = defendantAddressOnApplicationUpdated.getApplicationId();
        final DefendantUpdate defendant = defendantAddressOnApplicationUpdated.getDefendant();
        if(LOGGER.isInfoEnabled()){
            LOGGER.info("Defendant address is being updated for application id : {}, defendantId : {}, masterDefendantId : {}", applicationId, defendant.getId(),defendant.getMasterDefendantId());
        }
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findBy(applicationId);
        if (nonNull(applicationEntity)) {
            if(LOGGER.isInfoEnabled()){
                LOGGER.info("Application found in database applicationId : {}, defendantId : {}, masterDefendantId : {}", applicationId, defendant.getId(),defendant.getMasterDefendantId());
            }
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
            final CourtApplication courtApplicationWithDefendantAddress = buildCourtApplicationWithDefendantUpdate(persistedApplication, defendant);
            applicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplicationWithDefendantAddress).toString());
            courtApplicationRepository.save(applicationEntity);
            final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(applicationId);
            if (nonNull(initiateCourtApplicationEntity)) {
                final JsonObject initiateCourtApplicationJson = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
                final InitiateCourtApplicationProceedings persistedInitiateCourtApplication = jsonObjectConverter.convert(initiateCourtApplicationJson, InitiateCourtApplicationProceedings.class);
                final InitiateCourtApplicationProceedings updatedPersistedInitiateCourtApplication = initiateCourtApplicationProceedingsBuilder(persistedInitiateCourtApplication)
                        .withCourtApplication(courtApplicationWithDefendantAddress).build();
                initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedPersistedInitiateCourtApplication).toString());
                initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);
            }
        }
    }

    private CourtApplication buildCourtApplicationWithDefendantUpdate(final CourtApplication persistedApplication, final DefendantUpdate defendant) {
        final boolean isDefendantOrganisation = nonNull(defendant.getLegalEntityDefendant());
        final UUID updatedDefendantId = ofNullable(defendant.getMasterDefendantId()).orElse(defendant.getId());
        final CourtApplication.Builder courtApplication = CourtApplication.courtApplication().withValuesFrom(persistedApplication);
        updateApplicantWithUpdatedAddress(persistedApplication, defendant, isDefendantOrganisation, courtApplication, updatedDefendantId);
        updateSubjectWithUpdatedAddress(persistedApplication, defendant, isDefendantOrganisation, courtApplication, updatedDefendantId);
        updateRespondentsWithUpdatedAddress(persistedApplication, defendant, isDefendantOrganisation, courtApplication, updatedDefendantId);
        return courtApplication.build();
    }

    private static void updateRespondentsWithUpdatedAddress(final CourtApplication persistedApplication, final DefendantUpdate defendant,
                                                            final boolean isDefendantOrganisation, final CourtApplication.Builder courtApplication, final UUID updatedDefendantId) {
        if(null!=persistedApplication.getRespondents()) {
            final Optional<CourtApplicationParty> updatedRespondent = persistedApplication.getRespondents().stream()
                    .filter(resp -> nonNull(resp.getMasterDefendant()) && resp.getMasterDefendant().getMasterDefendantId()
                            .equals(updatedDefendantId)).findFirst();

            if (updatedRespondent.isPresent()) {
                if(LOGGER.isInfoEnabled()){
                    LOGGER.info("Match found for updated Defendant in Application Respondents, defendant: {} in application: {}", updatedDefendantId, persistedApplication.getId());
                }
                final List<CourtApplicationParty> courtApplicationRespondentsList = new ArrayList<>();
                persistedApplication.getRespondents().stream()
                        .filter(resp -> isNull(resp.getMasterDefendant()) || !resp.getMasterDefendant().getMasterDefendantId().equals(updatedDefendantId))
                        .forEach(courtApplicationRespondentsList::add);

                if (!isDefendantOrganisation) {
                    courtApplicationRespondentsList.add(CourtApplicationParty.courtApplicationParty()
                            .withValuesFrom(updatedRespondent.get())
                            .withMasterDefendant(buildPersonDefendant(updatedRespondent.get().getMasterDefendant(), defendant))
                            .withUpdatedOn(LocalDate.now())
                            .build());
                } else {
                    courtApplicationRespondentsList.add(CourtApplicationParty.courtApplicationParty()
                            .withValuesFrom(updatedRespondent.get())
                            .withMasterDefendant(buildOrganisationDefendant(updatedRespondent.get().getMasterDefendant(), defendant))
                            .withUpdatedOn(LocalDate.now())
                            .build());
                }
                courtApplication.withRespondents(courtApplicationRespondentsList);
                if(LOGGER.isInfoEnabled()){
                    LOGGER.info("Updated address in Application Respondents for defendant: {} in application: {}", updatedDefendantId, persistedApplication.getId());
                }
            }
        }
    }

    private static void updateSubjectWithUpdatedAddress(final CourtApplication persistedApplication, final DefendantUpdate defendant,
                                                        final boolean isDefendantOrganisation, final CourtApplication.Builder courtApplication, final UUID updatedDefendantId) {
        if(null!=persistedApplication.getSubject() && null!= persistedApplication.getSubject().getMasterDefendant() &&
                updatedDefendantId.equals(persistedApplication.getSubject().getMasterDefendant().getMasterDefendantId())){
            if(LOGGER.isInfoEnabled()){
                LOGGER.info("Match found for updated Defendant in Application Subject for defendant: {} in application: {}", updatedDefendantId, persistedApplication.getId());
            }
            if(!isDefendantOrganisation){
                courtApplication.withSubject(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getSubject())
                        .withMasterDefendant(buildPersonDefendant(persistedApplication.getSubject().getMasterDefendant(), defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
                if(LOGGER.isInfoEnabled()){
                    LOGGER.info("Person defendant updated with new address for defendant: {} in application: {}", updatedDefendantId, persistedApplication.getId());
                }
            }else{
                courtApplication.withSubject(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getSubject())
                        .withMasterDefendant(buildOrganisationDefendant(persistedApplication.getSubject().getMasterDefendant() , defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
                if(LOGGER.isInfoEnabled()){
                    LOGGER.info("Org defendant updated with new address for defendant: {} in application: {}", updatedDefendantId, persistedApplication.getId());
                }
            }
        }
    }

    private static void updateApplicantWithUpdatedAddress(final CourtApplication persistedApplication, final DefendantUpdate defendant,
                                                          final boolean isDefendantOrganisation, final CourtApplication.Builder courtApplication, final UUID updatedDefendantId) {
        if(null!=persistedApplication.getApplicant() && null!= persistedApplication.getApplicant().getMasterDefendant() &&
                updatedDefendantId.equals(persistedApplication.getApplicant().getMasterDefendant().getMasterDefendantId())){
            if(LOGGER.isInfoEnabled()){
                LOGGER.info("Match found for updated Defendant in Application : {}, , defendantId : {}", persistedApplication.getId(), updatedDefendantId);
            }
            if(!isDefendantOrganisation){
                courtApplication.withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getApplicant())
                        .withMasterDefendant(buildPersonDefendant(persistedApplication.getApplicant().getMasterDefendant(), defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
                if(LOGGER.isInfoEnabled()){
                    LOGGER.info("Person defendant updated with new address for defendant : {} in application: {}", updatedDefendantId, persistedApplication.getId());
                }
            }else{
                courtApplication.withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withValuesFrom(persistedApplication.getApplicant())
                        .withMasterDefendant(buildOrganisationDefendant(persistedApplication.getApplicant().getMasterDefendant(), defendant))
                        .withUpdatedOn(LocalDate.now())
                        .build());
                if(LOGGER.isInfoEnabled()){
                    LOGGER.info("Org defendant updated with new address for defendant: {} in application: {}", updatedDefendantId, persistedApplication.getId());
                }
            }
        }
    }

    private static MasterDefendant buildOrganisationDefendant(final MasterDefendant masterDefendant, final DefendantUpdate defendant) {
       return MasterDefendant.masterDefendant()
                .withValuesFrom(masterDefendant)
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withValuesFrom(masterDefendant.getLegalEntityDefendant())
                        .withOrganisation(Organisation.organisation()
                                .withValuesFrom(masterDefendant.getLegalEntityDefendant().getOrganisation())
                                .withAddress(defendant.getLegalEntityDefendant().getOrganisation().getAddress())
                                .build()).build()).build();
    }

    private static MasterDefendant buildPersonDefendant(final MasterDefendant masterDefendant, final DefendantUpdate defendant) {
        return MasterDefendant.masterDefendant()
                .withValuesFrom(masterDefendant)
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withValuesFrom(masterDefendant.getPersonDefendant())
                        .withPersonDetails(Person.person()
                                .withValuesFrom(masterDefendant.getPersonDefendant().getPersonDetails())
                                .withAddress(defendant.getPersonDefendant().getPersonDetails().getAddress())
                                .build()).build()).build();
    }

    private InitiateCourtApplicationEntity updateInitiateCourtApplication(final CourtApplicationProceedingsEdited initiateCourtApplicationProceedings, final InitiateCourtApplicationEntity courtApplicationEntity) {
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(initiateCourtApplicationProceedings.getCourtApplication().getId());
        initiateCourtApplicationEntity.setParentApplicationId(initiateCourtApplicationProceedings.getCourtApplication().getParentApplicationId());
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedingsEdited = InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withCourtApplication(initiateCourtApplicationProceedings.getCourtApplication())
                .withBoxHearing(initiateCourtApplicationProceedings.getBoxHearing())
                .withSummonsApprovalRequired(initiateCourtApplicationProceedings.getSummonsApprovalRequired())
                .withCourtHearing(this.jsonObjectConverter.convert(this.stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload()),
                        InitiateCourtApplicationProceedings.class).getCourtHearing()).build();
        initiateCourtApplicationEntity.setPayload(this.objectToJsonObjectConverter.convert(initiateCourtApplicationProceedingsEdited).toString());
        return initiateCourtApplicationEntity;
    }

    private InitiateCourtApplicationEntity getInitiateCourtApplication(final CourtApplicationProceedingsInitiated initiateCourtApplicationProceedings) {
        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = new InitiateCourtApplicationEntity();
        initiateCourtApplicationEntity.setApplicationId(initiateCourtApplicationProceedings.getCourtApplication().getId());
        initiateCourtApplicationEntity.setParentApplicationId(initiateCourtApplicationProceedings.getCourtApplication().getParentApplicationId());
        initiateCourtApplicationEntity.setPayload(this.objectToJsonObjectConverter.convert(initiateCourtApplicationProceedings).toString());
        return initiateCourtApplicationEntity;
    }

    private void addCourtApplicationToCase(final CourtApplication courtApplication, final UUID prosecutionCaseId, final String caseReference) {
        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findBy(courtApplication.getId());
        if (nonNull(courtApplicationEntity)) {
            final CourtApplicationCaseEntity courtApplicationCaseEntity = new CourtApplicationCaseEntity();
            final CourtApplicationCaseKey courtApplicationCaseKey = new CourtApplicationCaseKey(randomUUID(), courtApplication.getId(), prosecutionCaseId);
            courtApplicationCaseEntity.setId(courtApplicationCaseKey);
            courtApplicationCaseEntity.setCourtApplication(courtApplicationEntity);
            courtApplicationCaseEntity.setCaseReference(caseReference);
            courtApplicationCaseRepository.save(courtApplicationCaseEntity);
        }
    }

    private void saveEjectedApplication(final ApplicationEjected applicationEjected, final CourtApplicationEntity applicationEntity, final InitiateCourtApplicationEntity initiateCourtApplicationEntity) {
        if (nonNull(applicationEntity)) {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectConverter.convert(applicationJson, CourtApplication.class);
            final CourtApplication updatedApplication = courtApplicationBuilder(persistedApplication)
                    .withApplicationStatus(ApplicationStatus.EJECTED)
                    .withRemovalReason(applicationEjected.getRemovalReason()).build();
            applicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedApplication).toString());
            courtApplicationRepository.save(applicationEntity);

            final JsonObject initiateCourtApplicationJson = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
            final InitiateCourtApplicationProceedings persistedInitiateCourtApplication = jsonObjectConverter.convert(initiateCourtApplicationJson, InitiateCourtApplicationProceedings.class);
            final InitiateCourtApplicationProceedings updatedpersistedInitiateCourtApplication = initiateCourtApplicationProceedingsBuilder(persistedInitiateCourtApplication)
                    .withCourtApplication(updatedApplication).build();
            initiateCourtApplicationEntity.setPayload(objectToJsonObjectConverter.convert(updatedpersistedInitiateCourtApplication).toString());
            initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);

            final List<HearingApplicationEntity> hearingApplicationEntityList = hearingApplicationRepository.findByApplicationId(applicationEjected.getApplicationId());
            hearingApplicationEntityList.forEach(hearingApplicationEntity -> {
                final HearingEntity hearingEntity = hearingApplicationEntity.getHearing();
                final UUID applicationId = hearingApplicationEntity.getId().getApplicationId();
                final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
                final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);
                final Hearing updatedHearing = HearingEntityUtil.updateHearingWithApplication(hearing, applicationId);
                hearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
                hearingRepository.save(hearingEntity);
            });
        }
    }

    private CourtApplicationEntity getCourtApplicationEntity(final CourtApplication courtApplication) {
        final CourtApplicationEntity applicationEntity = new CourtApplicationEntity();
        applicationEntity.setApplicationId(courtApplication.getId());
        applicationEntity.setParentApplicationId(courtApplication.getParentApplicationId());
        applicationEntity.setPayload(objectToJsonObjectConverter.convert(courtApplication).toString());
        return applicationEntity;
    }

    private void makeStandaloneApplicationSearchable(final CourtApplication courtApplication) {
        if (CollectionUtils.isEmpty(courtApplication.getCourtApplicationCases())) {
            searchApplication.makeApplicationSearchable(courtApplication);
        }
    }

    private CourtApplication buildCourtApplicationWithStatus(CourtApplication courtApplication, ApplicationStatus applicationStatus) {
        return courtApplicationBuilder(courtApplication).withApplicationStatus(applicationStatus).build();
    }

    private CourtApplication buildCourtApplication(CourtApplication courtApplication) {
        return courtApplicationBuilder(courtApplication).build();
    }

    private CourtApplication.Builder courtApplicationBuilder(CourtApplication courtApplication) {
        return CourtApplication.courtApplication()
                .withValuesFrom(courtApplication)
                .withApplicationStatus(ApplicationStatus.DRAFT);
    }

    private InitiateCourtApplicationProceedings.Builder initiateCourtApplicationProceedingsBuilder(InitiateCourtApplicationProceedings initiateCourtApplicationProceedings) {
        return InitiateCourtApplicationProceedings.initiateCourtApplicationProceedings()
                .withValuesFrom(initiateCourtApplicationProceedings);
    }

    private void deDupAllOffencesForCourtApplication(final CourtApplication courtApplication) {

        if (courtApplication.getCourtApplicationCases() != null) {
            courtApplication.getCourtApplicationCases().stream().forEach(apCase -> filterDuplicateOffencesById(apCase.getOffences()));
        }
    }

    private void filterDuplicateOffencesById(final List<Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
        LOGGER.info("Removing duplicate offence, offences count:{} and offences count after filtering:{} ", offences.size(), offenceIds.size());
    }

    private CourtApplication getCourtApplicationCourtApplicationWithOutNowResults(final CourtApplication courtApplication) {
        return CourtApplication.courtApplication()
                .withValuesFrom(courtApplication)
                .withJudicialResults(getNonNowsResults(courtApplication.getJudicialResults()))
                .withCourtApplicationCases(getCourtApplicationCasesWithOutNowResults(courtApplication))
                .withCourtOrder(ofNullable(courtApplication.getCourtOrder()).map(courtOrder ->
                        CourtOrder.courtOrder().withValuesFrom(courtOrder)
                                .withCourtOrderOffences(courtOrder.getCourtOrderOffences().stream()
                                        .map(courtOrderOffence -> CourtOrderOffence.courtOrderOffence()
                                                .withValuesFrom(courtOrderOffence)
                                                .withOffence(getOffenceWithoutNowResults(courtOrderOffence.getOffence()))
                                                .build())
                                        .collect(toList()))
                                .build()).orElse(null))
                .build();
    }


    private List<CourtApplicationCase> getCourtApplicationCasesWithOutNowResults(final CourtApplication courtApplication) {
        return ofNullable(courtApplication.getCourtApplicationCases()).map(Collection::stream).orElseGet(Stream::empty)
                .map(courtApplicationCase -> CourtApplicationCase.courtApplicationCase()
                        .withValuesFrom(courtApplicationCase)
                        .withOffences(ofNullable(courtApplicationCase.getOffences()).map(Collection::stream).orElseGet(Stream::empty)
                                .map(this::getOffenceWithoutNowResults)
                                .collect(Collectors.collectingAndThen(toList(), list -> list.isEmpty() ? null : list)))
                        .build())
                .collect(Collectors.collectingAndThen(toList(), list -> list.isEmpty() ? null : list));
    }

    private Offence getOffenceWithoutNowResults(final Offence offence) {
        return Offence.offence().withValuesFrom(offence)
                .withJudicialResults(getNonNowsResults(offence.getJudicialResults()))
                .build();
    }

    private List<JudicialResult> getNonNowsResults(final List<JudicialResult> judicialResults) {
        if (isNull(judicialResults) || judicialResults.isEmpty()) {
            return judicialResults;
        }

        return judicialResults.stream()
                .filter(Objects::nonNull)
                .filter(jr -> !Boolean.TRUE.equals(jr.getPublishedForNows()))
                .collect(Collectors.collectingAndThen(toList(), list -> list.isEmpty() ? null : list));
    }
}
