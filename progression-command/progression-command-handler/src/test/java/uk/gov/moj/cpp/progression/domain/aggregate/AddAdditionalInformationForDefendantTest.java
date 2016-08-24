package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionalInformationAdded;
import uk.gov.moj.cpp.progression.test.utils.DefendantBuilder;

/**
 * 
 * @author jchondig
 *
 */
public class AddAdditionalInformationForDefendantTest extends CaseProgressionAggregateBaseTest {

    private static final DefendantCommand defendant = DefendantBuilder.defaultDefendant();


    @Test
    public void shouldUpdateDefenceSolicitorFirm() {

        Stream<Object> eventStream =
                        caseProgressionAggregate.addAdditionalInformationForDefendant(defendant);
        List<Object> events = asList(eventStream.toArray());

        assertThat("Has PleaUpdated event", events,
                        hasItem(isA(DefendantAdditionalInformationAdded.class)));
    }


}
