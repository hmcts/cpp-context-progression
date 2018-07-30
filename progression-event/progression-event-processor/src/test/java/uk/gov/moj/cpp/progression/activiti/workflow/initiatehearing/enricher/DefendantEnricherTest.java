package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.enricher;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.CASE_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.INITIATE_HEARING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.external.domain.hearing.Case;
import uk.gov.moj.cpp.external.domain.hearing.Defendant;
import uk.gov.moj.cpp.external.domain.hearing.Hearing;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.external.domain.hearing.Judge;
import uk.gov.moj.cpp.external.domain.hearing.Offence;
import uk.gov.moj.cpp.progression.service.ProgressionService;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

@RunWith(MockitoJUnitRunner.class)
public class DefendantEnricherTest {

    @InjectMocks
    private DefendantEnricher testObj;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private ProgressionService progressionService;

    @Mock
    ReferenceDataService referenceDataService;


    final ArgumentCaptor<InitiateHearing> captor = ArgumentCaptor.forClass(InitiateHearing.class);

    @Test
    public void shouldEnrichDefendant() throws Exception {

        //Given
        final String title = "abc";
        final String legislation = "xyz";
        final Offence offence = new Offence(UUID.randomUUID());
        offence.setTitle(title);
        offence.setLegislation(legislation);
        final Defendant defendant = new Defendant(UUID.randomUUID());
        defendant.getOffences().add(offence);

        final JsonObject jsonObject = Json.createObjectBuilder()
                        .add("defenceSolicitorFirm", new StringGenerator().next())
                        .add("interpreter", Json.createObjectBuilder().add("language", "language").add("needed", true).build())
                        .add("bailStatus", "bail")
                        .add("custodyTimeLimitDate", "xyz")
                        .build();

        final Hearing hearing = new Hearing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Judge(UUID.randomUUID()),
                new StringGenerator().next(),
                singletonList(new StringGenerator().next()),
                Arrays.asList(defendant)
        );

        final List<Case> cases = Arrays.asList(new Case(UUID.randomUUID(),new StringGenerator().next()));

        final InitiateHearing initiateHearing = new InitiateHearing(cases, hearing);

        when(delegateExecution.getVariable(INITIATE_HEARING_PAYLOAD)).thenReturn(initiateHearing);
        when(delegateExecution.getVariable(CASE_ID)).thenReturn(UUID.randomUUID());
        when(delegateExecution.getVariable(USER_ID)).thenReturn(UUID.randomUUID());
        when(progressionService.getDefendantByDefendantId(any(), any(), any())).thenReturn(Optional.of(jsonObject));


        //when
        testObj.execute(delegateExecution);

        //then
        verify(delegateExecution).setVariable(any(), captor.capture());
        final InitiateHearing result = captor.getAllValues().get(0);
        assertThat(initiateHearing.equals(result), equalTo(true));
        assertThat(result.getHearing().getDefendants().get(0).getOffences().get(0).getTitle().equals(title), equalTo(true));
        assertThat(result.getHearing().getDefendants().get(0).getOffences().get(0).getLegislation().equals(legislation), equalTo(true));
    }
}
