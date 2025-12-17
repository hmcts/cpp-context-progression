package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.query.laa.CaseSummary;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S1168")
public class CaseSummaryLaaConverter extends LAAConverter {

    public List<CaseSummary> convert(final CourtApplication courtApplication) {
        if (isEmpty(courtApplication.getCourtApplicationCases())) {
            return null;
        }
        return courtApplication.getCourtApplicationCases().stream().map(this::convertCase).toList();
    }

    private CaseSummary convertCase(final CourtApplicationCase courtApplicationCase) {
        return CaseSummary.caseSummary()
                .withCaseStatus(courtApplicationCase.getCaseStatus())
                .withProsecutionCaseId(courtApplicationCase.getProsecutionCaseId())
                .withProsecutionCaseReference(
                        ofNullable(courtApplicationCase.getProsecutionCaseIdentifier())
                                .map(ProsecutionCaseIdentifier::getCaseURN)
                                .orElse(null)
                )
                .build();
    }

}
