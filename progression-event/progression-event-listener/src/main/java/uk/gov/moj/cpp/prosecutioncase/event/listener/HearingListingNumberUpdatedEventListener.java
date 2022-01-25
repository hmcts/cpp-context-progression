package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import java.io.StringReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingListingNumberUpdated;
import uk.gov.justice.core.courts.ListingNumberUpdated;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.HearingEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantHearingRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.HearingRepository;

@ServiceComponent(EVENT_LISTENER)
public class HearingListingNumberUpdatedEventListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private CaseDefendantHearingRepository caseDefendantHearingRepository;

    @Handles("progression.event.listing-number-updated")
    public void handleListingNumberUpdatedEvent(final Envelope<ListingNumberUpdated> event) {

        final ListingNumberUpdated listingNumberUpdated = event.payload();
        final Map<UUID, Integer> listingMap = listingNumberUpdated.getOffenceListingNumbers().stream().collect(Collectors.toMap(OffenceListingNumbers::getOffenceId, OffenceListingNumbers::getListingNumber,Math::max));
        final UUID hearingId = listingNumberUpdated.getHearingId();

        final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingId);
        final JsonObject dbHearingJsonObject = jsonFromString(dbHearingEntity.getPayload());
        final Hearing hearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);


        updateListingNumber(listingMap, dbHearingEntity, hearing);

    }

    @Handles("progression.event.hearing-listing-number-updated")
    public void handleHaeringListingNumberUpdatedEvent(final Envelope<HearingListingNumberUpdated> event) {

        final HearingListingNumberUpdated hearingListingNumberUpdated = event.payload();
        final Map<UUID, Integer> listingMap = hearingListingNumberUpdated.getOffenceListingNumbers().stream().collect(Collectors.toMap(OffenceListingNumbers::getOffenceId, OffenceListingNumbers::getListingNumber,Math::max));
        final UUID hearingId = hearingListingNumberUpdated.getHearingId();

        final HearingEntity dbHearingEntity = hearingRepository.findBy(hearingId);
        if(dbHearingEntity == null){
            return;
        }
        final JsonObject dbHearingJsonObject = jsonFromString(dbHearingEntity.getPayload());
        final Hearing hearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, Hearing.class);


        updateListingNumber(listingMap, dbHearingEntity, hearing);

    }

    private void updateListingNumber(final Map<UUID, Integer> listingMap, final HearingEntity dbHearingEntity, final Hearing hearing) {
        final Hearing.Builder builder = Hearing.hearing().withValuesFrom(hearing)
                .withProsecutionCases(ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                        .map(prosecutionCase -> ProsecutionCase.prosecutionCase().withValuesFrom(prosecutionCase)
                                .withDefendants(prosecutionCase.getDefendants().stream()
                                        .map(defendant -> Defendant.defendant().withValuesFrom(defendant)
                                                .withOffences(defendant.getOffences().stream()
                                                        .map(offence -> Offence.offence().withValuesFrom(offence)
                                                                .withListingNumber(ofNullable(listingMap.get(offence.getId())).orElse(offence.getListingNumber()))
                                                                .build())
                                                        .collect(toList()))
                                                .build())
                                        .collect(toList()))
                                .build())
                        .collect(collectingAndThen(Collectors.toList(), getListOrNull())));

        final JsonObject updatedJsonObject = objectToJsonObjectConverter.convert(builder.build());
        dbHearingEntity.setPayload(updatedJsonObject.toString());
        // save in updated hearing in hearing table
        hearingRepository.save(dbHearingEntity);
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

    private <T> UnaryOperator<List<T>> getListOrNull() {
        return list -> list.isEmpty() ? null : list;
    }
}

