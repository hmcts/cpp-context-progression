package uk.gov.moj.cpp.progression.service.amp.mappers;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadCustodyEstablishmentDetails;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendantCases;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
public class AmpPcrMapper {

    public PcrEventPayload mapPcrForAmp(PrisonCourtRegisterGeneratedV2 pcrIn, String prisonEmail, Instant createdAt) {
        return PcrEventPayload.builder()
                .eventId(pcrIn.getId())
                .materialId(pcrIn.getMaterialId())
                .eventType(PcrEventType.PRISON_COURT_REGISTER_GENERATED)
                .timestamp(Instant.now())
                .defendant(mapDefendant(pcrIn, prisonEmail))
                .build();
    }

    private PcrEventPayloadDefendant mapDefendant(PrisonCourtRegisterGeneratedV2 pcrIn, String prisonEmail) {
        PcrEventPayloadCustodyEstablishmentDetails pcrCustodyEstablishment = PcrEventPayloadCustodyEstablishmentDetails.builder()
                .emailAddress(prisonEmail)
                .build();
        PrisonCourtRegisterDefendant pcrDefendant = pcrIn.getDefendant() == null
                ? PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().build()
                : pcrIn.getDefendant();

        return PcrEventPayloadDefendant.builder()
                .masterDefendantId(pcrDefendant.getMasterDefendantId())
                .name(pcrDefendant.getName())
                .dateOfBirth(mapDateOfBirth(pcrDefendant))
                .custodyEstablishmentDetails(pcrCustodyEstablishment)
                .cases(mapCases(pcrDefendant))
                .build();
    }

    private List<PcrEventPayloadDefendantCases> mapCases(PrisonCourtRegisterDefendant pcrDefendant) {
        return pcrDefendant.getProsecutionCasesOrApplications() == null
                ? Collections.emptyList()
                : pcrDefendant.getProsecutionCasesOrApplications().stream()
                .map(c ->
                        PcrEventPayloadDefendantCases.builder().urn(c.getCaseOrApplicationReference()).build()).toList();
    }

    private LocalDate mapDateOfBirth(PrisonCourtRegisterDefendant defendant) {
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
