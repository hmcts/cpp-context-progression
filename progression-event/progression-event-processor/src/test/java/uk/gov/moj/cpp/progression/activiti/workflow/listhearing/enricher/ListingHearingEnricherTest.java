package uk.gov.moj.cpp.progression.activiti.workflow.listhearing.enricher;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.PLEA;
import static uk.gov.moj.cpp.progression.activiti.common.ProcessMapConstant.SEND_CASE_FOR_LISTING_PAYLOAD;

import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.external.domain.listing.Defendant;
import uk.gov.moj.cpp.external.domain.listing.Hearing;
import uk.gov.moj.cpp.external.domain.listing.ListingCase;
import uk.gov.moj.cpp.external.domain.listing.Offence;
import uk.gov.moj.cpp.external.domain.listing.StatementOfOffence;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ListingHearingEnricherTest {

    @InjectMocks
    private ListingHearingEnricher testObj;

    @Mock
    private DelegateExecution delegateExecution;

    final ArgumentCaptor<ListingCase> captor = ArgumentCaptor.forClass(ListingCase.class);

    @Test
    public void shouldEnrichHearingType_PTP_and_EstimateMinutes_15() throws Exception {
        //Given
        final List<Hearing> hearings = Arrays.asList(new Hearing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Plea & Trial Preparation",
                new StringGenerator().next(),
                15,
                Arrays.asList(new Defendant(UUID.randomUUID(),
                        UUID.randomUUID(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        Arrays.asList(new Offence(UUID.randomUUID(),
                                new StringGenerator().next(),
                                new StringGenerator().next(),
                                new StringGenerator().next(),
                                new StatementOfOffence(new StringGenerator().next(), new StringGenerator().next())))
                ))
                , null,
                null,
                null
        ));
        final ListingCase listingCase = new ListingCase(UUID.randomUUID(), new StringGenerator().next(), hearings);

        when(delegateExecution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD)).thenReturn(listingCase);
        when(delegateExecution.getVariable(PLEA)).thenReturn(Arrays.asList());

        //when
        testObj.execute(delegateExecution);

        //then
        verify(delegateExecution).setVariable(any(), captor.capture());
        final ListingCase result = captor.getAllValues().get(0);
        assertThat(listingCase.equals(result), equalTo(true));

    }

    @Test
    public void shouldEnrichHearingType_SENTENCE_and_EstimateMinutes_30() throws Exception {
        //Given
        final List<Hearing> hearings = Arrays.asList(new Hearing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sentence",
                new StringGenerator().next(),
                30,
                Arrays.asList(new Defendant(UUID.randomUUID(),
                        UUID.randomUUID(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        new StringGenerator().next(),
                        Arrays.asList(new Offence(UUID.randomUUID(),
                                new StringGenerator().next(),
                                new StringGenerator().next(),
                                new StringGenerator().next(),
                                new StatementOfOffence(new StringGenerator().next(), new StringGenerator().next())))
                ))
                , null,
                null,
                null
        ));
        final ListingCase listingCase = new ListingCase(UUID.randomUUID(), new StringGenerator().next(), hearings);

        when(delegateExecution.getVariable(SEND_CASE_FOR_LISTING_PAYLOAD)).thenReturn(listingCase);
        when(delegateExecution.getVariable(PLEA)).thenReturn(Arrays.asList("GUILTY"));

        //when
        testObj.execute(delegateExecution);

        //then
        verify(delegateExecution).setVariable(any(), captor.capture());
        final ListingCase result = captor.getValue();
        assertThat(listingCase.equals(result), equalTo(true));

    }
}
