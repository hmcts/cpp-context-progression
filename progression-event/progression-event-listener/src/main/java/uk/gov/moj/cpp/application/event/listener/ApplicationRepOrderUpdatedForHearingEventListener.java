package uk.gov.moj.cpp.application.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.justice.core.courts.CourtApplication.courtApplication;
import static uk.gov.justice.core.courts.CourtApplicationParty.courtApplicationParty;
import static uk.gov.justice.core.courts.Hearing.hearing;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.ApplicationRepOrderUpdatedForHearing;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class ApplicationRepOrderUpdatedForHearingEventListener {
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationRepOrderUpdatedForHearingEventListener.class);

    @Handles("progression.event.application-rep-order-updated-for-hearing")
    public void processApplicationDefenceOrganisationChanged(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.event.application-rep-order-updated-for-hearing {} ", event.toObfuscatedDebugString());
        }
        final ApplicationRepOrderUpdatedForHearing applicationRepOrderUpdatedForHearing = jsonObjectConverter.convert(event.payloadAsJsonObject(), ApplicationRepOrderUpdatedForHearing.class);
        final HearingEntity hearingEntity = hearingRepository.findBy(applicationRepOrderUpdatedForHearing.getHearingId());

        if (isNull(hearingEntity)) {
            return;
        }
        final JsonObject hearingJson = stringToJsonObjectConverter.convert(hearingEntity.getPayload());
        final Hearing hearing = jsonObjectConverter.convert(hearingJson, Hearing.class);

        if (isNull(hearing)) {
            return;
        }
        final List<CourtApplication> courtApplications = hearing.getCourtApplications();
        if (isNull(courtApplications) || courtApplications.isEmpty()) {
            return;
        }

        final List<CourtApplication> updatedCourtApplications = hearing.getCourtApplications().stream()
                .map(courtApplication -> {
                    if (courtApplication.getId().equals(applicationRepOrderUpdatedForHearing.getApplicationId()) &&
                            isSubjectMatched(courtApplication.getSubject(), applicationRepOrderUpdatedForHearing.getSubjectId())) {
                        return courtApplication()
                                .withValuesFrom(courtApplication)
                                .withSubject(courtApplicationParty()
                                        .withValuesFrom(courtApplication.getSubject())
                                        .withAssociatedDefenceOrganisation(applicationRepOrderUpdatedForHearing.getAssociatedDefenceOrganisation())
                                        .build())
                                .build();
                    }
                    return courtApplication;
                })
                .toList();

        final Hearing updatedHearing =  hearing()
                .withValuesFrom(hearing)
                .withCourtApplications(updatedCourtApplications)
                .build();

        hearingEntity.setPayload(objectToJsonObjectConverter.convert(updatedHearing).toString());
        hearingRepository.save(hearingEntity);
    }

    private boolean isSubjectMatched(final CourtApplicationParty subject, final UUID subjectId) {
        return !isNull(subject) && subject.getId().equals(subjectId);
    }
}
