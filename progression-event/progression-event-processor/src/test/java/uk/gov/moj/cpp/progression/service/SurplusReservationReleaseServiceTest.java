package uk.gov.moj.cpp.progression.service;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonString;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The release is best effort by design: it gives a slot back that nothing will ever use, so a
 * failure must leave the share alone and let the overnight purge recover the slot. Most of these
 * tests are therefore about NOT throwing.
 */
@ExtendWith(MockitoExtension.class)
class SurplusReservationReleaseServiceTest {

    private static final String BASE_URI = "http://courtscheduler/listingcourtscheduler-api/rest/courtscheduler";

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private StatusLine statusLine;

    private SurplusReservationReleaseService service;

    @BeforeEach
    void setUp() throws Exception {
        service = spy(new SurplusReservationReleaseService());
        setField(service, "baseUri", BASE_URI);
        setField(service, "systemUserProvider", systemUserProvider);
    }

    /**
     * The batch release, not DELETE /sessions/{id}: that one also releases by hearing_id, where a
     * confirmed listing lives. This one gives back only holds no share has confirmed, which is
     * exactly what a surplus reservation is.
     */
    @Test
    void shouldReleaseEverySurplusBookingInOneBatchCall() throws Exception {
        final UUID bookingA = randomUUID();
        final UUID bookingB = randomUUID();
        final UUID systemUserId = randomUUID();
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(systemUserId));
        givenResponseStatus(202);

        service.release(new LinkedHashSet<>(List.of(bookingA, bookingB)));

        final ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        verify(service).execute(captor.capture());

        final HttpPost sent = captor.getValue();
        assertThat(sent.getURI().toString(), is(BASE_URI + "/unconfirmedBooking/release"));
        assertThat(sent.getFirstHeader("Content-Type").getValue(),
                is("application/vnd.courtscheduler.release.unconfirmed.bookings+json"));
        assertThat(sent.getFirstHeader("CJSCPPUID").getValue(), is(systemUserId.toString()));

        final JsonObject body = new StringToJsonObjectConverter().convert(EntityUtils.toString(sent.getEntity()));
        assertThat(body.getJsonArray("bookingIds").getValuesAs(JsonString.class).stream()
                        .map(JsonString::getString).collect(toList()),
                containsInAnyOrder(bookingA.toString(), bookingB.toString()));
        assertThat(body.size(), is(1));
    }

    @Test
    void shouldNotCallCourtschedulerWithNothingToRelease() throws Exception {
        service.release(Set.of());

        verify(service, never()).execute(any(HttpPost.class));
    }

    /**
     * A non-202 is logged, not thrown. The slot stays reserved until the purge, which is exactly
     * where we were before this call existed - the share must not fail over it.
     */
    @Test
    void shouldNotThrowWhenCourtschedulerRejectsTheRelease() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(randomUUID()));
        givenResponseStatus(500);

        service.release(Set.of(randomUUID()));

        verify(service).execute(any(HttpPost.class));
    }

    @Test
    void shouldNotThrowWhenTheCallItselfFails() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(of(randomUUID()));
        doThrow(new IOException("courtscheduler unreachable")).when(service).execute(any(HttpPost.class));

        assertDoesNotThrow(() -> service.release(Set.of(randomUUID())));

        verify(service).execute(any(HttpPost.class));
    }

    /**
     * No system user means no call at all - the request would be rejected anyway, and throwing
     * here would take the share down with it.
     */
    @Test
    void shouldNotCallCourtschedulerWhenThereIsNoSystemUser() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(empty());

        service.release(Set.of(randomUUID()));

        verify(service, never()).execute(any(HttpPost.class));
    }

    private void givenResponseStatus(final int status) throws IOException {
        when(statusLine.getStatusCode()).thenReturn(status);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        doReturn(httpResponse).when(service).execute(any(HttpPost.class));
    }

    /** The spy is a subclass, so the declared field lives on SurplusReservationReleaseService. */
    private static void setField(final Object target, final String name, final Object value) throws Exception {
        final java.lang.reflect.Field field =
                SurplusReservationReleaseService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
