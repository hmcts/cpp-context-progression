package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.CASE_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.external.domain.listing.Defendant;
import uk.gov.moj.cpp.external.domain.listing.Hearing;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.external.domain.listing.Offence;
import uk.gov.moj.cpp.progression.service.ProgressionService;

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

@RunWith(MockitoJUnitRunner.class)
public class ListingDefendantEnricherTest {
    final ArgumentCaptor<ListingCase> captor = ArgumentCaptor.forClass(ListingCase.class);

    @InjectMocks
    private ListingDefendantEnricher testObj;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private ProgressionService progressionService;

    @Test
    public void execute() throws Exception {
        //given
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("defenceSolicitorFirm", "abc")
                .add("person", Json.createObjectBuilder().add("id", UUID.randomUUID().toString()).add("firstName", "sati").build())
                .add("bailStatus", "bail")
                .add("custodyTimeLimitDate", "xyz")
                .build();
        final List<Hearing> hearings = Arrays.asList(new Hearing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PTP",
                new StringGenerator().next(),
                15,
                Arrays.asList(new Defendant(UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Arrays.asList(new Offence(UUID.randomUUID(),
                                null,
                                null,
                                null,
                                null))
                ))
                , UUID.randomUUID(),
                UUID.randomUUID(),
                new StringGenerator().next()
        ));
        final ListingCase arbitraryValue = new ListingCase(UUID.randomUUID(), new StringGenerator().next(), hearings);

        when(delegateExecution.getVariable("sendCaseForlistingPayload")).thenReturn(arbitraryValue);
        when(delegateExecution.getVariable(CASE_ID)).thenReturn(UUID.randomUUID());
        when(delegateExecution.getVariable(USER_ID)).thenReturn(UUID.randomUUID());
        when(progressionService.getDefendantByDefendantId(any(), any(), any())).thenReturn(Optional.of(jsonObject));


        //when
        testObj.execute(delegateExecution);

        //then
        verify(delegateExecution).setVariable(any(), captor.capture());
        final ListingCase result = captor.getValue();
        assertThat(result.getHearings().get(0).getDefendants().get(0).getFirstName().equals("sati"), equalTo(true));
        assertThat(result.getHearings().get(0).getDefendants().get(0).getBailStatus().equals("bail"), equalTo(true));
        assertThat(result.getHearings().get(0).getDefendants().get(0).getCustodyTimeLimit().equals("xyz"), equalTo(true));
        assertThat(result.getHearings().get(0).getDefendants().get(0).getDefenceOrganisation().equals("abc"), equalTo(true));
        assertThat(result.getHearings().get(0).getCourtRoomId().equals(arbitraryValue.getHearings().get(0).getCourtRoomId()), equalTo(true));
        assertThat(result.getHearings().get(0).getJudgeId().equals(arbitraryValue.getHearings().get(0).getJudgeId()), equalTo(true));
        assertThat(result.getHearings().get(0).getStartTime().equals(arbitraryValue.getHearings().get(0).getStartTime()), equalTo(true));


    }

}