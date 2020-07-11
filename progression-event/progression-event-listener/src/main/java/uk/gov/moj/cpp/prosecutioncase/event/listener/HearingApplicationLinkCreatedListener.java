package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingApplicationLinkCreated;
import uk.gov.justice.core.courts.HearingListingStatus;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
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

    private HearingApplicationEntity transformHearingApplicationEntity(final Hearing hearing, final UUID applicationId,
                                                                       HearingListingStatus hearingListingStatus) {

        removeNowsJudicialResults(hearing);

        HearingEntity hearingEntity = hearingRepository.findBy(hearing.getId());
        if (hearingEntity == null) {
            hearingEntity = new HearingEntity();
            hearingEntity.setHearingId(hearing.getId());
            hearingEntity.setListingStatus(hearingListingStatus);
        }

        hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
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
