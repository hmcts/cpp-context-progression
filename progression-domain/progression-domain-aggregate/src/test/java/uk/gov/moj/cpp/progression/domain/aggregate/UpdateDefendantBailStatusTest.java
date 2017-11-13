package uk.gov.moj.cpp.progression.domain.aggregate;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailStatusUpdatedForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdded;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAdditionFailed;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UpdateDefendantBailStatusTest {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    private CaseProgressionAggregate caseProgressionAggregate;


    @Before
    public void setUp(){
        caseProgressionAggregate =new CaseProgressionAggregate();
        caseProgressionAggregate.addDefendant(addDefendant);
    }
    @Test
    public void shouldDefendantsBailDocumentsTest() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final BailDocument bailDocument =new  BailDocument(randomUUID(),randomUUID());
        List<Object> eventStream = caseProgressionAggregate.updateDefendantBailStatus(addDefendant.getDefendantId(),"bailStatus", Optional.of(bailDocument),Optional.of(LocalDate.now()) ).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(BailStatusUpdatedForDefendant.class)));
        assertThat((caseProgressionAggregate.getDefendantsBailDocuments().get(addDefendant.getDefendantId())), is(bailDocument));
    }

}
