package uk.gov.justice.services;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.DefendantAlias;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.MasterDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.unifiedsearch.client.domain.Alias;
import uk.gov.justice.services.unifiedsearch.client.domain.Application;
import uk.gov.justice.services.unifiedsearch.client.domain.LaaReference;
import uk.gov.justice.services.unifiedsearch.client.domain.Offence;
import uk.gov.justice.services.unifiedsearch.client.domain.Party;
import uk.gov.justice.services.unifiedsearch.client.domain.Plea;
import uk.gov.justice.services.unifiedsearch.client.domain.RepresentationOrder;
import uk.gov.justice.services.unifiedsearch.client.domain.Verdict;
import uk.gov.justice.services.unifiedsearch.client.domain.VerdictType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class DomainToIndexMapper {
    private static final String SPACE = " ";
    public static final DateTimeFormatter ISO_8601_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String DEFENDANT = "DEFENDANT";

    public Party party(final DefendantUpdate defendant) {
        final Party party = new Party();
        party.setPartyId(defendant.getId().toString());
        party.setPncId(defendant.getPncId());
        party.set_party_type(DEFENDANT);
        party.setCroNumber(defendant.getCroNumber());
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        if (null != masterDefendantId) {
            party.setMasterPartyId(masterDefendantId.toString());
        }
        final Boolean proceedingsConcluded = defendant.getProceedingsConcluded();
        if (null != proceedingsConcluded) {
            party.setProceedingsConcluded(proceedingsConcluded);
        }

        legalDefendant(defendant.getLegalEntityDefendant(), party);
        personDefendant(defendant, party);
        alias(defendant.getAliases(), party);
        offences(defendant.getOffences(), party);
        final RepresentationOrder representationOrder = new RepresentationOrder();
        final AssociatedDefenceOrganisation associatedDefenceOrganisation = defendant.getAssociatedDefenceOrganisation();
        if (associatedDefenceOrganisation != null) {
            representationOrder.setApplicationReference(associatedDefenceOrganisation.getApplicationReference());
            representationOrder.setEffectiveFromDate(associatedDefenceOrganisation.getAssociationStartDate() != null ? associatedDefenceOrganisation.getAssociationStartDate().toString() : null);
            representationOrder.setEffectiveToDate(associatedDefenceOrganisation.getAssociationEndDate() != null ? associatedDefenceOrganisation.getAssociationEndDate().toString() : null);
            representationOrder.setLaaContractNumber(associatedDefenceOrganisation.getDefenceOrganisation() != null ? associatedDefenceOrganisation.getDefenceOrganisation().getLaaContractNumber() : null);
        }

        party.setRepresentationOrder(representationOrder);
        return party;
    }

    public Party party(final Defendant defendant) {
        final Party party = new Party();
        party.setPartyId(defendant.getId().toString());
        party.setPncId(defendant.getPncId());
        party.set_party_type(DEFENDANT);
        party.setCroNumber(defendant.getCroNumber());
        if (defendant.getProceedingsConcluded() != null) {
            party.setProceedingsConcluded(defendant.getProceedingsConcluded());
        }
        final ZonedDateTime courtProceedingsInitiated = defendant.getCourtProceedingsInitiated();
        if (null != courtProceedingsInitiated) {
            party.setCourtProceedingsInitiated(courtProceedingsInitiated.format(ISO_8601_FORMATTER));
        }
        final UUID masterDefendantId = defendant.getMasterDefendantId();
        if (null != masterDefendantId) {
            party.setMasterPartyId(masterDefendantId.toString());
        }
        legalDefendant(defendant.getLegalEntityDefendant(), party);
        personDefendant(defendant, party);
        alias(defendant.getAliases(), party);
        offences(defendant.getOffences(), party);
        return party;
    }

    public Party party(final MasterDefendant masterDefendant) {
        final Party party = new Party();
        final UUID masterDefendantId = masterDefendant.getMasterDefendantId();
        if (null != masterDefendantId) {
            party.setMasterPartyId(masterDefendantId.toString());
        }
        legalDefendant(masterDefendant.getLegalEntityDefendant(), party);
        personDefendant(party, masterDefendant.getPersonDefendant());
        return party;
    }

    public Party person(final Person personDetails) {
        final Party party = new Party();
        party.setFirstName(personDetails.getFirstName());
        party.setMiddleName(personDetails.getMiddleName());
        party.setLastName(personDetails.getLastName());
        party.setAddressLines(addressLines(personDetails.getAddress()));
        party.setDefendantAddress(defendantAddress(personDetails.getAddress()));
        setAddressPostCodeOfParty(party, personDetails.getAddress());
        party.setNationalInsuranceNumber(personDetails.getNationalInsuranceNumber());
        setDateOfBirthOfParty(party, personDetails);
        setGenderOfParty(party, personDetails);
        setTitleOfParty(party, personDetails);
        return party;
    }

    public Party organisation(final Organisation organisation) {
        final Party party = new Party();
        party.setOrganisationName(organisation.getName());
        party.setAddressLines(addressLines(organisation.getAddress()));
        party.setDefendantAddress(defendantAddress(organisation.getAddress()));
        setAddressPostCodeOfParty(party, organisation.getAddress());
        return party;
    }

    public Party prosecutingAuthority(final ProsecutingAuthority prosecutingAuthority) {
        final Party party = new Party();
        party.setOrganisationName(prosecutingAuthority.getName());
        party.setAddressLines(addressLines(prosecutingAuthority.getAddress()));
        party.setDefendantAddress(defendantAddress(prosecutingAuthority.getAddress()));
        setAddressPostCodeOfParty(party, prosecutingAuthority.getAddress());
        return party;
    }

    private Party alias(final List<DefendantAlias> defendantAliases, final Party party) {
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

    private Party legalDefendant(final LegalEntityDefendant legalEntityDefendant, final Party party) {
        if (legalEntityDefendant != null) {
            final Organisation organisation = legalEntityDefendant.getOrganisation();
            ofNullable(organisation).ifPresent(org -> {
                party.setOrganisationName(org.getName());
                party.setAddressLines(addressLines(org.getAddress()));
                party.setDefendantAddress(defendantAddress(org.getAddress()));
                setAddressPostCodeOfParty(party, org.getAddress());
            });

        }
        return party;
    }

    private Party personDefendant(final Defendant defendant, final Party party) {
        final PersonDefendant personDefendant = defendant.getPersonDefendant();

        personDefendant(party, personDefendant);
        return party;
    }

    private Party personDefendant(final DefendantUpdate defendant, final Party party) {
        final PersonDefendant personDefendant = defendant.getPersonDefendant();
        personDefendant(party, personDefendant);
        return party;
    }


    private Party offences(final List<uk.gov.justice.core.courts.Offence> offences, final Party party) {

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
                ofNullable(offence.getVerdict()).ifPresent(v -> offence1.setVerdict(verdict(v)));
                ofNullable(offence.getPlea()).ifPresent(plea -> offence1.setPleas(plea(plea)));
                if ( ofNullable(offence.getIndicatedPlea()).isPresent()
                        && IndicatedPleaValue.INDICATED_GUILTY.equals(offence.getIndicatedPlea().getIndicatedPleaValue())
                        && nonNull(offence.getIndicatedPlea().getOriginatingHearingId())
                        && nonNull(offence.getIndicatedPlea().getIndicatedPleaDate())
                        && !ofNullable(offence.getPlea()).isPresent()) {
                    ofNullable(offence.getIndicatedPlea()).ifPresent(indicatedPlea -> offence1.setPleas(pleaGuilty(indicatedPlea)));
                }
                indexOffences.add(offence1);

            }
        }
        party.setOffences(indexOffences);
        return party;
    }

    private List<Plea> pleaGuilty(final IndicatedPlea indicatedPlea) {
        final Plea plea = new  uk.gov.justice.services.unifiedsearch.client.domain.Plea();
        plea.setPleaValue("INDICATED_GUILTY");
        plea.setPleaDate(indicatedPlea.getIndicatedPleaDate().toString());
        if(indicatedPlea.getOriginatingHearingId() != null) {
            plea.setOriginatingHearingId(indicatedPlea.getOriginatingHearingId().toString());
        }
        return Collections.singletonList(plea);

    }

    private Verdict verdict(final uk.gov.justice.core.courts.Verdict orgVerdict) {
        final VerdictType verdictType = new VerdictType();
        verdictType.setVerdictTypeId(orgVerdict.getVerdictType().getId().toString());
        verdictType.setCategory(orgVerdict.getVerdictType().getCategory());
        verdictType.setCategoryType(orgVerdict.getVerdictType().getCategoryType());
        ofNullable(orgVerdict.getVerdictType().getDescription()).ifPresent(verdictType::setDescription);
        ofNullable(orgVerdict.getVerdictType().getSequence()).ifPresent(verdictType::setSequence);

        final Verdict verdict = new Verdict();
        ofNullable(orgVerdict.getVerdictDate()).ifPresent(date -> verdict.setVerdictDate(date.toString()));
        verdict.setVerdictType(verdictType);
        ofNullable(orgVerdict.getOriginatingHearingId()).ifPresent(id -> verdict.setOriginatingHearingId(id.toString()));

        return verdict;
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

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S2589"})
    private List<Plea> plea(final uk.gov.justice.core.courts.Plea pleSrc) {
        final Plea plea = new Plea();

        if (pleSrc != null) {
            if (pleSrc.getOriginatingHearingId() != null) {
                plea.setOriginatingHearingId(pleSrc.getOriginatingHearingId().toString());
            }

            if (pleSrc.getPleaValue() != null) {
                plea.setPleaValue(pleSrc.getPleaValue());
            }

            if (pleSrc.getPleaDate() != null) {
                plea.setPleaDate(pleSrc.getPleaDate().toString());
            }
        }

        return Collections.singletonList(plea);
    }


    private void mapNullableAttributes(final uk.gov.justice.core.courts.Offence offence, final Offence offence1) {
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

    public static String addressLines(final Address address) {
        if (address != null) {
            final String addressLineOne = address.getAddress1();
            final String addressLineTwo = address.getAddress2();
            final String addressLineThree = address.getAddress3();
            final String addressLineFour = address.getAddress4();
            final String addressLineFive = address.getAddress5();

            return Stream.of(addressLineOne, addressLineTwo, addressLineThree, addressLineFour, addressLineFive).
                    filter(StringUtils::isNotBlank).
                    map(StringUtils::trim).
                    collect(joining(SPACE));
        }
        return SPACE;
    }

    private void personDefendant(final Party party, final PersonDefendant personDefendant) {
        if (personDefendant != null) {
            final Person personDetails = personDefendant.getPersonDetails();
            party.setArrestSummonsNumber(personDefendant.getArrestSummonsNumber());
            party.setFirstName(personDetails.getFirstName());
            party.setMiddleName(personDetails.getMiddleName());
            party.setLastName(personDetails.getLastName());
            party.setAddressLines(addressLines(personDetails.getAddress()));
            party.setDefendantAddress(defendantAddress(personDetails.getAddress()));
            setAddressPostCodeOfParty(party, personDetails.getAddress());
            party.setNationalInsuranceNumber(personDetails.getNationalInsuranceNumber());
            setDateOfBirthOfParty(party, personDetails);
            setGenderOfParty(party, personDetails);
            setTitleOfParty(party, personDetails);
        }
    }


    private uk.gov.justice.services.unifiedsearch.client.domain.Address defendantAddress(final Address address) {
        final uk.gov.justice.services.unifiedsearch.client.domain.Address defendantAddress = new uk.gov.justice.services.unifiedsearch.client.domain.Address();

        if (address != null) {
            defendantAddress.setAddress1(address.getAddress1());
            defendantAddress.setAddress2(address.getAddress2());
            defendantAddress.setAddress3(address.getAddress3());
            defendantAddress.setAddress4(address.getAddress4());
            defendantAddress.setAddress5(address.getAddress5());
            defendantAddress.setPostCode(address.getPostcode());
        }
        return defendantAddress;
    }

    private void setAddressPostCodeOfParty(final Party party, final Address address) {
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

    public Application application(final Application courtApplication) {
        final Application application = new Application();
        application.setApplicationStatus(courtApplication.getApplicationStatus());
        application.setApplicationId(courtApplication.getApplicationId());
        return application;
    }
}
