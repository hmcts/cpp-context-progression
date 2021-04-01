package uk.gov.moj.cpp.progression.processor.summons;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.justice.core.courts.summons.SummonsProsecutor.summonsProsecutor;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.progression.processor.summons.SummonsPayloadUtil.populateAddress;

import uk.gov.justice.core.courts.LjaDetails;
import uk.gov.justice.core.courts.summons.SummonsProsecutor;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.service.ReferenceDataService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class SummonsService {

    private static final JsonObject EMPTY_JSON_OBJECT = createObjectBuilder().build();

    private static final String LOCAL_JUSTICE_AREA = "localJusticeArea";
    private static final String NATIONAL_COURT_CODE = "nationalCourtCode";

    private static final String NAME = "name";

    @Inject
    @ServiceComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    private ReferenceDataService referenceDataService;

    public SummonsProsecutor getProsecutor(final JsonEnvelope jsonEnvelope, final UUID prosecutionAuthorityId) {
        final Optional<JsonObject> optionalProsecutor = referenceDataService.getProsecutor(jsonEnvelope, prosecutionAuthorityId, requester);
        final JsonObject prosecutor = optionalProsecutor.orElse(EMPTY_JSON_OBJECT);
        return summonsProsecutor()
                .withName(prosecutor.getString("fullName", EMPTY))
                .withAddress(prosecutor.containsKey("address") ? populateAddress(prosecutor.getJsonObject("address")) : populateAddress(EMPTY_JSON_OBJECT))
                .withEmailAddress(prosecutor.getString("contactEmailAddress", EMPTY))
                .build();
    }

    public Optional<LjaDetails> getLjaDetails(final JsonEnvelope jsonEnvelope, final String ljaCode) {

        if (isNotBlank(ljaCode)) {
            final JsonObject ljaDetails = referenceDataService.getEnforcementAreaByLjaCode(jsonEnvelope, ljaCode, requester);
            if (nonNull(ljaDetails)) {
                final JsonObject localJusticeArea = ljaDetails.getJsonObject(LOCAL_JUSTICE_AREA);
                final String localJusticeAreaNationalCourtCode = localJusticeArea.getString(NATIONAL_COURT_CODE, EMPTY);
                final String ljaName = localJusticeArea.getString(NAME, EMPTY);

                final Optional<JsonObject> optionalLocalJusticeAreaResponse = referenceDataService.getLocalJusticeArea(jsonEnvelope, localJusticeAreaNationalCourtCode, requester);
                final JsonObject localJusticeAreaResponsePayload = optionalLocalJusticeAreaResponse.orElseThrow(() -> new IllegalArgumentException("No Local Justice Area Details found for Local Justice Area Code"));
                if (localJusticeAreaResponsePayload.equals(JsonValue.NULL)) {
                    throw new IllegalArgumentException("No Local Justice Area Details found for Local Justice Area Code");
                }
                final String welshLjaName = localJusticeAreaResponsePayload.getJsonArray("localJusticeAreas").getJsonObject(0).getString("welshName", null);
                return Optional.of(LjaDetails.ljaDetails().withLjaCode(localJusticeAreaNationalCourtCode).withLjaName(ljaName).withWelshLjaName(welshLjaName).build());
            }
        }
        return Optional.empty();
    }

}
