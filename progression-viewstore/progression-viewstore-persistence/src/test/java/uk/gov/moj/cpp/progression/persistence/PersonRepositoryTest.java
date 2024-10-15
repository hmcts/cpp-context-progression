package uk.gov.moj.cpp.progression.persistence;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import uk.gov.moj.cpp.progression.persistence.entity.Address;
import uk.gov.moj.cpp.progression.persistence.entity.Person;
import uk.gov.moj.cpp.progression.persistence.repository.AddressRepository;
import uk.gov.moj.cpp.progression.persistence.repository.PersonRepository;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @deprecated
 */
@SuppressWarnings("squid:S1133")
@Deprecated
@Transactional
@RunWith(CdiTestRunner.class)
public class PersonRepositoryTest {

    private static final UUID PERSON_ID_A = UUID.randomUUID();
    private static final String TITLE_A = "Mr";
    private static final String FIRSTNAME_A = "David";
    private static final String LASTNAME_A = "Lloyd";
    private static final String GENDER_A = "Male";
    private static final String HOME_TELEPHONE_A = "02012345678";
    private static final String WORK_TELEPHONE_A = "02022222223";
    private static final String MOBILE_A = "07712345678";
    private static final String FAX_A = "02012345678";
    private static final String EMAIL_A = "email1@email.com";


    private static final UUID PERSON_ID_B = UUID.randomUUID();
    private static final String TITLE_B = "Mrs";
    private static final String FIRSTNAME_B = "Kate";
    private static final String LASTNAME_B = "Sadler";
    private static final String GENDER_B = "Female";
    private static final String HOME_TELEPHONE_B = "02022222222";
    private static final String WORK_TELEPHONE_B = "02022222223";
    private static final String MOBILE_B = "07722222222";
    private static final String FAX_B = "02012222222";
    private static final String EMAIL_B = "email2@email.com";

    private static final UUID PERSON_ID_C = UUID.randomUUID();
    private static final String TITLE_C = null;
    private static final String FIRSTNAME_C = "Sam";
    private static final String LASTNAME_C = "Lloyd";
    private static final String GENDER_C = "Male";
    private static final String HOME_TELEPHONE_C = "02033333333";
    private static final String WORK_TELEPHONE_C = "02022222223";
    private static final String MOBILE_C = "07733333333";
    private static final String FAX_C = "02013333333";
    private static final String EMAIL_C = "email3@email.com";

    private static final String NATIONALITY = "British";
    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "England";
    private static final String ADDRESS_4 = "UK";
    private static final String POST_CODE = "W1T 1JY";
    private static final int DOB_YEAR = 1980;
    private static final Month DOB_MONTH = Month.DECEMBER;
    private static final int DOB_DAY = 25;

    @Inject
    private PersonRepository personRepository;

    @Inject
    private AddressRepository addressRepository;

    private Person personA;
    private Person personB;
    private Person personC;
    private Person personADuplicate;

    @Before
    public void setUpBefore() {

        List<Person> personList = personRepository.findPersonByCriteria(FIRSTNAME_A, LASTNAME_A, POST_CODE, LocalDate.of(DOB_YEAR, DOB_MONTH, DOB_DAY));

        personList.forEach(
                item -> personRepository.remove(item)
        );

        personA = createPerson(PERSON_ID_A, TITLE_A, FIRSTNAME_A, LASTNAME_A, GENDER_A, HOME_TELEPHONE_A, WORK_TELEPHONE_A, MOBILE_A, FAX_A, EMAIL_A);
        personRepository.save(personA);

        personB = createPerson(PERSON_ID_B, TITLE_B, FIRSTNAME_B, LASTNAME_B, GENDER_B, HOME_TELEPHONE_B, WORK_TELEPHONE_B, MOBILE_B, FAX_B, EMAIL_B);
        personRepository.save(personB);

        personC = createPerson(PERSON_ID_C, TITLE_C, FIRSTNAME_C, LASTNAME_C, GENDER_C, HOME_TELEPHONE_C, WORK_TELEPHONE_C, MOBILE_C, FAX_C, EMAIL_C);
        personRepository.save(personC);

        personADuplicate = createPerson(UUID.randomUUID(), TITLE_A, FIRSTNAME_A, LASTNAME_A, GENDER_A, HOME_TELEPHONE_A, WORK_TELEPHONE_A, MOBILE_A, FAX_A, EMAIL_A);
        personRepository.save(personADuplicate);

        personList = personRepository.findAll();
        assertNotNull(personList);
        assertFalse(personList.isEmpty());
    }


    @Test
    public void shouldFindPersonById() {
        final Person person = personRepository.findBy(PERSON_ID_A);

        assertThat(person, is(notNullValue()));
        assertThat(person.getPersonId(), equalTo(PERSON_ID_A));
        assertThat(person.getTitle(), equalTo(TITLE_A));
        assertThat(person.getFirstName(), equalTo(FIRSTNAME_A));
        assertThat(person.getLastName(), equalTo(LASTNAME_A));
        assertThat(person.getGender(), equalTo(GENDER_A));
    }

    @Test
    public void shouldReturnNullIfPersonNotFound() {
        final Person person = personRepository.findBy(UUID.randomUUID());

        assertThat(person, is(nullValue()));
    }

    @Test
    public void shouldReturnListOfPerson() {
        final List<Person> personList = personRepository.findByLastNameIgnoreCase(LASTNAME_B);
        assertThat(personList, hasSize(1));
        assertThat(personList, equalTo(singletonList(personB)));
    }

    @Test
    public void shouldReturnEmptyListWhenNotFoundByLastName() throws Exception {
        final List<Person> personList = personRepository.findByLastNameIgnoreCase("InvalidLastName");

        assertThat(personList, equalTo(emptyList()));
    }

    @Test
    public void shouldReturnListOfTwoSamePersonsWhenQueryingByFirstLastNameAndDateOfBirth() {

        final List<Person> persons = personRepository.findPersonByCriteria(FIRSTNAME_A, LASTNAME_A, POST_CODE, LocalDate.of(DOB_YEAR, DOB_MONTH, DOB_DAY));

        assertThat(persons.size(), equalTo(2));
        assertTrue(persons.get(0).getPersonId().equals(personADuplicate.getPersonId())
                || persons.get(0).getPersonId().equals(personA.getPersonId()));

        assertTrue(persons.get(1).getPersonId().equals(personA.getPersonId())
                || persons.get(1).getPersonId().equals(personADuplicate.getPersonId()));

        assertThat(persons.get(0).getFirstName(), equalTo(personADuplicate.getFirstName()));
        assertThat(persons.get(0).getLastName(), equalTo(personADuplicate.getLastName()));
        assertThat(persons.get(1).getFirstName(), equalTo(personA.getFirstName()));
        assertThat(persons.get(1).getLastName(), equalTo(personA.getLastName()));
    }


    @Test
    public void canGetPersonFromBuilder() {
        final List<Person> personList = personRepository.findPersonByCriteria(FIRSTNAME_A, LASTNAME_A, POST_CODE, LocalDate.of(DOB_YEAR, DOB_MONTH, DOB_DAY));

        personList.forEach(
                item -> personRepository.remove(item)
        );
        final Person person = new Person().builder()
                .personId(PERSON_ID_A)
                .title(TITLE_A)
                .firstName(FIRSTNAME_A)
                .lastName(LASTNAME_A)
                .nationality(NATIONALITY)
                .gender(GENDER_A)
                .homeTelephone(HOME_TELEPHONE_A)
                .workTelephone(WORK_TELEPHONE_A)
                .fax(FAX_A)
                .mobile(MOBILE_A)
                .email(EMAIL_A)
                .dateOfBirth(LocalDate.of(DOB_YEAR, DOB_MONTH, DOB_DAY))
                .address(UUID.randomUUID(), ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, POST_CODE).build();

        final Address addr = addressRepository.save(person.getAddress());
        person.setAddress(addr);
        final Person result = personRepository.save(person);
        assertEquals(PERSON_ID_A, result.getPersonId());
        assertEquals(TITLE_A, person.getTitle());
        assertEquals(FIRSTNAME_A, result.getFirstName());
        assertEquals(LASTNAME_A, result.getLastName());
        assertEquals(NATIONALITY, result.getNationality());
        assertEquals(GENDER_A, result.getGender());
        assertEquals(HOME_TELEPHONE_A, result.getHomeTelephone());
        assertEquals(WORK_TELEPHONE_A, result.getWorkTelephone());
        assertEquals(FAX_A, result.getFax());
        assertEquals(MOBILE_A, result.getMobile());
        assertEquals(EMAIL_A, result.getEmail());
        assertEquals(ADDRESS_1, result.getAddress().getAddress1());
        assertEquals(ADDRESS_2, result.getAddress().getAddress2());
        assertEquals(ADDRESS_3, result.getAddress().getAddress3());
        assertEquals(ADDRESS_4, result.getAddress().getAddress4());
        assertEquals(POST_CODE, result.getAddress().getPostCode());


    }

    private Person createPerson(final UUID personId, final String title,
                                final String firstName, final String lastName, final String gender,
                                final String homeTelephone, final String workTelephone,
                                final String mobile, final String fax, final String email) {
        return new Person().builder()
                .personId(personId)
                .title(title)
                .firstName(firstName)
                .lastName(lastName)
                .nationality(NATIONALITY)
                .gender(gender)
                .homeTelephone(homeTelephone)
                .workTelephone(workTelephone)
                .fax(fax)
                .mobile(mobile)
                .email(email)
                .dateOfBirth(LocalDate.of(DOB_YEAR, DOB_MONTH, DOB_DAY))
                .address(UUID.randomUUID(), ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, POST_CODE).build();

    }
}

