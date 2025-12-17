package uk.gov.moj.cpp.progression.service.utils;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;

import java.time.LocalDate;
import java.util.Optional;

public class DefendantDetailsExtractor {

    private DefendantDetailsExtractor() {
    }

    public static String getDefendantFullName(final Defendant defendant) {
        final String defendantFullName;

        if (nonNull(defendant.getPersonDefendant())) {
            final Person person = defendant.getPersonDefendant().getPersonDetails();
            defendantFullName = buildDefendantFullName(person);
        } else {
            final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
            final Organisation organisation = legalEntityDefendant.getOrganisation();
            defendantFullName = organisation.getName();
        }
        return defendantFullName;
    }

    public static String getDefendantLastName(final Defendant defendant) {
        if (nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails())) {
            return defendant.getPersonDefendant().getPersonDetails().getLastName();
        }
        return EMPTY;
    }

    public static Optional<LocalDate> getDefendantDateOfBirth(final Defendant defendant) {
        return nonNull(defendant.getPersonDefendant()) && nonNull(defendant.getPersonDefendant().getPersonDetails()) ? Optional.ofNullable(defendant.getPersonDefendant().getPersonDetails().getDateOfBirth()) : empty();
    }

    private static String buildDefendantFullName(final Person personDetails) {
        final StringBuilder sb = new StringBuilder();
        sb.append(personDetails.getFirstName()).append(SPACE);

        if (!isNullOrEmpty(personDetails.getMiddleName())) {
            sb.append(personDetails.getMiddleName()).append(SPACE);
        }

        sb.append(personDetails.getLastName());
        return sb.toString();
    }
}
