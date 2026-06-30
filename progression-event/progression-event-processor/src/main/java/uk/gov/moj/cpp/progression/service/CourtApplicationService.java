package uk.gov.moj.cpp.progression.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
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
    private static final String PROSECUTOR_CPS_FLAG_KEY = "cpsFlag";
    private static final String PROSECUTOR_CPS_CC_EMAIL_ADDRESS_KEY = "cpsCcEmailAddress";
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

            final String emailAddress = resolveProsecutorEmailAddress(jsonObject);
            if (nonNull(emailAddress)) {
                prosecutingAuthorityBuilder.withContact(contactNumber()
                        .withPrimaryEmail(emailAddress)
                        .build());
            }
        }
        return prosecutingAuthorityBuilder.build();
    }

    /**
     * Resolves the email address used for prosecutor/informant appeal notifications.
     * For CPS prosecutors (cpsFlag == true) the CPS Crown Court email (cpsCcEmailAddress) is used when present;
     * otherwise it falls back to the standard contactEmailAddress.
     */
    private String resolveProsecutorEmailAddress(final JsonObject jsonObject) {
        final boolean isCps = jsonObject.containsKey(PROSECUTOR_CPS_FLAG_KEY)
                && jsonObject.getBoolean(PROSECUTOR_CPS_FLAG_KEY);

        if (isCps) {
            final String cpsCcEmailAddress = jsonObject.getString(PROSECUTOR_CPS_CC_EMAIL_ADDRESS_KEY, null);
            if (nonNull(cpsCcEmailAddress) && !cpsCcEmailAddress.isBlank()) {
                return cpsCcEmailAddress;
            }
        }

        final String contactEmailAddress = jsonObject.getString(PROSECUTOR_CONTACT_EMAIL_ADDRESS_KEY, null);
        return (nonNull(contactEmailAddress) && !contactEmailAddress.isBlank()) ? contactEmailAddress : null;
    }

}
