package uk.gov.moj.cpp.progression.service.disqualificationreferral;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isBlank;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.ReferredPerson;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.json.JsonObjectBuilder;

public abstract class BaseDataAggregator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    public abstract String getCourtHouseName(final CourtCentre organisationUnit);

    public abstract String getLjaName(final LjaDetails ljaDetails);


    protected JsonObjectBuilder buildDefendantPerson(final ReferredPerson personDetails) {

        final Optional<LocalDate> dateOfBirth = Optional.ofNullable(personDetails.getDateOfBirth());
        final JsonObjectBuilder defendantBuilder = createObjectBuilder()
                .add("name", getFullName(personDetails));
        if(dateOfBirth.isPresent()) {
            defendantBuilder.add("dateOfBirth", getDateTimeFormatter().format(personDetails.getDateOfBirth()));
        }
        return defendantBuilder;
    }

    public static String getFullName(final ReferredPerson personDetails) {
        final String title = personDetails.getTitle();
        return (isBlank(title) ? "" : title + " ") + personDetails.getFirstName() + " " + personDetails.getLastName();
    }


    protected DateTimeFormatter getDateTimeFormatter() {
        return FORMATTER;
    }
}