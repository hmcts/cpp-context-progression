package uk.gov.moj.cpp.progression.activiti.service;

import java.util.Map;

import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import com.google.common.base.Strings;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Startup
@Singleton
public class ActivitiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiService.class);

    @Inject
    private RuntimeService runtimeService;

    public void startProcess(final String processName, final Map<String, Object> processMap) {
        LOGGER.info("Starting '{}' process.", processName);

        runtimeService.startProcessInstanceByKey(processName, processMap);
    }

    public void signalProcessByActivitiIdAndFieldName(final String activityId, final String fieldName, final Object businessKey) {
        final Execution execution = runtimeService.createExecutionQuery()
                .activityId(activityId)
                .variableValueEquals(fieldName, businessKey)
                .singleResult();
        if (execution != null && !Strings.isNullOrEmpty(execution.getId())) {
            LOGGER.info("Nudging process: Step Name {}, Process Id {}, BusinessKey {}", activityId, execution.getId(), businessKey);
            runtimeService.signal(execution.getId());
        } else {
            LOGGER.info("No process Found: Step Name {}, BusinessKey {}", activityId, businessKey);
        }
    }

}