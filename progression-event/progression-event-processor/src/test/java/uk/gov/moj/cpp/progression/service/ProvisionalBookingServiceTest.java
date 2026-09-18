package uk.gov.moj.cpp.progression.service;

import static java.util.Map.of;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;
import uk.gov.moj.cpp.progression.exception.DataValidationException;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import jakarta.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ProvisionalBookingService replaced the one progression used to get from listing-common, on the
 * promise that it calls the court scheduler the same way. Nothing in the compiler checks that, so
 * these pin the request: the resource path, the accept media type, the CJSCPPUID header and the
 * query parameters, plus what each response status turns into.
 *
 * The HTTP client is substituted through the package-private httpClient() seam rather than by
 * standing up a server, because the subject is the request that gets built and the response that
 * gets mapped.
 */
@ExtendWith(MockitoExtension.class)
public class ProvisionalBookingServiceTest {

    private static final String BASE_URI = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler";
    private static final UUID SYSTEM_USER_ID = UUID.fromString("2f9d4b17-5c8e-4a31-9f60-7b3a1d5e8c42");

    @Mock
    private SystemUserProvider systemUserProvider;

    @Mock
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse httpResponse;

    @Mock
    private org.apache.http.StatusLine statusLine;

    private ProvisionalBookingService provisionalBookingService;

    @BeforeEach
    void createServiceWithAStubbedHttpClient() {
        provisionalBookingService = new ProvisionalBookingService() {
            @Override
            HttpClient httpClient() {
                return httpClient;
            }
        };
        setField(provisionalBookingService, "baseUri", BASE_URI);
        setField(provisionalBookingService, "systemUserProvider", systemUserProvider);
        setField(provisionalBookingService, "stringToJsonObjectConverter", stringToJsonObjectConverter);
    }

    @Test
    public void shouldRejectNullParamsRatherThanCallTheCourtScheduler() {
        assertThrows(DataValidationException.class, () -> provisionalBookingService.getSlots(null));
    }

    @Test
    public void shouldFailWhenThereIsNoSystemUserToIdentifyTheCaller() {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(empty());

        assertThrows(IllegalStateException.class,
                () -> provisionalBookingService.getSlots(of("courtCentreId", randomUUID().toString())));
    }

    @Test
    public void shouldCallTheProvisionalBookingResourceWithTheParamsAndTheSystemUser() throws Exception {
        givenTheCourtSchedulerResponds(200, "{\"slots\":[]}");

        provisionalBookingService.getSlots(of("courtRoomId", "room-1"));

        final ArgumentCaptor<HttpUriRequest> request = ArgumentCaptor.forClass(HttpUriRequest.class);
        org.mockito.Mockito.verify(httpClient).execute(request.capture());
        final HttpGet issued = (HttpGet) request.getValue();

        assertThat(issued.getURI().toString(), containsString("/provisionalBooking"));
        assertThat(issued.getURI().toString(), containsString("courtRoomId=room-1"));
        assertThat(issued.getFirstHeader("Accept").getValue(),
                is("application/vnd.courtscheduler.get.provisional.booking+json"));
        assertThat(issued.getFirstHeader("CJSCPPUID").getValue(), is(SYSTEM_USER_ID.toString()));
    }

    @Test
    public void shouldReturnOkWithTheConvertedPayloadWhenTheCourtSchedulerAnswers() throws Exception {
        givenTheCourtSchedulerResponds(200, "{\"slots\":[]}");

        final Response response = provisionalBookingService.getSlots(of("courtCentreId", "abc"));

        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void shouldReturnServerErrorWhenTheCourtSchedulerRejectsTheRequest() throws Exception {
        givenTheCourtSchedulerResponds(404, "not found");

        final Response response = provisionalBookingService.getSlots(of("courtCentreId", "abc"));

        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void shouldReturnServerErrorRatherThanPropagateATransportFailure() throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
        when(httpClient.execute(any(HttpUriRequest.class))).thenThrow(new IOException("connection refused"));

        final Response response = provisionalBookingService.getSlots(of("courtCentreId", "abc"));

        assertThat(response.getStatus(), is(500));
        assertThat(response.getEntity().toString(), containsString("connection refused"));
    }

    private void givenTheCourtSchedulerResponds(final int statusCode, final String body) throws Exception {
        when(systemUserProvider.getContextSystemUserId()).thenReturn(Optional.of(SYSTEM_USER_ID));
        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(httpClient.execute(any(HttpUriRequest.class))).thenReturn(httpResponse);
        if (statusCode == 200) {
            when(httpResponse.getEntity()).thenReturn(new org.apache.http.entity.StringEntity(body));
        }
    }
}
