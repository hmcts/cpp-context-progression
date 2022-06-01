package uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Person;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.core.util.CollectionUtils;

@SuppressWarnings({"squid:S2384", "PMD.BeanMembersShouldSerialize", "squid:S00107"})
public class CourtApplicationSummary {

    private final String applicationId;

    private final String applicationTitle;

    private final String applicationReference;

    private final String applicationStatus;

    private final String applicantDisplayName;

    private final UUID applicantId;

    private final List<String> respondentDisplayNames;

    private final List<UUID> respondentIds;

    private final UUID subjectId;

    private final Boolean isAppeal;

    private UUID assignedUserId;

    private final String removalReason;

    private CourtApplicationSummary(final String applicationId, final String applicationTitle, final String applicationReference,
                                    final String applicationStatus, final String applicantDisplayName, final List<String> respondentDisplayNames, final Boolean isAppeal,
                                    final UUID assignedUserId, final String removalReason, final UUID applicantId, final List<UUID> respondentIds, final UUID subjectId) {
        this.applicationId = applicationId;
        this.applicationTitle = applicationTitle;
        this.applicationReference = applicationReference;
        this.applicationStatus = applicationStatus;
        this.applicantDisplayName = applicantDisplayName;
        this.respondentDisplayNames = respondentDisplayNames;
        this.isAppeal = isAppeal;
        this.assignedUserId = assignedUserId;
        this.removalReason = removalReason;
        this.applicantId = applicantId;
        this.respondentIds = respondentIds;
        this.subjectId = subjectId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getApplicationTitle() {
        return applicationTitle;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public String getApplicantDisplayName() {
        return applicantDisplayName;
    }

    public UUID getApplicantId() { return applicantId; }

    public List<UUID> getRespondentIds() { return respondentIds; }

    public UUID getSubjectId() { return subjectId; }

    public List<String> getRespondentDisplayNames() {
        return respondentDisplayNames;
    }

    public Boolean getIsAppeal() {
        return isAppeal;
    }

    public String getRemovalReason() {
        return removalReason;
    }

    public static CourtApplicationSummary.Builder applicationSummary() {
        return new CourtApplicationSummary.Builder();
    }

    public UUID getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(UUID assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {

        private String applicationId;

        private String applicationReference;

        private String applicationTitle;

        private String applicationStatus;

        private String applicantDisplayName;

        private UUID applicantId;

        private List<String> respondentDisplayNames;

        private List<UUID> respondentIds;

        private UUID subjectId;

        private Boolean isAppeal;

        private UUID assignedUserId;
        private String removalReason;

        public Builder withApplicantId(final UUID applicantId) {
            this.applicantId = applicantId;
            return this;
        }

        public Builder withRespondentIds(List<CourtApplicationParty> respondents) {
            if (!CollectionUtils.isEmpty(respondents)) {
                this.respondentIds =
                        respondents.stream()
                                .filter(courtApplicationParty -> nonNull(courtApplicationParty.getMasterDefendant()))
                                .map(courtApplicationParty -> courtApplicationParty.getMasterDefendant().getMasterDefendantId())
                                .collect(Collectors.toList());
            }
            return this;
        }

        public Builder withSubjectId(final UUID subjectId) {
            this.subjectId = subjectId;
            return this;
        }

        public Builder withApplicationId(final String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder withApplicationReference(final String applicationReference) {
            this.applicationReference = applicationReference;
            return this;
        }

        public Builder withApplicationTitle(final CourtApplicationType courtApplicationType) {
            this.applicationTitle = nonNull(courtApplicationType) ? courtApplicationType.getType() : EMPTY;
            return this;
        }

        public Builder withApplicationStatus(final ApplicationStatus status) {
            this.applicationStatus = nonNull(status) ? status.name() : EMPTY;
            return this;
        }

        public Builder withApplicantDisplayName(final CourtApplicationParty courtApplicationParty) {
            this.applicantDisplayName = extractDisplayName(courtApplicationParty);
            return this;
        }

        public Builder withRemovalReason(final String removalReason) {
            this.removalReason = removalReason;
            return this;
        }

        public Builder withRespondentDisplayNames(List<CourtApplicationParty> respondents) {
            if (!CollectionUtils.isEmpty(respondents)) {
                this.respondentDisplayNames =
                        respondents.stream().map(this::extractDisplayName)
                                .filter(StringUtils::isNotBlank)
                                .collect(Collectors.toList());
            }
            return this;
        }

        public Builder withIsAppeal(final Boolean appeal) {
            this.isAppeal = appeal;
            return this;
        }

        public Builder withAssignedUserId(final UUID assignedUserId) {
            this.assignedUserId = assignedUserId;
            return this;
        }

        public CourtApplicationSummary build() {
            return new CourtApplicationSummary(applicationId, applicationTitle, applicationReference, applicationStatus, applicantDisplayName, respondentDisplayNames, isAppeal, assignedUserId, removalReason, applicantId, respondentIds, subjectId);
        }

        private String extractDisplayName(final CourtApplicationParty applicationParty) {
            Optional<String> displayName = Optional.empty();
            final MasterDefendant masterDefendant = applicationParty.getMasterDefendant();
            if (nonNull(masterDefendant)) {
                if (nonNull(masterDefendant.getPersonDefendant())) {
                    displayName = getPersonName(masterDefendant.getPersonDefendant().getPersonDetails());
                }
                if (nonNull(masterDefendant.getLegalEntityDefendant())) {
                    displayName = Optional.of(masterDefendant.getLegalEntityDefendant().getOrganisation().getName());
                }
            }

            if (!displayName.isPresent()) {
                if (nonNull(applicationParty.getPersonDetails())) {
                    displayName = getPersonName(applicationParty.getPersonDetails());
                }
                if (nonNull(applicationParty.getOrganisation())) {
                    displayName = Optional.of(defaultString(applicationParty.getOrganisation().getName()));
                }
                if (nonNull(applicationParty.getProsecutingAuthority())) {
                    displayName = Optional.of(defaultString(applicationParty.getProsecutingAuthority().getProsecutionAuthorityCode()));
                }
            }
            return displayName.orElse(EMPTY);
        }


        private Optional<String> getPersonName(final Person person) {
            return Optional.of(Stream.of(person.getFirstName(), person.getMiddleName(), person.getLastName())
                    .filter(StringUtils::isNotBlank).collect(Collectors.joining(" ")));
        }
    }
}
