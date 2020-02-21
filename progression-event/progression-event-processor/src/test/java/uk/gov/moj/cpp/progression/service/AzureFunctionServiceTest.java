package uk.gov.moj.cpp.progression.service;

import org.apache.http.HttpStatus;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.moj.cpp.progression.helper.HttpConnectionHelper;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class AzureFunctionServiceTest {
    private final String PAYLOAD = "dummy payload";
    @Mock
    HttpConnectionHelper httpConnectionHelper;

    @Mock
    private ApplicationParameters applicationParameters;

    @Before
    public void initMocks() {
        Mockito.when(applicationParameters.getAzureFunctionHostName()).thenReturn("hostname");
        Mockito.when(applicationParameters.getRelayCaseOnCppFunctionPath()).thenReturn("RelayCaseOnCppFunctionPath");
        Mockito.when(applicationParameters.getSetCaseEjectedFunctionPath()).thenReturn("SetCaseEjectedFunctionPath");

    }

    @Test
    public void makeFunctionCall() throws IOException {
        Mockito.when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenReturn(HttpStatus.SC_ACCEPTED);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters);
        Integer response =azureFunctionService.makeFunctionCall(PAYLOAD);
        assertThat(response.intValue() , is(HttpStatus.SC_ACCEPTED));
    }

    @Test(expected = IOException.class)
    public void makeFunctionCallFailed() throws IOException {

        Mockito.when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenThrow(IOException.class);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters);
        Integer response =azureFunctionService.makeFunctionCall(PAYLOAD);
        assertThat(response.intValue() , is(HttpStatus.SC_ACCEPTED));
    }

    @Test
    public void relayCaseOnCPP() throws IOException {
        Mockito.when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenReturn(HttpStatus.SC_ACCEPTED);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters);
        Integer response =azureFunctionService.relayCaseOnCPP(PAYLOAD);
    }

    @Test(expected = IOException.class)
    public void relayCaseOnCPPFailed() throws IOException {

        Mockito.when(httpConnectionHelper.getResponseCode(Mockito.anyString(), Mockito.anyString())).thenThrow(IOException.class);
        AzureFunctionService azureFunctionService = new AzureFunctionService(httpConnectionHelper, applicationParameters);
        Integer response =azureFunctionService.relayCaseOnCPP(PAYLOAD);
    }
}