package uk.gov.moj.cpp.prosecution.event.listener;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.ConvictionDateAdded;
import uk.gov.justice.core.courts.ConvictionDateRemoved;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.prosecutioncase.event.listener.ConvictionDateEventListener;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.ProsecutionCaseEntity;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.ProsecutionCaseRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConvictionDateEventListenerTest {

    @Mock
    private ProsecutionCaseRepository prosecutionCaseRepository;

    @InjectMocks
    private ConvictionDateEventListener convictionDateEventListener;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void addConvictionDate() throws Exception {

        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();
        final LocalDate convictionDate = LocalDate.now();

        final ConvictionDateAdded convictionDateAdded = ConvictionDateAdded.convictionDateAdded()
                .withCaseId(prosecutionCaseId)
                .withOffenceId(offenceId)
                .withConvictionDate(convictionDate)
                .build();

        final List<JudicialResult> judicialResults = Collections.singletonList(JudicialResult.judicialResult().withApprovedDate(LocalDate.now()).build());

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Collections.singletonList(Defendant.defendant()
                        .withOffences(Collections.singletonList(Offence.offence()
                                .withId(offenceId)
                                .withLaaApplnReference(LaaReference
                                        .laaReference()
                                        .withApplicationReference("ABC123")
                                        .withStatusCode("statusCode")
                                        .withStatusId(randomUUID())
                                        .withStatusDescription("description")
                                        .build()).withJudicialResults(judicialResults)
                                .build()))
                        .build()))
                .build();

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        convictionDateEventListener.addConvictionDate(envelopeFrom(metadataWithRandomUUID("progression.event.conviction-date-added"),
                objectToJsonObjectConverter.convert(convictionDateAdded)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);

        verify(this.prosecutionCaseRepository).save(prosecutionCaseArgumentCaptor.capture());

        final ProsecutionCase prosecutionCaseResponse = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseArgumentCaptor.getValue().getPayload()), ProsecutionCase.class);

        assertThat(prosecutionCaseArgumentCaptor.getValue().getCaseId(), is(prosecutionCaseId));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getConvictionDate(), is(convictionDate));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getApplicationReference(), is("ABC123"));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getStatusCode(), is("statusCode"));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getLaaApplnReference().getStatusDescription(), is("description"));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getJudicialResults().size(), is(1));

    }

    @Test
    public void removeConvictionDate() throws Exception {

        final UUID prosecutionCaseId = randomUUID();
        final UUID offenceId = randomUUID();

        final ConvictionDateRemoved convictionDateRemoved = ConvictionDateRemoved.convictionDateRemoved()
                .withCaseId(prosecutionCaseId)
                .withOffenceId(offenceId)
                .build();

        final ProsecutionCase prosecutionCase = ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .withDefendants(Arrays.asList(Defendant.defendant()
                        .withOffences(Arrays.asList(Offence.offence()
                                .withId(offenceId)
                                .withConvictionDate(LocalDate.now())
                                .build()))
                        .build()))
                .build();

        ProsecutionCaseEntity prosecutionCaseEntity = new ProsecutionCaseEntity();
        prosecutionCaseEntity.setCaseId(prosecutionCaseId);
        prosecutionCaseEntity.setPayload(objectToJsonObjectConverter.convert(prosecutionCase).toString());

        when(prosecutionCaseRepository.findByCaseId(prosecutionCaseId)).thenReturn(prosecutionCaseEntity);

        convictionDateEventListener.removeConvictionDate(envelopeFrom(metadataWithRandomUUID("progression.event.conviction-date-removed"),
                objectToJsonObjectConverter.convert(convictionDateRemoved)));

        final ArgumentCaptor<ProsecutionCaseEntity> prosecutionCaseArgumentCaptor = ArgumentCaptor.forClass(ProsecutionCaseEntity.class);

        verify(this.prosecutionCaseRepository).save(prosecutionCaseArgumentCaptor.capture());

        final ProsecutionCase prosecutionCaseResponse = jsonObjectToObjectConverter.convert(stringToJsonObjectConverter.convert(prosecutionCaseArgumentCaptor.getValue().getPayload()), ProsecutionCase.class);

        assertThat(prosecutionCaseArgumentCaptor.getValue().getCaseId(), is(prosecutionCaseId));
        assertThat(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
        Assert.assertNull(prosecutionCaseResponse.getDefendants().get(0).getOffences().get(0).getConvictionDate());

    }
}
