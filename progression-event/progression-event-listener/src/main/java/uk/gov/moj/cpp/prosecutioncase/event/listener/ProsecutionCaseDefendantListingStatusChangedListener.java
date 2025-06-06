package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.progression.event.util.DuplicateOffencesHelper.filterDuplicateOffencesByIdForHearing;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllApplications;
import static uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV2;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChangedV3;
import uk.gov.justice.progression.courts.CaseAddedToHearingBdf;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.event.util.DuplicateOffencesHelper;
import uk.gov.moj.cpp.progression.util.CaseHelper;
import uk.gov.moj.cpp.progression.util.ReportingRestrictionHelper;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantListingStatusChangedListener {

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private HearingApplicationRepository hearingApplicationRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.prosecutionCase-defendant-listing-status-changed")
    public void process(final JsonEnvelope event) {
        final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = dedupAllReportingRestrictions(jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChanged.class));
        final HearingEntity hearingEntity = transformHearing(prosecutionCaseDefendantListingStatusChanged.getHearing(), prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus());
        final List<ProsecutionCase> prosecutionCases = prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases();

        saveCaseDefendantHearing(prosecutionCases, hearingEntity);
        updateHearingForMatchedDefendants(prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus(), prosecutionCases, prosecutionCaseDefendantListingStatusChanged.getHearing().getId());
    }

    @Handles("progression.event.prosecutionCase-defendant-listing-status-changed-v2")
    public void processV2(final JsonEnvelope event) {
        final ProsecutionCaseDefendantListingStatusChangedV2 updatedProsecutionCaseDefendantListingStatusChanged = dedupAllApplications(jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChangedV2.class));

        final ProsecutionCaseDefendantListingStatusChangedV2 prosecutionCaseDefendantListingStatusChanged = dedupAllReportingRestrictions(updatedProsecutionCaseDefendantListingStatusChanged);
        final HearingEntity hearingEntity = transformHearing(prosecutionCaseDefendantListingStatusChanged.getHearing(), prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus());
        final List<ProsecutionCase> prosecutionCases = prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases();
        final List<CourtApplication> courtApplications = prosecutionCaseDefendantListingStatusChanged.getHearing().getCourtApplications();

        if (isNotEmpty(prosecutionCases)) {
            saveCaseDefendantHearing(prosecutionCases, hearingEntity);
            updateHearingForMatchedDefendants(prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus(), prosecutionCases, prosecutionCaseDefendantListingStatusChanged.getHearing().getId());
        } else {
            saveHearingApplication(prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus(), courtApplications, hearingEntity);
        }
    }

    @Handles("progression.event.prosecutionCase-defendant-listing-status-changed-v3")
    public void processV3(final JsonEnvelope event) {

        final ProsecutionCaseDefendantListingStatusChangedV3 updatedProsecutionCaseDefendantListingStatusChanged = dedupAllApplications(jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChangedV3.class));

        final ProsecutionCaseDefendantListingStatusChangedV3 prosecutionCaseDefendantListingStatusChanged = dedupAllReportingRestrictions(updatedProsecutionCaseDefendantListingStatusChanged);
        final HearingEntity hearingEntity = transformHearing(prosecutionCaseDefendantListingStatusChanged.getHearing(), prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus());
        final List<ProsecutionCase> prosecutionCases = prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases();

        saveCaseDefendantHearing(prosecutionCases, hearingEntity);
        updateHearingForMatchedDefendants(prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus(), prosecutionCases, prosecutionCaseDefendantListingStatusChanged.getHearing().getId());
    }

    @Handles("progression.event.case-added-to-hearing-bdf")
    public void handlerCaseAddedToHearingBdf(final JsonEnvelope event){
        final CaseAddedToHearingBdf caseAddedToHearingBdf = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseAddedToHearingBdf.class);
        List<ProsecutionCase> cases = ReportingRestrictionHelper.dedupAllReportingRestrictionsForCases(caseAddedToHearingBdf.getProsecutionCases());

        cases.forEach(DuplicateOffencesHelper::filterDuplicateOffencesByIdForCase);
        removeNowsJudicialResultsFromCase(cases);

        HearingEntity hearingEntity = hearingRepository.findBy(caseAddedToHearingBdf.getHearingId());
        final JsonObject dbHearingJsonObject = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
        Hearing hearing = jsonObjectConverter.convert(dbHearingJsonObject, Hearing.class);
        final Hearing persistedHearing = CaseHelper.addCaseToHearing(hearing, caseAddedToHearingBdf.getProsecutionCases());

        hearingEntity.setPayload(objectToJsonObjectConverter.convert(persistedHearing).toString());
        hearingRepository.save(hearingEntity);

        saveCaseDefendantHearing(persistedHearing.getProsecutionCases(), hearingEntity);
        updateHearingForMatchedDefendants(hearingEntity.getListingStatus(), persistedHearing.getProsecutionCases(), caseAddedToHearingBdf.getHearingId());
    }

    private void updateHearingForMatchedDefendants(final HearingListingStatus hearingListingStatus, final List<ProsecutionCase> prosecutionCases, final UUID hearingId) {
        if (Objects.nonNull(prosecutionCases) && hearingListingStatus == HearingListingStatus.HEARING_INITIALISED) {
            prosecutionCases.forEach(
                    prosecutionCase -> prosecutionCase.getDefendants().forEach(
                            defendant -> {
                                final List<MatchDefendantCaseHearingEntity> entities = matchDefendantCaseHearingRepository.findByProsecutionCaseIdAndDefendantId(prosecutionCase.getId(), defendant.getId());
                                entities.forEach(entity -> {
                                    entity.setHearingId(hearingId);
                                    entity.setHearing(hearingRepository.findBy(hearingId));
                                    matchDefendantCaseHearingRepository.save(entity);
                                });
                            }
                    )
            );
        }
    }

    private CaseDefendantHearingEntity transformCaseDefendantHearingEntity(final Defendant defendant, final ProsecutionCase prosecutionCase, final HearingEntity hearingEntity) {
        final CaseDefendantHearingEntity caseDefendantHearingEntity = new CaseDefendantHearingEntity();
        caseDefendantHearingEntity.setId(new CaseDefendantHearingKey(prosecutionCase.getId(), defendant.getId(), hearingEntity.getHearingId()));
        caseDefendantHearingEntity.setHearing(hearingEntity);
        return caseDefendantHearingEntity;
    }

    private HearingApplicationEntity transformHearingApplicationEntity(final CourtApplication courtApplication, final HearingEntity hearingEntity) {
        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        hearingApplicationEntity.setId(new HearingApplicationKey(courtApplication.getId(), hearingEntity.getHearingId()));
        hearingApplicationEntity.setHearing(hearingEntity);
        return hearingApplicationEntity;
    }

    private HearingEntity transformHearing(final Hearing hearing, final HearingListingStatus hearingListingStatus) {
        HearingEntity hearingEntity = hearingRepository.findBy(hearing.getId());


        // no updates should be allowed to hearing payload once its been resulted unless results are shared again
        if (hearingEntity != null && hearingEntity.getListingStatus() == HearingListingStatus.HEARING_RESULTED) {
            return hearingEntity;
        }

        if (hearingEntity == null) {
            hearingEntity = new HearingEntity();
            hearingEntity.setHearingId(hearing.getId());
        }
        filterDuplicateOffencesByIdForHearing(hearing);
        hearingEntity.setListingStatus(hearingListingStatus);
        removeNowsJudicialResultsFromApplication(hearing);
        removeNowsJudicialResultsFromCase(hearing);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        return hearingEntity;
    }

    private void saveCaseDefendantHearing(List<ProsecutionCase> prosecutionCases, final HearingEntity hearingEntity) {
        if (isNotEmpty(prosecutionCases)) {
            prosecutionCases.forEach(pc ->
                    pc.getDefendants().forEach(d ->
                            caseDefendantHearingRepository.save(transformCaseDefendantHearingEntity(d, pc, hearingEntity))
                    )
            );
        }
    }

    private void saveHearingApplication(final HearingListingStatus hearingListingStatus, final List<CourtApplication> courtApplications, final HearingEntity hearingEntity) {
        if (isNotEmpty(courtApplications)) {
            if (hearingListingStatus == HearingListingStatus.SENT_FOR_LISTING) {
                courtApplications.forEach(ca -> hearingApplicationRepository.save(transformHearingApplicationEntity(ca, hearingEntity)));
            } else {
                hearingRepository.save(hearingEntity);
            }
        }
    }

    private void removeNowsJudicialResultsFromApplication(final Hearing hearing) {
        ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .filter(Objects::nonNull)
               .forEach(courtApplication -> ofNullable(courtApplication.getJudicialResults()).orElseGet(ArrayList::new)
                    .removeIf(result -> Boolean.TRUE.equals(result.getPublishedForNows())));


    }

    private void removeNowsJudicialResultsFromCase(final Hearing hearing){
        ofNullable(hearing.getProsecutionCases()).ifPresent(this::removeNowsJudicialResultsFromCase);
    }

    private void removeNowsJudicialResultsFromCase(final List<ProsecutionCase> cases){
        cases.stream()
                .map(ProsecutionCase::getDefendants).flatMap(Collection::stream)
                .map(Defendant::getOffences).flatMap(Collection::stream)
                .filter(offence -> isNotEmpty(offence.getJudicialResults()))
                .forEach(offence ->
                        offence.getJudicialResults().removeIf(result -> Boolean.TRUE.equals(result.getPublishedForNows())));
    }
}