package uk.gov.justice.services;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.unifiedsearch.client.domain.Alias;
import uk.gov.justice.services.unifiedsearch.client.domain.LaaReference;
import uk.gov.justice.services.unifiedsearch.client.domain.Offence;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DomainToIndexMapper {
    private static final String SPACE = " ";

    public Party party(final Defendant defendant) {
        final Party party = new Party();
        party.setPartyId(defendant.getId().toString());
        party.setPncId(defendant.getPncId());
        party.set_party_type("DEFENDANT");

        legalDefendant(defendant, party);
        personDefendant(defendant, party);
        alias(defendant, party);
        offences(defendant, party);
        return party;
    }

    private Party alias(final Defendant defendant, final Party party) {
        final List<DefendantAlias> defendantAliases = defendant.getAliases();
        final Set<Alias> aliasSet = new HashSet();
        if (defendantAliases != null) {
            for (final DefendantAlias defendantAlias : defendantAliases) {
                final Alias alias = new Alias();
                alias.setFirstName(defendantAlias.getFirstName());
                alias.setMiddleName(defendantAlias.getMiddleName());
                alias.setLastName(defendantAlias.getLastName());
                final String title = defendantAlias.getTitle();
                if (title != null) {
                    alias.setTitle(title);
                }
                alias.setOrganisationName(defendantAlias.getLegalEntityName());
                aliasSet.add(alias);
            }
        }
        party.setAliases(aliasSet);
        return party;
    }

    private Party legalDefendant(final Defendant defendant, final Party party) {
        final LegalEntityDefendant legalEntityDefendant = defendant.getLegalEntityDefendant();
        if (legalEntityDefendant != null) {
            party.setOrganisationName(legalEntityDefendant.getOrganisation().getName());
        }
        return party;
    }

    private Party personDefendant(final Defendant defendant, final Party party) {
        final PersonDefendant personDefendant = defendant.getPersonDefendant();
        personDefendant(defendant, party, personDefendant);
        return party;
    }


    private Party offences(final Defendant defendant, final Party party) {
        final List<uk.gov.justice.core.courts.Offence> offences = defendant.getOffences();

        final List<Offence> indexOffences = new ArrayList<>();
        if (offences != null) {
            for (final uk.gov.justice.core.courts.Offence offence : offences) {
                final Offence offence1 = new Offence();
                mapNullableAttributes(offence, offence1);

                offence1.setWording(offence.getWording());
                offence1.setLaaReference(laaReference(offence));
                offence1.setModeOfTrial(offence.getModeOfTrial());
                offence1.setOffenceCode(offence.getOffenceCode());
                offence1.setOffenceLegislation(offence.getOffenceLegislation());
                offence1.setOffenceTitle(offence.getOffenceTitle());

                indexOffences.add(offence1);
            }
        }
        party.setOffences(indexOffences);
        return party;
    }

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S2589"})
    private LaaReference laaReference(final uk.gov.justice.core.courts.Offence offence) {
        final LaaReference laaReference = new LaaReference();

        final uk.gov.justice.core.courts.LaaReference laaApplnReference = offence.getLaaApplnReference();

        if (offence != null && laaApplnReference != null
                && laaApplnReference.getApplicationReference() != null) {
            laaReference.setApplicationReference(laaApplnReference.getApplicationReference());
        }

        if (offence != null && laaApplnReference != null
                && laaApplnReference.getStatusDescription() != null) {
            laaReference.setStatusDescription(laaApplnReference.getStatusDescription());
        }

        if (offence != null && laaApplnReference != null
                && laaApplnReference.getStatusDescription() != null) {
            laaReference.setStatusCode(laaApplnReference.getStatusCode());
        }

        if (offence != null && laaApplnReference != null
                && laaApplnReference.getStatusDescription() != null) {
            laaReference.setStatusId(laaApplnReference.getStatusId().toString());
        }
        return laaReference;
    }

    private void mapNullableAttributes(final uk.gov.justice.core.courts.Offence offence,final Offence offence1) {
        if (offence.getArrestDate() != null) {
            offence1.setArrestDate(offence.getArrestDate().toString());
        }
        if (offence.getChargeDate() != null) {
            offence1.setChargeDate(offence.getChargeDate().toString());
        }
        if (offence.getEndDate() != null) {
            offence1.setEndDate(offence.getEndDate().toString());
        }
        if (offence.getStartDate() != null) {
            offence1.setStartDate(offence.getStartDate().toString());
        }
        if (offence.getDateOfInformation() != null) {
            offence1.setDateOfInformation(offence.getDateOfInformation().toString());
        }
        if (offence.getId() != null) {
            offence1.setOffenceId(offence.getId().toString());
        }
        if (offence.getOrderIndex() != null) {
            offence1.setOrderIndex(offence.getOrderIndex());
        }
        if (offence.getProceedingsConcluded() != null) {
            offence1.setProceedingsConcluded(offence.getProceedingsConcluded());
        }
    }

    private String addressLines(final Address address) {
        if (address != null) {
            final String addressLineOne = address.getAddress1();
            final String addressLineTwo = address.getAddress2();
            final String addressLineThree = address.getAddress3();
            final String addressLineFour = address.getAddress4();
            final String addressLineFive = address.getAddress5();

            return new StringBuilder(addressLineOne).append(SPACE)
                    .append(addressLineTwo)
                    .append(SPACE)
                    .append(addressLineThree)
                    .append(SPACE)
                    .append(addressLineFour)
                    .append(SPACE)
                    .append(addressLineFive).toString();
        }
        return SPACE;
    }

    private void personDefendant(final Defendant defendant,
                                 final Party party, final PersonDefendant personDefendant) {
        if (personDefendant != null) {
            party.setArrestSummonsNumber(personDefendant.getArrestSummonsNumber());
            final Person personDetails = defendant.getPersonDefendant().getPersonDetails();
            party.setFirstName(personDetails.getFirstName());
            party.setMiddleName(personDetails.getMiddleName());
            party.setLastName(personDetails.getLastName());
            party.setAddressLines(addressLines(personDetails.getAddress()));
            setAddressPostCodeOfParty(party, personDetails);
            party.setNationalInsuranceNumber(personDetails.getNationalInsuranceNumber());
            setDateOfBirthOfParty(party, personDetails);
            setGenderOfParty(party, personDetails);
            setTitleOfParty(party, personDetails);
        }
    }

    private void setAddressPostCodeOfParty(final Party party, final Person personDetails) {
        final Address address = personDetails.getAddress();
        if (address != null) {
            party.setPostCode(address.getPostcode());
        }
    }

    private void setTitleOfParty(final Party party, final Person personDetails) {
        final String title = personDetails.getTitle();
        if (title != null) {
            party.setTitle(title);
        }
    }

    private void setGenderOfParty(final Party party, final Person personDetails) {
        final Gender gender = personDetails.getGender();
        if (gender != null) {
            party.setGender(gender.toString());
        }
    }

    private void setDateOfBirthOfParty(final Party party, final Person personDetails) {
        final LocalDate dateOfBirth = personDetails.getDateOfBirth();
        if (dateOfBirth != null) {
            party.setDateOfBirth(dateOfBirth.toString());
        }
    }
}
