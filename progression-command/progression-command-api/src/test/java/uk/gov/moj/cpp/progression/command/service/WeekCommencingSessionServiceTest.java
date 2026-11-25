package uk.gov.moj.cpp.progression.command.service;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.progression.listing.common.service.CourtScheduleService;

import java.util.List;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeekCommencingSessionServiceTest {

    private static final String WEEK_COMMENCING_SESSION_ID = "ccb4af70-e150-479a-954c-1c6ae98d0a34";
    private static final String ORDINARY_SESSION_ID = "5b0d3a2e-8f61-4c7b-9e14-2a6f0c9d7e31";

    @Mock
    private CourtScheduleService courtScheduleService;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @InjectMocks
    private WeekCommencingSessionService weekCommencingSessionService;

    @BeforeEach
    void setUp() {
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    void shouldReturnTheSessionIdWhenTheSessionIsWeekCommencing() {
        givenCourtSchedules(List.of(WEEK_COMMENCING_SESSION_ID), session(WEEK_COMMENCING_SESSION_ID).add("isWeekCommencing", true));

        assertThat(weekCommencingSessionService.getWeekCommencingSessionIds(List.of(WEEK_COMMENCING_SESSION_ID)), is(List.of(WEEK_COMMENCING_SESSION_ID)));
    }

    @Test
    void shouldReturnNoIdsWhenTheSessionIsNotWeekCommencing() {
        givenCourtSchedules(List.of(ORDINARY_SESSION_ID), session(ORDINARY_SESSION_ID).add("isWeekCommencing", false));

        assertThat(weekCommencingSessionService.getWeekCommencingSessionIds(List.of(ORDINARY_SESSION_ID)), is(emptyList()));
    }

    @Test
    void shouldReturnNoIdsWhenTheSessionHasNoWeekCommencingFlag() {
        givenCourtSchedules(List.of(ORDINARY_SESSION_ID), session(ORDINARY_SESSION_ID));

        assertThat(weekCommencingSessionService.getWeekCommencingSessionIds(List.of(ORDINARY_SESSION_ID)), is(emptyList()));
    }

    @Test
    void shouldReturnOnlyTheWeekCommencingSessionIdWhenAnOrdinarySessionIsAlsoBooked() {
        final List<String> ids = List.of(ORDINARY_SESSION_ID, WEEK_COMMENCING_SESSION_ID);
        givenCourtSchedules(ids, session(ORDINARY_SESSION_ID).add("isWeekCommencing", false), session(WEEK_COMMENCING_SESSION_ID).add("isWeekCommencing", true));

        assertThat(weekCommencingSessionService.getWeekCommencingSessionIds(ids), is(List.of(WEEK_COMMENCING_SESSION_ID)));
    }

    @Test
    void shouldReturnNoIdsWhenCourtSchedulerFindsNoSessionForTheIds() {
        givenCourtSchedules(List.of(WEEK_COMMENCING_SESSION_ID));

        assertThat(weekCommencingSessionService.getWeekCommencingSessionIds(List.of(WEEK_COMMENCING_SESSION_ID)), is(emptyList()));
    }

    @Test
    void shouldReturnNoIdsWhenTheResponseHasNoCourtSchedules() {
        when(courtScheduleService.getCourtSchedulesById(List.of(WEEK_COMMENCING_SESSION_ID)))
                .thenReturn(Response.ok(createObjectBuilder().build()).build());

        assertThat(weekCommencingSessionService.getWeekCommencingSessionIds(List.of(WEEK_COMMENCING_SESSION_ID)), is(emptyList()));
    }

    @Test
    void shouldNotCallCourtSchedulerWhenThereAreNoCourtScheduleIds() {
        assertThat(weekCommencingSessionService.getWeekCommencingSessionIds(emptyList()), is(emptyList()));

        verify(courtScheduleService, never()).getCourtSchedulesById(any());
    }

    @Test
    void shouldFailWhenCourtSchedulerReturnsAnError() {
        when(courtScheduleService.getCourtSchedulesById(List.of(WEEK_COMMENCING_SESSION_ID)))
                .thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("courtscheduler down").build());

        final ServerErrorException exception = assertThrows(ServerErrorException.class,
                () -> weekCommencingSessionService.getWeekCommencingSessionIds(List.of(WEEK_COMMENCING_SESSION_ID)));

        assertThat(exception.getResponse().getStatus(), is(500));
        assertThat(exception.getMessage(), is("Court schedules by id endpoint returned an error"));
    }

    private void givenCourtSchedules(final List<String> ids, final JsonObjectBuilder... sessions) {
        final JsonArrayBuilder courtSchedules = createArrayBuilder();
        for (final JsonObjectBuilder session : sessions) {
            courtSchedules.add(session);
        }
        final JsonObject body = createObjectBuilder().add("courtSchedules", courtSchedules).build();
        when(courtScheduleService.getCourtSchedulesById(ids)).thenReturn(Response.ok(body).build());
    }

    private static JsonObjectBuilder session(final String courtScheduleId) {
        return createObjectBuilder()
                .add("courtScheduleId", courtScheduleId)
                .add("draft", true)
                .add("sessionDate", "2026-11-16");
    }
}
