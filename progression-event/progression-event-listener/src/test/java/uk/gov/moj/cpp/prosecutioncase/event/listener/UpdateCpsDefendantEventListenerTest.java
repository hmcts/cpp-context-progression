package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.CaseCpsDetailsUpdatedFromCourtDocument;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.events.CpsDefendantIdUpdated;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.mapping.SearchProsecutionCase;

import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateCpsDefendantEventListenerTest {

    @InjectMocks
    UpdateCpsDefendantEventListener updateCpsDefendantEventListener;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Envelope<CpsDefendantIdUpdated> cpsDefendantIdUpdatedEnvelope;

    @Mock
    private SearchProsecutionCase searchCase;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter(objectMapper);

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    @Captor
    ArgumentCaptor<ProsecutionCaseEntity> entityArgumentCaptor;

    private final UUID prosecutionCaseId = randomUUID();
    private final UUID defendantId1 = randomUUID();
    private final UUID defendantId2 = randomUUID();
    private final UUID cpsDefendantId1 = randomUUID();
    private final UUID cpsDefendantId2 = randomUUID();
    private final String cpsOrganisation = "AB12345";

    @Test
    public void shouldUpdateOnlyCpsDefendantId() {
        final UUID newCpsDefendantId = randomUUID();
        final CpsDefendantIdUpdated cpsDefendantIdUpdated = createCpsDefendantIdUpdated(defendantId1, newCpsDefendantId);

        when(cpsDefendantIdUpdatedEnvelope.payload()).thenReturn(cpsDefendantIdUpdated);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(createPayload().toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        updateCpsDefendantEventListener.cpsDefendantIdUpdated(cpsDefendantIdUpdatedEnvelope);

        verify(prosecutionCaseRepository, times(1)).save(entityArgumentCaptor.capture());

        ProsecutionCaseEntity actual = entityArgumentCaptor.getValue();
        ProsecutionCase actualCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(actual.getPayload()), ProsecutionCase.class);
        assertThat(actualCase.getDefendants().get(0).getId(), is(defendantId1));
        assertThat(actualCase.getDefendants().get(0).getCpsDefendantId(), is(newCpsDefendantId.toString()));
    }

    @Test
    public void shouldUpdateCpsDefendantId() {
        final String newCpsDefendantId = randomUUID().toString();
        JsonObject jsonObject = objectToJsonObjectConverter.convert(createCaseCpsDetailsUpdatedFromCourtDocument(defendantId1, newCpsDefendantId, cpsOrganisation));

        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(createPayload().toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        updateCpsDefendantEventListener.updateCpsDefendant(envelope);

        verify(prosecutionCaseRepository, times(1)).save(entityArgumentCaptor.capture());

        ProsecutionCaseEntity actual = entityArgumentCaptor.getValue();
        ProsecutionCase actualCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(actual.getPayload()), ProsecutionCase.class);
        assertThat(actualCase.getDefendants().get(0).getId(), is(defendantId1));
        assertThat(actualCase.getDefendants().get(0).getCpsDefendantId().toString(), is(newCpsDefendantId));
        assertThat(actualCase.getCpsOrganisation(), is(cpsOrganisation));
    }


    @Test
    public void shouldNotUpdateCpsDefendantIdIfDefendantIdNotMatched() {

        final CpsDefendantIdUpdated cpsDefendantIdUpdated = createCpsDefendantIdUpdated(randomUUID(), randomUUID());

        when(cpsDefendantIdUpdatedEnvelope.payload()).thenReturn(cpsDefendantIdUpdated);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(createPayload().toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        updateCpsDefendantEventListener.cpsDefendantIdUpdated(cpsDefendantIdUpdatedEnvelope);

        verify(prosecutionCaseRepository, times(1)).save(entityArgumentCaptor.capture());

        ProsecutionCaseEntity actual = entityArgumentCaptor.getValue();
        ProsecutionCase actualCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(actual.getPayload()), ProsecutionCase.class);
        assertThat(actualCase.getDefendants().get(0).getId(), is(defendantId1));
        assertThat(actualCase.getDefendants().get(0).getCpsDefendantId(), is(cpsDefendantId1.toString()));
        assertThat(actualCase.getDefendants().get(1).getId(), is(defendantId2));
        assertThat(actualCase.getDefendants().get(1).getCpsDefendantId(), is(cpsDefendantId2.toString()));
    }

    @Test
    public void shouldNotUpdateCpsDefendantIdIfNoMatchingDefendantId() {

        JsonObject jsonObject = objectToJsonObjectConverter.convert(createCaseCpsDetailsUpdatedFromCourtDocument(randomUUID(), randomUUID().toString(), cpsOrganisation));

        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(createPayload().toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        updateCpsDefendantEventListener.updateCpsDefendant(envelope);

        verify(prosecutionCaseRepository, times(1)).save(entityArgumentCaptor.capture());

        ProsecutionCaseEntity actual = entityArgumentCaptor.getValue();
        ProsecutionCase actualCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(actual.getPayload()), ProsecutionCase.class);
        assertThat(actualCase.getDefendants().get(0).getId(), is(defendantId1));
        assertThat(actualCase.getDefendants().get(0).getCpsDefendantId(), is(cpsDefendantId1.toString()));
        assertThat(actualCase.getDefendants().get(1).getId(), is(defendantId2));
        assertThat(actualCase.getDefendants().get(1).getCpsDefendantId(), is(cpsDefendantId2.toString()));
        assertThat(actualCase.getCpsOrganisation(), is(cpsOrganisation));
    }

    @Test
    public void shouldNotUpdateWhenCpsDrganisationDoesNotExist() {

        JsonObject jsonObject = objectToJsonObjectConverter.convert(createCaseCpsDetailsUpdatedFromCourtDocument(randomUUID(), randomUUID().toString(), null));

        when(envelope.payloadAsJsonObject()).thenReturn(jsonObject);

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setPayload(createPayload().toString());
        when(prosecutionCaseRepository.findByCaseId(any())).thenReturn(prosecutionCaseEntity);

        updateCpsDefendantEventListener.updateCpsDefendant(envelope);

        verify(prosecutionCaseRepository, times(1)).save(entityArgumentCaptor.capture());

        ProsecutionCaseEntity actual = entityArgumentCaptor.getValue();
        ProsecutionCase actualCase = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(actual.getPayload()), ProsecutionCase.class);
        assertThat(actualCase.getDefendants().get(0).getId(), is(defendantId1));
        assertThat(actualCase.getDefendants().get(0).getCpsDefendantId(), is(cpsDefendantId1.toString()));
        assertThat(actualCase.getDefendants().get(1).getId(), is(defendantId2));
        assertThat(actualCase.getDefendants().get(1).getCpsDefendantId(), is(cpsDefendantId2.toString()));
        assertThat(actualCase.getCpsOrganisation(), is(nullValue()));
    }

    private JsonObject createPayload() {

        final JsonArrayBuilder defendants = createArrayBuilder();
        final JsonObject defendant1 = createObjectBuilder()
                .add("id", defendantId1.toString())
                .add("cpsDefendantId", cpsDefendantId1.toString()).build();
        final JsonObject defendant2 = createObjectBuilder()
                .add("id", defendantId2.toString())
                .add("cpsDefendantId", cpsDefendantId2.toString()).build();
        defendants.add(defendant1);
        defendants.add(defendant2);

        return createObjectBuilder()
                .add("id", UUID.randomUUID().toString())
                .add("defendants", defendants)
                .build();
    }

    private CpsDefendantIdUpdated createCpsDefendantIdUpdated(final UUID defendantId, final UUID cpsDefendantId) {
        return CpsDefendantIdUpdated.cpsDefendantIdUpdated()
                .withCaseId(prosecutionCaseId)
                .withCpsDefendantId(cpsDefendantId.toString())
                .withDefendantId(defendantId)
                .build();
    }

    private CaseCpsDetailsUpdatedFromCourtDocument createCaseCpsDetailsUpdatedFromCourtDocument(final UUID defendantId, final String cpsDefendantId, final String cpsOrganisation) {
        return CaseCpsDetailsUpdatedFromCourtDocument.caseCpsDetailsUpdatedFromCourtDocument()
                .withCaseId(prosecutionCaseId)
                .withCpsDefendantId(cpsDefendantId)
                .withDefendantId(defendantId)
                .withCpsOrganisation(cpsOrganisation)
                .build();
    }
}
