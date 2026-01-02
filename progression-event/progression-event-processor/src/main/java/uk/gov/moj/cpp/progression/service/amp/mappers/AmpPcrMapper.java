package uk.gov.moj.cpp.progression.service.amp.mappers;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.core.courts.PrisonCourtRegisterGeneratedV2;
import uk.gov.justice.core.courts.prisonCourtRegisterDocument.PrisonCourtRegisterDefendant;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayload;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadCustodyEstablishmentDetails;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendants;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendantsCases;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventPayloadDefendantsDocuments;
import uk.gov.moj.cpp.progression.service.amp.dto.PcrEventType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Slf4j
public class AmpPcrMapper {

    public PcrEventPayload mapPcrForAmp(PrisonCourtRegisterGeneratedV2 pcrIn, String prisonEmail, Instant createdAt) {
        PcrEventPayloadCustodyEstablishmentDetails pcrCustodyEstablishment = PcrEventPayloadCustodyEstablishmentDetails.builder()
                .emailAddress(prisonEmail)
                .build();
        PrisonCourtRegisterDefendant pcrDefendant = pcrIn.getDefendant() == null
                ? PrisonCourtRegisterDefendant.prisonCourtRegisterDefendant().build()
                : pcrIn.getDefendant();
        PcrEventPayloadDefendantsDocuments pcrDocument = PcrEventPayloadDefendantsDocuments.builder()
                .materialId(pcrIn.getMaterialId())
                .timestamp(createdAt)
                .build();
        String caseUrn = pcrDefendant.getProsecutionCasesOrApplications() != null && pcrDefendant.getProsecutionCasesOrApplications().size() > 0
                ? pcrDefendant.getProsecutionCasesOrApplications().get(0).getCaseOrApplicationReference()
                : null;
        PcrEventPayloadDefendantsCases cases = PcrEventPayloadDefendantsCases.builder()
                .urn(caseUrn)
                .documents(List.of(pcrDocument))
                .build();
        PcrEventPayloadDefendants payloadDefendant = PcrEventPayloadDefendants.builder()
                .masterDefendantId(pcrDefendant.getMasterDefendantId())
                .name(pcrDefendant.getName())
                .dateOfBirth(getDateOfBirth(pcrDefendant))
                .custodyEstablishmentDetails(pcrCustodyEstablishment)
                .cases(List.of(cases))
                .build();
        return PcrEventPayload.builder()
                .eventId(pcrIn.getId())
                .eventType(PcrEventType.PCR)
                .timestamp(Instant.now())
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
