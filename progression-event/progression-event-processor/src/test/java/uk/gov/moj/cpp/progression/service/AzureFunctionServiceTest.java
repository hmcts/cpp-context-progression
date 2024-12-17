package uk.gov.moj.cpp.progression.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.platform.data.utils.rest.service.RestClientService;
import uk.gov.moj.cpp.progression.helper.HttpConnectionHelper;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AzureFunctionServiceTest {
    private final String PAYLOAD = "dummy payload";
    @Mock
    HttpConnectionHelper httpConnectionHelper;

    @Mock
    private RestClientService restClientService;

    @Mock
    private ApplicationParameters applicationParameters;


    @Test
    public void makeFunctionCall() throws IOException {

        when(applicationParameters.getAzureFunctionHostName()).thenReturn("hostname");
        when(applicationParameters.getSetCaseEjectedFunctionPath()).thenReturn("SetCaseEjectedFunctionPath");
        when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenReturn(HttpStatus.SC_ACCEPTED);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters, restClientService);
        Integer response = azureFunctionService.makeFunctionCall(PAYLOAD);
        assertThat(response.intValue(), is(HttpStatus.SC_ACCEPTED));
    }

    @Test
    public void makeFunctionCallFailed() throws IOException {

        when(applicationParameters.getAzureFunctionHostName()).thenReturn("hostname");
        when(applicationParameters.getSetCaseEjectedFunctionPath()).thenReturn("SetCaseEjectedFunctionPath");
        when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenThrow(IOException.class);

        assertThrows(IOException.class, () -> {
            AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters, restClientService);
            Integer response = azureFunctionService.makeFunctionCall(PAYLOAD);
            assertThat(response.intValue(), is(HttpStatus.SC_ACCEPTED));
        });
    }

    @Test
    public void relayCaseOnCPP() throws IOException {

        when(applicationParameters.getAzureFunctionHostName()).thenReturn("hostname");
        when(applicationParameters.getRelayCaseOnCppFunctionPath()).thenReturn("RelayCaseOnCppFunctionPath");
        when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenReturn(HttpStatus.SC_ACCEPTED);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters, restClientService);
        azureFunctionService.relayCaseOnCPP(PAYLOAD);
    }

    @Test
    public void relayCaseOnCPPFailed() throws IOException {

        when(applicationParameters.getAzureFunctionHostName()).thenReturn("hostname");
        when(applicationParameters.getRelayCaseOnCppFunctionPath()).thenReturn("RelayCaseOnCppFunctionPath");
        when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenThrow(IOException.class);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters, restClientService);

        assertThrows(IOException.class, () -> azureFunctionService.relayCaseOnCPP(PAYLOAD));
    }

    @Test
    public void concludeDefendantProceeding() {

        when(applicationParameters.getDefendantProceedingsConcludedApimUrl()).thenReturn("DefendantProceedingsConcludedApimUrl");
        when(applicationParameters.getSubscriptionKey()).thenReturn("SubscriptionKey");
        when(restClientService.post(Mockito.anyString(), Mockito.anyMap(), Mockito.anyString())).thenReturn(Response.status(201).build());
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters, restClientService);
        Integer response = azureFunctionService.concludeDefendantProceeding(PAYLOAD);
        assertThat(response.intValue(), is(HttpStatus.SC_CREATED));
    }

    @Test
    public void concludeDefendantProceedingFailed() {
        when(applicationParameters.getDefendantProceedingsConcludedApimUrl()).thenReturn("DefendantProceedingsConcludedApimUrl");
        when(applicationParameters.getSubscriptionKey()).thenReturn("SubscriptionKey");
        when(restClientService.post(any(), any(), any())).thenAnswer( invocation -> { throw new IOException(); });
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters, restClientService);
        assertThrows(IOException.class, () -> azureFunctionService.concludeDefendantProceeding(PAYLOAD));
    }
}