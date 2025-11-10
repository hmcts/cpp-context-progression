package uk.gov.moj.cpp.progression.query.api;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.CivilFeesQueryView;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CivilFeesQueryApiTest {

    @Mock
    private CivilFeesQueryView civilFeesQueryView;

    @InjectMocks
    private CivilFeesQueryApi civilFeesQueryApi;

    @Test
    void shouldHandleGetCivilFees() {

        final List<String> feeIds = List.of("3034e172-99d3-4970-bc5e-fd95dd62c9d7, 3034e172-99d3-4970-bc5e-fd95dd62c9d6");
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder().withId(randomUUID())
                .withName("progression.query.civil-fee-details"), createObjectBuilder().build());

        final JsonEnvelope query = createEnvelope("progression.query.civil-fee-details", createObjectBuilder()
                .add("feeIds", feeIds.toString())
                .build());
        when(civilFeesQueryView.getCivilFees(query)).thenReturn(envelope);
        assertThat(civilFeesQueryApi.getCivilFees(query), is(envelope));

        verify(civilFeesQueryView, times(1)).getCivilFees(query);
    }

}