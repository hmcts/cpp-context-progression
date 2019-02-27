package uk.gov.moj.cpp.progression.nows;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.domain.notification.Subscriptions;

import javax.json.JsonObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class SubscriptionClientTest {

    @InjectMocks
    private SubscriptionClient target;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Mock
    private Requester requester;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Test
    public void testGetAll() {
        final JsonEnvelope context = mock(JsonEnvelope.class);
        final Metadata metaData = mock(Metadata.class);
        when(context.metadata()).thenReturn(metaData);
        final JsonObject metaDataJson = mock(JsonObject.class);
        when(metaData.asJsonObject()).thenReturn(metaDataJson);
        when(metaData.streamId()).thenReturn(Optional.empty());
        when(metaData.id()).thenReturn(UUID.randomUUID());
        final LocalDate localDate = LocalDate.now();
        final UUID nowsTypeId = UUID.randomUUID();
        final JsonEnvelope jsonResultEnvelope = mock(JsonEnvelope.class);
        final JsonObject jsonResponsePayload = mock(JsonObject.class);
        when(jsonResultEnvelope.payloadAsJsonObject()).thenReturn(jsonResponsePayload);
        final Subscriptions expectedResult = new Subscriptions();
        when(jsonObjectToObjectConverter.convert(jsonResponsePayload, Subscriptions.class)).thenReturn(expectedResult);
        when(this.requester.request(any(JsonEnvelope.class))).thenReturn(jsonResultEnvelope);
        final Subscriptions result = target.getAll(context, nowsTypeId, localDate);
        verify(requester, times(1)).request(envelopeCaptor.capture());
        JsonObject requestPayload = (JsonObject) envelopeCaptor.getValue().payload();
        assertThat(requestPayload.getString(SubscriptionClient.AS_OF_DATE_QUERY_PARAMETER),
                is(LocalDate.now().format(DateTimeFormatter.ofPattern(SubscriptionClient.QUERY_DATE_FORMAT))));
        assertThat(requestPayload.getString(SubscriptionClient.NOWS_TYPE_ID_QUERY_PARAMETER),
                is(nowsTypeId.toString()));
        assertThat(result, is(expectedResult));
    }
}
