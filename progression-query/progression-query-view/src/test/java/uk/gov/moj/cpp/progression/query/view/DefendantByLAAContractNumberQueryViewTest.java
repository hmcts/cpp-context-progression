package uk.gov.moj.cpp.progression.query.view;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.query.DefendantByLAAContractNumberQueryView;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAAssociationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantLAAAssociationRepository;

import java.util.Collections;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantByLAAContractNumberQueryViewTest {
    @Mock
    private DefendantLAAAssociationRepository defendantLAAAssociationRepository;

    @InjectMocks
    private DefendantByLAAContractNumberQueryView defendantByLAAContractNumberQueryView;

    @Test
    public void shouldFindDefendantByLAAContractNumber() {
        final String laaContractNumber = "LAA1234";
        final String defendantId = randomUUID().toString();


        final JsonObject jsonObject = JsonObjects.createObjectBuilder()
                .add("laaContractNumber", laaContractNumber).build();

        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName("progression.query.defendants-by-laacontractnumber").build(),
                jsonObject);



        final DefendantLAAAssociationEntity defendantLAAAssociationEntity=new DefendantLAAAssociationEntity();
        defendantLAAAssociationEntity.setDefendantLAAKey(new DefendantLAAKey(fromString(defendantId), laaContractNumber));
        defendantLAAAssociationEntity.setAssociatedByLAA(true);

        when(defendantLAAAssociationRepository.findByLAAContractNUmber(laaContractNumber)).thenReturn(Collections.singletonList(defendantLAAAssociationEntity));

        final JsonEnvelope response = defendantByLAAContractNumberQueryView.getDefendantsByLAAContractNumber(jsonEnvelope);
        assertThat(response.payloadAsJsonObject().getJsonArray("defendants").size(), is(1));
        assertThat(response.payloadAsJsonObject().getJsonArray("defendants").getString(0), is(defendantId));


    }
}
