package uk.gov.moj.cpp.progression.service.amp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
// Generated and copied from AMP
public class PcrEventPayloadDefendantsInner {

    private UUID masterDefendantId;
    private String name;
    private LocalDate dateOfBirth;

    private PcrEventPayloadDefendantsInnerCustodyEstablishmentDetails custodyEstablishmentDetails;
    private List<PcrEventPayloadDefendantsInnerCasesInner> cases;
}

