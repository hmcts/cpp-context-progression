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
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;

import java.time.LocalDate;
import java.util.Arrays;

import javax.json.Json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CustodyTimeLimitProcessorTest {


    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> publicEnvelopeArgumentCaptor;

    @InjectMocks
    private CustodyTimeLimitProcessor custodyTimeLimitProcessor;

    @Test
    public void shouldProcessStopCustodyTimeLimitClock() {
        final String hearingId = randomUUID().toString();
        final String offence1Id = randomUUID().toString();
        final String offence2Id = randomUUID().toString();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.events.hearing.custody-time-limit-clock-stopped"),
                createObjectBuilder()
                        .add("hearingId", hearingId)
                        .add("offenceIds", Json.createArrayBuilder()
                                .add(offence1Id)
                                .add(offence2Id)
                                .build())
                        .build());

        custodyTimeLimitProcessor.processStopCustodyTimeLimitClock(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.stop-custody-time-limit-clock"));
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.offenceIds[0]", equalTo(offence1Id)),
                withJsonPath("$.offenceIds[1]", equalTo(offence2Id))
        )));


    }

    @Test
    public void shouldExtendCustodyTimeLimitResulted() {
        final String hearingId = randomUUID().toString();
        final String caseId = randomUUID().toString();
        final String offenceId = randomUUID().toString();
        final String extendedTimeLimit = LocalDate.now().toString();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.events.extend-custody-time-limit-resulted"),
                createObjectBuilder()
                        .add("hearingId", hearingId)
                        .add("caseId", caseId)
                        .add("offenceId", offenceId)
                        .add("extendedTimeLimit", extendedTimeLimit)
                        .build());

        custodyTimeLimitProcessor.processExtendCustodyTimeLimitResulted(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue().metadata().name(), is("progression.command.extend-custody-time-limit"));
        assertThat(envelopeArgumentCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.hearingId", equalTo(hearingId)),
                withJsonPath("$.caseId", equalTo(caseId)),
                withJsonPath("$.offenceId", equalTo(offenceId)),
                withJsonPath("$.extendedTimeLimit", equalTo(extendedTimeLimit))
        )));


    }

    @Test
    public void shouldProcessCustodyTimeLimitExtended() {
        final String hearing1Id = randomUUID().toString();
        final String hearing2Id = randomUUID().toString();
        final String offenceId = randomUUID().toString();
        final String extendedTimeLimit = LocalDate.now().toString();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("progression.events.custody-time-limit-extended"),
                createObjectBuilder()
                        .add("hearingIds", Json.createArrayBuilder()
                                .add(hearing1Id)
                                .add(hearing2Id)
                                .build()
                        )
                        .add("offenceId", offenceId)
                        .add("extendedTimeLimit", extendedTimeLimit)
                        .build());

        custodyTimeLimitProcessor.processCustodyTimeLimitExtended(event);

        verify(this.sender).send(this.publicEnvelopeArgumentCaptor.capture());

        assertThat(publicEnvelopeArgumentCaptor.getValue().metadata().name(), is("public.events.progression.custody-time-limit-extended"));
        assertThat(publicEnvelopeArgumentCaptor.getValue().payload().toString(), isJson(allOf(
                withJsonPath("$.hearingIds", equalTo(Arrays.asList(hearing1Id,hearing2Id))),
                withJsonPath("$.offenceId", equalTo(offenceId)),
                withJsonPath("$.extendedTimeLimit", equalTo(extendedTimeLimit))
        )));


    }
}

