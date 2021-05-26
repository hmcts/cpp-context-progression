package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.NextHearing;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

public class HearingBookingReferenceListExtractor {

    @Inject
    private HearingResultHelper hearingResultHelper;

    /**
     *  Returns distinct list of BookingReference.
     *  Collects all BookingReferences under ProsecutionCase->Defendant->Offences->JudicialResults(nullable)->NextHearing(nullable)
     */
    public List<UUID> extractBookingReferenceList(final Hearing hearing){
        final List<UUID> caseUUIDs = ofNullable(hearing.getProsecutionCases()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(pc -> pc.getDefendants().stream()
                        .flatMap(def -> def.getOffences().stream().filter(o -> nonNull(o.getJudicialResults()))
                                .flatMap(o -> o.getJudicialResults().stream().filter(jr -> nonNull(jr.getNextHearing()) && nonNull(jr.getNextHearing().getBookingReference())))))
                .map(jr -> jr.getNextHearing().getBookingReference())
                .distinct()
                .collect(Collectors.toList());

        final List<JudicialResult> judicialResults = ofNullable(hearing.getCourtApplications()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(courtApplication -> hearingResultHelper.getAllJudicialResultsFromApplication(courtApplication).stream())
                .collect(Collectors.toList());

        final List<UUID> applicationUUIDs = judicialResults.stream()
                .map(JudicialResult::getNextHearing)
                .filter(Objects::nonNull)
                .map(NextHearing::getBookingReference)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        return Stream.concat(caseUUIDs.stream(), applicationUUIDs.stream()).distinct().collect(Collectors.toList());

    }
}
