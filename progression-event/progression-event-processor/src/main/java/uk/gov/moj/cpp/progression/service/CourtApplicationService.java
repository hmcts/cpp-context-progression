package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.isNull;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.ProsecutingAuthority.prosecutingAuthority;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.core.courts.Address;
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



    @SuppressWarnings("pmd:NullAssignment")
    public ProsecutingAuthority getProsecutingAuthority(UUID prosecutionAuthorityId, final JsonEnvelope jsonEnvelope) {

        final ProsecutingAuthority.Builder prosecutingAuthorityBuilder = prosecutingAuthority().withProsecutionAuthorityId(prosecutionAuthorityId);

        final Optional<JsonObject> optionalProsecutorJson = referenceDataService.getProsecutor(jsonEnvelope, prosecutionAuthorityId, requester);
        if (optionalProsecutorJson.isPresent()) {
            final JsonObject jsonObject = optionalProsecutorJson.get();
            prosecutingAuthorityBuilder.withName(jsonObject.getString("fullName"))
                    .withWelshName(jsonObject.getString("nameWelsh", null))
                    .withAddress(isNull(jsonObject.getJsonObject("address")) ? null : jsonObjectToObjectConverter.convert(jsonObject.getJsonObject("address"), Address.class));

            if (jsonObject.containsKey(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY)) {
                prosecutingAuthorityBuilder.withContact(contactNumber()
                        .withPrimaryEmail(jsonObject.getString(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY))
                        .build());
            }
        }
        return prosecutingAuthorityBuilder.build();
    }

}
