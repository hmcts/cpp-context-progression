package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.core.courts.CustodialEstablishment.custodialEstablishment;
import static uk.gov.justice.core.courts.PersonDefendant.personDefendant;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.CustodialEstablishment;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelope;

import java.util.UUID;


import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UpdateDefendantServiceTest {

    @Mock
    private Sender sender;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private UpdateDefendantService updateDefendantService;

    @Mock
    private JsonEnvelope event;

    @Captor
    private ArgumentCaptor<DefaultJsonEnvelope> jsonEnvelopeCaptor;

    @BeforeEach
    public void setup() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void shouldCallCommandToUpdateDefendantForProsecutionCase() {

        final UUID caseId = randomUUID();
        final Defendant defendant = Defendant.defendant()
                .withId(randomUUID())
                .withProsecutionCaseId(caseId)
                .withPersonDefendant(personDefendant().build())
                .build();

        final CustodialEstablishment custodialEstablishment = custodialEstablishment().withId(randomUUID()).withCustody("custody").withName("name").build();

        final Metadata metadata = metadataFor("progression.command.update-defendant-for-prosecution-case", randomUUID());

        updateDefendantService.updateDefendantCustodialEstablishment(metadata, defendant, custodialEstablishment);

        verify(sender).send(jsonEnvelopeCaptor.capture());
        assertThat(jsonEnvelopeCaptor.getValue().metadata().name(), is(metadata.name()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("prosecutionCaseId"), is(caseId.toString()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getString("id"), is(defendant.getId().toString()));
        assertThat(jsonEnvelopeCaptor.getValue().payloadAsJsonObject().getJsonObject("defendant").getString("id"), is(defendant.getId().toString()));
    }


    private Metadata metadataFor(final String commandName, final UUID commandId) {
        return metadataBuilder()
                .withName(commandName)
                .withId(commandId)
                .withUserId(randomUUID().toString())
                .build();
    }

}