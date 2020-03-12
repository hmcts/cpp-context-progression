package uk.gov.moj.cpp.progression.query.view.service;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.QUERY_API;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociation;
import uk.gov.moj.cpp.defence.association.persistence.entity.DefenceAssociationDefendant;
import uk.gov.moj.cpp.defence.association.persistence.repository.DefenceAssociationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefenceOrganisationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefenceOrganisationService.class.getName());
    static final String ORGANISATION_ID = "organisationId";
    static final String ORGANISATION_NAME = "organisationName";
    static final String ADDRESS_LINE_1 = "addressLine1";
    static final String ADDRESS_LINE_2 = "addressLine2";
    static final String ADDRESS_LINE_3 = "addressLine3";
    static final String ADDRESS_LINE_4 = "addressLine4";
    static final String ADDRESS_POSTCODE = "addressPostcode";
    static final String USERSGROUPS_GET_ORGANISATION_DETAILS = "usersgroups.get-organisation-details";

    @Inject
    @ServiceComponent(QUERY_API)
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private DefenceAssociationRepository defenceAssociationRepository;

    public Optional<Organisation> getAssociatedDefenceOrganisation(UUID defendantId) {

        final DefenceAssociationDefendant defenceAssociationDefendant;
        try {

            defenceAssociationDefendant = defenceAssociationRepository.findByDefendantId(defendantId);
            final UUID organisationId = extractCurrentDefenceAssociatedOrganisation(defenceAssociationDefendant);
            return getOrganisationDetails(organisationId);

        } catch (final NoResultException | IllegalStateException e) {
            LOGGER.error("Failed to get Defence Association for defendantId={}", defendantId, e);
        }

        return Optional.empty();
    }


    private UUID extractCurrentDefenceAssociatedOrganisation(final DefenceAssociationDefendant defenceAssociationDefendant) {
        if (defenceAssociationDefendant == null ||
                defenceAssociationDefendant.getDefenceAssociations() == null ||
                defenceAssociationDefendant.getDefenceAssociations().isEmpty()) {
            return null;
        }
        final List<DefenceAssociation> defenceAssociations = defenceAssociationDefendant.getDefenceAssociations()
                .stream()
                .filter(d -> d.getEndDate() == null)
                .collect(Collectors.toList());

        if (defenceAssociations.size() > 1) {
            throw new IllegalStateException("Cannot have more than one Organisation Associated at any point in time");
        }

        return !defenceAssociations.isEmpty() ? defenceAssociations.get(0).getOrgId() : null;
    }


    private Optional<Organisation> getOrganisationDetails(final UUID organisationId) {
        final JsonObject payload = createObjectBuilder().add(ORGANISATION_ID, organisationId.toString()).build();
        final JsonEnvelope requestEnvelope = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName(USERSGROUPS_GET_ORGANISATION_DETAILS),
                payload);
        final JsonEnvelope response = requester.requestAsAdmin(requestEnvelope);

        Organisation organisation = null;
        if (nonNull(response.payloadAsJsonObject())) {
            final JsonObject organisationDetailsForUserJsonObject = response.payloadAsJsonObject();

            organisation = Organisation.organisation()
                    .withName(organisationDetailsForUserJsonObject.getString(ORGANISATION_NAME))
                    .withAddress(Address.address()
                            .withAddress1(organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_1))
                            .withAddress2(organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_2))
                            .withAddress3(organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_3))
                            .withAddress4(organisationDetailsForUserJsonObject.getString(ADDRESS_LINE_4))
                            .withPostcode(organisationDetailsForUserJsonObject.getString(ADDRESS_POSTCODE))
                            .build())
                    .build();
        }

        return ofNullable(organisation);
    }
}
