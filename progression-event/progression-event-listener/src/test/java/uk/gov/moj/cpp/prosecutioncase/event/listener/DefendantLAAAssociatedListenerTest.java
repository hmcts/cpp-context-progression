package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.event.listener.DefendantLAAAssociatedListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAAssociationEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.DefendantLAAKey;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.DefendantLAAAssociationRepository;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantLAAAssociatedListenerTest {

    private static final UUID defendantId = randomUUID();
    private static final String laaContractNumber = "LAA1234";
    private static final boolean isAssociatedByLAA = true;

    private static final String LAA_CONTRACT_NUMBER = "laaContractNumber";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String IS_ASSOCIATED_BY_LAA = "isAssociatedByLAA";

    @Mock
    private DefendantLAAAssociationRepository repository;

    @Captor
    private ArgumentCaptor<DefendantLAAAssociationEntity> argumentCaptor;


    @InjectMocks
    private DefendantLAAAssociatedListener eventListener;

    @Test
    public void shouldHandleDefendantLAAAssociationSaved() {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withUserId(randomUUID().toString()).withId(randomUUID()).withName("progression.event.defendant-laa-associated").build(),
                createPayloadForDefendantLAAAssociation());
        when(repository.findBy(new DefendantLAAKey(defendantId, laaContractNumber))).thenReturn(null);
        eventListener.defendantLAAAssociated(jsonEnvelope);
        verify(repository).save(argumentCaptor.capture());
        DefendantLAAAssociationEntity defendantLAAAssociationEntity = argumentCaptor.getValue();
        assertThat(defendantLAAAssociationEntity.getDefendantLAAKey().getDefendantId(), equalTo(defendantId));
        assertThat(defendantLAAAssociationEntity.getDefendantLAAKey().getLaaContractNumber(), equalTo(laaContractNumber));
        assertThat(defendantLAAAssociationEntity.isAssociatedByLAA(), equalTo(isAssociatedByLAA));

    }

    private static JsonObject createPayloadForDefendantLAAAssociation() {
        return JsonObjects.createObjectBuilder()
                .add(DEFENDANT_ID, defendantId.toString())
                .add(LAA_CONTRACT_NUMBER, laaContractNumber)
                .add(IS_ASSOCIATED_BY_LAA, isAssociatedByLAA)
                .build();

    }

}
