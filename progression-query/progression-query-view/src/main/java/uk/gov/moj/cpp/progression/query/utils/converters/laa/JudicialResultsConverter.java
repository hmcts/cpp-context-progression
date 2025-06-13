package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.external.ApiJudicialResult;
import uk.gov.justice.progression.query.laa.Category;

import java.util.List;

@SuppressWarnings("squid:S1168")
public class JudicialResultsConverter extends LAAConverter {

    public List<ApiJudicialResult> convert(final List<uk.gov.justice.core.courts.JudicialResult> judicialResults) {
        if (isEmpty(judicialResults)) {
            return null;
        }
        return judicialResults.stream().map(this::convertJudicialResult).toList();
    }

    private ApiJudicialResult convertJudicialResult(final uk.gov.justice.core.courts.JudicialResult judicialResult) {
        return ApiJudicialResult.apiJudicialResult()
                .withJudicialResultId(judicialResult.getJudicialResultId())
                .withIsAdjournmentResult(judicialResult.getIsAdjournmentResult())
                .withIsFinancialResult(judicialResult.getIsFinancialResult())
                .withIsConvictedResult(judicialResult.getIsConvictedResult())
                .withIsAvailableForCourtExtract(judicialResult.getIsAvailableForCourtExtract())
                .withOrderedHearingId(judicialResult.getOrderedHearingId())
                .withLabel(judicialResult.getLabel())
                .withResultText(judicialResult.getResultText())
                .withCjsCode(judicialResult.getCjsCode())
                .withRank(judicialResult.getRank())
                .withOrderedDate(judicialResult.getOrderedDate())
                .withLastSharedDateTime(judicialResult.getLastSharedDateTime())
                .withTerminatesOffenceProceedings(judicialResult.getTerminatesOffenceProceedings())
                .withCategory(Category.valueFor(ofNullable(judicialResult.getCategory()).map(JudicialResultCategory::toString).orElse(null)).orElse(null))
                .build();
    }

}
