package uk.gov.moj.cpp.progression.domain.aggregate;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class AddDefendantTest  {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    private CaseProgressionAggregate caseProgressionAggregate;


    @Before
    public void setUp(){
        caseProgressionAggregate =new CaseProgressionAggregate();
    }
    @Test
    public void shouldReturnDefendantAdded() {

        List<Object> eventStream = caseProgressionAggregate.addDefendant(addDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantAdded.class)));
    }

    @Test
    public void shouldReturnDefendantAlreadyExistsIfDefendantAlreadyExists() {

        caseProgressionAggregate.addDefendant(addDefendant);

        List<Object> eventStream = caseProgressionAggregate.addDefendant(addDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantAdditionFailed.class)));
    }
}
