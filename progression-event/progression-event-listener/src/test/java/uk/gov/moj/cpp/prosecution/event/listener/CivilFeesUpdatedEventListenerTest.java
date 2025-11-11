package uk.gov.moj.cpp.prosecution.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.FeeStatus.OUTSTANDING;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CivilFeesAdded;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.domain.constant.FeeStatus;
import uk.gov.moj.cpp.progression.domain.constant.FeeType;
import uk.gov.moj.cpp.prosecutioncase.event.listener.CivilFeesUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CivilFeesUpdatedEventListenerTest {

    @Mock
    private CivilFeeRepository civilFeeRepository;

    @InjectMocks
    private CivilFeesUpdatedEventListener listener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void updateCivilFees() {
        final UUID feeId = UUID.randomUUID();
        final String paymentRef = "TestRef001";

        final CivilFeesUpdated event = CivilFeesUpdated.civilFeesUpdated()
                .withFeeId(feeId)
                .withFeeStatus(OUTSTANDING)
                .withFeeType(FeeType.INITIAL.name())
                .withPaymentReference(paymentRef)
                .build();
        final CivilFeeEntity civilFeeEntity = new CivilFeeEntity(feeId, FeeType.INITIAL, FeeStatus.OUTSTANDING,paymentRef);

        when(civilFeeRepository.findBy(any())).thenReturn(civilFeeEntity);

        listener.processCivilFeesUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.civil-fees-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<CivilFeeEntity> argumentCaptor = ArgumentCaptor.forClass(CivilFeeEntity.class);
        verify(this.civilFeeRepository).save(argumentCaptor.capture());
        final CivilFeeEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getFeeId(), is(feeId));
        assertThat(savedEntity.getFeeType(), is(FeeType.INITIAL));
        assertThat(savedEntity.getFeeStatus(), is(FeeStatus.OUTSTANDING));
        assertEquals(paymentRef, savedEntity.getPaymentReference());
    }

    @Test
    void testProcessCivilFeesUpdatedEventWithNonExistentFee() {
        final UUID feeId = UUID.randomUUID();
        final String paymentRef = "TestRef001";

        final CivilFeesUpdated event = CivilFeesUpdated.civilFeesUpdated()
                .withFeeId(feeId)
                .withFeeStatus(OUTSTANDING)
                .withFeeType(FeeType.INITIAL.name())
                .withPaymentReference(paymentRef)
                .build();

        when(civilFeeRepository.findBy(any())).thenReturn(null);

        listener.processCivilFeesUpdated(envelopeFrom(metadataWithRandomUUID("progression.event.civil-fees-updated"),
                objectToJsonObjectConverter.convert(event)));

        verify(civilFeeRepository, never()).save(any());
    }

    @Test
    void addCivilFees(){
        final UUID feeId = UUID.randomUUID();
        final String paymentRef = "TestRef001";

        final CivilFeesAdded event = CivilFeesAdded.civilFeesAdded()
                .withFeeId(feeId)
                .withFeeStatus(OUTSTANDING)
                .withFeeType(FeeType.INITIAL.name())
                .withPaymentReference(paymentRef)
                .build();

        listener.processCivilFeeAdded(envelopeFrom(metadataWithRandomUUID("progression.event.civil-fees-added"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<CivilFeeEntity> argumentCaptor = ArgumentCaptor.forClass(CivilFeeEntity.class);
        verify(this.civilFeeRepository).save(argumentCaptor.capture());
        final CivilFeeEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getFeeId(), is(feeId));
        assertThat(savedEntity.getFeeType(), is(FeeType.INITIAL));
        assertThat(savedEntity.getFeeStatus(), is(FeeStatus.OUTSTANDING));
        assertEquals(paymentRef, savedEntity.getPaymentReference());
    }

}
