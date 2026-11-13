package uk.gov.moj.cpp.progression.listing.domain;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067", "squid:S2065", "PMD.BeanMembersShouldSerialize"})
public class Defendant {
    private Optional<BailStatus> bailStatus;

    private Optional<String> custodyTimeLimit;

    private Optional<String> dateOfBirth;

    private Optional<String> datesToAvoid;

    private Optional<String> defenceOrganisation;

    private Optional<String> firstName;

    private Optional<HearingLanguageNeeds> hearingLanguageNeeds;

    private final UUID id;

    private Optional<UUID> masterDefendantId;

    private Optional<ZonedDateTime> courtProceedingsInitiated;

    private Optional<String> lastName;

    private final UUID prosecutionCaseId;

    private final List<Offence> offences;

    private Optional<String> organisationName;

    private Optional<String> specificRequirements;

    private Optional<Boolean> isYouth;


    private final transient Optional<String> nationalityDescription;

    private final transient Optional<Address> address;

    @SuppressWarnings({"squid:S00107", "squid:S1067"})
    @JsonCreator
    public Defendant(@JsonProperty("bailStatus") final Optional<BailStatus> bailStatus,
                     @JsonProperty("custodyTimeLimit") final Optional<String> custodyTimeLimit,
                     @JsonProperty("dateOfBirth") final Optional<String> dateOfBirth,
                     @JsonProperty("datesToAvoid") final Optional<String> datesToAvoid,
                     @JsonProperty("defenceOrganisation") final Optional<String> defenceOrganisation,
                     @JsonProperty("firstName") final Optional<String> firstName,
                     @JsonProperty("hearingLanguageNeeds") final Optional<HearingLanguageNeeds> hearingLanguageNeeds,
                     @JsonProperty("id") final UUID id,
                     @JsonProperty("masterDefendantId") final Optional<UUID> masterDefendantId,
                     @JsonProperty("courtProceedingsInitiated") final Optional<ZonedDateTime> courtProceedingsInitiated,
                     @JsonProperty("lastName") final Optional<String> lastName,
                     @JsonProperty("prosecutionCaseId") final UUID prosecutionCaseId,
                     @JsonProperty("offences") final List<Offence> offences,
                     @JsonProperty("organisationName") final Optional<String> organisationName,
                     @JsonProperty("specificRequirements") final Optional<String> specificRequirements,
                     @JsonProperty("isYouth") final Optional<Boolean> isYouth,
                     @JsonProperty("nationalityDescription") final Optional<String> nationalityDescription,
                     @JsonProperty("address") final Optional<Address> address) {
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.dateOfBirth = dateOfBirth;
        this.datesToAvoid = datesToAvoid;
        this.defenceOrganisation = defenceOrganisation;
        this.firstName = firstName;
        this.hearingLanguageNeeds = hearingLanguageNeeds;
        this.id = id;
        this.masterDefendantId = masterDefendantId;
        this.courtProceedingsInitiated = courtProceedingsInitiated;
        this.lastName = lastName;
        this.prosecutionCaseId = prosecutionCaseId;
        this.offences = offences == null ? emptyList() : offences;
        this.organisationName = organisationName;
        this.specificRequirements = specificRequirements;
        this.isYouth = isYouth;
        this.nationalityDescription = nationalityDescription;
        this.address = address;
    }

    public static Builder defendant() {
        return new Defendant.Builder();
    }

    public Optional<BailStatus> getBailStatus() {
        return bailStatus.isPresent() ? bailStatus : empty();
    }

    public Optional<String> getCustodyTimeLimit() {
        return custodyTimeLimit.isPresent() ? custodyTimeLimit : empty();
    }

    public Optional<String> getDateOfBirth() {
        return dateOfBirth.isPresent() ? dateOfBirth : empty();
    }

    public Optional<String> getDatesToAvoid() {
        return datesToAvoid.isPresent() ? datesToAvoid : empty();
    }

    public Optional<String> getDefenceOrganisation() {
        return defenceOrganisation.isPresent() ? defenceOrganisation : empty();
    }

    public Optional<String> getFirstName() {
        return firstName.isPresent() ? firstName : empty();
    }

    public Optional<HearingLanguageNeeds> getHearingLanguageNeeds() {
        return hearingLanguageNeeds.isPresent() ? hearingLanguageNeeds : empty();
    }

    public UUID getId() {
        return id;
    }

    public Optional<UUID> getMasterDefendantId() {
        return masterDefendantId.isPresent() ? masterDefendantId : empty();
    }

    public Optional<ZonedDateTime> getCourtProceedingsInitiated() {
        return courtProceedingsInitiated.isPresent() ? courtProceedingsInitiated : empty();
    }

    public Optional<String> getLastName() {
        return lastName.isPresent() ? lastName : empty();
    }

    @SuppressWarnings({"squid:S2384"})
    public List<Offence> getOffences() {
        return offences;
    }

    public Optional<String> getOrganisationName() {
        return organisationName;
    }

    public Optional<String> getSpecificRequirements() {
        return specificRequirements;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public Optional<Boolean> getIsYouth() {
        return isYouth;
    }

    public Optional<String> getNationalityDescription() {
        return nationalityDescription;
    }

    public Optional<Address> getAddress() {
        return address;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Defendant))
            return false;
        final Defendant defendant = (Defendant) o;
        return Objects.equals(bailStatus, defendant.bailStatus) &&
                Objects.equals(custodyTimeLimit, defendant.custodyTimeLimit) &&
                Objects.equals(dateOfBirth, defendant.dateOfBirth) &&
                Objects.equals(datesToAvoid, defendant.datesToAvoid) &&
                Objects.equals(defenceOrganisation, defendant.defenceOrganisation) &&
                Objects.equals(firstName, defendant.firstName) &&
                Objects.equals(hearingLanguageNeeds, defendant.hearingLanguageNeeds) &&
                Objects.equals(id, defendant.id) &&
                Objects.equals(masterDefendantId, defendant.masterDefendantId) &&
                Objects.equals(courtProceedingsInitiated, defendant.courtProceedingsInitiated) &&
                Objects.equals(lastName, defendant.lastName) &&
                Objects.equals(prosecutionCaseId, defendant.prosecutionCaseId) &&
                Objects.equals(offences, defendant.offences) &&
                Objects.equals(organisationName, defendant.organisationName) &&
                Objects.equals(specificRequirements, defendant.specificRequirements) &&
                Objects.equals(isYouth, defendant.isYouth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bailStatus, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, masterDefendantId, courtProceedingsInitiated, lastName, prosecutionCaseId, offences, organisationName, specificRequirements, isYouth);
    }

    @Override
    public String toString() {
        return "Defendant{" +
                "bailStatus=" + bailStatus +
                ", custodyTimeLimit=" + custodyTimeLimit +
                ", dateOfBirth=" + dateOfBirth +
                ", datesToAvoid=" + datesToAvoid +
                ", defenceOrganisation=" + defenceOrganisation +
                ", firstName=" + firstName +
                ", hearingLanguageNeeds=" + hearingLanguageNeeds +
                ", id=" + id +
                ", masterDefendantId=" + masterDefendantId +
                ", courtProceedingsInitiated=" + courtProceedingsInitiated +
                ", lastName=" + lastName +
                ", prosecutionCaseId=" + prosecutionCaseId +
                ", offences=" + offences +
                ", organisationName=" + organisationName +
                ", specificRequirements=" + specificRequirements +
                ", isYouth=" + isYouth +
                ", nationalityDescription=" + nationalityDescription +
                ", address=" + address +
                '}';
    }

    @SuppressWarnings("pmd:BeanMembersShouldSerialize")
    public static final class Builder {
        private Optional<BailStatus> bailStatus = empty();
        private Optional<String> custodyTimeLimit = empty();
        private Optional<String> dateOfBirth = empty();
        private Optional<String> datesToAvoid = empty();
        private Optional<String> defenceOrganisation = empty();
        private Optional<String> firstName = empty();
        private Optional<HearingLanguageNeeds> hearingLanguageNeeds = empty();
        private UUID id;
        private Optional<UUID> masterDefendantId = empty();
        private Optional<ZonedDateTime> courtProceedingsInitiated = empty();
        private Optional<String> lastName = empty();
        private UUID prosecutionCaseId;
        private List<Offence> offences;
        private Optional<String> organisationName = empty();
        private Optional<String> specificRequirements = empty();
        private Optional<Boolean> isYouth = empty();
        private transient Optional<String> nationalityDescription = empty();
        private transient Optional<Address> address = empty();

        private Builder() {
        }

        public Builder withBailStatus(final Optional<BailStatus> bailStatus) {
            this.bailStatus = bailStatus;
            return this;
        }

        public Builder withCustodyTimeLimit(final Optional<String> custodyTimeLimit) {
            this.custodyTimeLimit = custodyTimeLimit;
            return this;
        }

        public Builder withDateOfBirth(final Optional<String> dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder withDatesToAvoid(final Optional<String> datesToAvoid) {
            this.datesToAvoid = datesToAvoid;
            return this;
        }

        public Builder withDefenceOrganisation(final Optional<String> defenceOrganisation) {
            this.defenceOrganisation = defenceOrganisation;
            return this;
        }

        public Builder withFirstName(final Optional<String> firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withHearingLanguageNeeds(final Optional<HearingLanguageNeeds> hearingLanguageNeeds) {
            this.hearingLanguageNeeds = hearingLanguageNeeds;
            return this;
        }

        public Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public Builder withMasterDefendantId(final Optional<UUID> masterDefendantId) {
            this.masterDefendantId = masterDefendantId;
            return this;
        }

        public Builder withCourtProceedingsInitiated(final Optional<ZonedDateTime> courtProceedingsInitiated) {
            this.courtProceedingsInitiated = courtProceedingsInitiated;
            return this;
        }

        public Builder withLastName(final Optional<String> lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
            this.prosecutionCaseId = prosecutionCaseId;
            return this;
        }

        @SuppressWarnings({"squid:S2384"})
        public Builder withOffences(final List<Offence> offences) {
            this.offences = offences;
            return this;
        }

        public Builder withOrganisationName(final Optional<String> organisationName) {
            this.organisationName = organisationName;
            return this;
        }

        public Builder withSpecificRequirements(final Optional<String> specificRequirements) {
            this.specificRequirements = specificRequirements;
            return this;
        }

        public Builder withIsYouth(final Optional<Boolean> isYouth) {
            this.isYouth = isYouth;
            return this;
        }


        public Builder withNationalityDescription(Optional<String> nationalityDescription) {
            this.nationalityDescription = nationalityDescription;
            return this;
        }


        public Builder withAddress(Optional<Address> address) {
            this.address = address;
            return this;
        }

        public Defendant build() {
            return new Defendant(bailStatus, custodyTimeLimit, dateOfBirth, datesToAvoid, defenceOrganisation, firstName, hearingLanguageNeeds, id, masterDefendantId, courtProceedingsInitiated, lastName, prosecutionCaseId, offences, organisationName, specificRequirements, isYouth, nationalityDescription, address);
        }
    }
}
