package uk.gov.moj.cpp.progression.query;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class CivilFeesQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(CivilFeesQueryView.class);
    private static final String FEE_ID = "feeId";
    private static final String FEE_IDS = "feeIds";
    private static final String FEE_TYPE = "feeType";
    private static final String FEE_STATUS = "feeStatus";
    private static final String PAYMENT_REFERENCE = "paymentReference";
    public static final String CIVIL_FEES = "civilFees";

    @Inject
    private CivilFeeRepository civilFeeRepository;

    @Handles("progression.query.civil-fee-details")
    public JsonEnvelope getCivilFees(final JsonEnvelope envelope) {
        final List<String> feeIds = List.of(JsonObjects.getString(envelope.payloadAsJsonObject(), FEE_IDS).get().split(","));

        List<UUID> uuidList = new ArrayList<>();
        feeIds.forEach(s -> uuidList.add(UUID.fromString(s.trim())));

        final List<CivilFees> civilFees = findCivilFees(uuidList);

        final List<JsonObject> civilFeesJsonList = civilFees.stream().map(this::createJsonObjectFromCivilFees).toList();

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        civilFeesJsonList.forEach(arrayBuilder::add);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(CIVIL_FEES, arrayBuilder.build());

        return envelopeFrom(
                envelope.metadata(),
                jsonObjectBuilder.build());
    }

    private JsonObject createJsonObjectFromCivilFees(CivilFees civilFee) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(FEE_ID, civilFee.getFeeId().toString())
                .add(FEE_TYPE, civilFee.getFeeType().toString())
                .add(FEE_STATUS, civilFee.getFeeStatus().toString());
        ofNullable(civilFee.getPaymentReference())
                .ifPresent(paymentReference -> jsonObjectBuilder.add(PAYMENT_REFERENCE,
                        paymentReference));
        return jsonObjectBuilder.build();
    }

    private List<CivilFees> findCivilFees(final List<UUID> feeIds) {

        final List<CivilFeeEntity> civilFeeEntities = civilFeeRepository.findByFeeIds(feeIds);
        if (isNull(civilFeeEntities)) {
            LOGGER.info("### No civilFee found with feeIds='{}'", feeIds);
            return emptyList();
        }

        return civilFeeEntities.stream().map(civilFeeEntity -> CivilFees.civilFees()
                .withFeeId(civilFeeEntity.getFeeId())
                .withFeeStatus(FeeStatus.valueOf(civilFeeEntity.getFeeStatus().name()))
                .withFeeType(FeeType.valueOf(civilFeeEntity.getFeeType().name()))
                .withPaymentReference(civilFeeEntity.getPaymentReference())
                .withFeeId(civilFeeEntity.getFeeId())
                .build()).toList();
    }

}
