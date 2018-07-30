package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing;

import java.util.Map;

import javax.inject.Inject;

import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;


public class InitiateHearingService {

    private static final String INITIATE_HEARING = "initiateHearing";

    @Inject
    private ActivitiService activitiService;

    public void startProcess(final Map<String, Object> processMap) {

        activitiService.startProcess(INITIATE_HEARING, processMap);
    }

}
