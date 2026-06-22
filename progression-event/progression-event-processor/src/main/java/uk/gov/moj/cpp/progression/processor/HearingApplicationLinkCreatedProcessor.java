package uk.gov.moj.cpp.progression.processor;

import java.util.Objects;
import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(Component.EVENT_PROCESSOR)
public class HearingApplicationLinkCreatedProcessor {

    private static final String HEARING_LANGUAGE = "hearingLanguage";
    private static final String COURT_APPLICATIONS = "courtApplications";
    public static final String HEARING_DAYS = "hearingDays";

    @Inject
    private Sender sender;
    @Inject
    private Enveloper enveloper;



    @Handles("progression.event.hearing-application-link-created")
    public void process(final JsonEnvelope event) {
        final JsonObjectBuilder payload = JsonObjects.createObjectBuilder();
        final JsonObject hearing  = event.payloadAsJsonObject().getJsonObject("hearing");
        payload.add("id",hearing.getString("id"));
        payload.add("courtCentre",hearing.getJsonObject("courtCentre"));
        if(Objects.nonNull(hearing.get(HEARING_LANGUAGE))) {
            payload.add(HEARING_LANGUAGE, hearing.getString(HEARING_LANGUAGE));
        }
        if(Objects.nonNull(hearing.get(COURT_APPLICATIONS))) {
            hearing.getJsonArray(COURT_APPLICATIONS).stream().map(JsonObject.class::cast)
                    .filter(app -> app.getString("id").equals(event.payloadAsJsonObject().getString("applicationId")))
                    .forEach(app -> payload.add("courtApplication", app));
        }
        payload.add("type", hearing.getJsonObject("type"));
        if (hearing.containsKey(HEARING_DAYS)) {
            payload.add(HEARING_DAYS, hearing.getJsonArray(HEARING_DAYS));
            sender.send(Enveloper.envelop(payload.build()).withName("progression.command.update-hearing-for-allocation-fields").withMetadataFrom(event));
        }
    }
}
