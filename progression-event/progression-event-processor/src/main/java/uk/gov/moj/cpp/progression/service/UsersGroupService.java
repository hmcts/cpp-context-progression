package uk.gov.moj.cpp.progression.service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.value.object.DefenceOrganisationVO;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.progression.service.MetadataUtil.metadataWithNewActionName;
@Slf4j
public class UsersGroupService {


    @ServiceComponent(Component.EVENT_PROCESSOR)
    @Inject
    private Requester requester;

    public Optional<DefenceOrganisationVO> getDefenceOrganisationDetails(final UUID organisationId, final Metadata metadata) {
        log.info("Getting defence organisation details for organisation id {} " + organisationId.toString());

        final JsonObject getOrganisationForUserRequest = Json.createObjectBuilder().add("organisationId", organisationId.toString()).build();
        final Metadata metadataWithActionName = metadataWithNewActionName(metadata, "usersgroups.get-organisation-details");
        final JsonEnvelope requestEnvelope = envelopeFrom(metadataWithActionName, getOrganisationForUserRequest);
        final JsonEnvelope response = requester.requestAsAdmin(requestEnvelope);
        final JsonObject jsonObject = response.payloadAsJsonObject();

        log.info("Response organisation details returned : {}" + response.toObfuscatedDebugString());

        return Optional.of(DefenceOrganisationVO.builder()
                .name(jsonObject.getString("organisationName"))
                .email(jsonObject.getString("email"))
                .phoneNumber(jsonObject.getString("phoneNumber"))
                .addressLine1(jsonObject.getString("addressLine1"))
                .addressLine2(jsonObject.getString("addressLine2"))
                .addressLine3(jsonObject.getString("addressLine3"))
                .addressLine4(jsonObject.getString("addressLine4"))
                .postcode(jsonObject.getString("addressPostcode"))
                .build());
  }
}




