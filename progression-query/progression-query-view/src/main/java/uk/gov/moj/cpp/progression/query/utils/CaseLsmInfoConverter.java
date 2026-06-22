package uk.gov.moj.cpp.progression.query.utils;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Person;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import uk.gov.justice.services.common.util.UtcClock;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CaseLsmInfoConverter {

    private static final String FIRST_NAME = "firstName";
    private static final String MIDDLE_NAME = "middleName";
    private static final String LAST_NAME = "lastName";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String ID = "id";
    private static final String MASTER_DEFENDANT_ID = "masterDefendantId";
    private static final String HAS_BEEN_RESULTED = "hasBeenResulted";
    private static final String OFFENCES = "offences";
    private static final String NEXT_HEARING = "nextHearing";
    private static final String OFFENCE_TITLE = "offenceTitle";
    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_TYPE = "type";
    private static final String HEARING_DAY = "hearingDay";

    public JsonArrayBuilder convertMatchedCaseDefendants(final List<Defendant> defendants, final Hearing hearing, final UUID matchedMasterDefendantId){
        return convertDefendants(defendants, hearing, Optional.of(matchedMasterDefendantId));
    }

    public JsonArrayBuilder convertRelatedCaseDefendants(final List<Defendant> defendants, final Hearing hearing){
        return convertDefendants(defendants, hearing, Optional.empty());
    }

    private JsonArrayBuilder convertDefendants(final List<Defendant> defendants, final Hearing hearing, final Optional<UUID> matchedMasterDefendantId) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();

        for (final Defendant defendant : defendants) {
            final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder()
                    .add(ID, defendant.getId().toString())
                    .add(MASTER_DEFENDANT_ID, defendant.getMasterDefendantId().toString());

            if(matchedMasterDefendantId.isPresent()) {
                jsonObjectBuilder.add(HAS_BEEN_RESULTED, hasBeenResulted(defendant, matchedMasterDefendantId.get()));
            }

            if (defendant.getPersonDefendant() != null){
                final Person person = defendant.getPersonDefendant().getPersonDetails();
                if (person.getFirstName() != null) {
                    jsonObjectBuilder.add(FIRST_NAME, person.getFirstName());
                }

                if (person.getMiddleName() != null) {
                    jsonObjectBuilder.add(MIDDLE_NAME, person.getMiddleName());
                }
                jsonObjectBuilder.add(LAST_NAME, person.getLastName());
            } else if (defendant.getLegalEntityDefendant() != null) {
                final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
                jsonObjectBuilder.add(ORGANISATION_NAME, legalEntityDefendant.getOrganisation().getName());
            } else {
                throw new IllegalArgumentException("Both PersonDefendant and LegalEntityDefendant are null!");
            }

            jsonObjectBuilder.add(OFFENCES, convertOffences(defendant.getOffences()));

            final JsonObjectBuilder nextHearingBuilder = convertHearing(hearing);
            if (nextHearingBuilder != null) {
                jsonObjectBuilder.add(NEXT_HEARING, nextHearingBuilder);
            }

            jsonArrayBuilder.add(jsonObjectBuilder);
        }
        return jsonArrayBuilder;
    }

    private boolean hasBeenResulted(final Defendant defendant, final UUID matchedMasterDefendantId) {
        if (!matchedMasterDefendantId.equals(defendant.getMasterDefendantId())){
            return false;
        }

        final long numberOfOffencesWithProceedingsConcluded = defendant.getOffences().stream()
                .filter(offence -> offence.getProceedingsConcluded() != null && offence.getProceedingsConcluded())
                .count();

        return numberOfOffencesWithProceedingsConcluded > 0;
    }

    private JsonObjectBuilder convertHearing(final Hearing hearing) {
        if (hearing == null){
            return null;
        }

        final Optional<HearingDay> nextHearingDate = hearing.getHearingDays().stream()
                .filter(day -> day.getSittingDay().isAfter(new UtcClock().now()))
                .min(Comparator.comparing(HearingDay::getSittingDay));

        if (!nextHearingDate.isPresent()){
            return null;
        }

        return JsonObjects.createObjectBuilder()
                .add(HEARING_ID, hearing.getId().toString())
                .add(HEARING_TYPE, hearing.getType().getDescription())
                .add(HEARING_DAY,  nextHearingDate.get().getSittingDay().format(DateTimeFormatter.ISO_INSTANT));
    }

    private JsonArrayBuilder convertOffences(List<Offence> offences) {
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();

        for (final Offence offence : offences) {
            final JsonObjectBuilder jsonObjectBuilder = JsonObjects.createObjectBuilder()
                    .add(OFFENCE_TITLE, offence.getOffenceTitle());

            jsonArrayBuilder.add(jsonObjectBuilder);
        }
        return jsonArrayBuilder;
    }

}
