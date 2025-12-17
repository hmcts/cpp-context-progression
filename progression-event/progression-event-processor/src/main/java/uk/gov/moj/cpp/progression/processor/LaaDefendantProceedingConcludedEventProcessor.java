package uk.gov.moj.cpp.progression.processor;

import static java.lang.Integer.parseInt;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.processor.utils.RetryHelper.retryHelper;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LaaDefendantProceedingConcludedChanged;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.exception.LaaAzureApimInvocationException;
import uk.gov.moj.cpp.progression.service.ApplicationParameters;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.transformer.DefendantProceedingConcludedTransformer;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(EVENT_PROCESSOR)
public class LaaDefendantProceedingConcludedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaaDefendantProceedingConcludedEventProcessor.class.getName());

    @Inject
    private AzureFunctionService azureFunctionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private DefendantProceedingConcludedTransformer proceedingConcludedConverter;

    @Inject
    private ApplicationParameters applicationParameters;

    @Handles("progression.event.laa-defendant-proceeding-concluded-changed")
    public void processEvent(final JsonEnvelope event) throws InterruptedException {
        LOGGER.info("progression.event.defendant-proceeding-concluded-changed event received with metadata {} and payload {}", event.metadata(), event.payloadAsJsonObject());

        final LaaDefendantProceedingConcludedChanged defendantProceedingConcludedChanged = jsonObjectToObjectConverter.convert(event.payloadAsJsonObject(), LaaDefendantProceedingConcludedChanged.class);
        final uk.gov.justice.progression.courts.exract.LaaDefendantProceedingConcludedChanged laaDefendantProceedingConcludedChanged = uk.gov.justice.progression.courts.exract.LaaDefendantProceedingConcludedChanged.laaDefendantProceedingConcludedChanged()
                .withProsecutionConcludedRequest(proceedingConcludedConverter.getProsecutionConcludedRequest(defendantProceedingConcludedChanged.getDefendants(), defendantProceedingConcludedChanged.getProsecutionCaseId(), defendantProceedingConcludedChanged.getHearingId()))
                .build();
        final String payload = objectToJsonObjectConverter.convert(laaDefendantProceedingConcludedChanged.getProsecutionConcludedRequest()).toString();


        final List<UUID> defendantList = defendantProceedingConcludedChanged.getDefendants().stream()
                .map(Defendant::getId)
                .collect(Collectors.toList());

        retryHelper()
                .withSupplier(() -> azureFunctionService.concludeDefendantProceeding(payload))
                .withApimUrl(applicationParameters.getDefendantProceedingsConcludedApimUrl())
                .withPayload(payload)
                .withRetryTimes(parseInt(applicationParameters.getRetryTimes()))
                .withRetryInterval(parseInt(applicationParameters.getRetryInterval()))
                .withExceptionSupplier(() -> new LaaAzureApimInvocationException(defendantList, defendantProceedingConcludedChanged.getHearingId().toString(), applicationParameters.getDefendantProceedingsConcludedApimUrl()))
                .withPredicate(statusCode -> statusCode > 429)
                .build()
                .postWithRetry();
    }
}
