package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.Address;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdateConfirmed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdateFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;

@RunWith(MockitoJUnitRunner.class)
public class UpdateDefendantTest {

    private static final UpdateDefendantCommand updateDefendant = DefendantBuilder.defaultUpdateDefendant();
    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    @InjectMocks
    private CaseProgressionAggregate caseProgressionAggregate;


    @Before
    public void setUp(){
        caseProgressionAggregate =new CaseProgressionAggregate();
    }
    @Test
    public void shouldReturnDefendantUpdated() {

        caseProgressionAggregate.addDefendant(addDefendant);
        final List<Object> eventStream = caseProgressionAggregate.updateDefendant(updateDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));
    }

    @Test
    public void shouldReturnDefendantUpdateFailed() {
        final List<Object> eventStream =
                        caseProgressionAggregate.updateDefendant(updateDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdateFailed.class)));
    }

    @Test
    public void shouldReturnDefendantUpdatedUpdateConfirmed() {
        Whitebox.setInternalState(caseProgressionAggregate, "caseInReview", true);
        caseProgressionAggregate.addDefendant(addDefendant);
        final List<Object> eventStream =
                        caseProgressionAggregate.updateDefendant(updateDefendant).collect(toList());

        assertThat(eventStream.size(), is(2));
        Object object = eventStream.get(0);
        assertThat(object.getClass(),
                        is(CoreMatchers.<Class<?>>equalTo(DefendantUpdateConfirmed.class)));
        object = eventStream.get(1);
        assertThat(object.getClass(),
                        is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));
    }

    @Test
    public void shouldReturnDefendantUpdatedNoUpdateConfirmed() {
        Whitebox.setInternalState(caseProgressionAggregate, "caseInReview", true);
        caseProgressionAggregate.addDefendant(addDefendant);
        final List<Object> eventStream =
                        caseProgressionAggregate
                                        .updateDefendant(DefendantBuilder
                                                        .updateDefendantPersonOnlyNoUpdate())
                                        .collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));
    }

    @Test
    public void shouldReturnDefendantUpdatedUpdateConfirmedPersonBailStatusUpdated() {
        Whitebox.setInternalState(caseProgressionAggregate, "caseInReview", true);
        final Address address =
                        new Address("3", "Cambridge Street", "Warwick Avenue", "London", "NW10");
        final Person person = new Person(UUID.randomUUID(), "Mr", "John", "Humpries",
                        LocalDate.now(), "British", "Male", null, null, null, null, null, address);
        final String bailStatus = "Bailed";
        final String homePhone = "123Home";
        final String workPhone = "123Work";
        final String mobile = "123Mobile";
        final String fax = "123fax";
        final String email = "email@moj.gov.uk";
        caseProgressionAggregate.addDefendant(DefendantBuilder
                        .addDefendantWithPersonDetails(homePhone, workPhone, mobile, fax, email));
        List<Object> eventStream = caseProgressionAggregate
                        .updateDefendant(DefendantBuilder
                                        .updateDefendantPersonBailStatusUpdate(person, bailStatus))
                        .collect(toList());

        assertThat(eventStream.size(), is(2));
        Object object = eventStream.get(0);
        assertThat(object.getClass(),
                        is(CoreMatchers.<Class<?>>equalTo(DefendantUpdateConfirmed.class)));

        final DefendantUpdateConfirmed duConfirmed = (DefendantUpdateConfirmed) object;
        assertThat(duConfirmed.getBailStatus(), is(bailStatus));
        asertPersonInstance(person, duConfirmed.getPerson());
        // Assert communication details ( Except address)
        assertCommunicationDetails(duConfirmed.getPerson(), homePhone, workPhone, mobile, fax,
                        email);
        object = eventStream.get(1);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));

        // Call update defendant again with same defendant. Should only result in DefendantUpdated
        // event
        eventStream = caseProgressionAggregate
                        .updateDefendant(DefendantBuilder
                                        .updateDefendantPersonBailStatusUpdate(person, bailStatus))
                        .collect(toList());
        assertThat(eventStream.size(), is(1));
        object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));

    }



    private void assertCommunicationDetails(final Person eventPerson, final String homePhone,
                    final String workPhone, final String mobile, final String fax,
                    final String email) {

        assertThat(email, is(eventPerson.getEmail()));
        assertThat(homePhone, is(eventPerson.getHomeTelephone()));
        assertThat(workPhone, is(eventPerson.getWorkTelephone()));
        assertThat(mobile, is(eventPerson.getMobile()));
        assertThat(fax, is(eventPerson.getFax()));


    }
    private void asertPersonInstance(final Person person, final Person eventPerson) {
        // Communication details from the addDefendanrt command person will be retailed.
        // So Person from updateDef command and UpdateConfirmed event will not be same
        assertThat(person, not(eventPerson));
        assertThat(person.getTitle(), is(eventPerson.getTitle()));
        assertThat(person.getFirstName(), is(eventPerson.getFirstName()));
        assertThat(person.getLastName(), is(eventPerson.getLastName()));
        assertThat(person.getDateOfBirth(), is(eventPerson.getDateOfBirth()));
        assertThat(person.getNationality(), is(eventPerson.getNationality()));
        assertThat(person.getGender(), is(eventPerson.getGender()));
        assertThat(person.getAddress(), is(eventPerson.getAddress()));

    }


}
