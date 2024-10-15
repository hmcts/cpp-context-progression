package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantCommand;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.Address;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdateFailed;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.Person;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.junit.jupiter.MockitoExtension;

/**
 *
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
@ExtendWith(MockitoExtension.class)
public class UpdateDefendantTest {

    private static final UpdateDefendantCommand updateDefendant = DefendantBuilder.defaultUpdateDefendant();
    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    @InjectMocks
    private CaseAggregate caseAggregate;


    @BeforeEach
    public void setUp(){
        caseAggregate =new CaseAggregate();
    }
    @Test
    public void shouldReturnDefendantUpdated() {

        caseAggregate.addDefendant(addDefendant);
        final List<Object> eventStream = caseAggregate.updateDefendant(updateDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));
    }

    @Test
    public void shouldReturnDefendantUpdateFailed() {
        final List<Object> eventStream =
                        caseAggregate.updateDefendant(updateDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdateFailed.class)));
    }

    @Test
    public void shouldReturnDefendantUpdatedUpdateConfirmed() {
        caseAggregate.addDefendant(addDefendant);
        final List<Object> eventStream =
                        caseAggregate.updateDefendant(updateDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);

        object = eventStream.get(0);
        assertThat(object.getClass(),
                        is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));
    }

    @Test
    public void shouldReturnDefendantUpdatedNoUpdateConfirmed() {
        caseAggregate.addDefendant(addDefendant);
        final List<Object> eventStream =
                        caseAggregate
                                        .updateDefendant(DefendantBuilder
                                                        .updateDefendantPersonOnlyNoUpdate())
                                        .collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));
    }

    @Test
    public void shouldReturnDefendantUpdatedUpdateConfirmedPersonBailStatusUpdated() {
        final Address address =
                        new Address("3", "Cambridge Street", "Warwick Avenue", "London", "NW10");
        final Person person = new Person(UUID.randomUUID(), "Dr", "John", "Humpries",
                        LocalDate.now(), "British", "Male", null, null, null, null, null, address);
        final String bailStatus = "Bailed";
        final String homePhone = "123Home";
        final String workPhone = "123Work";
        final String mobile = "123Mobile";
        final String fax = "123fax";
        final String email = "email@moj.gov.uk";
        caseAggregate.addDefendant(DefendantBuilder
                        .addDefendantWithPersonDetails(homePhone, workPhone, mobile, fax, email));
        List<Object> eventStream = caseAggregate
                        .updateDefendant(DefendantBuilder
                                        .updateDefendantPersonBailStatusUpdate(person, bailStatus))
                        .collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);

        object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));

        // Call update defendant again with same defendant. Should only result in DefendantUpdated
        // event
        eventStream = caseAggregate
                        .updateDefendant(DefendantBuilder
                                        .updateDefendantPersonBailStatusUpdate(person, bailStatus))
                        .collect(toList());
        assertThat(eventStream.size(), is(1));
        object = eventStream.get(0);
        assertThat(object.getClass(), is(CoreMatchers.<Class<?>>equalTo(DefendantUpdated.class)));

    }





}
