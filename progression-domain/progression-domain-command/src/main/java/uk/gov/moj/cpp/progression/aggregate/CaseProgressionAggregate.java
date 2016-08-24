package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.domain.event.Defendant;

public class CaseProgressionAggregate implements Aggregate {

    private UUID caseProgressionId;
    private Set<Defendant> defendants = new HashSet<>();

    @Override
    public Object apply(Object event) {
        return match(event).with(

                when(uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt.class).apply(e -> {
                    defendants.addAll(e.getDefendants());
                }), when(uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded.class)
                        .apply(e -> {
                            caseProgressionId = e.getCaseProgressionId();
                        }));

    }

    public Stream<Object> addAdditionalInformationForDefendant(Object defendantCommand) {

        // check if all defendant's sentence hearing review is true

        return apply(Stream.of(defendantCommand));
    }

}
