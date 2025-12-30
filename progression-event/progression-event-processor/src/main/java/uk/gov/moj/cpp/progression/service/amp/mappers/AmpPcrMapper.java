package uk.gov.moj.cpp.progression.service.amp.mappers;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.EventType;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendantsInner;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendantsInnerCustodyEstablishmentDetails;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
public class AmpPcrMapper {

    public PcrEventPayload mapPcrForAmp(PrisonCourtRegisterGeneratedV2 pcr) {
        PcrEventPayloadDefendantsInnerCustodyEstablishmentDetails pcrCustodyEstablishment = PcrEventPayloadDefendantsInnerCustodyEstablishmentDetails.builder()
                .emailAddress("TODO")
                .build();
        PrisonCourtRegisterDefendant pcrDefendant = pcr.getDefendant() == null
                ? PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().build()
                : pcr.getDefendant();
        // pcrDefendant.getDefendantResults().
        PcrEventPayloadDefendantsInner payloadDefendant = PcrEventPayloadDefendantsInner.builder()
                .masterDefendantId(pcrDefendant.getMasterDefendantId())
                .name(pcrDefendant.getName())
                .dateOfBirth(getDateOfBirth(pcrDefendant))
                .custodyEstablishmentDetails(pcrCustodyEstablishment)
                .build();
        return PcrEventPayload.builder()
                .eventId(pcr.getId())
                .eventType(EventType.PCR)
                .timestamp(OffsetDateTime.now())
                .defendants(List.of(payloadDefendant))
                .build();
    }

    private LocalDate getDateOfBirth(PrisonCourtRegisterDefendant defendant) {
        try {
            return defendant == null || defendant.getDateOfBirth() == null
                    ? null
                    : LocalDate.parse(defendant.getDateOfBirth());
        } catch (Exception e) {
            log.error("Failed to parse date of birth for defendant {}", defendant.getMasterDefendantId());
            return null;
        }
    }
}
