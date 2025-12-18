package uk.gov.justice.api.resource.service;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.AssociatedDefenceOrganisation;
import uk.gov.justice.core.courts.DefenceOrganisation;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefenceQueryService {

    public static final String DEFENCE_ADVOCATE_QUERY_ROLE_IN_CASE = "advocate.query.role-in-case-by-caseid";
    public static final String DEFENCE_QUERY_ASSOCIATED_ORGANISATIONS = "defence.query.associated-organisations";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceQueryService.class);
    private static final String BOTH = "both";
    private static final String PROSECUTING = "prosecuting";
    private static final String DEFENDING = "defending";
    private static final String IS_ADVOCATE_DEFENDING_OR_PROSECUTING = "isAdvocateDefendingOrProsecuting";


    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    public boolean isUserProsecutingCase(final JsonEnvelope jsonEnvelope, final String caseId) {

        final JsonEnvelope roleInCaseEnvelope = queryRoleInCase(jsonEnvelope, caseId);

        if (isNull(roleInCaseEnvelope) || !JsonValue.ValueType.OBJECT.equals(roleInCaseEnvelope.payload().getValueType())) {
            return false;
        }

        return Optional.of(roleInCaseEnvelope)
                .map(JsonEnvelope::payloadAsJsonObject)
                .filter(json -> nonNull(json.get(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .map(json -> PROSECUTING.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING))
                        || BOTH.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .orElse(false);
    }

    public boolean isUserProsecutingOrDefendingCase(final Envelope<?> jsonEnvelope, final String caseId) {
        final JsonEnvelope roleInCaseEnvelope = queryRoleInCase(jsonEnvelope, caseId);

        if (roleInCaseEnvelope == null || !JsonValue.ValueType.OBJECT.equals(roleInCaseEnvelope.payload().getValueType())) {
            return false;
        }

        return Optional.of(roleInCaseEnvelope)
                .map(JsonEnvelope::payloadAsJsonObject)
                .filter(json -> nonNull(json.get(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .map(json -> PROSECUTING.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING))
                        || DEFENDING.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING))
                        || BOTH.equals(json.getString(IS_ADVOCATE_DEFENDING_OR_PROSECUTING)))
                .orElse(false);
    }

    public List<UUID> getDefendantList(final Envelope<?> jsonEnvelope, final String caseId) {
        final JsonEnvelope roleInCaseEnvelope = queryRoleInCase(jsonEnvelope, caseId);

        if (nonNull(roleInCaseEnvelope) && roleInCaseEnvelope.payloadAsJsonObject().containsKey("authorizedDefendantIds")) {
            return roleInCaseEnvelope.payloadAsJsonObject().getJsonArray("authorizedDefendantIds").stream()
                    .map(jsonValue -> fromString(((JsonString) jsonValue).getString()))
                    .collect(toList());
        }

        return emptyList();
    }

    public List<AssociatedDefenceOrganisation> getAllAssociatedOrganisations(final UUID userId, final String defendantId) {

        final JsonEnvelope associatedOrganisationsEnvelope = queryAssociatedOrganisations(getDefenceQueryJsonEnvelop(userId), defendantId);

        if (nonNull(associatedOrganisationsEnvelope) && associatedOrganisationsEnvelope.payloadAsJsonObject().containsKey("associations")) {
            return associatedOrganisationsEnvelope.payloadAsJsonObject().getJsonArray("associations").stream()
                    .map(jsonValue -> toAssociatedDefenceOrganisation(jsonValue.asJsonObject()))
                    .collect(toList());
        }

        return emptyList();
    }

    private AssociatedDefenceOrganisation toAssociatedDefenceOrganisation(final JsonObject jsonObject) {
        final AssociatedDefenceOrganisation.Builder associatedDefenceOrganisation = AssociatedDefenceOrganisation.associatedDefenceOrganisation();
        ofNullable(jsonObject.getString("endDate", null)).filter(StringUtils::isNotBlank)
                .ifPresent(edt -> associatedDefenceOrganisation.withAssociationEndDate(ZonedDateTime.parse(edt).toLocalDate()));

        return associatedDefenceOrganisation
                .withAssociationStartDate(ZonedDateTime.parse(jsonObject.getString("startDate")).toLocalDate())
                .withDefenceOrganisation(DefenceOrganisation.defenceOrganisation()
                        .withOrganisation(Organisation.organisation()
                                .withId(fromString(jsonObject.getString("organisationId")))
                                .withName(jsonObject.getString("organisationName"))
                                .withAddress(toAddress(jsonObject.get("address")))
                                .build())
                        .build())
                .build();
    }

    private Address toAddress(final JsonValue addressJson) {
        final Address.Builder addressObjBuilder = Address.address();
        if (nonNull(addressJson)) {
            addressObjBuilder.withAddress1(addressJson.asJsonObject().getString("address1", EMPTY));
            addressObjBuilder.withAddress2(addressJson.asJsonObject().getString("address2", EMPTY));
            addressObjBuilder.withAddress3(addressJson.asJsonObject().getString("address3", EMPTY));
            addressObjBuilder.withAddress4(addressJson.asJsonObject().getString("address4", EMPTY));
            addressObjBuilder.withPostcode(addressJson.asJsonObject().getString("addressPostcode", EMPTY));
        }

        return addressObjBuilder.build();
    }

    private JsonEnvelope queryRoleInCase(final Envelope<?> envelope, final String caseId) {
        final JsonObject payload = createObjectBuilder().add("caseId", caseId).build();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(DEFENCE_ADVOCATE_QUERY_ROLE_IN_CASE)
                .build();

        final JsonEnvelope jsonEnvelope = requester.request(envelopeFrom(metadata, payload));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} received with payload {}", DEFENCE_ADVOCATE_QUERY_ROLE_IN_CASE, jsonEnvelope.toObfuscatedDebugString());
        }
        return jsonEnvelope;
    }

    private JsonEnvelope queryAssociatedOrganisations(final Envelope<?> envelope, final String defendantId) {
        final JsonObject payload = createObjectBuilder().add("defendantId", defendantId).build();

        final Metadata metadata = metadataFrom(envelope.metadata())
                .withName(DEFENCE_QUERY_ASSOCIATED_ORGANISATIONS)
                .build();

        final JsonEnvelope jsonEnvelope = requester.request(envelopeFrom(metadata, payload));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("{} received with payload {}", DEFENCE_QUERY_ASSOCIATED_ORGANISATIONS, jsonEnvelope.toObfuscatedDebugString());
        }
        return jsonEnvelope;
    }

    private JsonEnvelope getDefenceQueryJsonEnvelop(final UUID userId) {
        return envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(DEFENCE_QUERY_ASSOCIATED_ORGANISATIONS)
                        .withUserId(userId.toString())
                        .build(),
                createObjectBuilder()
                        .build()
        );
    }

}
