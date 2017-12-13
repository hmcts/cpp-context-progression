package uk.gov.moj.cpp.progression.domain.aggregate;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.command.defendant.UpdateDefendantInterpreter;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailStatusUpdatedForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.InterpreterUpdatedForDefendant;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UpdateDefendantInterpreterTest {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();
    private CaseProgressionAggregate caseProgressionAggregate;


    @Before
    public void setUp(){
        caseProgressionAggregate =new CaseProgressionAggregate();
        caseProgressionAggregate.addDefendant(addDefendant);
    }

    @Test
    public void shouldApplyInterpreterUpdatedForDefendant() {
        final UUID caseId = randomUUID();
        final Interpreter interpreter = new Interpreter(true, "English");
        final UpdateDefendantInterpreter updateDefendantInterpreter =
                new UpdateDefendantInterpreter(caseId, addDefendant.getDefendantId(), interpreter);

        List<Object> eventStream = caseProgressionAggregate.updateDefendantInterpreter(updateDefendantInterpreter).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(InterpreterUpdatedForDefendant.class)));
    }

}
