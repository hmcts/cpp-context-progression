package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class LegalStatusReferenceDataServiceTest {
    @Mock
    private Requester requester;

    @Spy
    Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private LegalStatusReferenceDataService legalStatusReferenceDataService;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    private static final UUID LEGAL_STATUS_ID = randomUUID();

    private static final LocalDate VALID_FROM = LocalDate.now();

    private static final LocalDate VALID_TO = LocalDate.now().plusDays(2);

    private static final String REFERENCEDATA_QUERY_LEGAL_STATUSES = "referencedata.query.legal-statuses";



    @Test
    public void shouldRequestForLegalStatusByStatusIdAndStatusCode() {
        //given
        final int seqNum = 10;
        final String statusCode = "AP";
        final String statusDescription = "Application Pending";
        final String defendantLevelStatus = "Granted";

        final JsonArrayBuilder legalStatusesArray = createArrayBuilder();
        final JsonObject legalStatusesJson =
                createObjectBuilder()
                        .add("id", LEGAL_STATUS_ID.toString())
                        .add("seqNum", seqNum)
                        .add("statusCode", statusCode)
                        .add("statusDescription", statusDescription)
                        .add("defendantLevelStatus", defendantLevelStatus)
                        .add("validFrom", VALID_FROM.toString())
                        .add("validTo", VALID_TO.toString())
                        .build();
        legalStatusesArray.add(legalStatusesJson);
        final JsonObject payload = createObjectBuilder()
                .add("legalStatuses", legalStatusesArray.build())
                .build();
        when(requester.request(any()))
                .thenReturn(JsonEnvelopeBuilder.envelope().withPayloadFrom(payload).with(metadataWithRandomUUID(REFERENCEDATA_QUERY_LEGAL_STATUSES)).build());


        //when
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(REFERENCEDATA_QUERY_LEGAL_STATUSES))
                .build();
        final Optional<JsonObject> result = legalStatusReferenceDataService.getLegalStatusByStatusIdAndStatusCode(
                envelope, statusCode);

        //then
        verify(requester).request(envelopeArgumentCaptor.capture());


        assertThat(result.get().getString("id"), is(LEGAL_STATUS_ID.toString()));
        assertThat(result.get().getString("statusDescription"), is(statusDescription));
        assertThat(result.get().getString("defendantLevelStatus"), is(defendantLevelStatus));
        assertThat(result.get().getString("validFrom"), is(VALID_FROM.toString()));
        assertThat(result.get().getString("validTo"), is(VALID_TO.toString()));

        verifyNoMoreInteractions(requester);
    }

}
