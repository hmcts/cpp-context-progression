package uk.gov.moj.cpp.progression.transformer;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingExtendedProcessed;
import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.hearing.courts.Initiate;
import uk.gov.justice.progression.courts.ProsecutionCasesReferredToCourt;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("squid:S1612")
public class ProsecutionCasesReferredToCourtTransformer {
    private ProsecutionCasesReferredToCourtTransformer(){

    }

    public static List<ProsecutionCasesReferredToCourt> transform(Initiate hearingInitiate, UUID summonsMaterialId) {
        final List<ProsecutionCasesReferredToCourt> listProsecutionCasesReferredToCourts = new ArrayList<>();
        if ( hearingInitiate.getHearing().getProsecutionCases()!=null) {
            hearingInitiate.getHearing().getProsecutionCases().stream().forEach(prosecutionCase ->
                    prosecutionCase.getDefendants().stream().forEach(defendant ->
                            listProsecutionCasesReferredToCourts.add(
                                    ProsecutionCasesReferredToCourt.prosecutionCasesReferredToCourt()
                                            .withCourtCentre(hearingInitiate.getHearing().getCourtCentre())
                                            .withDefendantId(defendant.getId())
                                            .withDefendantOffences(defendant.getOffences().stream().map(offence -> offence.getId()).collect(Collectors.toList()))
                                            .withHearingId(hearingInitiate.getHearing().getId())
                                            .withProsecutionCaseId(prosecutionCase.getId())
                                            .withSummonsMaterialId(summonsMaterialId)
                                            .withHearingDays(hearingInitiate.getHearing().getHearingDays())
                                            .withHearingType(hearingInitiate.getHearing().getType())
                                            .build())
                    )
            );
        }
        return listProsecutionCasesReferredToCourts;
    }

    public static List<ProsecutionCasesReferredToCourt> transform(final HearingExtendedProcessed hearingExtendedProcessed) {
        final List<ProsecutionCasesReferredToCourt> listProsecutionCasesReferredToCourts = new ArrayList<>();
        final HearingListingNeeds hearingListingNeeds = hearingExtendedProcessed.getHearingRequest();
        final Hearing hearing = hearingExtendedProcessed.getHearing();
        if (hearingExtendedProcessed.getHearingRequest().getProsecutionCases() != null) {
            hearingExtendedProcessed.getHearingRequest().getProsecutionCases().stream().forEach(prosecutionCase ->
                    prosecutionCase.getDefendants()
                            .forEach(defendant ->
                                    listProsecutionCasesReferredToCourts.add(
                                            ProsecutionCasesReferredToCourt.prosecutionCasesReferredToCourt()
                                                    .withCourtCentre(hearing.getCourtCentre())
                                                    .withDefendantId(defendant.getId())
                                                    .withDefendantOffences(defendant.getOffences().stream().map(offence -> offence.getId()).collect(Collectors.toList()))
                                                    .withHearingId(hearingListingNeeds.getId())
                                                    .withProsecutionCaseId(prosecutionCase.getId())
                                                    .withHearingDays(hearing.getHearingDays())
                                                    .withHearingType(hearing.getType())
                                                    .build())
                            )
            );
        }
        return listProsecutionCasesReferredToCourts;
    }


}
