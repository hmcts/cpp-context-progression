package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.service;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.CASE_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.HEARING_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;
import uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.InitiateHearingService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InitiateHearingServiceTest {

    @Mock
    ActivitiService activitiService;

    @InjectMocks
    private InitiateHearingService initiateHearingService;

    private final ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);

    @Test
    public void shouldStartProcess() throws Exception {
        //given
        final String caseId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String hearingId = UUID.randomUUID().toString();
        final Map<String, Object> processMap = new HashMap<>();
        processMap.put(CASE_ID, caseId);
        processMap.put(USER_ID, userId);
        processMap.put(HEARING_ID, hearingId);

        //when
        initiateHearingService.startProcess(processMap);

        //then
        verify(activitiService).startProcess(eq("initiateHearing"), captor.capture());
        assertThat(captor.getValue().get("caseId"), equalTo(caseId));
        assertThat(captor.getValue().get("userId"), equalTo(userId));
        assertThat(captor.getValue().get("hearingId"), equalTo(hearingId));
    }

}
