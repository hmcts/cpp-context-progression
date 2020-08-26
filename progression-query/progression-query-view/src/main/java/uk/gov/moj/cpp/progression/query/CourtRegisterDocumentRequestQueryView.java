package uk.gov.moj.cpp.progression.query;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.progression.domain.constant.RegisterStatus;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CourtRegisterRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CourtRegisterRequestRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;

@ServiceComponent(Component.QUERY_VIEW)
public class CourtRegisterDocumentRequestQueryView {
    private static final String FIELD_REQUEST_STATUS = "requestStatus";
    private static final String FIELD_COURT_REGISTER_DOCUMENTS = "courtRegisterDocumentRequests";
    private static final String FIELD_MATERIAL_ID = "materialId";
    private static final String FIELD_COURT_HOUSE = "courtHouse";
    private static final String FIELD_REGISTER_DATE = "registerDate";

    @Inject
    private CourtRegisterRequestRepository courtRegisterRequestRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.query.court-register-document-request")
    public JsonEnvelope getCourtRegisterRequests(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final String requestStatus = envelope.payloadAsJsonObject().getString(FIELD_REQUEST_STATUS);
        if (isNotBlank(requestStatus)) {
            if(RegisterStatus.RECORDED.toString().equalsIgnoreCase(requestStatus)) {
                final List<CourtRegisterRequestEntity> courtRegisterRequestEntities = courtRegisterRequestRepository.findByStatusRecorded();
                courtRegisterRequestEntities.forEach(courtRegisterRequestEntity -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(courtRegisterRequestEntity)));

            } else {
                final List<CourtRegisterRequestEntity> courtRegisterRequestEntity = courtRegisterRequestRepository.findByStatus(RegisterStatus.valueOf(requestStatus));
                courtRegisterRequestEntity.forEach(i -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(i)));
            }
        }
        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_COURT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }

    @Handles("progression.query.court-register-document-by-material")
    public JsonEnvelope getCourtRegisterByMaterial(final JsonEnvelope envelope) {
        final UUID materialId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_MATERIAL_ID));
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final List<CourtRegisterRequestEntity> courtRegisterRequestEntity = courtRegisterRequestRepository.findBySystemDocGeneratorId(materialId);
        courtRegisterRequestEntity.forEach(i -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(i)));
        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_COURT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }

    @Handles("progression.query.court-register-document-by-request-date")
    public JsonEnvelope getCourtRegistersByRequestDate(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        final Optional<LocalDate> registerDate = JsonObjects.getString(payload, FIELD_REGISTER_DATE).map(LocalDate::parse);

        registerDate.ifPresent(reqDate -> {
            List<CourtRegisterRequestEntity> courtRegisterRequestEntities;
            final String courtHousesAsString = payload.getString(FIELD_COURT_HOUSE, "");
            final Set<String> courtHouses = Stream.of(courtHousesAsString.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(toSet());

            if (courtHouses.isEmpty()) {
                courtRegisterRequestEntities = courtRegisterRequestRepository.findByRequestDate(reqDate);
            } else {
                courtRegisterRequestEntities = courtHouses.stream().map(courtHouse -> courtRegisterRequestRepository.findByRequestDateAndCourtHouse(reqDate, courtHouse))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
            courtRegisterRequestEntities.forEach(i -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(i)));
        });
        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_COURT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }
}
