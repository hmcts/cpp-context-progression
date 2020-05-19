package uk.gov.moj.cpp.progression.helper;

import uk.gov.justice.core.courts.Hearing;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

public class HearingBookingReferenceListExtractor {

    /**
     *  Returns distinct list of BookingReference.
     *  Collects all BookingReferences under ProsecutionCase->Defendant->Offences->JudicialResults(nullable)->NextHearing(nullable)
     */
    public List<UUID> extractBookingReferenceList(final Hearing hearing){
        return hearing.getProsecutionCases().stream()
                .flatMap(pc -> pc.getDefendants().stream()
                        .flatMap(def-> def.getOffences().stream().filter(o -> nonNull(o.getJudicialResults()))
                                .flatMap(o -> o.getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing()) && nonNull(jr.getNextHearing().getBookingReference())))))
                .map(jr -> jr.getNextHearing().getBookingReference())
                .distinct()
                .collect(Collectors.toList());

    }
}
