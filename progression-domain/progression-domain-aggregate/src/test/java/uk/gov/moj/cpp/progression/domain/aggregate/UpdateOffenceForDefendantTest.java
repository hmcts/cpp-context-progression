package uk.gov.moj.cpp.progression.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesChanged;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

public class UpdateOffenceForDefendantTest {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    private CaseProgressionAggregate caseProgressionAggregate;


    @Before
    public void setUp(){
        caseProgressionAggregate =new CaseProgressionAggregate();
        caseProgressionAggregate.addDefendant(addDefendant);
    }
    @Test
    public void shouldApplyOffencesForDefendantUpdated() {

        final UUID caseId = randomUUID();
        final List<OffenceForDefendant> offenceForDefendants = Arrays.asList(new OffenceForDefendant(randomUUID(), "offenceCode"
                ,  "section",
                " wording", LocalDate.now(), LocalDate.now(), 0, 0, null, null, null));


        final OffencesForDefendantUpdated offencesForDefendantUpdated =
                new OffencesForDefendantUpdated(caseId, addDefendant.getDefendantId(), offenceForDefendants);

        final List<Object> eventStream = caseProgressionAggregate.updateOffencesForDefendant(offencesForDefendantUpdated ).collect(toList());

        assertThat(eventStream.size(), is(2));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(OffencesForDefendantUpdated.class)));
        object = eventStream.get(1);
        assertThat(object.getClass(),
                        is(CoreMatchers.<Class<?>>equalTo(DefendantOffencesChanged.class)));

    }



}
