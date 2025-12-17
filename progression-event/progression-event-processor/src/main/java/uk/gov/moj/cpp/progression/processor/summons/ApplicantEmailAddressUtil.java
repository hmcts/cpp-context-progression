package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;

import java.util.Optional;

public class ApplicantEmailAddressUtil {

    public Optional<String> getApplicantEmailAddress(final CourtApplication courtApplication) {
        final CourtApplicationParty applicant = courtApplication.getApplicant();
        final MasterDefendant masterDefendant = applicant.getMasterDefendant();
        if (nonNull(masterDefendant)) {
            return getMasterDefendantEmailAddress(masterDefendant);
        }

        final Organisation organisation = applicant.getOrganisation();
        if (nonNull(organisation) && nonNull(organisation.getContact())) {
            return ofNullable(organisation.getContact().getPrimaryEmail());
        }

        final Person person = applicant.getPersonDetails();
        if (nonNull(person) && nonNull(person.getContact())) {
            return ofNullable(person.getContact().getPrimaryEmail());
        }

        final ProsecutingAuthority prosecutingAuthority = applicant.getProsecutingAuthority();
        if (nonNull(prosecutingAuthority) && nonNull(prosecutingAuthority.getContact())) {
            return ofNullable(prosecutingAuthority.getContact().getPrimaryEmail());
        }

        return empty();
    }

    private Optional<String> getMasterDefendantEmailAddress(final MasterDefendant masterDefendant) {
        final PersonDefendant personDefendant = masterDefendant.getPersonDefendant();
        if (nonNull(personDefendant) && nonNull(personDefendant.getPersonDetails()) && nonNull(personDefendant.getPersonDetails().getContact())) {
            return ofNullable(personDefendant.getPersonDetails().getContact().getPrimaryEmail());
        }

        final LegalEntityDefendant legalEntityDefendant = masterDefendant.getLegalEntityDefendant();
        if (nonNull(legalEntityDefendant)) {
            final Organisation organisation = legalEntityDefendant.getOrganisation();
            if (nonNull(organisation) && nonNull(organisation.getContact())) {
                return ofNullable(organisation.getContact().getPrimaryEmail());
            }
        }
        return empty();
    }
}
