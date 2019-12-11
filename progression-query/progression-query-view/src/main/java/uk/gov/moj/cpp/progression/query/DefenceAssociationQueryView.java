package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.fromString;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class DefenceAssociationQueryView {

    public static final String EMPTY_VALUE = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceAssociationQueryView.class);
    private static final String ID = "defendantId";
    private static final String ASSOCIATED = "Active Barrister/Solicitor of record";
    private static final String ASSOCIATION = "association";
    private static final String ORGANISATION_ID = "organisationId";
    private static final String ORGANISATION_NAME = "organisationName";
    private static final String STATUS = "status";
    private static final String ADDRESS = "address";
    private static final String ADDRESS_LINE_1 = "address1";
    private static final String ADDRESS_LINE_4 = "address4";
    private static final String ADDRESS_POSTCODE = "addressPostcode";
    private static final String EMAIL = "email";
    private static final String START_DATE = "startDate";
    private static final String REPRESENTATION_TYPE = "representationType";

    @Inject
    private DefenceAssociationRepository defenceAssociationRepository;

    @Handles("progression.query.associated-organisation")
    public JsonEnvelope getAssociatedOrganisation(final JsonEnvelope envelope) {
        final UUID defendantId = fromString(envelope.payloadAsJsonObject().getString(ID));
        final DefenceAssociationDefendant defenceAssociationDefendant;
        try {
            defenceAssociationDefendant = defenceAssociationRepository.findByDefendantId(defendantId);
        } catch (final NoResultException nre) {
            LOGGER.debug("No Association exist", nre);
            return emptyAssociation(envelope);
        }
        final DefenceAssociation defenceAssociation = extractCurrentDefenceAssociation(defenceAssociationDefendant);
        if (defenceAssociation == null) {
            return emptyAssociation(envelope);
        }
        return formResponseWithAssociationDetails(envelope, defenceAssociation);
    }

    private boolean isDefenceAssociationEmpty(final DefenceAssociationDefendant defenceAssociationDefendant) {
        return defenceAssociationDefendant == null ||
                defenceAssociationDefendant.getDefenceAssociations() == null ||
                defenceAssociationDefendant.getDefenceAssociations().isEmpty();
    }

    private JsonEnvelope formResponseWithAssociationDetails(final JsonEnvelope envelope, final DefenceAssociation defenceAssociation) {
        return JsonEnvelope.envelopeFrom(
                envelope.metadata(),
                formDefenceAssociationPayload(defenceAssociation));
    }

    private DefenceAssociation extractCurrentDefenceAssociation(final DefenceAssociationDefendant defenceAssociationDefendant) {
        if (isDefenceAssociationEmpty(defenceAssociationDefendant)) {
            return null;
        }
        final List<DefenceAssociation> defenceAssociations = defenceAssociationDefendant.getDefenceAssociations()
                .stream()
                .filter(d -> d.getEndDate() == null)
                .collect(Collectors.toList());

        if (defenceAssociations.size() > 1) {
            throw new IllegalStateException("Cannot have more than one Organisation Associated at any point in time");
        }

        return !defenceAssociations.isEmpty() ? defenceAssociations.get(0) : null;
    }

    private JsonObject formDefenceAssociationPayload(final DefenceAssociation defenceAssociation) {

        String organisationId = EMPTY_VALUE;
        String status = EMPTY_VALUE;
        String startDate = EMPTY_VALUE;
        String representationType = EMPTY_VALUE;
        if (defenceAssociation.getUserId() != null && defenceAssociation.getStartDate() != null) {
            organisationId = defenceAssociation.getOrgId().toString();
            startDate = ZonedDateTimes.toString(defenceAssociation.getStartDate());
            representationType = defenceAssociation.getRepresentationType();
            status = ASSOCIATED;
        }
        return formResponse(organisationId, status, startDate, representationType);
    }

    private JsonObject formResponse(final String organisationId,
                                    final String status,
                                    final String startDate,
                                    final String representationType) {

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
                        .add(START_DATE, startDate)
                        .add(REPRESENTATION_TYPE, representationType)
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
