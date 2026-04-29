package uk.gov.moj.cpp.progression.service.amp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Payload representing a stored PCR document event with defendants and case details.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
// Generated and copied from AMP
public class PcrEventPayload {

    private PcrEventType eventType;
    private UUID eventId;
    private UUID hearingId;
    private UUID materialId;
    private Instant timestamp;
    private PcrEventPayloadDefendant defendant;
}

