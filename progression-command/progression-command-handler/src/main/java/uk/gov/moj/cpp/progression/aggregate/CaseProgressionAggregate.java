package uk.gov.moj.cpp.progression.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.handler.ProgressionEventFactory;
import uk.gov.moj.cpp.progression.domain.event.Defendant;

public class CaseProgressionAggregate implements Aggregate {

    ProgressionEventFactory progressionEventFactory = new ProgressionEventFactory();

    private UUID caseProgressionId;
    private Set<Defendant> defendants = new HashSet<>();
    private Set<UUID> defendantIds = new HashSet<>();

    @Override
    public Object apply(Object event) {
        return match(event).with(

                        when(uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt.class)
                                        .apply(e -> {
                                            defendants.addAll(e.getDefendants());
                                            defendantIds = defendants.stream().map(Defendant::getId)
                                                            .collect(Collectors.toSet());
                                        }),
                        when(uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded.class)
                                        .apply(e -> {
                                            caseProgressionId = e.getCaseProgressionId();
                                        }));

    }

    public Stream<Object> addAdditionalInformationForDefendant(DefendantCommand defendant) {

        UUID defendantId = defendant.getDefendantId();
        // assertPrecondition(defendantIds.contains(defendantId)).orElseThrow(
        // "Cannot add additional information without defendant " + defendantId);

        // check if all defendant's sentence hearing review is true

        return apply(Stream.of(progressionEventFactory.addDefendantEvent(defendant)));
    }

}
