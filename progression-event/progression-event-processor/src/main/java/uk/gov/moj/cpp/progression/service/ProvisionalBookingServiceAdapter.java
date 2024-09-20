package uk.gov.moj.cpp.progression.service;

import static java.util.stream.Collectors.joining;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.common.service.ProvisionalBookingService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;

public class ProvisionalBookingServiceAdapter {

    private static final String BOOKING_IDS = "bookingIds";

    @Inject
    private ProvisionalBookingService provisionalBookingService;
    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    public Map<UUID, Set<UUID>> getSlots(final List<UUID> bookingReferences) {
        if (bookingReferences.isEmpty()) {
            return new HashMap<>();
        }

        final String bookingIds = bookingReferences.stream()
                .map(UUID::toString)
                .collect(joining(","));

        final Map<String, String> params = new HashMap<>();
        params.put(BOOKING_IDS, bookingIds);

        final Response response = provisionalBookingService.getSlots(params);
        if (response.getStatus() != HttpStatus.SC_OK) {
            String responsePayload = "";
            if (response.hasEntity()) {
                responsePayload = response.getEntity().toString();
            }
            throw new ServerErrorException("GetSlots endpoint returned an error: " + responsePayload, response.getStatus());
        }

        final JsonObject responseJson = objectToJsonObjectConverter.convert(response.getEntity());
        final JsonArray jsonArray = responseJson.getJsonArray("provisionalSlots");
        final Map<UUID, Set<UUID>> slotsMap = new HashMap<>();

        jsonArray.forEach(jsonValue -> {
            final JsonObject jsonObject = (JsonObject)jsonValue;
            final UUID courtScheduleId = UUID.fromString(jsonObject.getString("courtScheduleId"));
            final UUID bookingId = UUID.fromString(jsonObject.getString("bookingId"));
            if (!slotsMap.containsKey(bookingId)) {
                slotsMap.put(bookingId, new HashSet<>());
            }

            slotsMap.get(bookingId).add(courtScheduleId);
        });
        return slotsMap;
    }
}
