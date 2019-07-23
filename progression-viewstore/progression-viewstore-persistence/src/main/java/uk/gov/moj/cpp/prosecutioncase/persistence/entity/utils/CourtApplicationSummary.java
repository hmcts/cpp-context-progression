package uk.gov.moj.cpp.prosecutioncase.persistence.entity.utils;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultString;

import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Person;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.core.util.CollectionUtils;

@SuppressWarnings({"squid:S2384", "pmd:BeanMembersShouldSerialize"})
public class CourtApplicationSummary {

    private final String applicationId;

    private final String applicationTitle;

    private final String applicationReference;

    private final String applicationStatus;

    private final String applicantDisplayName;

    private final List<String> respondentDisplayNames;

    private final Boolean isAppeal;

    private CourtApplicationSummary(final String applicationId, final String applicationTitle, final String applicationReference,
                                    final String applicationStatus, final String applicantDisplayName, final List<String> respondentDisplayNames, final Boolean isAppeal) {
        this.applicationId = applicationId;
        this.applicationTitle = applicationTitle;
        this.applicationReference = applicationReference;
        this.applicationStatus = applicationStatus;
        this.applicantDisplayName = applicantDisplayName;
        this.respondentDisplayNames = respondentDisplayNames;
        this.isAppeal = isAppeal;
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

    public List<String> getRespondentDisplayNames() {
        return respondentDisplayNames;
    }

    public Boolean getIsAppeal() {
        return isAppeal;
    }

    public static CourtApplicationSummary.Builder applicationSummary() {
        return new CourtApplicationSummary.Builder();
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static class Builder {

        private String applicationId;

        private String applicationReference;

        private String applicationTitle;

        private String applicationStatus;

        private String applicantDisplayName;

        private List<String> respondentDisplayNames;

        private Boolean isAppeal;

        public Builder withApplicationId(final String applicationId) {
            this.applicationId = applicationId;
            return this;
        }

        public Builder withApplicationReference(final String applicationReference) {
            this.applicationReference = applicationReference;
            return this;
        }

        public Builder withApplicationTitle(final CourtApplicationType courtApplicationType) {
            this.applicationTitle = Objects.nonNull(courtApplicationType) ? courtApplicationType.getApplicationType() : EMPTY;
            return this;
        }

        public Builder withApplicationStatus(final ApplicationStatus status) {
            this.applicationStatus = Objects.nonNull(status) ? status.name() : EMPTY;
            return this;
        }

        public Builder withApplicantDisplayName(final CourtApplicationParty courtApplicationParty) {
            this.applicantDisplayName = extractDisplayName(courtApplicationParty);
            return this;
        }

        public Builder withRespondentDisplayNames(List<CourtApplicationRespondent> respondents) {
            if(!CollectionUtils.isEmpty(respondents)) {
                this.respondentDisplayNames =
                respondents.stream().map(respondent -> extractDisplayName(respondent.getPartyDetails()))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            }
            return this;
        }

        public Builder withIsAppeal(final Boolean appeal) {
            this.isAppeal = appeal;
            return this;
        }

        public CourtApplicationSummary build() {
            return new CourtApplicationSummary(applicationId, applicationTitle, applicationReference, applicationStatus, applicantDisplayName, respondentDisplayNames, isAppeal);
        }

        private String extractDisplayName(final CourtApplicationParty applicationParty) {
            Optional<String> displayName = Optional.empty();
            final Defendant defendant = applicationParty.getDefendant();
            if (Objects.nonNull(defendant) && Objects.nonNull(defendant.getPersonDefendant())) {
                displayName = getPersonName(defendant.getPersonDefendant().getPersonDetails());
            }
            if (!displayName.isPresent() && Objects.nonNull(applicationParty.getPersonDetails())) {
                displayName = getPersonName(applicationParty.getPersonDetails());
            }
            if (!displayName.isPresent() && Objects.nonNull(applicationParty.getOrganisation())) {
                displayName = Optional.of(defaultString(applicationParty.getOrganisation().getName()));
            }
            if (!displayName.isPresent() && Objects.nonNull(applicationParty.getProsecutingAuthority())) {
                displayName = Optional.of(defaultString(applicationParty.getProsecutingAuthority().getProsecutionAuthorityCode()));
            }
            return displayName.orElse(EMPTY);
        }

        private Optional<String> getPersonName(final Person person) {
            return Optional.ofNullable(Stream.of(person.getFirstName(), person.getMiddleName(), person.getLastName())
                    .filter(StringUtils::isNotBlank).collect(Collectors.joining(" ")));

        }
    }
}
