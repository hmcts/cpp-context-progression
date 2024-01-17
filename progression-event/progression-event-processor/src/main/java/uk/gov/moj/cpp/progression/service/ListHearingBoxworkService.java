package uk.gov.moj.cpp.progression.service;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.NextHearing;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class ListHearingBoxworkService {

    public static final UUID LHBW_RESULT_DEFINITION = UUID.fromString("e220d2f7-aaff-468f-a44e-e157022210e8");

    public boolean isLHBWResultedAndNeedToSendNotifications(final List<JudicialResult> judicialResults) {
        return isResultedWithLHBW(judicialResults)
                && ((isFixedDate(judicialResults) && hasNoCourtRoom(judicialResults)) || isWeekCommencingDate(judicialResults));
    }

    public NextHearing getNextHearingFromLHBWResult(final List<JudicialResult> judicialResults) {
        if (nonNull(judicialResults) && !judicialResults.isEmpty()) {
            return judicialResults.stream()
                    .filter(jr -> LHBW_RESULT_DEFINITION.equals(jr.getJudicialResultTypeId()))
                    .map(JudicialResult::getNextHearing)
                    .findFirst().orElseThrow(() -> new IllegalStateException("Error! Application resulted with LHBW must have nextHearing info"));
        }

        return NextHearing.nextHearing().build();
    }

    private boolean hasNoCourtRoom(final List<JudicialResult> judicialResults) {

        final NextHearing nextHearing = getNextHearingFromLHBWResult(judicialResults);

        return Optional.ofNullable(nextHearing)
                .filter(nh -> nonNull(nh.getCourtCentre()) && isNull(nh.getCourtCentre().getRoomId()))
                .isPresent();
    }

    private boolean isResultedWithLHBW(final List<JudicialResult> judicialResults) {
        return nonNull(judicialResults) && !judicialResults.isEmpty()
                && judicialResults.stream()
                .anyMatch(jr -> LHBW_RESULT_DEFINITION.equals(jr.getJudicialResultTypeId()));
    }

    private boolean isWeekCommencingDate(final List<JudicialResult> judicialResults) {
        return nonNull(getNextHearingFromLHBWResult(judicialResults).getWeekCommencingDate());
    }

    private boolean isFixedDate(final List<JudicialResult> judicialResults) {
        return nonNull(getNextHearingFromLHBWResult(judicialResults).getListedStartDateTime());
    }

}
