package uk.gov.moj.cpp.progression.command.helper;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.core.courts.LinkType.STANDALONE;
import static uk.gov.moj.cpp.progression.command.helper.ChildApplicationCreationValidator.ApplicationKind.ACTIVE;
import static uk.gov.moj.cpp.progression.command.helper.ChildApplicationCreationValidator.ApplicationKind.CHILD;
import static uk.gov.moj.cpp.progression.command.helper.ChildApplicationCreationValidator.ApplicationKind.INACTIVE;
import static uk.gov.moj.cpp.progression.command.helper.ChildApplicationCreationValidator.Outcome.NO_RULE;
import static uk.gov.moj.cpp.progression.command.helper.ChildApplicationCreationValidator.Outcome.PARENT_ONLY;
import static uk.gov.moj.cpp.progression.command.helper.ChildApplicationCreationValidator.Outcome.childOf;
import static uk.gov.moj.cpp.progression.enums.ApplicationSource.AAAG;
import static uk.gov.moj.cpp.progression.enums.ApplicationSource.MH;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtHearingRequest;
import uk.gov.justice.core.courts.InitiateCourtApplicationProceedings;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.command.service.HearingQueryService;
import uk.gov.moj.cpp.progression.command.service.ProsecutionCaseQueryService;
import uk.gov.moj.cpp.progression.enums.ApplicationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

/**
 * Enforces the CHD-3025 decision trees for creating a child application from Applications At A
 * Glance (AAAG) or Manage Hearings (MH). The rules run on data fetched from the viewstore / hearing
 * context; only the new application's own payload is taken from the request.
 */
public class ChildApplicationCreationValidator {

    enum ApplicationKind { CHILD, STANDALONE, ACTIVE, INACTIVE }

    /**
     * What the new application is allowed to be. {@code parentId} is set only when the new
     * application must be a child of that parent.
     */
    record Outcome(Type type, UUID parentId) {
        enum Type { NO_RULE, PARENT_ONLY, CHILD_OF }

        static final Outcome NO_RULE = new Outcome(Type.NO_RULE, null);
        static final Outcome PARENT_ONLY = new Outcome(Type.PARENT_ONLY, null);

        static Outcome childOf(final UUID parentId) {
            return new Outcome(Type.CHILD_OF, parentId);
        }
    }

    @Inject
    private ProsecutionCaseQueryService prosecutionCaseQueryService;

    @Inject
    private HearingQueryService hearingQueryService;

    @Inject
    private ChildApplicationPermissionChecker childApplicationPermissionChecker;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    public void validate(final JsonEnvelope command) {
        final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings = jsonObjectToObjectConverter.convert(command.payloadAsJsonObject(), InitiateCourtApplicationProceedings.class);
        final ApplicationSource applicationSource = initiateCourtApplicationProceedings.getApplicationSource();
        if (applicationSource != AAAG && applicationSource != MH) {
            return;
        }

        final UUID newParentId = initiateCourtApplicationProceedings.getCourtApplication().getParentApplicationId();
        final Outcome outcome = applicationSource == AAAG
                ? forAaag(newParentId, command)
                : forManageHearing(hearingIdOf(initiateCourtApplicationProceedings), command);

        enforce(outcome, newParentId, command);
    }

    Outcome forAaag(final UUID newParentId, final JsonEnvelope command) {
        if (isNull(newParentId)) {
            throw new BadRequestException("A parent application id is required when creating an application from Applications At A Glance");
        }
        final CourtApplication parent = prosecutionCaseQueryService.getCourtProceedingsForApplication(newParentId, command)
                .orElseThrow(() -> new BadRequestException("Parent application not found for id: " + newParentId));

        return switch (classify(parent)) {
            case CHILD -> throw new BadRequestException("A child application cannot have a child application");
            case ACTIVE -> throw new BadRequestException("A child application cannot be created on an application with active offences");
            case STANDALONE, INACTIVE -> childOf(parent.getId());
        };
    }

    Outcome forManageHearing(final UUID hearingId, final JsonEnvelope command) {
        final List<CourtApplication> applications = hearingQueryService.getCourtApplications(hearingId, command);
        if (applications.isEmpty()) {
            return NO_RULE;
        }
        if (applications.size() == 1) {
            return forSingleApplicationHearing(applications.get(0));
        }
        return forMultipleApplicationsHearing(applications);
    }

    private Outcome forSingleApplicationHearing(final CourtApplication application) {
        return switch (classify(application)) {
            case STANDALONE -> throw new BadRequestException("An application cannot be added to a hearing with a standalone application");
            case CHILD -> throw new BadRequestException("A child application cannot have a child application");
            case ACTIVE -> PARENT_ONLY;
            case INACTIVE -> childOf(application.getId());
        };
    }

    private Outcome forMultipleApplicationsHearing(final List<CourtApplication> applications) {
        final Map<Boolean, List<CourtApplication>> childrenAndParents = applications.stream()
                .collect(partitioningBy(application -> nonNull(application.getParentApplicationId())));
        final List<CourtApplication> parents = childrenAndParents.get(false);

        if (parents.isEmpty()) {
            throw new BadRequestException("A child application cannot have a child application");
        }
        if (parents.size() == 1) {
            return childOf(parents.get(0).getId());
        }

        final Set<ApplicationKind> parentKinds = parents.stream().map(this::classify).collect(toSet());
        if (parentKinds.contains(ApplicationKind.STANDALONE)) {
            throw new BadRequestException("An application cannot be added to a hearing with a standalone application among several applications");
        }
        if (parentKinds.contains(ACTIVE) && parentKinds.contains(INACTIVE)) {
            throw new BadRequestException("An application cannot be added to a hearing with both active and inactive applications");
        }
        if (parentKinds.contains(INACTIVE)) {
            throw new BadRequestException("An application cannot be added to a hearing with several inactive applications: parent application is ambiguous!");
        }
        return PARENT_ONLY;
    }

    void enforce(final Outcome outcome, final UUID newParentId, final JsonEnvelope command) {
        switch (outcome.type()) {
            case PARENT_ONLY -> {
                if (nonNull(newParentId)) {
                    throw new BadRequestException("A child application cannot be created on this hearing; only a parent application is allowed");
                }
            }
            case CHILD_OF -> {
                if (!Objects.equals(outcome.parentId(), newParentId)) {
                    throw new BadRequestException("The application must be created as a child of application of: " + outcome.parentId());
                }
                if (!childApplicationPermissionChecker.hasLinkCreateChildApplicationPermission(command.metadata())) {
                    throw new ForbiddenRequestException("User is not authorised to create a child application!");
                }
            }
            case NO_RULE -> {
                // cases-only hearing: existing behaviour
            }
        }
    }

    ApplicationKind classify(final CourtApplication courtApplication) {
        if (nonNull(courtApplication.getParentApplicationId())) {
            return CHILD;
        }
        if (nonNull(courtApplication.getType()) && courtApplication.getType().getLinkType() == STANDALONE) {
            return ApplicationKind.STANDALONE;
        }
        if (nonNull(courtApplication.getCourtOrder())) {
            return INACTIVE;
        }
        if (isNotEmpty(courtApplication.getCourtApplicationCases())) {
            final List<Offence> offences = courtApplication.getCourtApplicationCases().stream()
                    .map(CourtApplicationCase::getOffences)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .toList();
            if (offences.isEmpty()) {
                return ACTIVE;
            }
            return offences.stream().allMatch(offence -> TRUE.equals(offence.getProceedingsConcluded())) ? INACTIVE : ACTIVE;
        }
        throw new BadRequestException("Application has neither court application cases nor a court order: " + courtApplication.getId());
    }

    private UUID hearingIdOf(final InitiateCourtApplicationProceedings initiateCourtApplicationProceedings) {
        return ofNullable(initiateCourtApplicationProceedings.getCourtHearing())
                .map(CourtHearingRequest::getId)
                .orElseThrow(() -> new BadRequestException("A court hearing id is required when creating an application from Manage Hearings"));
    }
}
