package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CpsProsecutorUpdated;
import uk.gov.justice.core.courts.Marker;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseCpsProsecutorEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseCpsProsecutorRepository;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCpsProsecutorEventListenerTest {

    @InjectMocks
    private UpdateCpsProsecutorEventListener updateCpsProsecutorEventListener;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseCpsProsecutorRepository caseCpsProsecutorRepository;

    private final UUID id = randomUUID();

    @Captor
    private ArgumentCaptor<CaseCpsProsecutorEntity> argumentCaptor = forClass(CaseCpsProsecutorEntity.class);

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @BeforeEach
    public void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldUpdateCpsProsecutorWhenCaseCpsProsecutorAlreadyExists() {
        final CpsProsecutorUpdated cpsProsecutorUpdated = CpsProsecutorUpdated.cpsProsecutorUpdated()
                .withProsecutionCaseId(id)
                .withProsecutionAuthorityCode("TFL1")
                .withOldCpsProsecutor("TFL")
                .build();
        final CaseCpsProsecutorEntity entity = new CaseCpsProsecutorEntity();
        entity.setCaseId(id);
        entity.setOldCpsProsecutor("EL001");
        entity.setCpsProsecutor("TFL");
        when(caseCpsProsecutorRepository.findBy(cpsProsecutorUpdated.getProsecutionCaseId())).thenReturn(entity);
        final JsonObject eventPayload = objectToJsonObjectConverter.convert(cpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(eventPayload);
        updateCpsProsecutorEventListener.updateCpsProsecutor(envelope);
        verify(caseCpsProsecutorRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(notNullValue()));
        final CaseCpsProsecutorEntity updatedCpsProsecutorEntity = argumentCaptor.getValue();
        assertThat(updatedCpsProsecutorEntity.getCaseId(), is(id));
        assertThat(updatedCpsProsecutorEntity.getCpsProsecutor(), is("TFL1"));
        assertThat(updatedCpsProsecutorEntity.getOldCpsProsecutor(), is("TFL"));
    }

    @Test
    public void shouldUpdateCpsProsecutorWhenCaseCpsProsecutorDoesNotExists() {
        final CpsProsecutorUpdated cpsProsecutorUpdated = CpsProsecutorUpdated.cpsProsecutorUpdated()
                .withProsecutionCaseId(id)
                .withProsecutionAuthorityCode("TFL1")
                .withOldCpsProsecutor("TFL")
                .build();
        final JsonObject eventPayload = objectToJsonObjectConverter.convert(cpsProsecutorUpdated);
        when(envelope.payloadAsJsonObject()).thenReturn(eventPayload);
        updateCpsProsecutorEventListener.updateCpsProsecutor(envelope);
        verify(caseCpsProsecutorRepository).save(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue(), is(notNullValue()));
        final CaseCpsProsecutorEntity updatedCpsProsecutorEntity = argumentCaptor.getValue();
        assertThat(updatedCpsProsecutorEntity.getCaseId(), is(id));
        assertThat(updatedCpsProsecutorEntity.getCpsProsecutor(), is("TFL1"));
    }

    private ProsecutionCase getProsecutionCase() {
        return ProsecutionCase.prosecutionCase()
                .withId(id)
                .withAppealProceedingsPending(true)
                .withCaseStatus(null)
                .withBreachProceedingsPending(true)
                .withCaseMarkers(singletonList(Marker.marker().build()))
                .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                        .withCaseURN("123")
                        .withProsecutionAuthorityCode("TFL")
                        .withProsecutionAuthorityCode("Code")
                        .withProsecutionAuthorityName("Name")
                        .build())
                .withDefendants(emptyList())
                .build();
    }
}