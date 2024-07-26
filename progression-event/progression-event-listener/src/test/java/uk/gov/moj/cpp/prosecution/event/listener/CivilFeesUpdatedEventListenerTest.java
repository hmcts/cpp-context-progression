package uk.gov.moj.cpp.prosecution.event.listener;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.domain.constant.FeeStatus;
import uk.gov.moj.cpp.progression.domain.constant.FeeType;
import uk.gov.moj.cpp.prosecutioncase.event.listener.CivilFeesUpdatedEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CivilFeeEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CivilFeeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CivilFeesUpdatedEventListenerTest {

    @Mock
    private CivilFeeRepository civilFeeRepository;

    @InjectMocks
    private CivilFeesUpdatedEventListener listener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void addCivilFees(){
        final UUID caseId = UUID.randomUUID();
        final UUID feeId = UUID.randomUUID();
        final String paymentRef = "TestRef001";

        List<CivilFees> feeList = new ArrayList<CivilFees>();
        CivilFees civilFees = new CivilFees(feeId, uk.gov.justice.core.courts.FeeStatus.OUTSTANDING, uk.gov.justice.core.courts.FeeType.INITIAL,paymentRef);
        feeList.add(civilFees);

        final CivilFeesUpdated event = CivilFeesUpdated.civilFeesUpdated()
                .withCaseId(caseId)
                .withCivilFees(feeList)
                .build();
        final CivilFeeEntity civilFeeEntity = new CivilFeeEntity(feeId, FeeType.INITIAL, FeeStatus.OUTSTANDING,paymentRef);

        when(civilFeeRepository.findBy(any())).thenReturn(civilFeeEntity);

        listener.processEvent(envelopeFrom(metadataWithRandomUUID("progression.event.civil-fees-updated"),
                objectToJsonObjectConverter.convert(event)));

        final ArgumentCaptor<CivilFeeEntity> argumentCaptor = ArgumentCaptor.forClass(CivilFeeEntity.class);
        verify(this.civilFeeRepository).save(argumentCaptor.capture());
        final CivilFeeEntity savedEntity = argumentCaptor.getValue();

        assertThat(savedEntity.getFeeId(), is(feeId));
        assertThat(savedEntity.getFeeType(), is(FeeType.INITIAL));
        assertThat(savedEntity.getFeeStatus(), is(FeeStatus.OUTSTANDING));
        assertTrue(savedEntity.getPaymentReference().equals(paymentRef));
    }

}
