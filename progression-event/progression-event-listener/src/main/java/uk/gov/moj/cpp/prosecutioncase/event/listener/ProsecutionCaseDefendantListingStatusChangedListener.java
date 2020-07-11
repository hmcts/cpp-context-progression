package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseDefendantListingStatusChanged;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantHearingKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.MatchDefendantCaseHearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.MatchDefendantCaseHearingRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseDefendantListingStatusChangedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Inject
    private MatchDefendantCaseHearingRepository matchDefendantCaseHearingRepository;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.prosecutionCase-defendant-listing-status-changed")
    public void process(final JsonEnvelope event) {
        final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged = jsonObjectConverter.convert(event.payloadAsJsonObject(), ProsecutionCaseDefendantListingStatusChanged.class);
        final HearingEntity hearingEntity = transformHearing(prosecutionCaseDefendantListingStatusChanged.getHearing(), prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus());
        if (prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases() != null && !prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().isEmpty()) {
            prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases().forEach(pc ->
                    pc.getDefendants().forEach(d ->
                            caseDefendantHearingRepository.save(transformCaseDefendantHearingEntity(d, pc, hearingEntity))
                    )
            );
        }
        updateHearingForMatchedDefendants(prosecutionCaseDefendantListingStatusChanged);
    }

    private void updateHearingForMatchedDefendants(final ProsecutionCaseDefendantListingStatusChanged prosecutionCaseDefendantListingStatusChanged) {
        final UUID hearingId = prosecutionCaseDefendantListingStatusChanged.getHearing().getId();
        final List<ProsecutionCase> prosecutionCases = prosecutionCaseDefendantListingStatusChanged.getHearing().getProsecutionCases();
         if (Objects.nonNull(prosecutionCases) && prosecutionCaseDefendantListingStatusChanged.getHearingListingStatus() == HearingListingStatus.HEARING_INITIALISED) {
            prosecutionCases.forEach(
                    prosecutionCase -> prosecutionCase.getDefendants().forEach(
                            defendant -> {
                                final MatchDefendantCaseHearingEntity entity = matchDefendantCaseHearingRepository.findByDefendantId(defendant.getId());
                                if (Objects.nonNull(entity)) {
                                    entity.setHearingId(hearingId);
                                    entity.setHearing(hearingRepository.findBy(hearingId));
                                    matchDefendantCaseHearingRepository.save(entity);
                                }
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

    private HearingEntity transformHearing(final Hearing hearing, final HearingListingStatus hearingListingStatus) {
        HearingEntity hearingEntity = hearingRepository.findBy(hearing.getId());
        if (hearingEntity == null) {
            hearingEntity = new HearingEntity();
            hearingEntity.setHearingId(hearing.getId());
        }
        hearingEntity.setListingStatus(hearingListingStatus);
        hearingEntity.setPayload(objectToJsonObjectConverter.convert(getUpdatedHearing(hearing)).toString());
        return hearingEntity;
    }

    private Hearing getUpdatedHearing(final Hearing hearing) {
        Optional.ofNullable(hearing.getCourtApplications()).ifPresent(
                courtApplications -> courtApplications.stream().filter(Objects::nonNull).forEach(ProsecutionCaseDefendantListingStatusChangedListener::getCourtApplicationJudResultsforNonNows
                )
        );

        Optional.ofNullable(hearing.getProsecutionCases()).ifPresent(
                prosecutionCases -> prosecutionCases.stream().filter(Objects::nonNull).forEach(ProsecutionCaseDefendantListingStatusChangedListener::getProsecutionCaseJudResultsforNonNows));

        return hearing;
    }

    private static void getCourtApplicationJudResultsforNonNows(CourtApplication courtApplication) {
        ofNullable(courtApplication.getJudicialResults()).ifPresent(
                judicialResults -> {
                    final List<JudicialResult> caJudicialResults = judicialResults.stream()
                            .filter(Objects::nonNull)
                            .filter(jr -> !Boolean.TRUE.equals(jr.getPublishedForNows()))
                            .collect(Collectors.toList());
                    courtApplication.getJudicialResults().clear();
                    courtApplication.getJudicialResults().addAll(caJudicialResults);
                }
        );
    }

    private static void getProsecutionCaseJudResultsforNonNows(ProsecutionCase prosecutionCase) {
        Optional.ofNullable(prosecutionCase.getDefendants()).ifPresent(
                defendants -> defendants.stream().filter(Objects::nonNull).forEach(defendant -> {
                    Optional.ofNullable(defendant.getOffences()).ifPresent(
                            offences -> offences.stream().filter(Objects::nonNull).forEach(offence -> {
                                final List<JudicialResult> offenceJudicialResults = getNonNowsResults(offence.getJudicialResults());
                                if (nonNull(offenceJudicialResults) && !offenceJudicialResults.isEmpty()) {
                                    offence.getJudicialResults().clear();
                                    offence.getJudicialResults().addAll(offenceJudicialResults);
                                }
                            }));
                    Optional.ofNullable(defendant.getDefendantCaseJudicialResults()).ifPresent(
                            judicialResults -> {
                                final List<JudicialResult> defendantCaseJudicialResults = getNonNowsResults(judicialResults);
                                if (nonNull(defendantCaseJudicialResults) && !defendantCaseJudicialResults.isEmpty()) {
                                    defendant.getDefendantCaseJudicialResults().clear();
                                    defendant.getDefendantCaseJudicialResults().addAll(defendantCaseJudicialResults);
                                }
                            });
                }));
    }

    private static List<JudicialResult> getNonNowsResults(final List<JudicialResult> judicialResults) {
        if (isNull(judicialResults) || judicialResults.isEmpty()) {
            return judicialResults;
        }

        return judicialResults.stream()
                .filter(Objects::nonNull)
                .filter(jr -> !Boolean.TRUE.equals(jr.getPublishedForNows()))
                .collect(toList());
    }
}