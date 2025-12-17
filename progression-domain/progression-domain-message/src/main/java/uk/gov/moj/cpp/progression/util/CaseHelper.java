package uk.gov.moj.cpp.progression.util;

import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class CaseHelper {

    static public Hearing addCaseToHearing(final Hearing hearing, List<ProsecutionCase> prosecutionCases){
        final Hearing updatedHearing = Hearing.hearing().withValuesFrom(hearing)
                .withProsecutionCases(ofNullable(hearing.getProsecutionCases()).orElseGet(ArrayList::new)).build();

        final List<ProsecutionCase> newlyAddedProsecutionCase = new ArrayList<>();
        prosecutionCases.forEach(prosecutionCase -> {
            final Optional<ProsecutionCase> extendedProsecutionCase = ofNullable(updatedHearing.getProsecutionCases()).stream().flatMap(Collection::stream).filter(prosecutionCase1 -> prosecutionCase1.getId().equals(prosecutionCase.getId())).findAny();
            if (extendedProsecutionCase.isPresent()) {
                final List<Defendant> newlyAddedDefendant = new ArrayList<>();
                prosecutionCase.getDefendants().forEach(defendant -> {
                    final List<Offence> newlyAddedOffence = new ArrayList<>();
                    Optional<Defendant> extendedDefended = ofNullable(extendedProsecutionCase.get().getDefendants()).stream().flatMap(Collection::stream).filter(def -> def.getId().equals(defendant.getId())).findAny();
                    if(extendedDefended.isPresent()){
                        defendant.getOffences().forEach(offence -> {
                            Optional<Offence> extendedOffence = ofNullable(extendedDefended.get().getOffences()).stream().flatMap(Collection::stream).filter(off -> off.getId().equals(offence.getId())).findAny();
                            if(extendedOffence.isEmpty()){
                                newlyAddedOffence.add(offence);
                            }
                        });
                        extendedDefended.get().getOffences().addAll(newlyAddedOffence);
                    } else {
                        newlyAddedDefendant.add(defendant);
                    }
                });
                extendedProsecutionCase.get().getDefendants().addAll(newlyAddedDefendant);
            } else {
                newlyAddedProsecutionCase.add(prosecutionCase);
            }
        });
        updatedHearing.getProsecutionCases().addAll(newlyAddedProsecutionCase);
        return updatedHearing;
    }
}
