package uk.gov.moj.cpp.prosecutioncase.event.listener;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.FormCreated.formCreated;
import static uk.gov.justice.core.courts.FormDefendants.formDefendants;
import static uk.gov.justice.core.courts.FormDefendantsUpdated.formDefendantsUpdated;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.FormCreated;
import uk.gov.justice.core.courts.FormDefendantsUpdated;
import uk.gov.justice.core.courts.FormType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.prosecutioncase.persistence.entity.CaseDefendantOffence;
import uk.gov.moj.cpp.prosecutioncase.persistence.repository.CaseDefendantOffenceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class FormEventListenerTest {

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private CaseDefendantOffenceRepository caseDefendantOffenceRepository;

    @Captor
    private ArgumentCaptor<CaseDefendantOffence> caseDefendantOffenceArgumentCaptor;


    @InjectMocks
    private FormEventListener formEventListener;

    @BeforeEach
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCreateForm() {

        final UUID courtFormId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        final FormCreated event = formCreated()
                .withCourtFormId(courtFormId)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormId(randomUUID())
                .withFormData("Test Data")
                .withFormType(FormType.BCM)
                .withUserId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.form-created"),
                objectToJsonObjectConverter.convert(event));

        formEventListener.formCreated(jsonEnvelope);

        verify(this.caseDefendantOffenceRepository, times(1)).save(caseDefendantOffenceArgumentCaptor.capture());

        final CaseDefendantOffence caseDefendantOffence = caseDefendantOffenceArgumentCaptor.getValue();

        assertThat(caseDefendantOffence.getCaseId(), is(caseId));
        assertThat(caseDefendantOffence.getDefendantId(), is(defendantId));
        assertThat(caseDefendantOffence.getFormType(), is(FormType.BCM));

    }


    @Test
    public void shouldHandleFormDefendantsUpdated() {

        final UUID courtFormId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final FormDefendantsUpdated event = formDefendantsUpdated()
                .withCourtFormId(courtFormId)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormType(FormType.BCM)
                .withUserId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.form-defendants-updated"),
                objectToJsonObjectConverter.convert(event));

        formEventListener.formDefendantsUpdated(jsonEnvelope);

        verify(this.caseDefendantOffenceRepository, times(1)).save(caseDefendantOffenceArgumentCaptor.capture());

        final CaseDefendantOffence caseDefendantOffence = caseDefendantOffenceArgumentCaptor.getValue();

        assertThat(caseDefendantOffence.getCaseId(), is(caseId));
        assertThat(caseDefendantOffence.getDefendantId(), is(defendantId));

    }

    @Test
    public void shouldHandleFormDefendantsUpdatedWithoutOffences() {

        final UUID courtFormId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();

        final FormDefendantsUpdated event = formDefendantsUpdated()
                .withCourtFormId(courtFormId)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormType(FormType.BCM)
                .withUserId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.form-defendants-updated"),
                objectToJsonObjectConverter.convert(event));

        formEventListener.formDefendantsUpdated(jsonEnvelope);

        verify(this.caseDefendantOffenceRepository, times(1)).save(caseDefendantOffenceArgumentCaptor.capture());

        final CaseDefendantOffence caseDefendantOffence = caseDefendantOffenceArgumentCaptor.getValue();

        assertThat(caseDefendantOffence.getCaseId(), is(caseId));
        assertThat(caseDefendantOffence.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldUpdateForm() {

        final UUID courtFormId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        final FormCreated event = formCreated()
                .withCourtFormId(courtFormId)
                .withCaseId(caseId)
                .withFormDefendants(asList(formDefendants()
                        .withDefendantId(defendantId)
                        .build()))
                .withFormId(randomUUID())
                .withFormData("Test Data")
                .withFormType(FormType.BCM)
                .withUserId(randomUUID())
                .build();

        final JsonEnvelope jsonEnvelope = envelopeFrom(metadataWithRandomUUID("progression.event.form-created"),
                objectToJsonObjectConverter.convert(event));
        final List<CaseDefendantOffence> caseDefendantOffences = new ArrayList<>();
        caseDefendantOffences.add(new CaseDefendantOffence(randomUUID(), defendantId, caseId, courtFormId,FormType.BCM));
        when(caseDefendantOffenceRepository.findByCourtFormId(any())).thenReturn(caseDefendantOffences);

        formEventListener.formUpdated(jsonEnvelope);

        verify(this.caseDefendantOffenceRepository, times(1)).save(caseDefendantOffenceArgumentCaptor.capture());

        final CaseDefendantOffence caseDefendantOffence = caseDefendantOffenceArgumentCaptor.getValue();

        assertThat(caseDefendantOffence.getCaseId(), is(caseId));
        assertThat(caseDefendantOffence.getDefendantId(), is(defendantId));
        assertThat(caseDefendantOffence.getFormType(), is(FormType.BCM));

    }
}