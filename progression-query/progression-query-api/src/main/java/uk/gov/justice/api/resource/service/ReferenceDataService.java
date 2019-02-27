package uk.gov.justice.api.resource.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.progression.courts.exract.ProsecutingAuthority;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1067", "squid:S1192"})
public class ReferenceDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class);
    public static final String REFERENCEDATA_GET_ORGANISATION = "referencedata.query.organisation-unit";
    public static final String REFERENCEDATA_GET_PROSECUTOR = "referencedata.query.prosecutor";
    private static final String ADDRESS_1 = "address1";
    private static final String ADDRESS_2 = "address2";
    private static final String ADDRESS_3 = "address3";
    private static final String ADDRESS_4 = "address4";
    private static final String ADDRESS_5 = "address5";
    private static final String POSTCODE = "postcode";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private Enveloper enveloper;


    public Optional<JsonObject> getOrganisationUnitById(final JsonEnvelope event, final UUID courtCentreId) {
        final JsonObject payload = createObjectBuilder().add("id", courtCentreId.toString()).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_ORGANISATION).apply(payload);
        final JsonEnvelope jsonEnvelop = requester.request(request);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'referencedata.query.organisation-unit' {} received with payload {}", courtCentreId, jsonEnvelop.toObfuscatedDebugString());
        }
        return Optional.of(jsonEnvelop.payloadAsJsonObject());
    }

    public Address getCourtCentreAddress(final JsonEnvelope jsonEnvelope, final UUID courtCentreId) {
        LOGGER.debug("Calling referenceDataService getOrganisationUnitById with courtCenterId: {} ", courtCentreId);
        final JsonObject courtCentreJson = this
                .getOrganisationUnitById(jsonEnvelope, courtCentreId)
                .orElseThrow(RuntimeException::new);
        LOGGER.debug("Found court center from reference data search : {}  ", courtCentreJson);

        return Address.address()
                .withAddress1(courtCentreJson.getString(ADDRESS_1))
                .withAddress2(courtCentreJson.getString(ADDRESS_2, null))
                .withAddress3(courtCentreJson.getString(ADDRESS_3, null))
                .withAddress4(courtCentreJson.getString(ADDRESS_4, null))
                .withAddress5(courtCentreJson.getString(ADDRESS_5, null))
                .withPostcode(courtCentreJson.getString(POSTCODE, null))
                .build();

    }

    public ProsecutingAuthority getProsecutor(final JsonEnvelope event, final String prosecutorId) {
        final JsonObject prosecutorJson = getProsecutorById(event, prosecutorId).orElseThrow(RuntimeException::new);
        final JsonObject addressJson = prosecutorJson.getJsonObject("address");
        return ProsecutingAuthority.prosecutingAuthority().withName(prosecutorJson.getString("fullName"))
                .withAddress(
                        Address.address()
                                .withAddress1(addressJson.getString(ADDRESS_1, null))
                                .withAddress2(addressJson.getString(ADDRESS_2, null))
                                .withAddress3(addressJson.getString(ADDRESS_3, null))
                                .withAddress4(addressJson.getString(ADDRESS_4, null))
                                .withAddress5(addressJson.getString(ADDRESS_5, null))
                                .withPostcode(addressJson.getString(POSTCODE, null))
                                .build())
                .build();
    }

    private Optional<JsonObject> getProsecutorById(final JsonEnvelope event, final String prosecutorId) {
        final JsonObject payload = createObjectBuilder().add("id", prosecutorId).build();
        final JsonEnvelope request = enveloper.withMetadataFrom(event, REFERENCEDATA_GET_PROSECUTOR).apply(payload);
        final JsonEnvelope jsonEnvelop = requester.request(request);
        LOGGER.info("'referencedata.get.prosecutor' {} received with payload {}", prosecutorId, jsonEnvelop.payloadAsJsonObject());
        return Optional.ofNullable(jsonEnvelop.payloadAsJsonObject());
    }
}
