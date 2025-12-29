package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.progression.courts.HearingTrialVacated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.Objects;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingTrialVacatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingTrialVacatedEventListener.class);

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Handles("progression.event.hearing-trial-vacated")
    public void handleHearingTrialVacatedEvent(final JsonEnvelope event) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.hearing-trial-vacated {} ", event.toObfuscatedDebugString());
        }

        final JsonObject payload = event.payloadAsJsonObject();
        final HearingTrialVacated hearingTrialVacated = jsonObjectToObjectConverter.convert(payload, HearingTrialVacated.class);

        final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingTrialVacated.getHearingId());

        if (nonNull(dbHearingEntity)) {

            final JsonObject dbHearingJsonObject = jsonFromString(dbHearingEntity.getPayload());

            final Hearing dbHearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);
            final Hearing.Builder builder = Hearing.hearing().withValuesFrom(dbHearing);
            builder.withIsVacatedTrial(nonNull(hearingTrialVacated.getVacatedTrialReasonId()));


            final JsonObject updatedJsonObject = objectToJsonObjectConverter.convert(builder.build());
            dbHearingEntity.setPayload(updatedJsonObject.toString());
            // save in updated hearing in hearing table
            hearingRepository.save(dbHearingEntity);
        }

    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}

