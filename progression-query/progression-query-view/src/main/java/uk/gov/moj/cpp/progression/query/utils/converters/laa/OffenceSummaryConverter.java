package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.progression.query.laa.OffenceSummary;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
@SuppressWarnings("squid:S1168")
public class OffenceSummaryConverter extends LAAConverter {

    @Inject
    private LaaApplnReferenceConverter laaApplnReferenceConverter;

    public List<OffenceSummary> convert(final List<CourtApplicationCase> courtApplicationCases) {
        if (isEmpty(courtApplicationCases)) {
            return null;
        }
        return courtApplicationCases.stream()
                .map(CourtApplicationCase::getOffences)
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(List::stream)
                .map(offence -> OffenceSummary.offenceSummary()
                        .withOffenceCode(offence.getOffenceCode())
                        .withOffenceId(offence.getId())
                        .withArrestDate(offence.getArrestDate())
                        .withChargeDate(offence.getChargeDate())
                        .withDateOfInformation(offence.getDateOfInformation())
                        .withEndDate(offence.getEndDate())
                        .withLaaApplnReference(laaApplnReferenceConverter.convert(offence.getLaaApplnReference()))
                        .withModeOfTrial(offence.getModeOfTrial())
                        .withOffenceLegislation(offence.getOffenceLegislation())
                        .withOffenceTitle(offence.getOffenceTitle())
                        .withOrderIndex(offence.getOrderIndex())
                        .withProceedingsConcluded(offence.getProceedingsConcluded())
                        .withStartDate(offence.getStartDate())
                        .withWording(offence.getWording())
                        .build())
                .toList();
    }
}