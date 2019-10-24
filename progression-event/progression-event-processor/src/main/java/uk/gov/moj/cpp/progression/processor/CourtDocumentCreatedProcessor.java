package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class CourtDocumentCreatedProcessor {

    private static final String PROGRESSION_COMMAND_UPDATE_FINANCIAL_MEANS_DATA = "progression.command.update-financial-means-data";


    @Inject
    private Sender sender;

    @Handles("progression.event.court-document-created")
    public void processCourtDocumentCreated(final JsonEnvelope envelope) {

        sender.send(
                Enveloper.envelop(envelope.payloadAsJsonObject())
                        .withName(PROGRESSION_COMMAND_UPDATE_FINANCIAL_MEANS_DATA)
                        .withMetadataFrom(envelope));

    }
}
