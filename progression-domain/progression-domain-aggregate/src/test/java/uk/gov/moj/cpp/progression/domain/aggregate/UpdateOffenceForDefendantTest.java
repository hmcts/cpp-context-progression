package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.moj.cpp.progression.aggregate.CaseAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @deprecated This is deprecated for Release 2.4
 *
 */
@SuppressWarnings("squid:S1133")
@Deprecated
public class UpdateOffenceForDefendantTest {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    private CaseAggregate caseAggregate;


    @BeforeEach
    public void setUp(){
        caseAggregate =new CaseAggregate();
        caseAggregate.addDefendant(addDefendant);
    }
    @Test
    public void shouldApplyOffencesForDefendantUpdated() {

        final UUID caseId = randomUUID();
        final List<OffenceForDefendant> offenceForDefendants = Arrays.asList(new OffenceForDefendant(randomUUID(), "offenceCode"
                ,  "section",
                " wording", LocalDate.now(), LocalDate.now(), 0, 0, null, null, null));


        final OffencesForDefendantUpdated offencesForDefendantUpdated =
                new OffencesForDefendantUpdated(caseId, addDefendant.getDefendantId(), offenceForDefendants);

        final List<Object> eventStream = caseAggregate.updateOffencesForDefendant(offencesForDefendantUpdated ).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(OffencesForDefendantUpdated.class)));

    }



}
