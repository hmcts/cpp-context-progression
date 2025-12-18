package uk.gov.moj.cpp.progression.processor;

import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.ApplicationOffencesUpdated;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.progression.query.laa.HearingSummary;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@SuppressWarnings({"squid:S3457", "squid:S3655",})
@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationOffencesUpdatedProcessor {
    static final String PRIVATE_COMMAND_PROGRESSION_UPDATE_LAA_REFERENCE_FOR_HEARING = "progression.command.update-application-laa-reference-for-hearing";
    static final String PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED = "public.progression.application-offences-updated";
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationOffencesUpdatedProcessor.class.getCanonicalName());

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private CourtApplicationRepository repository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;


    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private ProgressionService progressionService;

    @Handles("progression.event.application-offences-updated")
    public void handleApplicationOffencesUpdatedEvent(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received prosecution application offences updated with payload : {}", event.toObfuscatedDebugString());
        }

        final ApplicationOffencesUpdated applicationOffencesUpdated =
                jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), ApplicationOffencesUpdated.class);
        final CourtApplicationEntity applicationEntity = courtApplicationRepository.findByApplicationId(applicationOffencesUpdated.getApplicationId());

        if (nonNull(applicationEntity)) {
            final JsonObject applicationJson = stringToJsonObjectConverter.convert(applicationEntity.getPayload());
            final CourtApplication persistedApplication = jsonObjectToObjectConverter.convert(applicationJson, CourtApplication.class);

            if (nonNull(persistedApplication.getSubject()) && persistedApplication.getSubject().getId().equals(applicationOffencesUpdated.getSubjectId()) && isNotEmpty(persistedApplication.getCourtApplicationCases())) {
                Optional<Offence> updatedOffence = persistedApplication.getCourtApplicationCases().stream()
                        .flatMap(applicationCase -> applicationCase.getOffences().stream())
                        .filter(offence -> offence.getId().equals(applicationOffencesUpdated.getOffenceId()))
                        .findFirst();

                if (updatedOffence.isPresent()) {
                    sender.send(enveloper.withMetadataFrom(event, PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED).apply(event.payloadAsJsonObject()));
                    updateHearing(event, applicationOffencesUpdated);
                } else {
                    LOGGER.warn("Matched offence is not found. OffenceId : " + applicationOffencesUpdated.getOffenceId());
                }
            } else {
                LOGGER.warn("Matched subject is not found. Subject : " + applicationOffencesUpdated.getSubjectId());
            }
        } else {
            LOGGER.warn("Matched entity is not found. ApplicationId : " + applicationOffencesUpdated.getApplicationId());
        }


    }

    private void updateHearing(JsonEnvelope event, ApplicationOffencesUpdated applicationOffencesUpdated) {
        Optional<List<HearingSummary>> hearings = progressionService.getHearingsForApplication(applicationOffencesUpdated.getApplicationId());
        if(hearings.isPresent()) {
            JsonArrayBuilder arrayBuilder = createArrayBuilder();

            List<String> hearingIds = hearings.get().stream()
                    .map(hearingSummary -> String.valueOf(hearingSummary.getHearingId()))
                    .toList();
            for (String hearingId : hearingIds) {
                final JsonObject jsonObject = createObjectBuilder()
                        .add("hearingId", hearingId)
                        .add("applicationId", applicationOffencesUpdated.getApplicationId().toString())
                        .add("subjectId", applicationOffencesUpdated.getSubjectId().toString())
                        .add("offenceId", applicationOffencesUpdated.getOffenceId().toString())
                        .add("laaReference", objectToJsonObjectConverter.convert(applicationOffencesUpdated.getLaaReference()))
                        .build();

                sender.send(enveloper.withMetadataFrom(event, PRIVATE_COMMAND_PROGRESSION_UPDATE_LAA_REFERENCE_FOR_HEARING).apply(jsonObject));
            }
        }
    }


}