package uk.gov.moj.cpp.progression.query;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAAssociationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantLAAAssociationRepository;

import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@ServiceComponent(Component.QUERY_VIEW)
public class DefendantByLAAContractNumberQueryView {

    @Inject
    private DefendantLAAAssociationRepository defendantLAAAssociationRepository;

    @Handles("progression.query.defendants-by-laacontractnumber")
    public JsonEnvelope getDefendantsByLAAContractNumber(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String laaContractNumber = payload.getString("laaContractNumber");
        final List<DefendantLAAAssociationEntity> defenceLAAAssociations = defendantLAAAssociationRepository.findByLAAContractNUmber(laaContractNumber);
        final JsonObject responsePayload = JsonObjects.createObjectBuilder()
                .add("defendants",convertProsecutionCaseEntityToDefendantsList(defenceLAAAssociations))
                .build();
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                responsePayload);
    }

    private  JsonArray convertProsecutionCaseEntityToDefendantsList(final List<DefendantLAAAssociationEntity> defenceLAAAssociations) {
        final List<String> defendantIdList = defenceLAAAssociations.stream().filter( Objects :: nonNull)
                .map(DefendantLAAAssociationEntity ::getDefendantLAAKey)
                .map(defendantLAAKey -> defendantLAAKey.getDefendantId().toString())
                .collect(toList());
        final JsonArrayBuilder jsonArrayBuilder = JsonObjects.createArrayBuilder();
        defendantIdList.stream().forEach(jsonArrayBuilder :: add);
        return  jsonArrayBuilder.build();
    }




}
