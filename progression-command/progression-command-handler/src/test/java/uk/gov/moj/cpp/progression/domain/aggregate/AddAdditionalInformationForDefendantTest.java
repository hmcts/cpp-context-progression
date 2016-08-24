package uk.gov.moj.cpp.progression.domain.aggregate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import java.util.Set;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.moj.cpp.progression.command.defendant.DefendantCommand;
import uk.gov.moj.cpp.progression.test.utils.DefendantBuilder;

/**
 * 
 * @author jchondig
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class AddAdditionalInformationForDefendantTest extends CaseProgressionAggregateBaseTest {

    private static final DefendantCommand defendant = DefendantBuilder.defaultDefendant();


    @Mock
    private Set<UUID> defendantIds;

    @Test
    public void shouldThowExceptionAdditionalInformationForDefendant() {

        try {
            caseProgressionAggregate.addAdditionalInformationForDefendant(defendant);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Cannot add additional information without defendant "
                            + defendant.getDefendantId()));
            return;
        }

        fail("Expected exception not thrown.");
    }


}
