package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.core.courts.CaseMarkersUpdated;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.List;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S3655")
@ServiceComponent(EVENT_LISTENER)
public class ProsecutionCaseMarkersUpdatedListener {
    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private ProsecutionCaseRepository repository;

    private static final Logger LOGGER = LoggerFactory.getLogger(ProsecutionCaseMarkersUpdatedListener.class);

    @Handles("progression.event.case-markers-updated")
    public void processCaseMarkersUpdated(final JsonEnvelope event) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("received event  progression.event.case-markers-updated {} ", event.toObfuscatedDebugString());
        }

        final CaseMarkersUpdated caseMarkersUpdatedEvent = jsonObjectConverter.convert(event.payloadAsJsonObject(), CaseMarkersUpdated.class);
        final ProsecutionCaseEntity prosecutionCaseEntity = repository.findByCaseId(caseMarkersUpdatedEvent.getProsecutionCaseId());
        final JsonObject prosecutionCaseJson = jsonFromString(prosecutionCaseEntity.getPayload());
        final ProsecutionCase prosecutionCase = jsonObjectConverter.convert(prosecutionCaseJson, ProsecutionCase.class);
        final ProsecutionCase updatedProsecutionCase = updateCaseMarkers(prosecutionCase, caseMarkersUpdatedEvent.getCaseMarkers());

        repository.save(getProsecutionCaseEntity(updatedProsecutionCase));

    }

    private ProsecutionCase updateCaseMarkers(final ProsecutionCase prosecutionCase, final List<Marker> caseMarkers) {
        return ProsecutionCase.prosecutionCase()
                .withValuesFrom(prosecutionCase)
                .withCaseMarkers(caseMarkers)
                .build();
    }

    private ProsecutionCaseEntity getProsecutionCaseEntity(final ProsecutionCase prosecutionCase) {
        final ProsecutionCaseEntity pCaseEntity = new ProsecutionCaseEntity();
        pCaseEntity.setCaseId(prosecutionCase.getId());
        if (nonNull(prosecutionCase.getGroupId())) {
            pCaseEntity.setGroupId(prosecutionCase.getGroupId());
        }
        pCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        return pCaseEntity;
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }

}
