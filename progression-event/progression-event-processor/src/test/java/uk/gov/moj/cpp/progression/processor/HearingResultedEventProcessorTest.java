package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelope;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingResultedEventProcessorTest {

    @InjectMocks
    private HearingResultedEventProcessor eventProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultJsonEnvelope> senderJsonEnvelopeCaptor;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldProcessRetentionCalculated() {
        //Given
        final String hearingId = randomUUID().toString();
        final JsonObject caseRetentionLengthCalculated = createObjectBuilder()
                .add("hearingId", hearingId)
                .add("hearingType", "Trial")
                .add("retentionPolicy", createObjectBuilder()
                        .add("policyType", "2")
                        .add("period", "7Y0M0D")
                        .build())
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.events.case-retention-length-calculated"),
                caseRetentionLengthCalculated);

        //When
        eventProcessor.processRetentionCalculated(event);

        //Then
        verify(sender).send(senderJsonEnvelopeCaptor.capture());

        final DefaultJsonEnvelope commandEvent = senderJsonEnvelopeCaptor.getValue();
        assertThat(commandEvent.metadata().name(), is("public.events.progression.case-retention-length-calculated"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.hearingType", equalTo("Trial")),
                withJsonPath("$.retentionPolicy.policyType", equalTo("2")),
                withJsonPath("$.retentionPolicy.period", equalTo("7Y0M0D"))
        )));

    }
}