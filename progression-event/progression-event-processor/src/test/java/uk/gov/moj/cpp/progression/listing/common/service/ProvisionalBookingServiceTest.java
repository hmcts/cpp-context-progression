package uk.gov.moj.cpp.progression.listing.common.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvisionalBookingServiceTest {

    @Mock
    private SystemUserProvider systemUserProvider;

    private final ProvisionalBookingService service = new ProvisionalBookingService();
    private HttpServer server;
    private final CountDownLatch unblock = new CountDownLatch(1);

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        setField("baseUri", "http://127.0.0.1:" + server.getAddress().getPort());
        setField("systemUserProvider", systemUserProvider);
        setField("stringToJsonObjectConverter", new StringToJsonObjectConverter());
        setField("courtSchedulerRequestConfig", new CourtSchedulerRequestConfig(5000, 10000));
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(randomUUID()));
    }

    @AfterEach
    void tearDown() {
        unblock.countDown();
        server.stop(0);
    }

    @Test
    void shouldReturnTheSlotsCourtschedulerAnswers() {
        serve(exchange -> {
            final byte[] body = "{\"provisionalSlots\":[]}".getBytes(UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        final Response response = service.getSlots(Map.of("bookingIds", randomUUID().toString()));

        assertThat(response.getStatus(), is(200));
        assertThat(((JsonObject) response.getEntity()).getJsonArray("provisionalSlots").isEmpty(), is(true));
    }

    @Test
    void shouldGiveUpOnACourtschedulerThatNeverAnswers() throws Exception {
        serve(exchange -> {
            try {
                unblock.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.close();
        });
        setField("courtSchedulerRequestConfig", new CourtSchedulerRequestConfig(5000, 300));

        final Response response = assertTimeoutPreemptively(Duration.ofSeconds(5),
                () -> service.getSlots(Map.of("bookingIds", randomUUID().toString())));

        assertThat(response.getStatus(), is(500));
    }

    private void serve(final HttpHandler handler) {
        server.createContext("/unconfirmedBooking", handler);
        server.start();
    }

    private void setField(final String name, final Object value) throws Exception {
        final java.lang.reflect.Field field = ProvisionalBookingService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
