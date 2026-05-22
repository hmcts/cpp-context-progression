package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.CivilCaseExists;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class CivilCaseExistsProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CivilCaseExistsProcessor.class);

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Handles("progression.event.civil-case-exists")
    public void processEvent(final JsonEnvelope event) {

        final CivilCaseExists civilCaseExists = jsonObjectConverter.convert(event.payloadAsJsonObject(), CivilCaseExists.class);

        LOGGER.info("received event progression.event.group-case-exists with groupId {}, prosecutionCaseId {}, case urn {} ", civilCaseExists.getGroupId(), civilCaseExists.getProsecutionCaseId(), civilCaseExists.getCaseUrn());

        final JsonObject payload = nonNull(civilCaseExists.getGroupId()) ? createPayloadWithGroupId(civilCaseExists) : createPayloadWithoutGroupId(civilCaseExists);

        sender.send(
                Enveloper.envelop(payload)
                .withName("public.progression.events.civil-case-exists")
                .withMetadataFrom(event));

    }

    private JsonObject createPayloadWithoutGroupId(final CivilCaseExists civilCaseExists) {
        return createObjectBuilder()
                .add("prosecutionCaseId", civilCaseExists.getProsecutionCaseId().toString())
                .add("caseUrn", civilCaseExists.getCaseUrn())
                .build();
    }

    private JsonObject createPayloadWithGroupId(final CivilCaseExists civilCaseExists) {
        return createObjectBuilder()
                .add("groupId", civilCaseExists.getGroupId().toString())
                .add("prosecutionCaseId", civilCaseExists.getProsecutionCaseId().toString())
                .add("caseUrn", civilCaseExists.getCaseUrn())
                .build();
    }

}
