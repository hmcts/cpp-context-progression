package uk.gov.moj.cpp.progression.query;


import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static uk.gov.moj.cpp.progression.domain.constant.FeeStatus.*;
import static uk.gov.moj.cpp.progression.domain.constant.FeeType.*;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.constant.FeeStatus;
import uk.gov.moj.cpp.progression.domain.constant.FeeType;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CivilFeesQueryViewTest {

    @Mock
    private CivilFeeRepository civilFeeRepository;

    @InjectMocks
    private CivilFeesQueryView civilFeesQueryView;

    @Test
    void shouldGetCivilFees() {

        UUID uuidOne = UUID.randomUUID();
        UUID uuidTwo = UUID.randomUUID();

        final CivilFeeEntity civilFeeEntityOne = createCivilFeeEntity(uuidOne, INITIAL, OUTSTANDING, "payRef01");
        final CivilFeeEntity civilFeeEntityTwo = createCivilFeeEntity(uuidOne, CONTESTED, NOT_APPLICABLE, null);

        List<CivilFeeEntity> civilFeeEntities = List.of(civilFeeEntityOne, civilFeeEntityTwo);

        when(civilFeeRepository.findByFeeIds(List.of(uuidOne, uuidTwo))).thenReturn(civilFeeEntities);
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add("feeIds", uuidOne + "," + uuidTwo)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.civil-fee-details").build(),
                jsonObject);

        JsonEnvelope response = civilFeesQueryView.getCivilFees(jsonEnvelope);
        assertEquals(2, response.payloadAsJsonObject().getJsonArray("civilFees").size());
        verify(civilFeeRepository, times(1)).findByFeeIds(List.of(uuidOne, uuidTwo));
    }

    @Test
    void shouldHandleNonExistingEntity() {
        UUID uuidOne = UUID.randomUUID();
        UUID uuidTwo = UUID.randomUUID();

        when(civilFeeRepository.findByFeeIds(List.of(uuidOne, uuidTwo))).thenReturn(Collections.emptyList());
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add("feeIds", uuidOne + "," + uuidTwo)
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.civil-fee-details").build(),
                jsonObject);

        JsonEnvelope response = civilFeesQueryView.getCivilFees(jsonEnvelope);
        assertEquals(0, response.payloadAsJsonObject().getJsonArray("civilFees").size());
        verify(civilFeeRepository, times(1)).findByFeeIds(List.of(uuidOne, uuidTwo));
    }

    @Test
    void shouldHandleGetCivilFeesForOneId() {
        UUID uuidOne = UUID.randomUUID();

        final CivilFeeEntity civilFeeEntityOne = createCivilFeeEntity(uuidOne, INITIAL, OUTSTANDING, "payRef01");

        List<CivilFeeEntity> civilFeeEntities = List.of(civilFeeEntityOne);

        when(civilFeeRepository.findByFeeIds(List.of(uuidOne))).thenReturn(civilFeeEntities);
        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add("feeIds", String.valueOf(uuidOne))
                .build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.civil-fee-details").build(),
                jsonObject);

        JsonEnvelope response = civilFeesQueryView.getCivilFees(jsonEnvelope);
        assertEquals(1, response.payloadAsJsonObject().getJsonArray("civilFees").size());
        verify(civilFeeRepository, times(1)).findByFeeIds(List.of(uuidOne));
    }

    private CivilFeeEntity createCivilFeeEntity(UUID uuid, FeeType feeType, FeeStatus feeStatus, String paymentReference) {
        CivilFeeEntity civilFeeEntity = new CivilFeeEntity();
        civilFeeEntity.setFeeId(uuid);
        civilFeeEntity.setFeeType(feeType);
        civilFeeEntity.setFeeStatus(feeStatus);
        civilFeeEntity.setPaymentReference(paymentReference);
        return civilFeeEntity;
    }
}