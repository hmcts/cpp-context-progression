package uk.gov.moj.cpp.progression.service;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.Charset.defaultCharset;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import java.io.IOException;

import javax.ws.rs.core.Response;

import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CpsRestNotificationServiceTest {

    private static final String COURT_DOCUMENT_REST_API_URL = "https://spnl-apim-int-gw.cpp.nonlive/probation/api/v1/hearing/details";

    @Mock
    private RestEasyClientService restEasyClientService;

    @Mock
    private Response response;

    @InjectMocks
    private CpsRestNotificationService cpsRestNotificationService;

    @Before
    public void setUp() {
        setField(cpsRestNotificationService, "cpsPayloadTransformAndSendUrl", COURT_DOCUMENT_REST_API_URL);
    }

    @Test
    public void shouldProcessHearingPopulatedToProbationCaseworker() throws IOException {

        when(restEasyClientService.post(eq(COURT_DOCUMENT_REST_API_URL), any(), any())).thenReturn(response);
        when(response.getStatus()).thenReturn(200);

        final String payloadAsString = Resources.toString(getResource("CpsRestNotification.json"), defaultCharset());
        cpsRestNotificationService.sendMaterial(payloadAsString);

        verify(restEasyClientService).post(eq(COURT_DOCUMENT_REST_API_URL), any(), any());
    }
}
