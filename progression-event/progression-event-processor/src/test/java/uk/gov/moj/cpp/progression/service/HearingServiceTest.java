package uk.gov.moj.cpp.progression.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.json.Json.createReader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingServiceTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope jsonEnvelope;

    @InjectMocks
    private HearingService hearingService;

    private static final UUID CASE_ID = randomUUID();
    private static final UUID APPLICATION_ID = randomUUID();

    private final String HEARING_EVENT_LOG = "hearing.get-hearing-event-log-document.json";
    private final String AAAG_HEARING_EVENT_LOG = "hearing.get-aaag-hearing-event-log-document.json";
    private static final String HEARING_EVENT_LOG_DOCUMENT = "hearing.get-hearing-event-log-for-documents";

    @Test
    public void shouldNotRetrieveHearingEventLogsByCaseIdWhenNoHearingLogs() {
        JsonEnvelope jsonEnvelope = buildJsonEnvelope();

        when(requester.request(any(JsonEnvelope.class), any(Class.class))).thenReturn(jsonEnvelope);
        JsonObject hearingEventLogs = hearingService.getHearingEventLogs(jsonEnvelope, CASE_ID, Optional.empty());
        assertThat(hearingEventLogs.get("hearings"), is(JsonValue.NULL));
    }

    @Test
    public void shouldRetrieveHearingEventLogsByCaseId() {
        JsonEnvelope jsonEnvelope = getUserEnvelope(HEARING_EVENT_LOG);
        when(requester.request(any(JsonEnvelope.class), any(Class.class))).thenReturn(jsonEnvelope);

       JsonObject hearingEventLogs = hearingService.getHearingEventLogs(jsonEnvelope, CASE_ID, Optional.empty());

        assertThat(hearingEventLogs.getJsonArray("hearings").size(), is(greaterThan(0)));
        assertThat(hearingEventLogs.getString("requestedTime"), is("15:35 on 11 Jul 2023"));
        assertThat(hearingEventLogs.getString("requestedUser"), is("Alex Thomas"));
        assertThat(hearingEventLogs.getJsonArray("hearings").getJsonObject(0).getJsonArray("caseIds").size(), is(greaterThan(0)));
        assertThat(hearingEventLogs.getJsonArray("hearings").getJsonObject(0).getJsonArray("caseUrns").size(), is(greaterThan(0)));
    }

    @Test
    public void shouldRetrieveHearingEventLogsByApplicationId() {
        JsonEnvelope jsonEnvelope = getUserEnvelope(AAAG_HEARING_EVENT_LOG);
        when(requester.request(any(JsonEnvelope.class), any(Class.class))).thenReturn(jsonEnvelope);

        JsonObject hearingEventLogs = hearingService.getHearingEventLogs(jsonEnvelope, CASE_ID, Optional.of(APPLICATION_ID.toString()));

        assertThat(hearingEventLogs.getJsonArray("hearings").size(), is(greaterThan(0)));
        assertThat(hearingEventLogs.getString("requestedTime"), is("15:35 on 11 Jul 2023"));
        assertThat(hearingEventLogs.getString("requestedUser"), is("Alex Thomas"));
        assertThat(hearingEventLogs.getJsonArray("hearings").getJsonObject(0).getJsonArray("applicationReferences").size(), is(greaterThan(0)));
        assertThat(hearingEventLogs.getJsonArray("hearings").getJsonObject(0).getJsonArray("applicants").size(), is(greaterThan(0)));
        assertThat(hearingEventLogs.getJsonArray("hearings").getJsonObject(0).getJsonArray("respondents").size(), is(greaterThan(0)));
    }

    private JsonEnvelope getUserEnvelope(String fileName) {
        return envelopeFrom(
                metadataBuilder().
                        withName(HEARING_EVENT_LOG_DOCUMENT).
                        withId(randomUUID()),
                createReader(getClass().getClassLoader().
                        getResourceAsStream(fileName)).
                        readObject()
        );
    }

    public static JsonEnvelope buildJsonEnvelope() {
        return JsonEnvelope.envelopeFrom(
                JsonEnvelope.metadataBuilder().withId(randomUUID()).withName(HEARING_EVENT_LOG_DOCUMENT).build(),
                createObjectBuilder().addNull("hearings").build());
    }
}