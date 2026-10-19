package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CourtFeeForCivilApplicationUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InitiateCourtApplicationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InitiateCourtApplicationRepository;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_LISTENER)
public class CourtFeeForCivilApplicationUpdatedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtFeeForCivilApplicationUpdatedEventListener.class);
    private static final String COURT_APPLICATION_PAYMENT = "courtApplicationPayment";
    private static final String COURT_APPLICATION = "courtApplication";

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

    @Inject
    private InitiateCourtApplicationRepository initiateCourtApplicationRepository;

    @Handles("progression.event.court-fee-for-civil-application-updated")
    public void processEvent(final JsonEnvelope event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("received event progression.event.court-fee-for-civil-application-updated {} ", event.toObfuscatedDebugString());
        }

        final JsonObject civilApplicationObj = event.payloadAsJsonObject();
        final JsonObject courtApplicationPaymentObj = civilApplicationObj.getJsonObject(COURT_APPLICATION_PAYMENT);

        final CourtFeeForCivilApplicationUpdated courtFeeForCivilApplicationUpdated = jsonObjectConverter.convert(civilApplicationObj, CourtFeeForCivilApplicationUpdated.class);
        final UUID applicationId = courtFeeForCivilApplicationUpdated.getApplicationId();

        final CourtApplicationEntity courtApplicationEntity = courtApplicationRepository.findByApplicationId(applicationId);
        final JsonObject courtApplication = stringToJsonObjectConverter.convert(courtApplicationEntity.getPayload());
        final JsonObjectBuilder courtApplicationPayload = updatePaymentPayload(courtApplicationPaymentObj, courtApplication);
        courtApplicationEntity.setPayload(courtApplicationPayload.build().toString());
        courtApplicationRepository.save(courtApplicationEntity);

        final InitiateCourtApplicationEntity initiateCourtApplicationEntity = initiateCourtApplicationRepository.findBy(applicationId);
        final JsonObject initiateCourtApplication = stringToJsonObjectConverter.convert(initiateCourtApplicationEntity.getPayload());
        final JsonObjectBuilder appPaymentBuilder = createObjectBuilder();
        initiateCourtApplication.forEach((k, v) -> {
            if (k.equals(COURT_APPLICATION)) {
                final JsonObjectBuilder initiateCourtApplicationPayload = updatePaymentPayload(courtApplicationPaymentObj, initiateCourtApplication.getJsonObject(COURT_APPLICATION));
                appPaymentBuilder.add(k, initiateCourtApplicationPayload.build());
            } else {
                appPaymentBuilder.add(k, v);
            }
        });
        initiateCourtApplicationEntity.setPayload(appPaymentBuilder.build().toString());
        initiateCourtApplicationRepository.save(initiateCourtApplicationEntity);
    }

    private JsonObject createApplicationPaymentObject(JsonObject appPayment, JsonObject courtApplicationPaymentObj) {
        final JsonObjectBuilder appPaymentBuilder = createObjectBuilder();
        courtApplicationPaymentObj.forEach((k, v) -> {
            if (appPayment.containsKey(k)) {
                appPaymentBuilder.add(k, courtApplicationPaymentObj.get(k));
            } else if (!appPayment.containsKey(k) && courtApplicationPaymentObj.containsKey(k)) {
                appPaymentBuilder.add(k, courtApplicationPaymentObj.get(k));
            }
            else {
                appPaymentBuilder.add(k, appPayment.get(k));
            }
        });

        appPayment.forEach((k, v) -> {
            if (!courtApplicationPaymentObj.containsKey(k)) {
                appPaymentBuilder.add(k, appPayment.get(k));
            }
        });

        return appPaymentBuilder.build();
    }

    private JsonObjectBuilder updatePaymentPayload(final JsonObject courtApplicationPaymentObj, final JsonObject courtApplication) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final AtomicBoolean isPresent = new AtomicBoolean(false);
        courtApplication.forEach((k, v) -> {
            if (k.equals(COURT_APPLICATION_PAYMENT)) {
                final JsonObject appPayment = courtApplication.getJsonObject(COURT_APPLICATION_PAYMENT);
                jsonObjectBuilder.add(COURT_APPLICATION_PAYMENT, createApplicationPaymentObject(appPayment, courtApplicationPaymentObj));
                isPresent.set(true);
            } else {
                jsonObjectBuilder.add(k, v);
            }
        });

        if (!isPresent.get()) {
            jsonObjectBuilder.add(COURT_APPLICATION_PAYMENT, courtApplicationPaymentObj);
        }
        return jsonObjectBuilder;
    }
}
