package uk.gov.moj.cpp.progression.task;

import static uk.gov.moj.cpp.progression.task.Task.TaskNames.GENERATE_OPA_NOTICE_TASK;

public enum Task {

    GENERATE_OPA_NOTICE(GENERATE_OPA_NOTICE_TASK);

    private final String taskName;

    Task(final String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }

    public static class TaskNames {
        public static final String GENERATE_OPA_NOTICE_TASK = "generate-opa-notice-notice";
        private TaskNames() {
        }
    }
}
