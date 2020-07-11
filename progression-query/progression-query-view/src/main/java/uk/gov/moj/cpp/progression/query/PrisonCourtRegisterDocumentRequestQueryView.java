package uk.gov.moj.cpp.progression.query;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.PrisonCourtRegisterEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.PrisonCourtRegisterRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

@ServiceComponent(Component.QUERY_VIEW)
public class PrisonCourtRegisterDocumentRequestQueryView {
    private static final String FIELD_COURT_CENTRE_ID = "courtCentreId";
    private static final String FIELD_PRISON_COURT_REGISTER_DOCUMENTS = "prisonCourtRegisterDocumentRequests";

    @Inject
    private PrisonCourtRegisterRepository prisonCourtRegisterRepository;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("progression.query.prison-court-register-document-by-court-centre")
    public JsonEnvelope getPrisonCourtRegistersByCourtCentre(final JsonEnvelope envelope) {
        final UUID courtCentreId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_COURT_CENTRE_ID));
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        final JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        final List<PrisonCourtRegisterEntity> prisonCourtRegisterEntities = prisonCourtRegisterRepository.findByCourtCentreId(courtCentreId);
        prisonCourtRegisterEntities.forEach(i -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(i)));
        return envelopeFrom(envelope.metadata(),
                jsonObjectBuilder.add(FIELD_PRISON_COURT_REGISTER_DOCUMENTS, jsonArrayBuilder.build()).build());
    }
}
