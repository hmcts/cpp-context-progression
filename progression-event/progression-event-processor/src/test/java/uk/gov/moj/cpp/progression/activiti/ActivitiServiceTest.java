package uk.gov.moj.cpp.progression.activiti;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.CASE_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.HEARING_ID;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ActivitiServiceTest {

    private final String processName = "listHearing";
    @Mock
    private RuntimeService runtimeService;

    @Mock
    private ExecutionQuery executionQuery;

    @Mock
    private Execution execution;

    @Mock
    private ProcessInstance processInstance;

    @InjectMocks
    private ActivitiService activitiService;

    private final ArgumentCaptor<HashMap> captor = ArgumentCaptor.forClass(HashMap.class);


    @Test
    public void shouldStartProcessWhenProcessNameAndMapProvided(){
        //given
        final String caseId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String hearingId = UUID.randomUUID().toString();
        final Map<String, Object> processMap = new HashMap<>();
        processMap.put(CASE_ID, caseId);
        processMap.put(USER_ID, userId);
        processMap.put(HEARING_ID, hearingId);

        //when
        activitiService.startProcess(processName,processMap);

        //then
        verify(runtimeService).startProcessInstanceByKey(eq(processName), captor.capture());
        assertThat(captor.getValue().get("caseId"), equalTo(caseId));
        assertThat(captor.getValue().get("userId"), equalTo(userId));
        assertThat(captor.getValue().get("hearingId"), equalTo(hearingId));
    }
    @Test
    public void shouldNotSignalProcessIfProcessNotInExecution() {
        //Given
        final String arbitraryActivitiId = "dummyId";
        final String arbitraryFiledName = "dummyFiled";
        final UUID arbitraryBussniessKey = UUID.randomUUID();
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.activityId(arbitraryActivitiId)).thenReturn(executionQuery);
        when(executionQuery.variableValueEquals(arbitraryFiledName, arbitraryBussniessKey)).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(null);

        //when
        activitiService.signalProcessByActivitiIdAndFieldName(arbitraryActivitiId, arbitraryFiledName,arbitraryBussniessKey);

        //then
        verify(runtimeService, never()).signal(any());
    }

    @Test
    public void shouldSignalProcessIfProcessInExecution() {
        //Given
        final String arbitraryActivitiId = "dummyId";
        final String arbitraryFiledName = "dummyFiled";
        final UUID arbitraryBussniessKey = UUID.randomUUID();
        final String arbitraryProcessId = "12345678";
        when(runtimeService.createExecutionQuery()).thenReturn(executionQuery);
        when(executionQuery.activityId(arbitraryActivitiId)).thenReturn(executionQuery);
        when(executionQuery.variableValueEquals(arbitraryFiledName, arbitraryBussniessKey)).thenReturn(executionQuery);
        when(executionQuery.singleResult()).thenReturn(processInstance);
        when(processInstance.getId()).thenReturn(arbitraryProcessId);

        //when
        activitiService.signalProcessByActivitiIdAndFieldName(arbitraryActivitiId, arbitraryFiledName,arbitraryBussniessKey);

        //then
        //then
        verify(runtimeService).signal(arbitraryProcessId);
    }
}