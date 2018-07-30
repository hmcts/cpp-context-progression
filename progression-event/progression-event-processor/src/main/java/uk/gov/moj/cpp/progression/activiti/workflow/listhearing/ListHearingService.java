package uk.gov.moj.cpp.progression.activiti.workflow.listhearing;

import uk.gov.moj.cpp.progression.activiti.service.ActivitiService;

import java.util.Map;

import javax.inject.Inject;


public class ListHearingService {
    private static final String LIST_HEARING = "listHearing";

    @Inject
    private ActivitiService activitiService;

    public void startProcess(final Map<String, Object> processMap) {
        activitiService.startProcess(LIST_HEARING, processMap);
    }
}
