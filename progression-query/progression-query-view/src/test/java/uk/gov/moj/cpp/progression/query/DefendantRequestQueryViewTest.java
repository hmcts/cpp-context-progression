package uk.gov.moj.cpp.progression.query;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantRequestEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantRequestRepository;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantRequestQueryViewTest {

    @InjectMocks
    private DefendantRequestQueryView defendantRequestQueryView;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private DefendantRequestRepository defendantRequestRepository;

    @Test
    public void shouldGetDefendantRequest(){
        final JsonEnvelope envelope = envelopeFrom(metadataWithDefaults().withName("progression.query.defendant-request"), createObjectBuilder().add("defendantId", randomUUID().toString()).build());
        when(defendantRequestRepository.findBy(any())).thenReturn(new DefendantRequestEntity());

        final JsonObject defendantRequestCreatedPayload = createObjectBuilder()
                        .add("defendantId",randomUUID().toString())
                        .add("prosecutionCaseId",randomUUID().toString())
                        .add("referralReasonId",randomUUID().toString())
                        .add("summonsType","SJP_REFERRAL")
                .build();
        when(stringToJsonObjectConverter.convert(any())).thenReturn(defendantRequestCreatedPayload);
        final JsonEnvelope result = defendantRequestQueryView.getDefendantRequest(envelope);

        final JsonObject resultAsJson = result.payloadAsJsonObject();
        assertThat(resultAsJson.getString("defendantId"), is(notNullValue()));
    }
}
