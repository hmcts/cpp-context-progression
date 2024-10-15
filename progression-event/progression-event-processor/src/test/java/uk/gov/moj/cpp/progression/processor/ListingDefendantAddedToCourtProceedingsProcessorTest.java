package uk.gov.moj.cpp.progression.processor;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings.publicListingNewDefendantAddedForCourtProceedings;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;

import uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.progression.service.ProgressionService;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ListingDefendantAddedToCourtProceedingsProcessorTest {

    @Mock
    private ProgressionService progressionService;

    @InjectMocks
    private ListingDefendantAddedToCourtProceedingsProcessor processor;

    @Test
    public void shouldPrepareSummonsDataForNewlyAddedDefendant() {
        final UUID hearingId = randomUUID();
        PublicListingNewDefendantAddedForCourtProceedings eventPayload = publicListingNewDefendantAddedForCourtProceedings()
                .withHearingId(hearingId)
                .withCaseId(randomUUID())
                .withDefendantId(randomUUID())
                .build();

        final Metadata eventEnvelopeMetadata = metadataBuilder()
                .withName("public.listing.new-defendant-added-for-court-proceedings")
                .withId(randomUUID())
                .build();
        final Envelope<PublicListingNewDefendantAddedForCourtProceedings> eventEnvelope = envelopeFrom(eventEnvelopeMetadata, eventPayload);

        processor.process(eventEnvelope);

        verify(progressionService).prepareSummonsDataForAddedDefendant(eventEnvelope);
        verify(progressionService).populateHearingToProbationCaseworker(eq(eventEnvelopeMetadata), eq(hearingId));
    }

}