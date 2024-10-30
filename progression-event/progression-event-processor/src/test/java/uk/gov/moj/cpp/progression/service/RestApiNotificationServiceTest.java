package uk.gov.moj.cpp.progression.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.io.IOException;

import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RestApiNotificationServiceTest {

    private static final String API_NOTIFICATION_REST_API_URL = "https://spnl-apim-int-gw.cpp.nonlive/apiNotification/v1";

    @Mock
    private RestEasyClientService restEasyClientService;

    @Mock
    private Response response;

    @InjectMocks
    private RestApiNotificationService restApiNotificationService;

    @BeforeEach
    public void setUp() {
        setField(restApiNotificationService, "apiNotificationUrl", API_NOTIFICATION_REST_API_URL);
    }

    @Test
    public void shouldSendApiNotification() throws IOException {

        when(restEasyClientService.post(eq(API_NOTIFICATION_REST_API_URL), any(), any())).thenReturn(response);
        when(response.getStatus()).thenReturn(200);

        final String payloadAsString = Resources.toString(getResource("CpsRestNotification.json"), defaultCharset());
        restApiNotificationService.sendApiNotification(payloadAsString);

        verify(restEasyClientService).post(eq(API_NOTIFICATION_REST_API_URL), any(), any());
    }
}