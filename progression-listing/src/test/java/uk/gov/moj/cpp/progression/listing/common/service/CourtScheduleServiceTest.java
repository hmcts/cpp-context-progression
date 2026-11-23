package uk.gov.moj.cpp.progression.listing.common.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class CourtScheduleServiceTest {

    private static final String FIRST_ID = "ccb4af70-e150-479a-954c-1c6ae98d0a34";
    private static final String SECOND_ID = "5b0d3a2e-8f61-4c7b-9e14-2a6f0c9d7e31";
    private static final String COURT_SCHEDULES = "{\"courtSchedules\":[{\"courtScheduleId\":\"" + FIRST_ID + "\",\"isWeekCommencing\":true,\"sessionDate\":\"2026-11-16\"}]}";

    private final SystemUserProvider systemUserProvider = mock(SystemUserProvider.class);
    private final AtomicReference<HttpExchange> received = new AtomicReference<>();
    private final UUID systemUserId = randomUUID();

    private HttpServer courtScheduler;
    private CourtScheduleService service;

    @BeforeEach
    void setUp() throws IOException {
        courtScheduler = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        courtScheduler.start();
        service = new CourtScheduleService();
        service.systemUserProvider = systemUserProvider;
        service.stringToJsonObjectConverter = new StringToJsonObjectConverter();
        setField(service, "baseUri", "http://localhost:" + courtScheduler.getAddress().getPort() + "/courtscheduler");
    }

    @AfterEach
    void tearDown() {
        courtScheduler.stop(0);
    }

    @Test
    void shouldGetTheSessionsByIdAsTheSystemUserAndReturnTheCourtSchedules() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        givenCourtSchedulerResponds(200, COURT_SCHEDULES);

        final Response response = service.getCourtSchedulesById(List.of(FIRST_ID, SECOND_ID));

        assertThat(received.get().getRequestMethod(), is("GET"));
        assertThat(received.get().getRequestURI().getPath(), is("/courtscheduler/sessions"));
        assertThat(received.get().getRequestURI().getQuery(), is("ids=" + FIRST_ID + "," + SECOND_ID));
        assertThat(received.get().getRequestHeaders().getFirst("Accept"), is("application/vnd.courtscheduler.search.court-schedules-by-id+json"));
        assertThat(received.get().getRequestHeaders().getFirst("CJSCPPUID"), is(systemUserId.toString()));
        assertThat(response.getStatus(), is(200));
        final JsonObject courtSchedule = ((JsonObject) response.getEntity()).getJsonArray("courtSchedules").getJsonObject(0);
        assertThat(courtSchedule.getString("courtScheduleId"), is(FIRST_ID));
        assertThat(courtSchedule.getBoolean("isWeekCommencing"), is(true));
        assertThat(courtSchedule.getString("sessionDate"), is("2026-11-16"));
    }

    @Test
    void shouldReturnServerErrorWithTheBodyWhenCourtSchedulerDoesNotReturnOk() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        givenCourtSchedulerResponds(403, "forbidden");

        final Response response = service.getCourtSchedulesById(List.of(FIRST_ID));

        assertThat(response.getStatus(), is(500));
        assertThat(response.getEntity(), is("forbidden"));
    }

    @Test
    void shouldReturnServerErrorWhenCourtSchedulerCannotBeReached() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        courtScheduler.stop(0);

        final Response response = service.getCourtSchedulesById(List.of(FIRST_ID));

        assertThat(response.getStatus(), is(500));
    }

    @Test
    @Timeout(10)
    void shouldReturnServerErrorWhenCourtSchedulerDoesNotAnswerWithinTheTimeout() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        setField(service, "socketTimeoutMs", "200");
        courtScheduler.createContext("/courtscheduler/sessions", received::set);

        final Response response = service.getCourtSchedulesById(List.of(FIRST_ID));

        assertThat(response.getStatus(), is(500));
    }

    @Test
    void shouldFailWhenThereIsNoSystemUser() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(empty());

        assertThrows(IllegalStateException.class, () -> service.getCourtSchedulesById(List.of(FIRST_ID)));
    }

    private void givenCourtSchedulerResponds(final int status, final String body) {
        courtScheduler.createContext("/courtscheduler/sessions", exchange -> {
            received.set(exchange);
            final byte[] bytes = body.getBytes(UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
}
