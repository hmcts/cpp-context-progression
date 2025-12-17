package uk.gov.moj.cpp.progression.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.moj.cpp.progression.command.helper.HandlerTestHelper.metadataFor;

import uk.gov.justice.listing.courts.CorrectHearingDaysWithoutCourtCentre;
import uk.gov.justice.progression.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.progression.command.helper.FileResourceObjectMapper;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CorrectHearingDaysWithoutCourtCentreHandlerTest {

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(
            HearingDaysWithoutCourtCentreCorrected.class);

    @InjectMocks
    private CorrectHearingDaysWithoutCourtCentreHandler correctHearingDaysWithoutCourtCentreHandler;

    private final FileResourceObjectMapper handlerTestHelper = new FileResourceObjectMapper();

    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @BeforeEach
    public void setUp() {
        when(eventSource.getStreamById(any())).thenReturn(eventStream);
    }

    @Test
    public void shouldHandleCorrectHearingDaysWithoutCourtCentre() throws EventStreamException, IOException {
        final Envelope<CorrectHearingDaysWithoutCourtCentre> envelope = createEnvelope();

        correctHearingDaysWithoutCourtCentreHandler.handleCorrectHearingDaysWithoutCourtCentre(envelope);

        final CorrectHearingDaysWithoutCourtCentre correctHearingDaysWithoutCourtCentre = envelope.payload();

        assertThat(eventStream, eventStreamAppendedWith(
            streamContaining(
                jsonEnvelope(
                    withMetadataEnvelopedFrom(envelope).withName("progression.events.hearing-days-without-court-centre-corrected"),
                    payloadIsJson(
                            allOf(withJsonPath("$.id", is(correctHearingDaysWithoutCourtCentre.getId().toString())),
                                    withJsonPath("$.hearingDays[0].courtCentreId", is(correctHearingDaysWithoutCourtCentre.getHearingDays().get(0).getCourtCentreId().toString())),
                                    withJsonPath("$.hearingDays[0].courtRoomId", is(envelope.payload().getHearingDays().get(0).getCourtRoomId().toString())),
                                    withJsonPath("$.hearingDays[0].listedDurationMinutes", is(envelope.payload().getHearingDays().get(0).getListedDurationMinutes())),
                                    withJsonPath("$.hearingDays[0].listingSequence", is(envelope.payload().getHearingDays().get(0).getListingSequence())),
                                    withJsonPath("$.hearingDays[0].sittingDay", is(ZONE_DATETIME_FORMATTER.format(envelope.payload().getHearingDays().get(0).getSittingDay())))
                            )
                    )
                )
            )
        ));
    }

    private Envelope<CorrectHearingDaysWithoutCourtCentre> createEnvelope() throws IOException {
        final CorrectHearingDaysWithoutCourtCentre correctHearingDaysWithoutCourtCentre =
                handlerTestHelper.convertFromFile("json/progression.court-hearing-days-without-court-centre.json", CorrectHearingDaysWithoutCourtCentre.class);

        final Envelope<CorrectHearingDaysWithoutCourtCentre> envelope =
                envelopeFrom(metadataFor("progression.command.correct-hearing-days-without-court-centre",
                        randomUUID()),
                        correctHearingDaysWithoutCourtCentre);

        return envelope;
    }
}
