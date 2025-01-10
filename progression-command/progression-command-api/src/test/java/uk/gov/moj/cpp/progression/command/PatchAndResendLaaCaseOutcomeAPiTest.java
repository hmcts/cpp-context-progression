package uk.gov.moj.cpp.progression.command;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.progression.command.CaseHearings.caseHearings;
import static uk.gov.moj.cpp.progression.command.PatchAndResendLaaOutcomeConcluded.patchAndResendLaaOutcomeConcluded;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatchAndResendLaaCaseOutcomeAPiTest {
    private static final ObjectToJsonObjectConverter objectToJsonConverter = new ObjectToJsonObjectConverter(new ObjectMapperProducer().objectMapper());

    @Mock
    private Sender sender;
    @Captor
    private ArgumentCaptor<Envelope<?>> envelopeCaptor;

    @InjectMocks
    private PatchAndResendLaaCaseOutcomeAPi patchAndResendLaaCaseOutcomeAPi;

    @Test
    void shouldHandleAPIRequestAndEmitHandlerCommands() {
        final CaseHearings caseHearing1 = caseHearings()
                .withCaseId(randomUUID())
                .withHearingId(randomUUID())
                .withResultDate(now())
                .build();
        final CaseHearings caseHearing2 = caseHearings()
                .withCaseId(randomUUID())
                .withHearingId(randomUUID())
                .withResultDate(now().plusDays(1))
                .build();
        final PatchAndResendLaaOutcomeConcluded payload = patchAndResendLaaOutcomeConcluded().withCaseHearings(asList(caseHearing1, caseHearing2)).build();

        final MetadataBuilder metadataBuilder = metadataFrom(metadataWithRandomUUID("progression.command.patch-and-resend-laa-outcome-concluded").withUserId(randomUUID().toString()).build());
        final JsonEnvelope envelope = envelopeFrom(metadataBuilder, objectToJsonConverter.convert(payload));

        patchAndResendLaaCaseOutcomeAPi.handle(envelope);

        verify(sender, times(2)).send(envelopeCaptor.capture());
        List<Envelope<?>> currentEvents = envelopeCaptor.getAllValues();
        assertThat(currentEvents, hasSize(2));
        assertThat(currentEvents.get(0).metadata().name(), is("progression.command.handler.patch-and-resend-laa-outcome-concluded"));
        assertThat(currentEvents.get(0).payload().toString(),
                allOf(containsString(caseHearing1.getCaseId().toString()), containsString(caseHearing1.getHearingId().toString())));
        assertThat(currentEvents.get(1).metadata().name(), is("progression.command.handler.patch-and-resend-laa-outcome-concluded"));
        assertThat(currentEvents.get(1).payload().toString(),
                allOf(containsString(caseHearing2.getCaseId().toString()), containsString(caseHearing2.getHearingId().toString())));
    }
}