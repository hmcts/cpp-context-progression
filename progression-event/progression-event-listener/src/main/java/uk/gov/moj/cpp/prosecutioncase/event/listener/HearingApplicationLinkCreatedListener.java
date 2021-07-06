package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import java.util.ArrayList;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.courts.HearingDeletedForCourtApplication;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingApplicationKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class HearingApplicationLinkCreatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingApplicationRepository repository;

    @Inject
    private HearingRepository hearingRepository;

    @Handles("progression.event.hearing-application-link-created")
    public void process(final JsonEnvelope event) {
        final HearingApplicationLinkCreated hearingApplicationLinkCreated
                = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingApplicationLinkCreated.class);
        repository.save(transformHearingApplicationEntity
                (hearingApplicationLinkCreated.getHearing(), hearingApplicationLinkCreated.getApplicationId(),
                        hearingApplicationLinkCreated.getHearingListingStatus()));
    }

    @Handles("progression.event.hearing-deleted-for-court-application")
    public void processHearingDeletedForCourtApplicationEvent(final JsonEnvelope event) {

        final HearingDeletedForCourtApplication hearingDeletedForCourtApplication
                = jsonObjectConverter.convert(event.payloadAsJsonObject(), HearingDeletedForCourtApplication.class);

        repository.removeByHearingIdAndCourtApplicationId(hearingDeletedForCourtApplication.getHearingId(), hearingDeletedForCourtApplication.getCourtApplicationId());

    }

    private HearingApplicationEntity transformHearingApplicationEntity(final Hearing hearing, final UUID applicationId,
                                                                       HearingListingStatus hearingListingStatus) {
        removeNowsJudicialResults(hearing);
        HearingEntity hearingEntity = hearingRepository.findBy(hearing.getId());
        if (hearingEntity == null) {
            hearingEntity = new HearingEntity();
            hearingEntity.setHearingId(hearing.getId());
            hearingEntity.setListingStatus(hearingListingStatus);
        }
        if (nonNull(hearingEntity.getPayload())) {
            final Hearing originalHearing = jsonObjectConverter.convert(stringToJsonObjectConverter.convert(hearingEntity.getPayload()), Hearing.class);
            final Hearing.Builder hearingBuilder = Hearing.hearing().withValuesFrom(originalHearing);
            if (isNotEmpty(hearing.getCourtApplications())) {
                hearingBuilder.withCourtApplications(hearing.getCourtApplications());
            }
            if (isNotEmpty(hearing.getProsecutionCases())) {
                final List<ProsecutionCase> prosecutionCases = ofNullable(originalHearing.getProsecutionCases()).orElseGet(ArrayList::new);
                prosecutionCases.addAll(hearing.getProsecutionCases());
                hearingBuilder.withProsecutionCases(prosecutionCases);
            }
            final Hearing hearingUpdated = hearingBuilder.build();
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearingUpdated).toString());
        } else {
            hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
        }
        final HearingApplicationEntity hearingApplicationEntity = new HearingApplicationEntity();
        HearingApplicationKey hearingApplicationKey = new HearingApplicationKey();
        hearingApplicationEntity.setId(hearingApplicationKey);
        hearingApplicationEntity.getId().setApplicationId(applicationId);
        hearingApplicationEntity.getId().setHearingId(hearing.getId());
        hearingApplicationEntity.setHearing(hearingEntity);
        return hearingApplicationEntity;
    }

    private void removeNowsJudicialResults(final Hearing hearing) {
        Optional.ofNullable(hearing.getCourtApplications()).ifPresent(
                courtApplications -> courtApplications.stream()
                        .filter(Objects::nonNull)
                        .forEach(courtApplication ->
                                ofNullable(courtApplication.getJudicialResults()).ifPresent(
                                        judicialResults -> {
                                            final List<JudicialResult> caJudicialResults = judicialResults.stream()
                                                    .filter(Objects::nonNull).filter(jr -> !jr.getPublishedForNows().equals(Boolean.TRUE))
                                                    .collect(Collectors.toList());
                                            courtApplication.getJudicialResults().clear();
                                            courtApplication.getJudicialResults().addAll(caJudicialResults);
                                        }
                                ))
        );
    }

}
