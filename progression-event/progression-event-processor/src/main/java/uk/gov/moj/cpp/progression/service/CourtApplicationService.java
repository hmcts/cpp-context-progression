package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.ProsecutingAuthority;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtApplicationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationService.class);

    private static final String PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY = "contactEmailAddress";
    private static final String PROSECUTOR_OUCODE_KEY = "oucode";
    private static final String PROSECUTOR_MAJOR_CREDITOR_CODE_KEY = "majorCreditorCode";

    @Inject
    private RefDataService referenceDataService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;


    public CourtApplicationParty getCourtApplicationPartyByProsecutingAuthority(UUID prosecutionAuthorityId, final JsonEnvelope jsonEnvelope) {
        final CourtApplicationParty.Builder builder = CourtApplicationParty.courtApplicationParty();
            builder.withProsecutingAuthority(fetchProsecutingAuthorityInformation(prosecutionAuthorityId, jsonEnvelope));
        return builder.build();
    }


    @SuppressWarnings("pmd:NullAssignment")
    private ProsecutingAuthority fetchProsecutingAuthorityInformation(UUID prosecutionAuthorityId, final JsonEnvelope jsonEnvelope) {

        LOGGER.warn("******-appeals fetchProsecutingAuthorityInformation prosecutionAuthorityId= {}", prosecutionAuthorityId);
        final ProsecutingAuthority.Builder prosecutingAuthorityBuilder = prosecutingAuthority().withProsecutionAuthorityId(prosecutionAuthorityId);

        final Optional<JsonObject> optionalProsecutorJson = referenceDataService.getProsecutor(jsonEnvelope, prosecutionAuthorityId, requester);
        LOGGER.warn("****** fetched info from ref data: optionalProsecutorJson = {}", optionalProsecutorJson);
        if (optionalProsecutorJson.isPresent()) {
            LOGGER.warn("****** fetched info from ref data: optionalProsecutorJson.get = {}", optionalProsecutorJson.get());
            final JsonObject jsonObject = optionalProsecutorJson.get();
            prosecutingAuthorityBuilder.withName(jsonObject.getString("fullName"))
                    .withWelshName(jsonObject.getString("nameWelsh", null))
                    .withAddress(isNull(jsonObject.getJsonObject("address")) ? null : jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("address"), Address.class));

            if (jsonObject.containsKey(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY)) {
                prosecutingAuthorityBuilder.withContact(contactNumber()
                        .withPrimaryEmail(jsonObject.getString(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY))
                        .build());
            }

            if (jsonObject.containsKey(PROSECUTOR_OUCODE_KEY)) {
                prosecutingAuthorityBuilder.withProsecutionAuthorityOUCode(jsonObject.getString(PROSECUTOR_OUCODE_KEY));
            }

            if (jsonObject.containsKey(PROSECUTOR_MAJOR_CREDITOR_CODE_KEY)) {
                prosecutingAuthorityBuilder.withMajorCreditorCode(jsonObject.getString(PROSECUTOR_MAJOR_CREDITOR_CODE_KEY));
            }
        }
        return prosecutingAuthorityBuilder.build();
    }

}
