package uk.gov.moj.cpp.progression.command;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelope;
import uk.gov.justice.services.messaging.spi.DefaultJsonEnvelopeProvider;

import java.time.format.DateTimeFormatter;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CorrectHearingDaysWithoutCourtCentreApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<DefaultEnvelope> envelopeCaptor;

    @InjectMocks
    private CorrectHearingDaysWithoutCourtCentreApi correctHearingDaysWithoutCourtCentreApi;

    private static final DateTimeFormatter ZONE_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final String CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE = "progression.correct-hearing-days-without-court-centre";
    private static final String PROGRESSION_COMMAND_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE = "progression.command.correct-hearing-days-without-court-centre";

    @Test
    public void shouldHandleCorrectHearingDaysWithoutCourtCentre() {
        final JsonEnvelope commandApiEnvelope = buildEnvelope();
        correctHearingDaysWithoutCourtCentreApi.handleCorrectHearingDaysWithoutCourtCentre(commandApiEnvelope);

        verify(sender, times(1)).send(envelopeCaptor.capture());

        final DefaultEnvelope commandEnvelope = envelopeCaptor.getValue();

        assertThat(commandEnvelope.metadata().name(), is(PROGRESSION_COMMAND_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE));
        assertThat(commandEnvelope.payload(), equalTo(commandApiEnvelope.payloadAsJsonObject()));
    }

    private JsonEnvelope buildEnvelope() {
        final JsonObject payload = createObjectBuilder()
                .add("hearingDays", createArrayBuilder().add(populateCorrectedHearingDays()))
                .add("id", randomUUID().toString()).build();

        final Metadata metadata = Envelope
                .metadataBuilder()
                .withName(CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE)
                .withId(randomUUID())
                .withUserId(randomUUID().toString())
                .build();

        return new DefaultJsonEnvelopeProvider().envelopeFrom(metadata, payload);
    }

    private JsonObjectBuilder populateCorrectedHearingDays() {
        return createObjectBuilder()
                .add("listedDurationMinutes", 20)
                .add("listingSequence", 0)
                .add("courtCentreId", randomUUID().toString())
                .add("courtRoomId", randomUUID().toString())
                .add("sittingDay", ZONE_DATETIME_FORMATTER.format(new UtcClock().now()));
    }

}
