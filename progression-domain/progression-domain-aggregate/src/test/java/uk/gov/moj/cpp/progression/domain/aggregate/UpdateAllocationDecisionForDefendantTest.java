package uk.gov.moj.cpp.progression.domain.aggregate;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;
import uk.gov.moj.cpp.progression.aggregate.CaseProgressionAggregate;
import uk.gov.moj.cpp.progression.command.defendant.AddDefendant;
import uk.gov.moj.cpp.progression.domain.aggregate.utils.DefendantBuilder;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailDocument;
import uk.gov.moj.cpp.progression.domain.event.defendant.BailStatusUpdatedForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantAllocationDecisionUpdated;
import uk.gov.moj.cpp.progression.domain.event.defendant.DefendantOffencesDoesNotHaveRequiredModeOfTrial;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffenceForDefendant;
import uk.gov.moj.cpp.progression.domain.event.defendant.OffencesForDefendantUpdated;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UpdateAllocationDecisionForDefendantTest {

    private static final AddDefendant addDefendant = DefendantBuilder.defaultAddDefendant();

    @Mock
    JsonEnvelope jsonEnvelope;

    private JsonObject jsonObject;

    @InjectMocks
    private CaseProgressionAggregate caseProgressionAggregate;

    @Before
    public void setUp(){
        caseProgressionAggregate =new CaseProgressionAggregate();
        caseProgressionAggregate.addDefendant(addDefendant);

        jsonObject=Json.createObjectBuilder().add("caseId",addDefendant.getCaseId().toString()).add("defendantId",addDefendant.getDefendantId().toString())
                .add("allocationDecision","Yes").build();
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);

    }
    @Test
    public void shouldReturnDefendantOffencesDoesNotHaveRequiredModeOfTrial() {

        List<Object> eventStream = caseProgressionAggregate.updateDefendantAllocationDecision(jsonEnvelope).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantOffencesDoesNotHaveRequiredModeOfTrial.class)));
    }


    @Test
    public void shouldReturnDefendantAllocationDecisionUpdated() {


        final List<OffenceForDefendant> offenceForDefendants = Arrays.asList(new OffenceForDefendant(randomUUID(), "offenceCode"
                , "indicatedPlea", "section",
                " wording", LocalDate.now(), LocalDate.now(), 0, 0, "EWAY"));


        final OffencesForDefendantUpdated offencesForDefendantUpdated =
                new OffencesForDefendantUpdated(addDefendant.getCaseId(), addDefendant.getDefendantId(), offenceForDefendants);

        caseProgressionAggregate.updateOffencesForDefendant(offencesForDefendantUpdated ).collect(toList());

        List<Object> eventStream = caseProgressionAggregate.updateDefendantAllocationDecision(jsonEnvelope).collect(toList());

        assertThat(eventStream.size(), is(1));
        Object object = eventStream.get(0);
        assertThat(object.getClass() , is(CoreMatchers.<Class<?>>equalTo(DefendantAllocationDecisionUpdated.class)));
    }

}
