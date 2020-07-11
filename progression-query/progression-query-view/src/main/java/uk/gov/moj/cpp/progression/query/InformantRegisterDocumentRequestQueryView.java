package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.fromString;
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
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.InformantRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.InformantRegisterRepository;

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
public class InformantRegisterDocumentRequestQueryView {
    private static final String FIELD_REQUEST_STATUS = "requestStatus";
    private static final String FIELD_INFORMANT_REGISTER_DOCUMENTS = "informantRegisterDocumentRequests";
    private static final String FIELD_FILE_ID = "fileId";
    private static final String FIELD_PROSECUTION_AUTHORITY_CODE = "prosecutionAuthorityCode";
    private static final String FIELD_REGISTER_DATE = "registerDate";

    @Inject
    private InformantRegisterRepository informantRegisterRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.query.informant-register-document-request")
    public JsonEnvelope getInformantRegisterRequests(final JsonEnvelope envelope) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final String requestStatus = envelope.payloadAsJsonObject().getString(FIELD_REQUEST_STATUS);
        if (isNotBlank(requestStatus)) {
            final List<InformantRegisterEntity> informantRegisterEntities = informantRegisterRepository.findByStatus(RegisterStatus.valueOf(requestStatus));
            informantRegisterEntities.forEach(informantRegisterEntity -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(informantRegisterEntity)));
        }
        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_INFORMANT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }

    @Handles("progression.query.informant-register-document-by-material")
    public JsonEnvelope getInformantRegistersByMaterial(final JsonEnvelope envelope) {
        final UUID fileId = fromString(envelope.payloadAsJsonObject().getString(FIELD_FILE_ID));

        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        final List<InformantRegisterEntity> informantRegisterEntities = informantRegisterRepository.findByFileId(fileId);
        informantRegisterEntities.forEach(informantRegisterEntity -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(informantRegisterEntity)));

        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_INFORMANT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }

    @Handles("progression.query.informant-register-document-by-request-date")
    public JsonEnvelope getInformantRegistersByRequestDate(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();

        final Optional<LocalDate> registerDate = JsonObjects.getString(payload, FIELD_REGISTER_DATE).map(LocalDate::parse);

        registerDate.ifPresent(regDate -> {
            List<InformantRegisterEntity> informantRegisterEntities;
            final String prosecutionAuthoritiesAsString = payload.getString(FIELD_PROSECUTION_AUTHORITY_CODE, "");
            final Set<String> prosecutionAuthorities = Stream.of(prosecutionAuthoritiesAsString.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotEmpty)
                    .collect(toSet());

            if (prosecutionAuthorities.isEmpty()) {
                informantRegisterEntities = informantRegisterRepository.findByRegisterDate(regDate);
            } else {
                informantRegisterEntities = prosecutionAuthorities.stream().map(prosecutionAuthority -> informantRegisterRepository.findByRegisterDateAndProsecutionAuthorityCode(regDate, prosecutionAuthority))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
            informantRegisterEntities.forEach(i -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(i)));
        });

        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_INFORMANT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }
}
