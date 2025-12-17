package uk.gov.moj.cpp.progression.processor.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.processor.utils.RetryHelper.retryHelper;

import uk.gov.moj.cpp.progression.exception.LaaAzureApimInvocationException;
import uk.gov.moj.cpp.progression.processor.utils.RetryHelper;

import java.util.ArrayList;
import java.util.UUID;
import java.util.function.IntSupplier;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)

public class RetryHelperTest {

    @Mock
    IntSupplier supplier;

    @BeforeEach
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

    @Test
    public void shouldThrowExceptionAfterExceedingRetryCount() {

        when(supplier.getAsInt()).thenReturn(500);

        assertThrows(LaaAzureApimInvocationException.class, () -> retryHelper()
                .withSupplier(() -> supplier.getAsInt())
                .withRetryTimes(3)
                .withRetryInterval(200)
                .withExceptionSupplier(() -> new LaaAzureApimInvocationException(new ArrayList<>(), UUID.randomUUID().toString(),"url"))
                .withPredicate(statusCode -> statusCode > 429)
                .build()
                .postWithRetry());
        ;
        verify(supplier, times(3)).getAsInt();
    }

}
