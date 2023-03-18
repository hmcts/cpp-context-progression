package uk.gov.moj.cpp.progression.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedResent;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.transformer.DefendantProceedingConcludedTransformer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class LaaDefendantProceedingConcludedResendEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaaDefendantProceedingConcludedResendEventProcessor.class.getName());

    @Inject
    private AzureFunctionService azureFunctionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DefendantProceedingConcludedTransformer proceedingConcludedConverter;


    @Handles("progression.event.laa-defendant-proceeding-concluded-resent")
    public void resendProceedingConcludedToLaa(final JsonEnvelope event) {
        LOGGER.info("progression.event.laa-defendant-proceeding-concluded-resent event received with metadata {} and payload {}", event.metadata(), event.payloadAsJsonObject());
        final LaaDefendantProceedingConcludedResent laaDefendantProceedingConcludedResent = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), LaaDefendantProceedingConcludedResent.class);
        final LaaDefendantProceedingConcludedChanged defendantProceedingConcludedChanged = laaDefendantProceedingConcludedResent.getLaaDefendantProceedingConcludedChanged();

        final uk.gov.justice.progression.courts.exract.LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged = uk.gov.justice.progression.courts.exract.LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged()
                .withProsecutionConcludedRequest(proceedingConcludedConverter.getProsecutionConcludedRequest(defendantProceedingConcludedChanged.getDefendants(), defendantProceedingConcludedChanged.getProsecutionCaseId(), defendantProceedingConcludedChanged.getHearingId()))
                .build();

        final String payload = objectToJsonObjectConverter.convert(laaDefendantProceedingConcludedChanged.getProsecutionConcludedRequest()).toString();

        LOGGER.info("Invoking Azure concludeDefendantProceeding function with payload with payload {}", payload);
        final Integer statusCode = azureFunctionService.concludeDefendantProceeding(payload);
        LOGGER.info("Azure concludeDefendantProceeding function completed with status code {}", statusCode);

    }




}
