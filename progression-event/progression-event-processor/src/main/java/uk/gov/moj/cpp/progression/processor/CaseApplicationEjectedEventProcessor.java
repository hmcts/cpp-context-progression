package uk.gov.moj.cpp.progression.processor;

import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.progression.courts.GetHearingsAtAGlance;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.AzureFunctionService;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.io.IOException;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:CallToDeprecatedMethod")
public class CaseApplicationEjectedEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseApplicationEjectedEventProcessor.class.getCanonicalName());
    private static final String CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";
    private static final String APPLICATION_ID = "applicationId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String REMOVAL_REASON = "removalReason";
    private static final String SURREY_POLICE_ORIG_ORGANISATION = "045AA00";
    private static final String SUSSEX_POLICE_ORIG_ORGANISATION = "047AA00";
    private static final String A_4 = "A4";
    private static final String ZERO_FOUR = "04";
    private static final String EVENT_RECEIVED = "Received '{}' event with payload {}";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ProgressionService progressionService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private AzureFunctionService azureFunctionService;

    @Handles("progression.event.case-ejected")
    public void processCaseEjected(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_RECEIVED, "progression.event.case-ejected", event.toObfuscatedDebugString());
        }
        final JsonArray hearingIds = getHearingIdsForCaseAllApplications(event);
        final JsonObject payload = event.payloadAsJsonObject();
        final String removalReason = payload.getString(REMOVAL_REASON);
        sendPublicMessage(event, hearingIds, payload.getString(PROSECUTION_CASE_ID), PROSECUTION_CASE_ID, removalReason);
        setCaseEjectedStorage(event, payload.getString(PROSECUTION_CASE_ID));
    }

    @Handles("progression.event.case-ejected-via-bdf")
    public void processCaseEjectedViaBdf(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_RECEIVED, "progression.event.case-ejected-via-bdf", event.toObfuscatedDebugString());
        }
        final JsonArray hearingIds = getHearingIdsForCaseAllApplications(event);
        final JsonObject payload = event.payloadAsJsonObject();
        final String removalReason = payload.getString(REMOVAL_REASON);
        sendPublicMessage(event, hearingIds, payload.getString(PROSECUTION_CASE_ID), PROSECUTION_CASE_ID, removalReason);
        setCaseEjectedStorage(event, payload.getString(PROSECUTION_CASE_ID));
    }

    @Handles("progression.event.application-ejected")
    public void processApplicationEjected(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_RECEIVED, "progression.event.application-ejected", event.toObfuscatedDebugString());
        }
        final JsonObject payload = event.payloadAsJsonObject();
        final String applicationId = payload.getString(APPLICATION_ID);

        final JsonArray hearingIds = getHearingIdsForAllApplications(event, applicationId);
        final String removalReason = payload.getString(REMOVAL_REASON);
        sendPublicMessage(event, hearingIds, applicationId, APPLICATION_ID, removalReason);
    }

    private JsonArray getHearingIdsForCaseAllApplications(final JsonEnvelope event) {
        final String prosecutionCaseId = event.payloadAsJsonObject().getString(PROSECUTION_CASE_ID);
        final JsonArrayBuilder hearingIdsBuilder = JsonObjects.createArrayBuilder();
        progressionService.getProsecutionCaseDetailById(event, prosecutionCaseId).ifPresent(prosecutionCaseJsonObject -> {
            final GetHearingsAtAGlance hearingsAtAGlance = jsonObjectToObjectConverter.
                    convert(prosecutionCaseJsonObject.getJsonObject("hearingsAtAGlance"),
                            GetHearingsAtAGlance.class);
            if (isNotEmpty(hearingsAtAGlance.getHearings())) {
                hearingsAtAGlance.getHearings().stream().forEach(hearing -> hearingIdsBuilder.add(hearing.getId().toString()));
            }
        });
        return hearingIdsBuilder.build();
    }

    private void addHearingIds(JsonArray hearingIds, JsonObjectBuilder payloadBuilder) {
        if (isNotEmpty(hearingIds)) {
            payloadBuilder.add("hearingIds", hearingIds);
        }
    }

    private void setCaseEjectedStorage(final JsonEnvelope event, final String prosecutionCaseId) {
        progressionService.getProsecutionCaseDetailById(event, prosecutionCaseId).ifPresent(prosecutionCaseJsonObject -> {
            final ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.
                    convert(prosecutionCaseJsonObject.getJsonObject("prosecutionCase"),
                            ProsecutionCase.class);

            if (prosecutionCase != null && prosecutionCase.getProsecutionCaseIdentifier() != null) {
                final JsonObjectBuilder payloadBuilder = JsonObjects.createObjectBuilder();
                final ProsecutionCaseIdentifier caseIdentifier = prosecutionCase.getProsecutionCaseIdentifier();

                payloadBuilder.add("CaseId", prosecutionCaseId);
                payloadBuilder.add("ProsecutorCode", nonNull(prosecutionCase.getOriginatingOrganisation()) ? getOriginatingOrganisation(prosecutionCase.getOriginatingOrganisation()) : "");
                payloadBuilder.add("InitiationCode", prosecutionCase.getInitiationCode().toString());
                payloadBuilder.add("CaseReference", caseIdentifier.getProsecutionAuthorityReference() != null ? caseIdentifier.getProsecutionAuthorityReference() : caseIdentifier.getCaseURN());

                try {
                    final Integer statusCode = azureFunctionService.makeFunctionCall(payloadBuilder.build().toString());
                    LOGGER.info(String.format("Azure function status code %s", statusCode));
                } catch (IOException ex) {
                    LOGGER.error(String.format("Failed to call Azure function %s", ex));
                }
            }
        });
    }

    private String getOriginatingOrganisation(final String originatingOrganisation) {
        final String trimmedValue = StringUtils.trim(originatingOrganisation);
        // re-transform done from staging.prosecutors.spi to match next message for ejected case
        if (SURREY_POLICE_ORIG_ORGANISATION.equalsIgnoreCase(trimmedValue) || SUSSEX_POLICE_ORIG_ORGANISATION.equalsIgnoreCase(trimmedValue)) {
            return trimmedValue.replace(ZERO_FOUR, A_4);
        }
        return originatingOrganisation;
    }

    private JsonArray getHearingIdsForAllApplications(final JsonEnvelope event, final String applicationId) {
        final JsonArrayBuilder hearingIdsBuilder = JsonObjects.createArrayBuilder();
        progressionService.getCourtApplicationById(event, applicationId).ifPresent(applicationAtAGlance -> {
            final JsonArray hearings = applicationAtAGlance.getJsonArray("hearings");

            if (isNotEmpty(hearings)) {
                hearings.getValuesAs(JsonObject.class).stream()
                        .forEach(hearing -> hearingIdsBuilder.add(hearing.getString("id")));
            }
        });
        return hearingIdsBuilder.build();
    }

    public void sendPublicMessage(final JsonEnvelope event, final JsonArray hearingIds, final String id, final String idKey, final String removalReason) {
        final JsonObjectBuilder payloadBuilder = JsonObjects.createObjectBuilder();
        payloadBuilder.add(idKey, id);
        payloadBuilder.add(REMOVAL_REASON, removalReason);
        addHearingIds(hearingIds, payloadBuilder);
        sender.send(enveloper.withMetadataFrom(event, CASE_OR_APPLICATION_EJECTED).apply(payloadBuilder.build()));
    }
}
