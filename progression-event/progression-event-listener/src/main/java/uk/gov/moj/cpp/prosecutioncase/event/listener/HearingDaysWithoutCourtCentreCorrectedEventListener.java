package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingDaysWithoutCourtCentreCorrectedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDaysWithoutCourtCentreCorrectedEventListener.class);

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.events.hearing-days-without-court-centre-corrected")
    public void correctHearingDaysWithoutCourtCentre(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.events.hearing-days-without-court-centre-corrected {} ", event.toObfuscatedDebugString());
        }
        final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrected = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), HearingDaysWithoutCourtCentreCorrected.class);

        final List<HearingDay> hearingDays = hearingDaysWithoutCourtCentreCorrected.getHearingDays();
        final UUID hearingId = hearingDaysWithoutCourtCentreCorrected.getId();
        final UUID courtCentreId = hearingDays.get(0).getCourtCentreId();
        final UUID courtRoomId = hearingDays.get(0).getCourtRoomId();

        final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingId);
        final JsonObject dbHearingJsonObject = jsonFromString(dbHearingEntity.getPayload());
        LOGGER.info("existing hearingObject : {}", dbHearingJsonObject);

        final Hearing dbHearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);

        if(isEmpty(dbHearing.getHearingDays())){
            LOGGER.info("No hearing days to correct, returning ");
            return;
        }

        final List<HearingDay> hearingDayListToBeReplaced = createHearingDaysToBeReplaced(dbHearing.getHearingDays(), courtCentreId, courtRoomId);

        dbHearing.getHearingDays().clear();
        dbHearing.getHearingDays().addAll(hearingDayListToBeReplaced);

        final JsonObject updatedJsonObject = objectToJsonObjectConverter.convert(dbHearing);
        dbHearingEntity.setPayload(updatedJsonObject.toString());

        LOGGER.info("updated hearingObject : {}", updatedJsonObject);

        hearingRepository.save(dbHearingEntity);

        LOGGER.info("HearingDays replaced with this payload {} for this hearingId : {}", updatedJsonObject, dbHearingEntity.getHearingId());
    }

    private List<HearingDay> createHearingDaysToBeReplaced(final List<HearingDay> hearingDays, final UUID courtCentreId, final UUID courtRoomId) {
        final List<HearingDay> hearingDayListToBeReplaced = new ArrayList<>();
        hearingDays.forEach(hearingDay ->
            hearingDayListToBeReplaced.add(HearingDay.hearingDay()
                    .withValuesFrom(hearingDay)
                    .withCourtCentreId(courtCentreId)
                    .withCourtRoomId(courtRoomId).build())
        );
        return hearingDayListToBeReplaced;
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = JsonObjects.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
