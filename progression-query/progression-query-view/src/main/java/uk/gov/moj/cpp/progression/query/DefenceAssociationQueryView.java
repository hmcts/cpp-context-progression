package uk.gov.moj.cpp.progression.query;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationHistory;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S3655"})
@ServiceComponent(Component.QUERY_VIEW)
public class DefenceAssociationQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceAssociationQueryView.class);
    private static final String ID = "defendantId";
    private static final String ASSOCIATED = "ASSOCIATED";
    private static final String ASSOCIATION = "association";
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String STATUS = "status";
    private static final String ADDRESS = "address";
    private static final String ADDRESS_LINE_1 = "addressLine1";
    private static final String ADDRESS_LINE_4 = "addressLine4";
    private static final String ADDRESS_POSTCODE = "addressPostcode";
    private static final String EMAIL = "email";
    private static final String ASSOCIATION_DATE = "associationDate";
    public static final String EMPTY_VALUE = "";

    @Inject
    private DefenceAssociationRepository defenceAssociationRepository;

    @Handles("progression.query.associated-organisation")
    public JsonEnvelope getDefendantRequest(final JsonEnvelope envelope) {
        final Optional<UUID> defendantId = JsonObjects.getUUID(envelope.payloadAsJsonObject(), ID);
        final DefenceAssociation defenceAssociationEntity;
        try {
            defenceAssociationEntity = defenceAssociationRepository.findByDefendantId(defendantId.get());
        } catch (NoResultException nre) {
            LOGGER.debug("No Association exist", nre);
            return emptyAssociation(envelope);
        }
        final DefenceAssociationHistory defenceAssociationHistory = extractDefenceAssociationDetails(defenceAssociationEntity);
        if (defenceAssociationHistory == null) {
            return emptyAssociation(envelope);
        }
        return formResponseWithAssociationDetails(envelope, defenceAssociationHistory);
    }

    private boolean isDefenceAssociationEmpty(final DefenceAssociation defenceAssociationEntity) {
        return defenceAssociationEntity == null ||
                defenceAssociationEntity.getDefenceAssociationHistories() == null ||
                defenceAssociationEntity.getDefenceAssociationHistories().isEmpty();
    }

    private JsonEnvelope formResponseWithAssociationDetails(final JsonEnvelope envelope, final DefenceAssociationHistory defenceAssociationHistory) {
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                formDefenceAssociationPayload(defenceAssociationHistory));
    }

    private boolean isAssociation(DefenceAssociationHistory defenceAssociationHistory) {
        return defenceAssociationHistory.getGrantorUserId() != null &&
                defenceAssociationHistory.getGranteeUserId() == null &&
                defenceAssociationHistory.getEndDate() == null &&
                defenceAssociationHistory.getAgentFlag().equals(false);
    }

    private DefenceAssociationHistory extractDefenceAssociationDetails(final DefenceAssociation defenceAssociationEntity) {
        if (isDefenceAssociationEmpty(defenceAssociationEntity)) {
            return null;
        }
        final List<DefenceAssociationHistory> defenceAssociationHistoryList =
                defenceAssociationEntity.getDefenceAssociationHistories().stream()
                        .filter(this::isAssociation)
                        .collect(Collectors.toList());

        return !defenceAssociationHistoryList.isEmpty() ? defenceAssociationHistoryList.get(0) : null;
    }

    private JsonObject formDefenceAssociationPayload(DefenceAssociationHistory defenceAssociationHistory) {

        String organisationId = "";
        String status = "";
        String associationDate = "";
        if (defenceAssociationHistory.getGrantorUserId() != null && defenceAssociationHistory.getStartDate() != null) {
            organisationId = defenceAssociationHistory.getGrantorOrgId().toString();
            associationDate = ZonedDateTimes.toString(defenceAssociationHistory.getStartDate());
            status = ASSOCIATED;
        }
        return formResponse(organisationId, status, associationDate);
    }

    private JsonObject formResponse(final String organisationId, final String status, final String associationDate) {
        return Json.createObjectBuilder()
                .add(ASSOCIATION, Json.createObjectBuilder()
                        .add(ORGANISATION_ID, organisationId)
                        .add(ORGANISATION_NAME, EMPTY_VALUE)
                        .add(STATUS, status)
                        .add(ADDRESS, Json.createObjectBuilder()
                                .add(ADDRESS_LINE_1, EMPTY_VALUE)
                                .add(ADDRESS_LINE_4, EMPTY_VALUE)
                                .add(ADDRESS_POSTCODE, EMPTY_VALUE)
                                .add(EMAIL, EMPTY_VALUE)
                        )
                        .add(ASSOCIATION_DATE, associationDate)
                )
                .build();
    }

    private JsonEnvelope emptyAssociation(final JsonEnvelope envelope) {
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                Json.createObjectBuilder()
                        .add(ASSOCIATION, Json.createObjectBuilder())
                        .build());
    }
}
