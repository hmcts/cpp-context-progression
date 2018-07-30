package uk.gov.moj.cpp.progression.activiti.workflow.initiatehearing.task;

import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.INITIATE_HEARING_PAYLOAD;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.USER_ID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.external.domain.hearing.InitiateHearing;
import uk.gov.moj.cpp.progression.helper.JsonHelper;

@ServiceComponent(Component.EVENT_PROCESSOR)
@Named
public class InitiateHearingTask implements JavaDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitiateHearingTask.class.getName());

    private static final String HEARING_COMMAND_INITIATE = "hearing.initiate";

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("hearing.initiate-dummy")
    public void doesNothing(final JsonEnvelope jsonEnvelope) {
        //required by framework
    }

    @Override
    public void execute(final DelegateExecution execution) throws Exception {
        final InitiateHearing initiateHearing =
                        (InitiateHearing) execution.getVariable(INITIATE_HEARING_PAYLOAD);
        final String userId = execution.getVariable(USER_ID).toString();
        final JsonObject jsonObject = objectToJsonObjectConverter.convert(initiateHearing);
        LOGGER.info("initiate hearing  json: {}", jsonObject);

        final JsonEnvelope postRequestEnvelope = JsonHelper.assembleEnvelopeWithPayloadAndMetaDetails(jsonObject, HEARING_COMMAND_INITIATE, initiateHearing.getHearing().getId().toString() , userId);
        sender.send(postRequestEnvelope);
    }

}
