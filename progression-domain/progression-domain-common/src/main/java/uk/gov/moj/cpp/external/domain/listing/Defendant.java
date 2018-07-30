package uk.gov.moj.cpp.external.domain.listing;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("squid:S3776")
@JsonInclude(NON_NULL)
public final class Defendant implements Serializable {

    private static final long serialVersionUID = 7475556789419275121L;
    private final UUID id;
    private final UUID personId;
    private final String firstName;
    private final String lastName;
    private final String dateOfBirth;
    private final String bailStatus;
    private final String custodyTimeLimit;
    private final String defenceOrganisation;
    private final List<Offence> offences;

    @JsonCreator
    public Defendant(@JsonProperty("id") final UUID id,
                     @JsonProperty("personId") final UUID personId,
                     @JsonProperty("firstName") final String firstName,
                     @JsonProperty("lastName") final String lastName,
                     @JsonProperty("dateOfBirth") final String dateOfBirth,
                     @JsonProperty("bailStatus") final String bailStatus,
                     @JsonProperty("custodyTimeLimit") final String custodyTimeLimit,
                     @JsonProperty("defenceOrganisation") final String defenceOrganisation,
                     @JsonProperty("offences") final List<Offence> offences) {

        this.id = id;
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.defenceOrganisation = defenceOrganisation;
        this.offences = (null == offences) ? new ArrayList<>() : new ArrayList<>(offences);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public String getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public String getDefenceOrganisation() {
        return defenceOrganisation;
    }

    public List<Offence> getOffences() {
        return new ArrayList<>(offences);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Defendant)) {
            return false;
        }

        final Defendant defendant = (Defendant) o;

        if (id != null ? !id.equals(defendant.id) : defendant.id != null) {
            return false;
        }
        if (personId != null ? !personId.equals(defendant.personId) : defendant.personId != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(defendant.firstName) : defendant.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(defendant.lastName) : defendant.lastName != null) {
            return false;
        }
        if (dateOfBirth != null ? !dateOfBirth.equals(defendant.dateOfBirth) : defendant.dateOfBirth != null) {
            return false;
        }
        if (bailStatus != null ? !bailStatus.equals(defendant.bailStatus) : defendant.bailStatus != null) {
            return false;
        }
        if (custodyTimeLimit != null ? !custodyTimeLimit.equals(defendant.custodyTimeLimit) : defendant.custodyTimeLimit != null) {
            return false;
        }
        if (defenceOrganisation != null ? !defenceOrganisation.equals(defendant.defenceOrganisation) : defendant.defenceOrganisation != null) {
            return false;
        }
        return offences != null ? offences.equals(defendant.offences) : defendant.offences == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (personId != null ? personId.hashCode() : 0);
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (dateOfBirth != null ? dateOfBirth.hashCode() : 0);
        result = 31 * result + (bailStatus != null ? bailStatus.hashCode() : 0);
        result = 31 * result + (custodyTimeLimit != null ? custodyTimeLimit.hashCode() : 0);
        result = 31 * result + (defenceOrganisation != null ? defenceOrganisation.hashCode() : 0);
        result = 31 * result + (offences != null ? offences.hashCode() : 0);
        return result;
    }
}
