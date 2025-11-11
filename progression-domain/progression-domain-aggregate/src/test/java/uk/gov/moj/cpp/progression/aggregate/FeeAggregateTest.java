package uk.gov.moj.cpp.progression.aggregate;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CivilFees;
import uk.gov.justice.core.courts.CivilFeesAdded;
import uk.gov.justice.core.courts.CivilFeesUpdated;
import uk.gov.justice.core.courts.FeeStatus;
import uk.gov.justice.core.courts.FeeType;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeeAggregateTest {

    @Mock
    JsonEnvelope envelope;

    @Mock
    JsonObject jsonObj;

    @Spy
    ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private FeeAggregate feeAggregate;

    @BeforeEach
    public void setUp() {
        this.feeAggregate = new FeeAggregate();
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldUpdateCivilFees() {

        final CivilFees civilFees = CivilFees.civilFees()
                .withFeeType(FeeType.INITIAL)
                .withFeeStatus(FeeStatus.OUTSTANDING)
                .build();
        final List<Object> eventStream = feeAggregate.updateCivilFees(civilFees).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CivilFeesUpdated.class)));
    }

    @Test
    void shouldAddCivilFee() {

        final CivilFees civilFees = CivilFees.civilFees()
                .withFeeType(FeeType.INITIAL)
                .withFeeStatus(FeeStatus.OUTSTANDING)
                .build();
        final List<Object> eventStream = feeAggregate.addCivilFee(civilFees).collect(toList());
        assertThat(eventStream.size(), is(1));
        final Object object = eventStream.get(0);
        assertThat(object.getClass(), is(equalTo(CivilFeesAdded.class)));
    }
}