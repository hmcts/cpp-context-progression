package uk.gov.moj.cpp.progression.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.core.courts.ListingNumberUpdated;
import uk.gov.justice.core.courts.OffenceListingNumbers;
import uk.gov.justice.core.courts.ProsecutionCaseListingNumberIncreased;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingListingNumberUpdatedEventProcessorTest {

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Mock
    private Sender sender;

    @InjectMocks
    private HearingListingNumberUpdatedEventProcessor hearingListingNumberUpdatedEventProcessor;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    private ProgressionService progressionService;

    @Test
    public void shouldCallProsecutionCaseCommand(){
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final ListingNumberUpdated listingNumberUpdated = ListingNumberUpdated.listingNumberUpdated()
                .withProsecutionCaseIds(Collections.singletonList(caseId))
                .withOffenceListingNumbers(Collections.singletonList(OffenceListingNumbers.offenceListingNumbers()
                        .withListingNumber(10)
                        .withOffenceId(offenceId)
                        .build()))
                .build();
        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.listing-number-updated")
                .withId(randomUUID())
                .build();
        final Envelope<ListingNumberUpdated> event = Envelope.envelopeFrom(eventEnvelopeMetadata, listingNumberUpdated);

        hearingListingNumberUpdatedEventProcessor.handleListingNumberUpdatedEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.update-listing-number-to-prosecution-case"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.prosecutionCaseId", equalTo(caseId.toString())),
                withJsonPath("$.offenceListingNumbers[0].listingNumber", equalTo(10)),
                withJsonPath("$.offenceListingNumbers[0].offenceId", equalTo(offenceId.toString()))
        )));
    }

    @Test
    public void shouldProcessProsecutionCaseListingNumberIncreased(){
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final ProsecutionCaseListingNumberIncreased prosecutionCaseListingNumberIncreased = ProsecutionCaseListingNumberIncreased.prosecutionCaseListingNumberIncreased()
                .withProsecutionCaseId(caseId)
                .withOffenceListingNumbers(Collections.singletonList(OffenceListingNumbers.offenceListingNumbers()
                        .withListingNumber(10)
                        .withOffenceId(offenceId)
                        .build()))
                .withHearingId(randomUUID())
                .build();
        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("progression.event.listing-number-updated")
                .withId(randomUUID())
                .build();
        final Envelope<ProsecutionCaseListingNumberIncreased> event = Envelope.envelopeFrom(eventEnvelopeMetadata, prosecutionCaseListingNumberIncreased);

        hearingListingNumberUpdatedEventProcessor.processProsecutionCaseListingNumberIncreased(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        assertThat(commandEvent.metadata().name(), is("progression.command.update-listing-number-to-hearing"));
        assertThat(commandEvent.payload().toString(), isJson(allOf(
                withJsonPath("$.prosecutionCaseId", equalTo(caseId.toString())),
                withJsonPath("$.offenceListingNumbers[0].listingNumber", equalTo(10)),
                withJsonPath("$.offenceListingNumbers[0].offenceId", equalTo(offenceId.toString()))
        )));
    }
}
