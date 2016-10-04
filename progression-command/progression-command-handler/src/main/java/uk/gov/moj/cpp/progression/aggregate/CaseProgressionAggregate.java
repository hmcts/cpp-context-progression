package uk.gov.moj.cpp.progression.aggregate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.command.handler.ProgressionEventFactory;
import uk.gov.moj.cpp.progression.domain.constant.CaseStatusEnum;
import uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing;
import uk.gov.moj.cpp.progression.domain.event.Defendant;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;

public class CaseProgressionAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseProgressionAggregate.class);

    ProgressionEventFactory progressionEventFactory = new ProgressionEventFactory();

    private UUID caseProgressionId;
    private Set<Defendant> defendants = new HashSet<>();
    private Set<UUID> defendantIds = new HashSet<>();
    private boolean isAllDefendantReviewed;
    private boolean isAnyDefendantPending;

    @Override
    public Object apply(Object event) {
        return match(event).with(

                when(uk.gov.moj.cpp.progression.domain.event.CaseAddedToCrownCourt.class)
                        .apply(e -> {
                            defendants.addAll(e.getDefendants());
                            defendantIds = defendants.stream().map(Defendant::getId)
                                    .collect(Collectors.toSet());
                        }),
                when(uk.gov.moj.cpp.progression.domain.event.CasePendingForSentenceHearing.class)
                        .apply(e -> {

                        }),
                when(uk.gov.moj.cpp.progression.domain.event.CaseReadyForSentenceHearing.class)
                        .apply(e -> {

                        }),
                when(uk.gov.moj.cpp.progression.domain.event.CaseAssignedForReviewUpdated.class)
                        .apply(e -> {

                        }),
                when(uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded.class)
                        .apply(e -> {
                            caseProgressionId = e.getCaseProgressionId();
                            Defendant defendant = defendants.stream()
                                    .filter((d) -> d.getId()
                                            .equals(e.getDefendantId()))
                                    .findAny().get();
                            defendant.setSentenceHearingReviewDecision(true);
                            if (e.getAdditionalInformationEvent() != null) {
                                defendant.setIsAdditionalInfoAvilable(true);
                            }
                        }));

    }

    private void checkAllDefendant() {

        // check if all defendant is reviewed
        Defendant defReviewRequire = defendants.stream()
                .filter((d) -> (d.getSentenceHearingReviewDecision() == null
                        || d.getSentenceHearingReviewDecision() == false))
                .findFirst().orElse(null);
        if (defReviewRequire == null) {
            isAllDefendantReviewed = true;
        }

        // check if any defendant additional information is required
        Defendant def = defendants.stream()
                .filter((d) -> (d.getIsAdditionalInfoAvilable() != null
                        && d.getIsAdditionalInfoAvilable() == true))
                .findFirst().orElse(null);

        if (def != null) {
            isAnyDefendantPending = true;
        }

    }


    public Stream<Object> addAdditionalInformationForDefendant(DefendantCommand defendant) {
        Stream.Builder<Object> streamBuilder = Stream.builder();
        UUID defendantId = defendant.getDefendantId();

        if (!defendantIds.contains(defendantId)) {
            LOGGER.warn("Cannot add additional information without defendant " + defendantId);
            return Stream.empty();
        }

        Defendant def = defendants.stream()
                .filter((d) -> d.getId().equals(defendant.getDefendantId())).findAny()
                .get();
        def.setSentenceHearingReviewDecision(true);
        if (defendant.getAdditionalInformation() != null) {
            def.setIsAdditionalInfoAvilable(true);
        }
        // check if all defendant's reviewed
        checkAllDefendant();
        if (caseProgressionId == null) {
            caseProgressionId = defendant.getCaseProgressionId();
        }
        if (isAllDefendantReviewed) {
            if (isAnyDefendantPending) {
                streamBuilder.add(new CasePendingForSentenceHearing(caseProgressionId,
                        CaseStatusEnum.PENDING_FOR_SENTENCING_HEARING));
            } else {
                streamBuilder.add(new CaseReadyForSentenceHearing(caseProgressionId,
                        CaseStatusEnum.READY_FOR_SENTENCING_HEARING, LocalDateTime.now()));
            }
        }

        streamBuilder.add(progressionEventFactory.addDefendantEvent(defendant));
        return apply(streamBuilder.build());
    }

}
