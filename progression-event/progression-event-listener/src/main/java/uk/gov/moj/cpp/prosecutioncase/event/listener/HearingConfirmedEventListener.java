package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import java.io.StringReader;
import java.util.Comparator;

import javax.json.Json;
import javax.json.JsonObject;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingOffencesUpdatedV2;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class HearingConfirmedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingConfirmedEventListener.class);

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private StringToJsonObjectConverter StringToJsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.hearing-initiate-enriched")
    public void processHearingInitiatedEnrichedEvent(final Envelope<Initiate> hearingInitiate) {
        final UUID hearingId = hearingInitiate.payload().getHearing().getId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event progression.hearing-initiate-enriched in listener for hearing id {} ", hearingId);
        }
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);

        if (Objects.nonNull(hearingEntity)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("adding confirmed date for hearing with id: {} ", hearingId);
            }
                hearingEntity.setConfirmedDate(LocalDate.now());
                hearingRepository.save(hearingEntity);
        }
    }

    @Handles("progression.event.hearing-offences-updated-v2")
    public void processHearingOffencesUpdatedV2(final Envelope<HearingOffencesUpdatedV2> event){
        HearingOffencesUpdatedV2 hearingOffencesUpdated = event.payload();
        final UUID hearingId = event.payload().getHearingId();
        final HearingEntity hearingEntity = hearingRepository.findBy(hearingId);
        if (Objects.nonNull(hearingEntity)) {
            final JsonObject hearingJson = StringToJsonObjectConverter.convert(hearingEntity.getPayload());
            final Hearing hearing = jsonObjectToObjectConverter.convert(hearingJson, Hearing.class);
            if (isNotEmpty(hearing.getProsecutionCases()) &&
                    (isNull(hearing.getHasSharedResults()) || !hearing.getHasSharedResults())) {
                hearing.getProsecutionCases().stream()
                        .flatMap(prosecutionCase -> prosecutionCase.getDefendants().stream())
                        .forEach(defendant -> {
                                    if (defendant.getId().equals(hearingOffencesUpdated.getDefendantId())) {
                                        if(hearingOffencesUpdated.getUpdatedOffences() != null) {
                                            defendant.getOffences().replaceAll(offence -> hearingOffencesUpdated.getUpdatedOffences().stream().filter(o -> o.getId().equals(offence.getId())).findFirst().orElse(offence) );
                                        }
                                        if(hearingOffencesUpdated.getNewOffences() != null){
                                            defendant.getOffences().addAll(hearingOffencesUpdated.getNewOffences().stream().filter(off -> defendant.getOffences().stream().noneMatch(doff -> doff.getId().equals(off.getId()))).toList());
                                        }
                                        defendant.getOffences().sort(Comparator.comparing(o -> ofNullable(o.getOrderIndex()).orElse(0)));
                                    }
                                }
                        );
                hearingEntity.setPayload(objectToJsonObjectConverter.convert(hearing).toString());
                hearingRepository.save(hearingEntity);
            }

        }
    }

//    @Handles("progression.event-hearing-remove-duplicate-application-bdf")
//    public void processHearingRemoveDuplicateApplicationBdfEvent(final Envelope<EventHearingRemoveDuplicateApplicationBdf> removeDuplicateApplicationBdf) {
//        final Hearing hearingPayload = removeDuplicateApplicationBdf.payload().getHearing();
//
//        if (LOGGER.isInfoEnabled()) {
//            LOGGER.info("received event progression.event-hearing-remove-duplicate-application-bdf in listener for hearing id {} ", hearingPayload.getId());
//        }
//
//        final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingPayload.getId());
//        final JsonObject dbHearingJson = jsonFromString(dbHearingEntity.getPayload());
//        final Hearing dbHearingObject = jsonObjectToObjectConverter.convert(dbHearingJson, Hearing.class);
//
//        final Hearing updatedHearingWithUniqueApplication = Hearing.hearing().withValuesFrom(dbHearingObject)
//                .withCourtApplications(hearingPayload.getCourtApplications())
//                .build();
//
//        final String updatedHearingWithUniqueApplicationPayload = objectToJsonObjectConverter.convert(updatedHearingWithUniqueApplication).toString();
//        dbHearingEntity.setPayload(updatedHearingWithUniqueApplicationPayload);
//        hearingRepository.save(dbHearingEntity);
//    }

    private static JsonObject jsonFromString(String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }
}
