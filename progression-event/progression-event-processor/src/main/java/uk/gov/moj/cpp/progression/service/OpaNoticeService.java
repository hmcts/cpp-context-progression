package uk.gov.moj.cpp.progression.service;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.jobstore.api.task.ExecutionStatus.STARTED;
import static uk.gov.moj.cpp.progression.task.Task.GENERATE_OPA_NOTICE;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.jobstore.api.ExecutionService;
import uk.gov.moj.cpp.jobstore.api.task.ExecutionInfo;
import uk.gov.moj.cpp.jobstore.persistence.Priority;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpaNoticeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpaNoticeService.class);
    private static final String OPA_NOTICE_KEY = "opaNotices";
    private static final String TRIGGER_DATE = "triggerDate";
    private static final String METADATA = "metadata";
    private static final String PAYLOAD = "payload";
    private static final String GENERATE_OPA_PUBLIC_LIST_NOTICE = "progression.command.generate-opa-public-list-notice";
    private static final String GENERATE_OPA_PRESS_LIST_NOTICE = "progression.command.generate-opa-press-list-notice";
    private static final String GENERATE_OPA_RESULT_LIST_NOTICE = "progression.command.generate-opa-result-list-notice";

    @Inject
    private UtcClock utcClock;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private ExecutionService executionService;

    public void generateOpaPublicListNotice(final JsonEnvelope envelope) {
        final String triggerDate = envelope.payloadAsJsonObject().getString(TRIGGER_DATE);

        progressionService.getPublicListNotices(envelope)
                .ifPresent(opaNotices -> startGenerateOpaNoticeTasks(GENERATE_OPA_PUBLIC_LIST_NOTICE, opaNotices, triggerDate));
    }

    public void generateOpaPressListNotice(final JsonEnvelope envelope) {
        final String triggerDate = envelope.payloadAsJsonObject().getString(TRIGGER_DATE);

        progressionService.getPressListNotices(envelope)
                .ifPresent(opaNotices -> startGenerateOpaNoticeTasks(GENERATE_OPA_PRESS_LIST_NOTICE, opaNotices, triggerDate));
    }

    public void generateOpaResultListNotice(final JsonEnvelope envelope) {
        final String triggerDate = envelope.payloadAsJsonObject().getString(TRIGGER_DATE);

        progressionService.getResultListNotices(envelope)
                .ifPresent(opaNotices -> startGenerateOpaNoticeTasks(GENERATE_OPA_RESULT_LIST_NOTICE, opaNotices, triggerDate));
    }

    private void startGenerateOpaNoticeTasks(final String command, final JsonObject opaNoticeList, final String triggerDate) {
        final List<JsonObject> opaNotices = convertNotices(command, opaNoticeList, triggerDate);

        LOGGER.info("{} received and size of results from database is {}", command, opaNotices.size());

        for (final JsonObject opaNotice : opaNotices) {
            final ExecutionInfo executionInfo = new ExecutionInfo(opaNotice, GENERATE_OPA_NOTICE.getTaskName(), utcClock.now(), STARTED,  Priority.MEDIUM);

            executionService.executeWith(executionInfo);
        }
    }

    private List<JsonObject> convertNotices(final String commandName,
                                            final JsonObject opaNotices,
                                            final String triggerAt) {
        return opaNotices.getJsonArray(OPA_NOTICE_KEY).stream()
                .map(JsonObject.class::cast)
                .map(payload -> createJsonObject(commandName, payload, triggerAt))
                .collect(toList());
    }

    private JsonObject createJsonObject(final String commandName,
                                        final JsonObject opaNotice,
                                        final String triggerDate) {
        final JsonObject payload = JsonObjects.createObjectBuilder(opaNotice)
                .add(TRIGGER_DATE, triggerDate)
                .build();

        return createObjectBuilder().add(METADATA, commandName).add(PAYLOAD, payload).build();
    }
}
