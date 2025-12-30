package uk.gov.moj.cpp.progression.service.amp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
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

    private UUID eventId;
    private EventType eventType;
    private OffsetDateTime timestamp;
    private List<PcrEventPayloadDefendantsInner> defendants = new ArrayList<>();
}

