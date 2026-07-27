package uk.gov.moj.cpp.progression.service;

import java.time.ZonedDateTime;

public record AvailableHearingSlot(String courtRoomId, ZonedDateTime hearingStartTime) {
}
