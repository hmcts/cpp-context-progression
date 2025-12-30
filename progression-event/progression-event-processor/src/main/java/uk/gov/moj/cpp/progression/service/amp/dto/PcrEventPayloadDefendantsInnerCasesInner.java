package uk.gov.moj.cpp.progression.service.amp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
// Generated and copied from AMP
public class PcrEventPayloadDefendantsInnerCasesInner {
    private String urn;
    private List<PcrEventPayloadDefendantsInnerCasesInnerDocumentsInner> documents;
}

