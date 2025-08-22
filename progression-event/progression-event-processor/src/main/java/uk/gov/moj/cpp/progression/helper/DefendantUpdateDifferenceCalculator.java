package uk.gov.moj.cpp.progression.helper;

import static java.util.Objects.nonNull;

import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedPerson;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.DefendantUpdate;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;

/**
 * https://tools.hmcts.net/jira/browse/GPE-12938 We need to update matched defendant fields which
 * are updated in original defendant <br>
 * <p><ul>
 * <li>We have originalDefendantPreviousVersion, originalDefendantNextVersion We calculate changed
 * fields according to lists below (List is provided in Story)
 * <li>We apply same changes over matchedDefendantPreviousVersion, we we will have DefendantUpdate
 * for matched defendants
 * </ul>
 * <b>Here we update these fields</b>
 * <pre>
 *  1. Name
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getFirstName()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getLastName()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getMiddleName()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getTitle()
 * 2. Address
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getAddress()
 * 3. Youth
 *      defendantUpdate.getIsYouth()
 * 4. DOB
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getDateOfBirth()
 * 5. Gender
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getGender()
 * 6. Nationality
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getNationalityId()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getNationalityCode()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getNationalityDescription()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getAdditionalNationalityId()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getAdditionalNationalityCode()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getAdditionalNationalityDescription()
 * 7. Def contact details
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getContact().getPrimaryEmail()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getContact().getSecondaryEmail()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getContact().getFax()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getContact().getMobile()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getContact().getHome()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getContact().getWork()
 * 8. Special Needs
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getSpecificRequirements()
 * 9. Written lang needs defendantUpdate.getPersonDefendant().getPersonDetails().getInterpreterLanguageNeeds()
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getDocumentationLanguageNeeds()
 * 10. Income and employment
 *      defendantUpdate.getPersonDefendant().getEmployerOrganisation()
 *      defendantUpdate.getPersonDefendant().getEmployerPayrollReference()
 * 11. NI Number
 *      defendantUpdate.getPersonDefendant().getPersonDetails().getNationalInsuranceNumber()
 * 12. Driver number
 *      defendantUpdate.getPersonDefendant().getDriverLicenceCode()
 *      defendantUpdate.getPersonDefendant().getDriverNumber()
 *      defendantUpdate.getPersonDefendant().getDriverLicenseIssue()
 * 13. Parent guardian details
 *      defendantUpdate.getAssociatedPersons()[PARENT].getContact().getPrimaryEmail()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getContact().getSecondaryEmail()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getContact().getFax()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getContact().getMobile()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getContact().getHome()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getContact().getWork()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getFirstName()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getLastName()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getMiddleName()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getTitle()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getAddress()
 *      defendantUpdate.getAssociatedPersons()[PARENT].getGender()
 *      ...
 * </pre>
 */
public class DefendantUpdateDifferenceCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefendantUpdateDifferenceCalculator.class);

    public static final String ROLE_PARENT = "PARENT";
    public static final String ROLE_PARENT_GUARDIAN = "PARENTGUARDIAN";
    private final DefendantUpdate originalDefendantPreviousVersion;
    private final DefendantUpdate originalDefendantNextVersion;
    private final DefendantUpdate matchedDefendantPreviousVersion;

    public DefendantUpdateDifferenceCalculator(
            final DefendantUpdate originalDefendantPreviousVersion,
            final DefendantUpdate originalDefendantNextVersion,
            final DefendantUpdate matchedDefendantPreviousVersion
    ) {
        this.originalDefendantPreviousVersion = originalDefendantPreviousVersion;
        this.originalDefendantNextVersion = originalDefendantNextVersion;
        this.matchedDefendantPreviousVersion = matchedDefendantPreviousVersion;
    }

    public DefendantUpdate calculateDefendantUpdate() {
        final PersonDefendant personDefendant = matchedDefendantPreviousVersion.getPersonDefendant();
        final Person personDetails = personDefendant.getPersonDetails();
        DefendantUpdate.Builder defendantUpdateBuilder = DefendantUpdate.defendantUpdate();

        defendantUpdateBuilder.withId(matchedDefendantPreviousVersion.getId())
                .withMasterDefendantId(matchedDefendantPreviousVersion.getMasterDefendantId())
                .withAliases(matchedDefendantPreviousVersion.getAliases())
                .withAssociatedDefenceOrganisation(matchedDefendantPreviousVersion.getAssociatedDefenceOrganisation())
                .withCroNumber(matchedDefendantPreviousVersion.getCroNumber())
                .withDefenceOrganisation(matchedDefendantPreviousVersion.getDefenceOrganisation())
                .withIsYouth(newValue(DefendantUpdate::getIsYouth))
                .withJudicialResults(matchedDefendantPreviousVersion.getJudicialResults())
                .withLegalEntityDefendant(matchedDefendantPreviousVersion.getLegalEntityDefendant())
                .withMitigation(matchedDefendantPreviousVersion.getMitigation())
                .withMitigationWelsh(matchedDefendantPreviousVersion.getMitigationWelsh())
                .withNumberOfPreviousConvictionsCited(matchedDefendantPreviousVersion.getNumberOfPreviousConvictionsCited())
                .withOffences(matchedDefendantPreviousVersion.getOffences())
                .withPersonDefendant(
                        getPersonDefendant(personDefendant, personDetails)
                )
                .withPncId(matchedDefendantPreviousVersion.getPncId())
                .withProceedingsConcluded(matchedDefendantPreviousVersion.getProceedingsConcluded())
                .withProsecutionAuthorityReference(matchedDefendantPreviousVersion.getProsecutionAuthorityReference())
                .withProsecutionCaseId(matchedDefendantPreviousVersion.getProsecutionCaseId())
                .withWitnessStatement(matchedDefendantPreviousVersion.getWitnessStatement())
                .withWitnessStatementWelsh(matchedDefendantPreviousVersion.getWitnessStatementWelsh());


                List<AssociatedPerson> associatedPersonList = calculateAssociatedPersons();
                if(nonNull(associatedPersonList) && !associatedPersonList.isEmpty())
                {
                    defendantUpdateBuilder.withAssociatedPersons(associatedPersonList);
                }

                return defendantUpdateBuilder.build();
    }

    private PersonDefendant getPersonDefendant(final PersonDefendant personDefendant, final Person personDetails) {
        PersonDefendant.Builder personDefendantBuilder =  PersonDefendant.personDefendant()
                .withPersonDetails(
                        calculatePerson(defendantUpdate -> defendantUpdate.getPersonDefendant().getPersonDetails(), personDetails)
                )
                .withArrestSummonsNumber(personDefendant.getArrestSummonsNumber())
                .withBailStatus(personDefendant.getBailStatus())
                .withBailConditions(personDefendant.getBailConditions())

                .withCustodyTimeLimit(personDefendant.getCustodyTimeLimit())
                .withDriverLicenceCode(newValue(
                        defendantUpdate -> defendantUpdate.getPersonDefendant().getDriverLicenceCode()))
                .withDriverNumber(newValue(
                        defendantUpdate -> defendantUpdate.getPersonDefendant().getDriverNumber()))
                .withDriverLicenseIssue(newValue(
                        defendantUpdate -> defendantUpdate.getPersonDefendant().getDriverLicenseIssue()))
                .withEmployerOrganisation(
                        calculateEmployerOrganization(defendantUpdate -> defendantUpdate.getPersonDefendant().getEmployerOrganisation()))
                .withEmployerPayrollReference(newValue(
                        defendantUpdate -> defendantUpdate.getPersonDefendant().getEmployerPayrollReference()));

        CustodialEstablishment custodialEstablishment =
                calculateCustodialEstablishment(defendantUpdate -> defendantUpdate.getPersonDefendant().getCustodialEstablishment());

        if(nonNull(custodialEstablishment) && nonNull(custodialEstablishment.getCustody()) && nonNull(custodialEstablishment.getId()) && nonNull(custodialEstablishment.getName())){
            personDefendantBuilder.withCustodialEstablishment(custodialEstablishment);
        }
        return personDefendantBuilder.build();
    }

    /**
     * It seems many unnecessary conditions are written, this needs understanding user journey / refactoring / regression testing.
     * @return
     */
    @SuppressWarnings("squid:S2589")
    public List<AssociatedPerson> calculateAssociatedPersons() {
        final List<AssociatedPerson> associatedPersons = matchedDefendantPreviousVersion.getAssociatedPersons();
        final Optional<AssociatedPerson> previousParent = checkNullList(originalDefendantPreviousVersion.getAssociatedPersons())
                .filter(associatedPerson -> ROLE_PARENT.equalsIgnoreCase(associatedPerson.getRole())).findFirst();
        final Optional<AssociatedPerson> nextParent = checkNullList(originalDefendantNextVersion.getAssociatedPersons())
                .filter(associatedPerson -> ROLE_PARENT.equalsIgnoreCase(associatedPerson.getRole())).findFirst();
        // no change, originals are null
        if (!previousParent.isPresent() && !nextParent.isPresent()) {
            return associatedPersons; // keep same
        }

        // added, remove old if exists (I know unnecessary conditions above, These are kept for clarification)
        if (!previousParent.isPresent() && nextParent.isPresent()) {
            LOGGER.info("AssociatedPersons validation Defendant is Not PARENT, DefendantUpdate have: {}", nextParent.get().getRole());
            final List<AssociatedPerson> newAssociatedPersons = checkNullList(associatedPersons)
                    .filter(associatedPerson -> !ROLE_PARENT.equalsIgnoreCase(associatedPerson.getRole()))
                    .collect(Collectors.toList());
            newAssociatedPersons.add(nextParent.get());
            return newAssociatedPersons;
        }
        // removed, (I know unnecessary conditions above, These are kept for clarification)
        if (previousParent.isPresent() && !nextParent.isPresent()) {
            LOGGER.info("AssociatedPersons validation DefendantUpdate is Not PARENT, Defendant have: {}", previousParent.get().getRole());
            return checkNullList(associatedPersons)
                    .filter(associatedPerson -> !ROLE_PARENT.equalsIgnoreCase(associatedPerson.getRole()))
                    .collect(Collectors.toList());
        }
        // changed, merge, (I know unnecessary conditions above, These are kept for clarification)
        if (previousParent.isPresent() && nextParent.isPresent()) {
            LOGGER.info("AssociatedPersons validation DefendantUpdate is {}, Defendant have: {}", nextParent.get().getRole(), previousParent.get().getRole());
            final List<AssociatedPerson> newAssociatedPersons = checkNullList(associatedPersons)
                    .filter(associatedPerson -> (!ROLE_PARENT.equalsIgnoreCase(associatedPerson.getRole())))
                    .collect(Collectors.toList());

            Function<DefendantUpdate, Person> parentPersonFunction = defendantUpdate ->
                    Optional.ofNullable(defendantUpdate)
                            .map(DefendantUpdate::getAssociatedPersons)
                            .map(this::checkNullList)
                            .flatMap(list -> list
                                    .filter(ap -> ROLE_PARENT.equalsIgnoreCase(ap.getRole()) ||
                                            ROLE_PARENT_GUARDIAN.equalsIgnoreCase(ap.getRole()))
                                    .map(AssociatedPerson::getPerson)
                                    .findFirst()
                            )
                            .orElse(null);
            final Person parentPerson = calculatePerson(parentPersonFunction, nextParent.get().getPerson());
            LOGGER.info("calculateAssociatedPerson newAssociatedPersons: {}", parentPerson);
            //Check if parent person to be set is not empty.
            if (nonNull(parentPerson.getLastName()) && nonNull(parentPerson.getGender())){
                newAssociatedPersons.add(AssociatedPerson.associatedPerson()
                        .withPerson(parentPerson)
                        .withRole(ROLE_PARENT)
                        .build());
            }
            return newAssociatedPersons;
        }

        return associatedPersons; // actually unreachable
    }

    private <T> Stream<T> checkNullList(final List<T> list) {
        return list == null ? Stream.empty() : list.stream();
    }

    private Organisation calculateEmployerOrganization(Function<DefendantUpdate, Organisation> organisationFunction) {
        final Pair<Boolean, Organisation> objectResults = checkNullableObjectResults(organisationFunction);
        if (objectResults.getKey()) {
            return objectResults.getRight();
        }
        organisationFunction = organisationFunction.andThen(organisation -> organisation != null ? organisation : Organisation.organisation().build());
        return Organisation.organisation()
                .withName(newValue(organisationFunction.andThen(Organisation::getName)))
                .withRegisteredCharityNumber(newValue(organisationFunction.andThen(Organisation::getRegisteredCharityNumber)))
                .withIncorporationNumber(newValue(organisationFunction.andThen(Organisation::getIncorporationNumber)))
                .withContact(calculateContactNumber(organisationFunction.andThen(Organisation::getContact)))
                .withAddress(calculateAddress(organisationFunction.andThen(Organisation::getAddress)))
                .build();
    }

    private Person calculatePerson(Function<DefendantUpdate, Person> personFunction, final Person personDetails) {
        final Pair<Boolean, Person> objectResults = checkNullableObjectResults(personFunction);
        if (objectResults.getKey()) {
            return objectResults.getRight();
        }
        personFunction = personFunction.andThen(person -> person != null ? person : Person.person().build());
        return Person.person()
                .withFirstName(newValue(personFunction.andThen(Person::getFirstName)))
                .withLastName(newValue(personFunction.andThen(Person::getLastName)))
                .withMiddleName(newValue(personFunction.andThen(Person::getMiddleName)))
                .withTitle(newValue(personFunction.andThen(Person::getTitle)))
                .withDateOfBirth(newValue(personFunction.andThen(Person::getDateOfBirth)))
                .withGender(newValue(personFunction.andThen(Person::getGender)))
                .withNationalInsuranceNumber(newValue(personFunction.andThen(Person::getNationalInsuranceNumber)))
                .withNationalityId(newValue(personFunction.andThen(Person::getNationalityId)))
                .withNationalityCode(newValue(personFunction.andThen(Person::getNationalityCode)))
                .withNationalityDescription(newValue(personFunction.andThen(Person::getNationalityDescription)))
                .withSpecificRequirements(newValue(personFunction.andThen(Person::getSpecificRequirements)))
                .withInterpreterLanguageNeeds(newValue(personFunction.andThen(Person::getInterpreterLanguageNeeds)))
                .withContact(calculateContactNumber(personFunction.andThen(Person::getContact)))
                .withAddress(calculateAddress(personFunction.andThen(Person::getAddress)))
                .withAdditionalNationalityId(newValue(personFunction.andThen(Person::getAdditionalNationalityId)))
                .withAdditionalNationalityCode(newValue(personFunction.andThen(Person::getAdditionalNationalityCode)))
                .withAdditionalNationalityDescription(newValue(personFunction.andThen(Person::getAdditionalNationalityDescription)))
                .withDisabilityStatus(personDetails.getDisabilityStatus())
                .withDocumentationLanguageNeeds(personDetails.getDocumentationLanguageNeeds())
                .withHearingLanguageNeeds(personDetails.getHearingLanguageNeeds())
                .withEthnicity(personDetails.getEthnicity())
                .withOccupation(personDetails.getOccupation())
                .withOccupationCode(personDetails.getOccupationCode())
                .withPersonMarkers(personDetails.getPersonMarkers())
                .build();
    }

    private CustodialEstablishment calculateCustodialEstablishment(Function<DefendantUpdate, CustodialEstablishment> custodialEstablishmentFunction) {
        final Pair<Boolean, CustodialEstablishment> objectResults = checkNullableObjectResults(custodialEstablishmentFunction);
        if (objectResults.getKey()) {
            return objectResults.getRight();
        }
        custodialEstablishmentFunction = custodialEstablishmentFunction.andThen(person -> person != null ? person : CustodialEstablishment.custodialEstablishment().build());
        return CustodialEstablishment.custodialEstablishment()
                .withCustody(newValue(custodialEstablishmentFunction.andThen(CustodialEstablishment::getCustody)))
                .withId(newValue(custodialEstablishmentFunction.andThen(CustodialEstablishment::getId)))
                .withName(newValue(custodialEstablishmentFunction.andThen(CustodialEstablishment::getName)))
                .build();
    }

    private Address calculateAddress(Function<DefendantUpdate, Address> addressFunction) {
        final Pair<Boolean, Address> objectResults = checkNullableObjectResults(addressFunction);
        if (objectResults.getKey()) {
            return objectResults.getRight();
        }
        addressFunction = addressFunction.andThen(address -> address != null ? address : Address.address().build());

        return Address.address()
                .withAddress1(newValue(addressFunction.andThen(Address::getAddress1)))
                .withAddress2(newValue(addressFunction.andThen(Address::getAddress2)))
                .withAddress3(newValue(addressFunction.andThen(Address::getAddress3)))
                .withAddress4(newValue(addressFunction.andThen(Address::getAddress4)))
                .withAddress5(newValue(addressFunction.andThen(Address::getAddress5)))
                .withPostcode(newValue(addressFunction.andThen(Address::getPostcode)))
                .withWelshAddress1(newValue(addressFunction.andThen(Address::getWelshAddress1)))
                .withWelshAddress2(newValue(addressFunction.andThen(Address::getWelshAddress2)))
                .withWelshAddress3(newValue(addressFunction.andThen(Address::getWelshAddress3)))
                .withWelshAddress4(newValue(addressFunction.andThen(Address::getWelshAddress4)))
                .withWelshAddress5(newValue(addressFunction.andThen(Address::getWelshAddress5)))
                .build();

    }

    private ContactNumber calculateContactNumber(Function<DefendantUpdate, ContactNumber> contactNumberFunction) {
        final Pair<Boolean, ContactNumber> objectResults = checkNullableObjectResults(contactNumberFunction);
        if (objectResults.getKey()) {
            return objectResults.getRight();
        }
        contactNumberFunction = contactNumberFunction.andThen(contactNumber -> contactNumber != null ? contactNumber : ContactNumber.contactNumber().build());
        return ContactNumber.contactNumber()
                .withPrimaryEmail(newValue(
                        contactNumberFunction.andThen(ContactNumber::getPrimaryEmail)))
                .withSecondaryEmail(newValue(
                        contactNumberFunction.andThen(ContactNumber::getSecondaryEmail)))
                .withWork(newValue(
                        contactNumberFunction.andThen(ContactNumber::getWork)))
                .withHome(newValue(
                        contactNumberFunction.andThen(ContactNumber::getHome)))
                .withMobile(newValue(
                        contactNumberFunction.andThen(ContactNumber::getMobile)))
                .withFax(newValue(
                        contactNumberFunction.andThen(ContactNumber::getFax)))
                .build();
    }

    /**
     * @param contactNumberFunction
     * @param <T>
     * @return
     */
    private <T> Pair<Boolean, T> checkNullableObjectResults(final Function<DefendantUpdate, T> contactNumberFunction) {
        if (contactNumberFunction.apply(originalDefendantPreviousVersion) == null && contactNumberFunction.apply(originalDefendantNextVersion) == null) {
            return Pair.of(true, contactNumberFunction.apply(matchedDefendantPreviousVersion));
        }
        if (contactNumberFunction.apply(originalDefendantPreviousVersion) != null && contactNumberFunction.apply(originalDefendantNextVersion) == null) {
            return Pair.of(true, null);
        }
        return Pair.of(false, null); // No Results
    }


    /*
     * Calculates New Value
     * If it's updated gets updated value, ow old value
     * */
    private <T> T newValue(final Function<DefendantUpdate, T> extractor) {
        if (Objects.equals(extractor.apply(originalDefendantPreviousVersion), extractor.apply(originalDefendantNextVersion))) {
            return extractor.apply(matchedDefendantPreviousVersion);
        } else {
            return extractor.apply(originalDefendantNextVersion);
        }
    }
}
