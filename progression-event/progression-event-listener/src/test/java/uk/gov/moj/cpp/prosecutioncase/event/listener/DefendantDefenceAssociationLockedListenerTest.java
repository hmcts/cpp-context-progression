package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.event.listener.DefendantDefenceAssociationLockedListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantDefenceAssociationLockedListenerTest {

    private static final UUID defendantId = randomUUID();
    private static final UUID prosecutionCaseId = randomUUID();
    private static final boolean lockedByRepOrder = true;

    private static final String DEFENDANT_ID = "defendantId";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String LOCKED_BY_REP_ORDER = "lockedByRepOrder";

    @Mock
    private ProsecutionCaseRepository repository;

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> argumentCaptor;

    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private ProsecutionCaseEntity prosecutionCaseEntity;




    @InjectMocks
    private DefendantDefenceAssociationLockedListener eventListener;

    @Test
    public void handleDefendantAssociationLock () {
        final JsonEnvelope jsonEnvelope = JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withUserId(randomUUID().toString()).withId(randomUUID()).withName("progression.event.defendant-defence-association-locked").build(),
                createPayloadForDefendantDefenceAssociationLocked());
        final ProsecutionCase prosCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(getDefendants(defendantId, prosecutionCaseId, lockedByRepOrder))
                .build();
        final JsonObject jsonObject = Json.createObjectBuilder()
                .add("payload", Json.createObjectBuilder()
                        .add("defendants", Json.createArrayBuilder().add(Json.createObjectBuilder()
                                .add("id", defendantId.toString()).build())
                                .build())
                        .build()).build();
        when(repository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);
        when(prosecutionCaseEntity.getPayload()).thenReturn(jsonObject.toString());
        when(jsonObjectConverter.convert(jsonObject, ProsecutionCase.class)).thenReturn(prosCase);
        when(objectToJsonObjectConverter.convert(prosCase)).thenReturn(jsonObject);

        eventListener.processDefendantAssociationLock(jsonEnvelope);

        verify(repository).save(argumentCaptor.capture());
        final ProsecutionCase prosecutionCase = this.jsonObjectConverter.convert
                (jsonFromString(argumentCaptor.getValue().getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCase.getDefendants().get(0).getAssociationLockedByRepOrder(), equalTo(lockedByRepOrder));



    }

    private List<Defendant> getDefendants(final UUID defendantId, final UUID prosecutionCaseId, final boolean lockByRepOrder) {
        List<Defendant> defendantList = new ArrayList<>();
        defendantList.add(Defendant.defendant().
                withId(defendantId)
                .withProsecutionCaseId(prosecutionCaseId)
                .withAssociationLockedByRepOrder(lockByRepOrder)
                .withLegalAidStatus("Granted")
                .build());
        return defendantList;
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {

        JsonObject object;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr))) {
            object = jsonReader.readObject();
        }

        return object;
    }



    private static JsonObject createPayloadForDefendantDefenceAssociationLocked() {
        return Json.createObjectBuilder()
                .add(DEFENDANT_ID, defendantId.toString())
                .add(PROSECUTION_CASE_ID, prosecutionCaseId.toString())
                .add(LOCKED_BY_REP_ORDER, lockedByRepOrder)
                .build();

    }


}
