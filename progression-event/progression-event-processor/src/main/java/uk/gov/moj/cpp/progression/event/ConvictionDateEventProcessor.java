package uk.gov.moj.cpp.progression.event;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import javax.inject.Inject;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateAdded;
import uk.gov.moj.cpp.progression.domain.event.ConvictionDateRemoved;

@ServiceComponent(EVENT_PROCESSOR)
public class ConvictionDateEventProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("public.hearing.offence-conviction-date-changed")
    public void handleHearingOffenceConvictionDateChangedPublicEvent(final JsonEnvelope event) {

        ConvictionDateAdded convictionDateAdded = this.jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(),
                ConvictionDateAdded.class);

        this.sender.send(this.enveloper.withMetadataFrom(event, "progression.command.offence-conviction-date-changed")
                .apply(this.objectToJsonObjectConverter.convert(convictionDateAdded)));
    }

    @Handles("public.hearing.offence-conviction-date-removed")
    public void handleHearingOffenceConvictionDateRemovedPublicEvent(final JsonEnvelope event) {

        ConvictionDateRemoved convictionDateRemoved = this.jsonObjectToObjectConverter
                .convert(event.payloadAsJsonObject(), ConvictionDateRemoved.class);

        this.sender.send(this.enveloper.withMetadataFrom(event, "progression.command.offence-conviction-date-removed")
                .apply(this.objectToJsonObjectConverter.convert(convictionDateRemoved)));
    }

}
