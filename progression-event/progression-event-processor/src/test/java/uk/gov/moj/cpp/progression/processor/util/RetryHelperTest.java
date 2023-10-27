package uk.gov.moj.cpp.progression.processor.util;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.processor.utils.RetryHelper.retryHelper;

import uk.gov.moj.cpp.progression.exception.LaaAzureApimInvocationException;
import uk.gov.moj.cpp.progression.processor.utils.RetryHelper;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.IntSupplier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)

public class RetryHelperTest {

    @Mock
    IntSupplier supplier;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldInvokeSupplierMethodOnlyOnce() throws Exception{

        when(supplier.getAsInt()).thenReturn(420);

        RetryHelper.Builder builder = retryHelper()
                .withSupplier(() -> supplier.getAsInt())
                .withRetryTimes(3)
                .withRetryInterval(200)
                .withPredicate(statusCode -> statusCode > 429);

        RetryHelper retryHelper = builder.build();
        retryHelper.postWithRetry();

        verify(supplier).getAsInt();
    }

    @Test(expected=LaaAzureApimInvocationException.class)
    public void shouldThrowExceptionAfterExceedingRetryCount() throws Exception{

        retryHelper()
                .withSupplier(() -> 500)
                .withRetryTimes(3)
                .withRetryInterval(200)
                .withExceptionSupplier(() -> new LaaAzureApimInvocationException(new ArrayList<>(), UUID.randomUUID().toString(),"url"))
                .withPredicate(statusCode -> statusCode > 429)
                .build()
                .postWithRetry();
        verify(supplier, times(3)).getAsInt();
    }

}
