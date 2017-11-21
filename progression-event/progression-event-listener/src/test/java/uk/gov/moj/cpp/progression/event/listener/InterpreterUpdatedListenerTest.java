package uk.gov.moj.cpp.progression.event.listener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.messaging.DefaultJsonEnvelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.progression.domain.event.defendant.Interpreter;
import uk.gov.moj.cpp.progression.domain.event.defendant.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.progression.persistence.entity.CaseProgressionDetail;
import uk.gov.moj.cpp.progression.persistence.entity.Defendant;
import uk.gov.moj.cpp.progression.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.progression.persistence.repository.CaseProgressionDetailRepository;

import javax.json.Json;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;

@RunWith(MockitoJUnitRunner.class)
public class InterpreterUpdatedListenerTest {

    private UUID caseId = randomUUID();
    private UUID defendantId = randomUUID();

    @Mock
    private CaseProgressionDetailRepository caseRepository;

    @Mock
    private CaseProgressionDetail caseDetail;

    @Mock
    private Defendant defendant;

    @Mock
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @InjectMocks
    private InterpreterUpdatedListener listener;

    @Before
    public void setUp() {
        when(caseRepository.findBy(caseId)).thenReturn(caseDetail);
        when(caseDetail.getDefendant(defendantId)).thenReturn(defendant);
    }

    @Test
    public void shouldUpdateInterpreter() {

        final String language = "French";

        final JsonEnvelope envelope = DefaultJsonEnvelope.envelope()
                .with(metadataWithRandomUUID("progression.events.interpreter-for-defendant-updated"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId").withPayloadOf(
                        Json.createObjectBuilder().add("needed", true)
                                .add("language", language).build(), "interpreter")
                .build();

        when(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), InterpreterUpdatedForDefendant.class)).thenReturn(
                new InterpreterUpdatedForDefendant(caseId, defendantId, new Interpreter(Boolean.TRUE, language)));

        listener.interpreterUpdated(envelope);

        final ArgumentCaptor<InterpreterDetail> captor =
                        ArgumentCaptor.forClass(InterpreterDetail.class);

        verify(defendant).setInterpreter(captor.capture());

        assertThat(captor.getValue().getLanguage(), is(language));
        assertThat(captor.getValue().getNeeded(), is(Boolean.TRUE));
    }



}
