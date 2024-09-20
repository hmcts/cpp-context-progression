package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @deprecated This is deprecated for Release 2.4
 *
 */
@Deprecated
public class AddDefendantTest  {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    private CaseAggregate caseAggregate;


    @BeforeEach
    public void setUp(){
        caseAggregate =new CaseAggregate();
    }
    @Test
    public void shouldReturnDefendantAdded() {

        final List<Object> eventStream = caseAggregate.addDefendant(addDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantAdded.class)));
    }

    @Test
    public void shouldReturnDefendantAlreadyExistsIfDefendantAlreadyExists() {

        caseAggregate.addDefendant(addDefendant);

        final List<Object> eventStream = caseAggregate.addDefendant(addDefendant).collect(toList());

        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantAdditionFailed.class)));
    }
}
