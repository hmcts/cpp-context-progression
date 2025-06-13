package uk.gov.moj.cpp.progression.query.utils.converters.laa;

import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.progression.query.laa.HearingSummary;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
@SuppressWarnings("squid:S1168")
public class HearingSummaryLaaConverter extends LAAConverter {

    @Inject
    private CourtCentreConverter courtCentreConverter;

    @Inject
    private HearingDayConverter hearingDayConverter;

    @Inject
    private HearingTypeConverter hearingTypeConverter;

    @Inject
    private DefenceCounselConverter defenceCounselConverter;


    public List<HearingSummary> convert(final List<Hearing> hearingList) {
        if (isEmpty(hearingList)) {
            return null;
        }

        return hearingList.stream().map(this::convertHearing).toList();
    }

    private HearingSummary convertHearing(final Hearing hearing) {
        return HearingSummary.hearingSummary()
                .withHearingId(hearing.getId())
                .withCourtCentre(courtCentreConverter.convert(hearing.getCourtCentre()))
                .withHearingDays(hearingDayConverter.convert(hearing.getHearingDays()))
                .withHearingType(hearingTypeConverter.convert(hearing.getType()))
                .withDefenceCounsel(defenceCounselConverter.convert(hearing.getDefenceCounsels()))
                .withDefendantIds(collectDefendantIds(hearing.getProsecutionCases()))
                .withEstimatedDuration(hearing.getEstimatedDuration())
                .withJurisdictionType(uk.gov.justice.progression.query.laa.JurisdictionType.valueFor(ofNullable(hearing.getJurisdictionType()).map(JurisdictionType::toString).orElse(null)).orElse(null))
                .build();
    }

    private List<UUID> collectDefendantIds(final List<ProsecutionCase> prosecutionCases) {
        if (isEmpty(prosecutionCases)) {
            return null;
        }
        return prosecutionCases.stream()
                .map(ProsecutionCase::getDefendants)
                .flatMap(List::stream)
                .map(uk.gov.justice.core.courts.Defendant::getId)
                .toList();
    }


}
