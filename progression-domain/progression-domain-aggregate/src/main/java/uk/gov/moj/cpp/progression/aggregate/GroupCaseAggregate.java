package uk.gov.moj.cpp.progression.aggregate;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Stream.empty;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.core.courts.CourtProceedingsInitiated;
import uk.gov.justice.core.courts.CourtReferral;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.moj.cpp.progression.events.CaseRemovedFromGroupCases;
import uk.gov.moj.cpp.progression.events.CivilCaseExists;
import uk.gov.moj.cpp.progression.events.LastCaseToBeRemovedFromGroupCasesRejected;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class GroupCaseAggregate implements Aggregate {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupCaseAggregate.class);
    private static final long serialVersionUID = 101L;

    private final Set<UUID> groupCases = new HashSet<>();
    private final Set<UUID> memberCases = new HashSet<>();
    private UUID groupMaster;
    private ProsecutionCase masterCase;

    @Override
    public Object apply(final Object event) {
        return match(event).with(
                when(CourtProceedingsInitiated.class).apply(e ->
                        e.getCourtReferral().getProsecutionCases().forEach(pCase -> {
                            if (nonNull(pCase.getGroupId())) {
                                final UUID caseId = pCase.getId();
                                this.groupCases.add(caseId);
                                this.memberCases.add(caseId);
                                if (nonNull(pCase.getIsGroupMaster()) && pCase.getIsGroupMaster()) {
                                    this.groupMaster = caseId;
                                    this.masterCase = pCase;
                                }
                            }
                        })
                ),
                when(CaseRemovedFromGroupCases.class).apply(e -> {
                    this.memberCases.remove(e.getRemovedCase().getId());
                    if (nonNull(e.getNewGroupMaster())) {
                        this.groupMaster = e.getNewGroupMaster().getId();
                        this.masterCase = e.getNewGroupMaster();
                    }
                }),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> initiateCourtProceedings(final CourtReferral courtReferral) {
        LOGGER.info("Court Proceedings being initiated");
        return apply(Stream.of(CourtProceedingsInitiated.courtProceedingsInitiated().withCourtReferral(courtReferral).build()));
    }

    public Stream<Object> raiseGroupCaseExists(final UUID groupId, final ProsecutionCase prosecutionCase) {

        final ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();
        LOGGER.info("Raise Civil Case Exists event for URN = {} with prosecutionCaseId = {}", prosecutionCaseIdentifier.getCaseURN(), prosecutionCase.getId());
        return apply(
                Stream.of(
                        CivilCaseExists.civilCaseExists()
                                .withGroupId(groupId)
                                .withCaseUrn(prosecutionCaseIdentifier.getCaseURN())
                                .withProsecutionCaseId(prosecutionCase.getId())
                                .build()
                )
        );

    }

    public Stream<Object> removeCaseFromGroupCases(final UUID groupId, final ProsecutionCase removedCase, final ProsecutionCase newGroupMaster) {
        if (isNull(removedCase)) {
            LOGGER.info("Null cannot be removed from group.");
            return empty();
        } else if (!this.memberCases.contains(removedCase.getId())) {
            LOGGER.info("Case with id {} already removed from group.", removedCase.getId());
            return empty();
        } else {
            return apply(Stream.of(CaseRemovedFromGroupCases
                    .caseRemovedFromGroupCases()
                    .withGroupId(groupId)
                    .withMasterCaseId(groupMaster)
                    .withRemovedCase(removedCase)
                    .withNewGroupMaster(newGroupMaster)
                    .build()));
        }
    }

    public Stream<Object> rejectLastCaseToBeRemovedFromGroup(final UUID groupId, final UUID removedCaseId) {
        return apply(Stream.of(LastCaseToBeRemovedFromGroupCasesRejected
                .lastCaseToBeRemovedFromGroupCasesRejected()
                .withGroupId(groupId)
                .withCaseId(removedCaseId)
                .build()));
    }

    public UUID getNewGroupMaster(final UUID removedCaseId) {
        if (removedCaseId.equals(this.groupMaster)) {
            return this.memberCases.stream()
                    .filter(member -> !(member.equals(this.groupMaster)))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public boolean canBeRemoved(final UUID caseId) {
        return this.memberCases.contains(caseId)
                && this.memberCases.size() > 1;
    }

    public Set<UUID> getMemberCases() {
        return Collections.unmodifiableSet(memberCases);
    }

    public UUID getGroupMaster() {
        return groupMaster;
    }

    public ProsecutionCase getMasterCase() {
        return masterCase;
    }
}
