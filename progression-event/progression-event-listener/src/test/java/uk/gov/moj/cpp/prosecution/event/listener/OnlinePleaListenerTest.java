package uk.gov.moj.cpp.prosecution.event.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.events.OnlinePleaPcqVisitedRecorded;
import uk.gov.moj.cpp.progression.events.OnlinePleaRecorded;
import uk.gov.moj.cpp.progression.plea.json.schemas.Offence;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnline;
import uk.gov.moj.cpp.progression.plea.json.schemas.PleadOnlinePcqVisited;
import uk.gov.moj.cpp.prosecutioncase.event.listener.OnlinePleaListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@RunWith(MockitoJUnitRunner.class)
public class OnlinePleaListenerTest {

    @Captor
    private ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseEntityArgumentCaptor;
    @Captor
    private ArgumentCaptor<OnlinePlea> onlinePleaArgumentCaptor;

    @InjectMocks
    private OnlinePleaListener onlinePleaListener;

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @Mock
    private OnlinePleaRepository onlinePleaRepository;

    @Mock
    private Envelope<OnlinePleaRecorded> onlinePleaRecordedEnvelope;

    @Mock
    private Envelope<OnlinePleaPcqVisitedRecorded> onlinePleaPcqVisitedRecordedEnvelope;

    @Mock
    private Metadata metadata;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Before
    public void init() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreateOnlinePleaAndUpdateOffence() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        when(onlinePleaRecordedEnvelope.metadata()).thenReturn(metadata);
        when(metadata.createdAt()).thenReturn(Optional.of(now()));
        when(onlinePleaRecordedEnvelope.payload()).thenReturn(OnlinePleaRecorded.onlinePleaRecorded()
                .withPleadOnline(PleadOnline.pleadOnline()
                        .withCaseId(caseId)
                        .withDefendantId(defendantId)
                        .withOffences(asList(Offence.offence().withId(offenceId.toString()).build()))
                        .build())
                .withCaseId(caseId).build());
        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(caseId)
                .withDefendants(createDefendants(defendantId, offenceId, caseId))
                .build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);

        onlinePleaListener.onlinePleaRecorded(onlinePleaRecordedEnvelope);

        verify(prosecutionCaseRepository).save(prosecutionCaseEntityArgumentCaptor.capture());
        verify(onlinePleaRepository).save(onlinePleaArgumentCaptor.capture());


        final OnlinePlea onlinePleaArgumentCaptorValue = onlinePleaArgumentCaptor.getValue();
        assertThat(onlinePleaArgumentCaptorValue.getCaseId(), is(caseId));

        final ProsecutionCaseEntity prosecutionCaseEntityArgumentCaptorValue = prosecutionCaseEntityArgumentCaptor.getValue();
        final ProsecutionCase prosecutionCasePersisted = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntityArgumentCaptorValue.getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCaseEntityArgumentCaptorValue.getCaseId(), is(caseId));
        assertThat(prosecutionCasePersisted.getDefendants().get(0).getOffences().get(0).getOnlinePleaReceived(), is(true));
    }

    @Test
    public void shouldUpdateDefendantPcqIdWhenOnlinePleaPcqVisitedRecorded() {
        final String caseUrn = "CASEURN";
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID pcqId = randomUUID();

        when(onlinePleaPcqVisitedRecordedEnvelope.metadata()).thenReturn(metadata);
        when(metadata.createdAt()).thenReturn(Optional.of(now()));
        when(onlinePleaPcqVisitedRecordedEnvelope.payload()).thenReturn(getOnlinePleaPcqVisitedRecorded(caseUrn, caseId, defendantId, pcqId));

        final ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(caseId);
        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase().withId(caseId)
                .withDefendants(createDefendants(defendantId, randomUUID(), caseId)).build();
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());
        when(prosecutionCaseRepository.findByCaseId(caseId)).thenReturn(prosecutionCaseEntity);

        onlinePleaListener.onlinePleaPcqVisitedRecorded(onlinePleaPcqVisitedRecordedEnvelope);

        verify(prosecutionCaseRepository).save(prosecutionCaseEntityArgumentCaptor.capture());

        final ProsecutionCaseEntity prosecutionCaseEntityArgumentCaptorValue = prosecutionCaseEntityArgumentCaptor.getValue();
        assertThat(prosecutionCaseEntityArgumentCaptorValue.getCaseId(), is(caseId));
        final ProsecutionCase prosecutionCasePersisted = jsonObjectToObjectConverter.convert(jsonFromString(prosecutionCaseEntityArgumentCaptorValue.getPayload()), ProsecutionCase.class);
        assertThat(prosecutionCasePersisted.getDefendants().get(0).getPcqId(), is(pcqId.toString()));
    }

    private OnlinePleaPcqVisitedRecorded getOnlinePleaPcqVisitedRecorded(String caseUrn, UUID caseId, UUID defendantId, UUID pcqId) {
        return OnlinePleaPcqVisitedRecorded.onlinePleaPcqVisitedRecorded()
                .withPleadOnlinePcqVisited(PleadOnlinePcqVisited.pleadOnlinePcqVisited()
                        .withCaseId(caseId)
                        .withUrn(caseUrn)
                        .withDefendantId(defendantId)
                        .withPcqId(pcqId)
                        .build())
                .withCaseId(caseId).build();
    }

    private List<Defendant> createDefendants(final UUID matchedDefendantId, final UUID offenceId, final UUID caseId) {
        final List<Defendant> defendants = new ArrayList<>();
        defendants.add(Defendant.defendant()
                .withId(matchedDefendantId)
                .withProsecutionCaseId(caseId)
                .withOffences(asList(uk.gov.justice.core.courts.Offence.offence()
                        .withId(offenceId)
                        .withOnlinePleaReceived(false)
                        .build()))
                .build());
        return defendants;
    }

    private static JsonObject jsonFromString(String jsonObjectStr) {

        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }


}